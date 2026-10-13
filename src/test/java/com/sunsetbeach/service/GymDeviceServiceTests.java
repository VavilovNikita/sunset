package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.GymAcCommandInput;
import com.sunsetbeach.model.GymDevice;
import com.sunsetbeach.model.GymDeviceConfig;
import com.sunsetbeach.model.GymDeviceConfigInput;
import com.sunsetbeach.model.GymDeviceType;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.GymDeviceRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.tuya.TuyaClient;
import com.sunsetbeach.tuya.TuyaException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The gym module against a real database with Tuya mocked at the {@link TuyaClient} boundary (the
 * HTTP layer has its own tests): the state cache, what a failed read does to the list, that every
 * command is audited with its outcome, the stored last air-conditioner command (what, when, who),
 * and the settings rules. Not {@code @Transactional}: the audit log commits independently, so
 * everything created here is removed in {@code @AfterEach}.
 */
@SpringBootTest(properties = "app.tuya.confirm-delay-ms=0")
class GymDeviceServiceTests extends AbstractIntegrationTest {

    @Autowired private GymDeviceService gymDeviceService;
    @Autowired private GymSettingsService gymSettingsService;
    @Autowired private GymDeviceRepository gymDeviceRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;
    @MockitoBean private TuyaClient tuya;

    private String userId;

    @BeforeEach
    void setUp() {
        reset(tuya);
        gymDeviceService.clearStateCache();
        gymDeviceRepository.deleteAll();
        UserEntity user = new UserEntity();
        user.setEmail("gym-test-" + UUID.randomUUID() + "@test.local");
        user.setName(user.getEmail());
        user.setFullName("Nok Cashier");
        user.setPasswordHash("unused");
        user.setRole(Role.CASHIER);
        user.setActive(true);
        user.setTokenVersion(0);
        userId = userRepository.saveAndFlush(user).getId();
        StaffPrincipal principal = new StaffPrincipal(userId, user.getEmail(), Role.CASHIER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER"))));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        auditLogRepository.deleteAll(gymAuditEntries());
        gymDeviceRepository.deleteAll();
        userRepository.deleteById(userId);
    }

    // ---- state: cache and failures -----------------------------------------------------------

    @Test
    void relayState_isCached_untilRefreshIsAskedFor() {
        GymDeviceConfig fan = addRelay("roof fan 1", "tuya-fan-1");
        stateOf("tuya-fan-1", true, true);

        GymDevice first = only(gymDeviceService.list(false).getDevices(), fan.getId());
        gymDeviceService.list(false);
        verify(tuya, times(1)).deviceState("tuya-fan-1");
        assertThat(first.getIsOn().get()).isTrue();
        assertThat(first.getOnline().get()).isTrue();
        assertThat(first.getStateReadAt().isPresent()).isTrue();

        stateOf("tuya-fan-1", false, true);
        GymDevice refreshed = only(gymDeviceService.list(true).getDevices(), fan.getId());
        verify(tuya, times(2)).deviceState("tuya-fan-1");
        assertThat(refreshed.getIsOn().get()).isFalse();
        // and the refreshed read is what the next plain list serves
        assertThat(only(gymDeviceService.list(false).getDevices(), fan.getId()).getIsOn().get()).isFalse();
        verify(tuya, times(2)).deviceState("tuya-fan-1");
    }

    @Test
    void anAirConditioner_neverCostsAStateRead() {
        addAc("Air 1", "tuya-ac-1");

        GymDevice ac = gymDeviceService.list(true).getDevices().get(0);

        verify(tuya, never()).deviceState(anyString());
        assertThat(ac.getIsOn().isPresent()).as("no live state exists for an IR air conditioner").isFalse();
        assertThat(ac.getLastCommand().isPresent()).isFalse();
    }

    @Test
    void oneRelayFailing_doesNotTakeTheListDown() {
        GymDeviceConfig a = addRelay("roof fan 1", "tuya-fan-1");
        GymDeviceConfig b = addRelay("roof fan 2", "tuya-fan-2");
        when(tuya.deviceState("tuya-fan-1")).thenThrow(new TuyaException(TuyaException.Kind.NETWORK, "Tuya is not responding"));
        stateOf("tuya-fan-2", true, true);

        List<GymDevice> devices = gymDeviceService.list(true).getDevices();

        GymDevice failed = only(devices, a.getId());
        assertThat(failed.getStateError().get()).isEqualTo("Tuya is not responding");
        assertThat(failed.getIsOn().isPresent()).isFalse();
        assertThat(only(devices, b.getId()).getIsOn().get()).isTrue();
    }

    @Test
    void integrationDown_isFoundOutOnce_notOncePerRelay() {
        addRelay("roof fan 1", "tuya-fan-1");
        addRelay("roof fan 2", "tuya-fan-2");
        addRelay("wall fan", "tuya-fan-3");
        when(tuya.deviceState(anyString()))
                .thenThrow(new TuyaException(TuyaException.Kind.UNAVAILABLE, "Tuya integration is unavailable — check the IoT Core subscription (extend the trial period at iot.tuya.com)"));

        List<GymDevice> devices = gymDeviceService.list(true).getDevices();

        verify(tuya, times(1)).deviceState(anyString());
        assertThat(devices).hasSize(3).allSatisfy(d -> assertThat(d.getStateError().get()).startsWith("Tuya integration is unavailable"));
    }

    @Test
    void inactiveDevices_areNotListed_andOrderFollowsSortOrder() {
        GymDeviceConfig first = addRelay("B second", "tuya-b");
        GymDeviceConfig second = addRelay("A first", "tuya-a");
        GymDeviceConfig hidden = addRelay("hidden", "tuya-h");
        gymSettingsService.update(hidden.getId(), relayInput("hidden", "tuya-h", false));
        stateOf("tuya-a", false, true);
        stateOf("tuya-b", false, true);

        assertThat(gymDeviceService.list(false).getDevices()).extracting(GymDevice::getId).containsExactly(first.getId(), second.getId());

        gymSettingsService.reorder(List.of(second.getId(), first.getId(), hidden.getId()));

        assertThat(gymDeviceService.list(false).getDevices()).extracting(GymDevice::getId).containsExactly(second.getId(), first.getId());
    }

    // ---- relay commands ----------------------------------------------------------------------

    @Test
    void relayCommand_usesTheConfiguredSwitchCode_dropsTheCache_andIsAudited() {
        GymDeviceConfig fan = addRelay("roof fan 1", "tuya-fan-1");
        stateOf("tuya-fan-1", false, true);
        gymDeviceService.list(false); // fills the cache with "off"
        stateOf("tuya-fan-1", true, true);

        GymDevice after = gymDeviceService.setRelay(fan.getId(), true);

        verify(tuya).sendCommand("tuya-fan-1", "switch_1", true);
        verify(tuya, times(2)).deviceState("tuya-fan-1"); // list + the confirming read, not the stale cache
        assertThat(after.getIsOn().get()).isTrue();
        List<AuditLogEntity> entries = commandEntries(fan.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).isEqualTo("Switched on \"roof fan 1\"");
        assertThat(entries.get(0).getActorUserId()).isEqualTo(userId);
        assertThat(entries.get(0).getActorRole()).isEqualTo(Role.CASHIER);
    }

    @Test
    void relayCommand_whoseConfirmingReadIsStillTheOldValue_saysSo_insteadOfShowingItAsTruth() {
        GymDeviceConfig fan = addRelay("wall fan", "tuya-fan-3");
        stateOf("tuya-fan-3", true, true); // Tuya's cloud has not caught up with the command yet

        GymDevice after = gymDeviceService.setRelay(fan.getId(), false);

        verify(tuya).sendCommand("tuya-fan-3", "switch_1", false);
        assertThat(after.getStateError().get()).startsWith("The command was sent, but the device has not confirmed");
        // and the stale read is not kept as the cached state
        stateOf("tuya-fan-3", false, true);
        assertThat(only(gymDeviceService.list(false).getDevices(), fan.getId()).getIsOn().get()).isFalse();
    }

    @Test
    void failedRelayCommand_isAudited_withTheReason_andThrown() {
        GymDeviceConfig fan = addRelay("wall fan", "tuya-fan-3");
        org.mockito.Mockito.doThrow(new TuyaException(TuyaException.Kind.REJECTED, "Tuya rejected the request. Details are in the server log"))
                .when(tuya).sendCommand("tuya-fan-3", "switch_1", false);

        assertThatThrownBy(() -> gymDeviceService.setRelay(fan.getId(), false)).isInstanceOf(TuyaException.class);

        List<AuditLogEntity> entries = commandEntries(fan.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).startsWith("Could not switch off \"wall fan\"").contains("Tuya rejected the request");
    }

    @Test
    void commandsOnTheWrongKindOfDevice_orADisabledOne_orAMissingOne_areRefused() {
        GymDeviceConfig fan = addRelay("roof fan 1", "tuya-fan-1");
        GymDeviceConfig ac = addAc("Air 1", "tuya-ac-1");
        GymDeviceConfig off = addRelay("old fan", "tuya-old");
        gymSettingsService.update(off.getId(), relayInput("old fan", "tuya-old", false));

        assertThatThrownBy(() -> gymDeviceService.setRelay(ac.getId(), true)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> gymDeviceService.sendAcCommand(fan.getId(), acCommand(1, 0, 24, 1))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> gymDeviceService.setRelay(off.getId(), true)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> gymDeviceService.setRelay("nope", true)).isInstanceOf(NotFoundException.class);
        verify(tuya, never()).sendCommand(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    // ---- air conditioner commands ------------------------------------------------------------

    @Test
    void acCommand_isSentToTheHubAndRemote_andRemembersWhatWhenAndWho() {
        GymDeviceConfig ac = gymSettingsService.create(acInput("Air 1", "tuya-ac-1", "hub-1", "remote-1"));

        GymDevice after = gymDeviceService.sendAcCommand(ac.getId(), acCommand(1, 0, 24, 2));

        verify(tuya).sendAirConditionerCommand("hub-1", "remote-1", 1, 0, 24, 2);
        var last = after.getLastCommand().get();
        assertThat(last.getPower()).isEqualTo(1);
        assertThat(last.getMode()).isEqualTo(0);
        assertThat(last.getTemp()).isEqualTo(24);
        assertThat(last.getWind()).isEqualTo(2);
        assertThat(last.getSentBy()).isEqualTo("Nok Cashier");
        assertThat(last.getSentAt()).isNotNull();
        // it survives a reload: it is stored, not held in memory
        assertThat(gymDeviceService.list(false).getDevices().get(0).getLastCommand().get().getTemp()).isEqualTo(24);
        assertThat(commandEntries(ac.getId())).singleElement()
                .satisfies(e -> assertThat(e.getSummary()).isEqualTo("Sent to \"Air 1\": on, cool, 24°, fan 2"));
    }

    @Test
    void acCommand_defaultsTheRemoteToTheVirtualDeviceId() {
        GymDeviceConfig ac = gymSettingsService.create(acInput("Air 2", "tuya-ac-2", "hub-2", null));

        assertThat(ac.getRemoteId().get()).isEqualTo("tuya-ac-2");
        gymDeviceService.sendAcCommand(ac.getId(), acCommand(0, 2, 26, 0));
        verify(tuya).sendAirConditionerCommand("hub-2", "tuya-ac-2", 0, 2, 26, 0);
    }

    @Test
    void failedAcCommand_keepsThePreviousLastCommand_andIsAudited() {
        GymDeviceConfig ac = gymSettingsService.create(acInput("Air 1", "tuya-ac-1", "hub-1", "remote-1"));
        gymDeviceService.sendAcCommand(ac.getId(), acCommand(1, 0, 24, 1));
        org.mockito.Mockito.doThrow(new TuyaException(TuyaException.Kind.UNAVAILABLE, "This API is not subscribed in the Tuya project"))
                .when(tuya).sendAirConditionerCommand("hub-1", "remote-1", 1, 0, 18, 3);

        assertThatThrownBy(() -> gymDeviceService.sendAcCommand(ac.getId(), acCommand(1, 0, 18, 3))).isInstanceOf(TuyaException.class);

        assertThat(gymDeviceService.list(false).getDevices().get(0).getLastCommand().get().getTemp()).isEqualTo(24);
        assertThat(commandEntries(ac.getId())).hasSize(2)
                .anySatisfy(e -> assertThat(e.getSummary()).startsWith("Could not send to \"Air 1\"").contains("18°"));
    }

    // ---- settings ----------------------------------------------------------------------------

    @Test
    void settings_validateTheTypeSpecificFields() {
        assertThatThrownBy(() -> gymSettingsService.create(relayInput("fan", "tuya-x", true, null))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> gymSettingsService.create(acInput("Air", "tuya-y", null, null))).isInstanceOf(ValidationException.class);
    }

    @Test
    void settings_refuseTheSameTuyaDeviceTwice_andAuditEveryChange() {
        GymDeviceConfig fan = addRelay("roof fan 1", "tuya-fan-1");

        assertThatThrownBy(() -> addRelay("again", "tuya-fan-1")).isInstanceOf(ConflictException.class);
        gymSettingsService.update(fan.getId(), relayInput("roof fan one", "tuya-fan-1", true));
        gymSettingsService.delete(fan.getId());

        assertThat(gymAuditEntries()).extracting(AuditLogEntity::getAction)
                .containsExactlyInAnyOrder(AuditAction.GYM_DEVICE_CREATED, AuditAction.GYM_DEVICE_UPDATED, AuditAction.GYM_DEVICE_DELETED);
    }

    @Test
    void reorder_needsExactlyTheConfiguredDevices() {
        GymDeviceConfig a = addRelay("a", "tuya-a");
        GymDeviceConfig b = addRelay("b", "tuya-b");

        assertThatThrownBy(() -> gymSettingsService.reorder(List.of(a.getId()))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> gymSettingsService.reorder(List.of(a.getId(), a.getId()))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> gymSettingsService.reorder(List.of(a.getId(), "other"))).isInstanceOf(BadRequestException.class);

        assertThat(gymSettingsService.reorder(List.of(b.getId(), a.getId()))).extracting(GymDeviceConfig::getId).containsExactly(b.getId(), a.getId());
    }

    @Test
    void switchCodes_areTheBooleanStatusCodesOnly() {
        Map<String, JsonNode> status = new LinkedHashMap<>();
        JsonMapper json = JsonMapper.builder().build();
        status.put("switch_1", json.readTree("true"));
        status.put("countdown_1", json.readTree("0"));
        status.put("relay_status", json.readTree("\"power_off\""));
        when(tuya.deviceState("tuya-fan-1")).thenReturn(new TuyaClient.TuyaDeviceState(true, status));

        assertThat(gymSettingsService.listSwitchCodes("tuya-fan-1")).containsExactly("switch_1");
    }

    // ---- helpers -----------------------------------------------------------------------------

    private void stateOf(String tuyaId, boolean on, boolean online) {
        Map<String, JsonNode> status = new LinkedHashMap<>();
        status.put("switch_1", JsonMapper.builder().build().readTree(String.valueOf(on)));
        when(tuya.deviceState(tuyaId)).thenReturn(new TuyaClient.TuyaDeviceState(online, status));
    }

    private GymDeviceConfig addRelay(String name, String tuyaId) {
        return gymSettingsService.create(relayInput(name, tuyaId, true));
    }

    private GymDeviceConfig addAc(String name, String tuyaId) {
        return gymSettingsService.create(acInput(name, tuyaId, "hub-1", null));
    }

    private static GymDeviceConfigInput relayInput(String name, String tuyaId, boolean active) {
        return relayInput(name, tuyaId, active, "switch_1");
    }

    private static GymDeviceConfigInput relayInput(String name, String tuyaId, boolean active, String switchCode) {
        return new GymDeviceConfigInput(name, GymDeviceType.RELAY, tuyaId).switchCode(switchCode).active(active);
    }

    private static GymDeviceConfigInput acInput(String name, String tuyaId, String hub, String remote) {
        return new GymDeviceConfigInput(name, GymDeviceType.AC_IR, tuyaId).infraredId(hub).remoteId(remote).active(true);
    }

    private static GymAcCommandInput acCommand(int power, int mode, int temp, int wind) {
        return new GymAcCommandInput(power, mode, temp, wind);
    }

    private static GymDevice only(List<GymDevice> devices, String id) {
        return devices.stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
    }

    private List<AuditLogEntity> gymAuditEntries() {
        return auditLogRepository.findAll().stream().filter(e -> e.getEntityType() == AuditEntityType.GYM_DEVICE).toList();
    }

    private List<AuditLogEntity> commandEntries(String deviceId) {
        return gymAuditEntries().stream()
                .filter(e -> e.getAction() == AuditAction.GYM_DEVICE_COMMAND && deviceId.equals(e.getEntityId()))
                .toList();
    }
}
