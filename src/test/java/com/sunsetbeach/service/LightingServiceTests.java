package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.lighting.WledClient;
import com.sunsetbeach.lighting.WledClient.WledInfo;
import com.sunsetbeach.lighting.WledClient.WledPreset;
import com.sunsetbeach.lighting.WledClient.WledState;
import com.sunsetbeach.lighting.WledClient.WledStateChange;
import com.sunsetbeach.lighting.WledException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.LightingDevice;
import com.sunsetbeach.model.LightingDeviceCheckInput;
import com.sunsetbeach.model.LightingDeviceCheckResult;
import com.sunsetbeach.model.LightingDeviceConfig;
import com.sunsetbeach.model.LightingDeviceCreateInput;
import com.sunsetbeach.model.LightingDeviceUpdateInput;
import com.sunsetbeach.model.LightingStateInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.LightingDeviceRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The lighting module against a real database with the controllers faked at the {@link WledClient}
 * boundary (the HTTP layer and the address guard have their own tests): the parallel read and what a
 * dead controller does to the list, the cache, that every command is audited with its outcome and a
 * failure names the controller, and the settings rules (names unique ignoring case and spaces, an
 * address that is not private LAN refused). Not {@code @Transactional}: the audit log commits
 * independently, so everything created here is removed in {@code @AfterEach}.
 */
// Its own context (the WledClient mock), so a small pool: every context holds its pool open for the whole suite.
@SpringBootTest(properties = {"app.lighting.confirm-delay-ms=0", "spring.datasource.hikari.maximum-pool-size=3"})
class LightingServiceTests extends AbstractIntegrationTest {

    @Autowired private LightingService lightingService;
    @Autowired private LightingSettingsService settingsService;
    @Autowired private LightingDeviceRepository deviceRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;
    @MockitoBean private WledClient wled;

    private String userId;

    @BeforeEach
    void setUp() {
        reset(wled);
        lightingService.clearStateCache();
        deviceRepository.deleteAll();
        UserEntity user = new UserEntity();
        user.setEmail("lighting-test-" + UUID.randomUUID() + "@test.local");
        user.setName(user.getEmail());
        user.setFullName("Nok Manager");
        user.setPasswordHash("unused");
        user.setRole(Role.MANAGER);
        user.setActive(true);
        user.setTokenVersion(0);
        userId = userRepository.saveAndFlush(user).getId();
        StaffPrincipal principal = new StaffPrincipal(userId, user.getEmail(), Role.MANAGER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        auditLogRepository.deleteAll(lightingAuditEntries());
        deviceRepository.deleteAll();
        userRepository.deleteById(userId);
    }

    // ---- reading -------------------------------------------------------------------------------

    @Test
    void list_readsAllControllersAtTheSameTime_andADeadOneDoesNotTakeTheRestDown() {
        LightingDeviceConfig a = add("Pool bar", "10.77.0.11");
        LightingDeviceConfig b = add("Lobby", "10.77.0.12");
        LightingDeviceConfig dead = add("Roof", "10.77.0.13");
        // Each read waits until all three have started: this can only pass when they run concurrently.
        CountDownLatch allStarted = new CountDownLatch(3);
        AtomicBoolean concurrent = new AtomicBoolean(true);
        when(wled.state(anyString(), anyInt())).thenAnswer(inv -> {
            allStarted.countDown();
            if (!allStarted.await(5, TimeUnit.SECONDS)) {
                concurrent.set(false);
            }
            if ("10.77.0.13".equals(inv.getArgument(0))) {
                throw new WledException(WledException.Kind.UNREACHABLE, "did not answer in time");
            }
            return new WledState(true, 128, 2);
        });
        when(wled.presets(anyString(), anyInt())).thenReturn(List.of(new WledPreset(1, "Warm"), new WledPreset(2, "Sunset")));

        List<LightingDevice> devices = lightingService.list(true).getDevices();

        assertThat(concurrent).isTrue();
        assertThat(devices).extracting(LightingDevice::getName).containsExactly("Pool bar", "Lobby", "Roof");
        LightingDevice pool = only(devices, a.getId());
        assertThat(pool.getOnline()).isTrue();
        assertThat(pool.getIsOn()).isTrue();
        assertThat(pool.getBrightness()).isEqualTo(128);
        assertThat(pool.getPresetId().get()).isEqualTo(2);
        assertThat(pool.getPresets()).extracting("name").containsExactly("Warm", "Sunset");
        assertThat(pool.getStateReadAt()).isNotNull();
        assertThat(only(devices, b.getId()).getOnline()).isTrue();
        LightingDevice roof = only(devices, dead.getId());
        assertThat(roof.getOnline()).isFalse();
        assertThat(roof.getStateError()).isEqualTo("Controller \"Roof\" did not answer in time");
        assertThat(roof.getIsOn()).isNull();
        assertThat(roof.getPresets()).isEmpty();
    }

    @Test
    void list_isCached_untilRefreshIsAskedFor_andAFailedReadIsNotCached() {
        LightingDeviceConfig a = add("Pool bar", "10.77.0.11");
        when(wled.state("10.77.0.11", 80)).thenReturn(new WledState(true, 128, null));
        when(wled.presets(anyString(), anyInt())).thenReturn(List.of());

        lightingService.list(false);
        lightingService.list(false);
        verify(wled, times(1)).state("10.77.0.11", 80);

        when(wled.state("10.77.0.11", 80)).thenReturn(new WledState(false, 90, null));
        LightingDevice refreshed = only(lightingService.list(true).getDevices(), a.getId());
        verify(wled, times(2)).state("10.77.0.11", 80);
        assertThat(refreshed.getIsOn()).isFalse();
        assertThat(only(lightingService.list(false).getDevices(), a.getId()).getIsOn()).isFalse();
        verify(wled, times(2)).state("10.77.0.11", 80);

        when(wled.state("10.77.0.11", 80)).thenThrow(new WledException(WledException.Kind.UNREACHABLE, "could not be reached"));
        assertThat(only(lightingService.list(true).getDevices(), a.getId()).getOnline()).isFalse();
        // the failure was not stored: a plain list asks the controller again instead of serving the old state
        org.mockito.Mockito.doReturn(new WledState(true, 10, null)).when(wled).state("10.77.0.11", 80);
        assertThat(only(lightingService.list(false).getDevices(), a.getId()).getOnline()).isTrue();
    }

    @Test
    void list_skipsDisabledControllers_andNeverContactsThem() {
        add("Pool bar", "10.77.0.11");
        LightingDeviceConfig off = add("Spare", "10.77.0.12");
        settingsService.update(off.getId(), new LightingDeviceUpdateInput().active(false));
        when(wled.state(anyString(), anyInt())).thenReturn(new WledState(true, 1, null));
        when(wled.presets(anyString(), anyInt())).thenReturn(List.of());

        assertThat(lightingService.list(true).getDevices()).extracting(LightingDevice::getName).containsExactly("Pool bar");
        verify(wled, never()).state(eq("10.77.0.12"), anyInt());
    }

    @Test
    void presetsThatCannotBeRead_doNotMakeTheControllerOffline() {
        LightingDeviceConfig a = add("Pool bar", "10.77.0.11");
        when(wled.state(anyString(), anyInt())).thenReturn(new WledState(true, 5, null));
        when(wled.presets(anyString(), anyInt())).thenThrow(new WledException(WledException.Kind.BAD_RESPONSE, "answered with an error (HTTP 404)"));

        LightingDevice device = only(lightingService.list(true).getDevices(), a.getId());

        assertThat(device.getOnline()).isTrue();
        assertThat(device.getPresets()).isEmpty();
    }

    // ---- commands ------------------------------------------------------------------------------

    @Test
    void command_sendsOnlyWhatWasAsked_returnsWhatTheControllerReports_andIsAudited() {
        LightingDeviceConfig a = add("Pool bar", "10.77.0.11", 8080);
        when(wled.state("10.77.0.11", 8080)).thenReturn(new WledState(true, 200, 3));
        when(wled.presets(anyString(), anyInt())).thenReturn(List.of(new WledPreset(3, "Party")));

        LightingDevice result = lightingService.setState(a.getId(), new LightingStateInput().isOn(true).brightness(200).presetId(3));

        verify(wled).setState("10.77.0.11", 8080, new WledStateChange(true, 200, 3));
        assertThat(result.getIsOn()).isTrue();
        assertThat(result.getBrightness()).isEqualTo(200);
        assertThat(result.getPresetId().get()).isEqualTo(3);
        assertThat(result.getPresets()).hasSize(1);
        List<AuditLogEntity> entries = commandEntries(a.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getActorUserId()).isEqualTo(userId);
        assertThat(entries.get(0).getSummary()).isEqualTo("Set \"Pool bar\": on, brightness 200, preset 3");
    }

    @Test
    void command_justOff_sendsOnlyOff() {
        LightingDeviceConfig a = add("Pool bar", "10.77.0.11");
        when(wled.state(anyString(), anyInt())).thenReturn(new WledState(false, 200, null));
        when(wled.presets(anyString(), anyInt())).thenReturn(List.of());

        LightingDevice result = lightingService.setState(a.getId(), new LightingStateInput().isOn(false));

        verify(wled).setState("10.77.0.11", 80, new WledStateChange(false, null, null));
        assertThat(result.getIsOn()).isFalse();
    }

    @Test
    void command_toAnUnreachableController_namesIt_isAuditedAsAFailure_andLeavesNoStaleState() {
        LightingDeviceConfig a = add("Pool bar", "10.77.0.11");
        when(wled.state(anyString(), anyInt())).thenReturn(new WledState(true, 100, null));
        when(wled.presets(anyString(), anyInt())).thenReturn(List.of());
        lightingService.list(true); // fills the cache
        org.mockito.Mockito.doThrow(new WledException(WledException.Kind.UNREACHABLE, "did not answer in time"))
                .when(wled).setState(anyString(), anyInt(), any());

        assertThatThrownBy(() -> lightingService.setState(a.getId(), new LightingStateInput().isOn(false)))
                .isInstanceOfSatisfying(WledException.class, e -> assertThat(e.getMessage()).isEqualTo("Controller \"Pool bar\" did not answer in time"));

        List<AuditLogEntity> entries = commandEntries(a.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).isEqualTo("Could not set \"Pool bar\" (off): Controller \"Pool bar\" did not answer in time");
    }

    @Test
    void command_whoseConfirmingReadFails_isStillAnAnswer_notAnError() {
        LightingDeviceConfig a = add("Pool bar", "10.77.0.11");
        when(wled.state(anyString(), anyInt())).thenThrow(new WledException(WledException.Kind.UNREACHABLE, "did not answer in time"));
        when(wled.presets(anyString(), anyInt())).thenReturn(List.of());

        LightingDevice result = lightingService.setState(a.getId(), new LightingStateInput().isOn(true));

        verify(wled).setState("10.77.0.11", 80, new WledStateChange(true, null, null));
        assertThat(result.getStateError()).startsWith("The command was sent, but the state could not be confirmed");
        assertThat(result.getIsOn()).isNull();
        assertThat(commandEntries(a.getId())).hasSize(1);
    }

    @Test
    void command_rules() {
        LightingDeviceConfig a = add("Pool bar", "10.77.0.11");
        LightingDeviceConfig disabled = add("Spare", "10.77.0.12");
        settingsService.update(disabled.getId(), new LightingDeviceUpdateInput().active(false));

        assertThatThrownBy(() -> lightingService.setState(a.getId(), new LightingStateInput())).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> lightingService.setState("nope", new LightingStateInput().isOn(true))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> lightingService.setState(disabled.getId(), new LightingStateInput().isOn(true))).isInstanceOf(ConflictException.class);

        verify(wled, never()).setState(anyString(), anyInt(), any());
    }

    // ---- settings ------------------------------------------------------------------------------

    @Test
    void create_audits_andTheNameIsUniqueIgnoringCaseAndSpaces() {
        LightingDeviceConfig a = add("Pool   Bar", "10.77.0.11");
        assertThat(a.getName()).isEqualTo("Pool   Bar");
        assertThat(a.getPort()).isEqualTo(80);
        assertThat(a.getActive()).isTrue();
        assertThat(auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.LIGHTING_DEVICE_CREATED && a.getId().equals(e.getEntityId()))).hasSize(1);

        for (String clash : List.of("pool bar", "  POOL BAR ", "Pool Bar")) {
            assertThatThrownBy(() -> settingsService.create(new LightingDeviceCreateInput(clash, "10.77.0.12")))
                    .as(clash)
                    .isInstanceOf(ConflictException.class);
        }
        assertThat(deviceRepository.count()).isEqualTo(1);
        // renaming onto an existing name is refused too; renaming to itself differently-cased is fine
        LightingDeviceConfig b = add("Lobby", "10.77.0.12");
        assertThatThrownBy(() -> settingsService.update(b.getId(), new LightingDeviceUpdateInput().name("pool bar"))).isInstanceOf(ConflictException.class);
        assertThat(settingsService.update(a.getId(), new LightingDeviceUpdateInput().name("POOL BAR")).getName()).isEqualTo("POOL BAR");
    }

    @Test
    void anAddressThatIsNotPrivateLan_isRefusedOnSave_andNothingIsStored() {
        for (String host : List.of("8.8.8.8", "127.0.0.1", "localhost", "169.254.169.254", "0.0.0.0", "::1", "172.32.0.1")) {
            assertThatThrownBy(() -> settingsService.create(new LightingDeviceCreateInput("Bad " + host, host)))
                    .as(host)
                    .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getFieldErrors()).containsKey("host"));
        }
        assertThat(deviceRepository.count()).isZero();

        LightingDeviceConfig good = add("Pool bar", "10.77.0.11");
        assertThatThrownBy(() -> settingsService.update(good.getId(), new LightingDeviceUpdateInput().host("169.254.169.254")))
                .isInstanceOf(ValidationException.class);
        assertThat(deviceRepository.findById(good.getId()).orElseThrow().getHost()).isEqualTo("10.77.0.11");
    }

    @Test
    void update_changesOnlyWhatWasSent_clearsTheCache_andIsAudited() {
        LightingDeviceCreateInput input = new LightingDeviceCreateInput("Pool bar", "10.77.0.11");
        input.location("Roof");
        LightingDeviceConfig a = settingsService.create(input);
        when(wled.state(anyString(), anyInt())).thenReturn(new WledState(true, 1, null));
        when(wled.presets(anyString(), anyInt())).thenReturn(List.of());
        lightingService.list(false);
        verify(wled, times(1)).state("10.77.0.11", 80);

        LightingDeviceConfig updated = settingsService.update(a.getId(), new LightingDeviceUpdateInput().host("10.77.0.99").port(81));

        assertThat(updated.getName()).isEqualTo("Pool bar");
        assertThat(updated.getLocation().get()).isEqualTo("Roof");
        assertThat(updated.getHost()).isEqualTo("10.77.0.99");
        assertThat(updated.getPort()).isEqualTo(81);
        lightingService.list(false); // not served from the old address's cache
        verify(wled, times(1)).state("10.77.0.99", 81);
        assertThat(settingsService.update(a.getId(), new LightingDeviceUpdateInput().location("  ")).getLocation().get()).isNull();
        List<AuditLogEntity> updates = auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.LIGHTING_DEVICE_UPDATED && a.getId().equals(e.getEntityId())).toList();
        assertThat(updates).hasSize(2);
        assertThat(updates.stream().map(AuditLogEntity::getSummary)).anyMatch(s -> s.contains("10.77.0.11:80 -> 10.77.0.99:81"));
    }

    @Test
    void reorder_andDelete() {
        LightingDeviceConfig a = add("A", "10.77.0.11");
        LightingDeviceConfig b = add("B", "10.77.0.12");
        assertThat(a.getSortOrder()).isLessThan(b.getSortOrder());

        List<LightingDeviceConfig> reordered = settingsService.reorder(List.of(b.getId(), a.getId()));
        assertThat(reordered).extracting(LightingDeviceConfig::getName).containsExactly("B", "A");
        assertThatThrownBy(() -> settingsService.reorder(List.of(a.getId()))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> settingsService.reorder(List.of(a.getId(), a.getId()))).isInstanceOf(BadRequestException.class);

        settingsService.delete(a.getId());
        assertThat(settingsService.list()).extracting(LightingDeviceConfig::getName).containsExactly("B");
        assertThatThrownBy(() -> settingsService.delete(a.getId())).isInstanceOf(NotFoundException.class);
        assertThat(auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.LIGHTING_DEVICE_DELETED && a.getId().equals(e.getEntityId()))).hasSize(1);
    }

    @Test
    void check_reportsWhatTheControllerSays_orThatItDoesNotAnswer_andRefusesAForbiddenAddress() {
        when(wled.info("10.77.0.11", 80)).thenReturn(new WledInfo("Pool bar", "0.15.0", 150, "60019423b441"));
        when(wled.info("10.77.0.12", 8080)).thenThrow(new WledException(WledException.Kind.UNREACHABLE, "did not answer in time"));

        LightingDeviceCheckResult ok = settingsService.check(new LightingDeviceCheckInput("10.77.0.11"));
        assertThat(ok.getReachable()).isTrue();
        assertThat(ok.getInfo().getVersion()).isEqualTo("0.15.0");
        assertThat(ok.getInfo().getLedCount()).isEqualTo(150);
        assertThat(ok.getInfo().getMac()).isEqualTo("60019423b441");
        assertThat(ok.getInfo().getName()).isEqualTo("Pool bar");

        LightingDeviceCheckResult down = settingsService.check(new LightingDeviceCheckInput("10.77.0.12").port(8080));
        assertThat(down.getReachable()).isFalse();
        assertThat(down.getError()).isEqualTo("The controller did not answer in time");
        assertThat(down.getInfo()).isNull();

        assertThatThrownBy(() -> settingsService.check(new LightingDeviceCheckInput("169.254.169.254"))).isInstanceOf(ValidationException.class);
        verify(wled, never()).info(eq("169.254.169.254"), anyInt());
        assertThat(deviceRepository.count()).as("a check saves nothing").isZero();
    }

    // ---- helpers -------------------------------------------------------------------------------

    private LightingDeviceConfig add(String name, String host) {
        return settingsService.create(new LightingDeviceCreateInput(name, host));
    }

    private LightingDeviceConfig add(String name, String host, int port) {
        return settingsService.create(new LightingDeviceCreateInput(name, host).port(port));
    }

    private static LightingDevice only(List<LightingDevice> devices, String id) {
        return devices.stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
    }

    private List<AuditLogEntity> lightingAuditEntries() {
        return auditLogRepository.findAll().stream().filter(e -> e.getEntityType() == AuditEntityType.LIGHTING_DEVICE).toList();
    }

    private List<AuditLogEntity> commandEntries(String deviceId) {
        return lightingAuditEntries().stream()
                .filter(e -> e.getAction() == AuditAction.LIGHTING_DEVICE_COMMAND && deviceId.equals(e.getEntityId()))
                .toList();
    }
}
