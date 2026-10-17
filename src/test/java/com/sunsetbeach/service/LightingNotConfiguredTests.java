package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sunsetbeach.entity.LightingDeviceEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.lighting.LanAddressGuard;
import com.sunsetbeach.lighting.WledClient;
import com.sunsetbeach.model.LightingDeviceCheckInput;
import com.sunsetbeach.model.LightingDeviceCreateInput;
import com.sunsetbeach.model.LightingDeviceUpdateInput;
import com.sunsetbeach.model.LightingStateInput;
import com.sunsetbeach.repository.LightingDeviceRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * With {@code LIGHTING_ALLOWED_CIDRS} unset lighting is off: nothing is sent to any controller, the
 * calls that would make the server send a request are a 409, and the housekeeping that sends nothing
 * (rename, deactivate, reorder, delete) keeps working. Plain unit test - the guard is the real one,
 * everything else is mocked, so no Spring context (and no connection pool) is spent on it.
 */
class LightingNotConfiguredTests {

    private static final String MESSAGE = "Lighting is not configured on the server";

    private final LightingDeviceRepository devices = mock(LightingDeviceRepository.class);
    private final WledClient wled = mock(WledClient.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final LanAddressGuard off = new LanAddressGuard("");
    private final LightingService lightingService = new LightingService(devices, wled, off, audit, Clock.systemUTC(), 10, 0);
    private final LightingSettingsService settings = new LightingSettingsService(devices, mock(LightingService.class), off, wled, audit);
    private LightingDeviceEntity saved;

    @BeforeEach
    void stubRepository() {
        saved = new LightingDeviceEntity();
        saved.setName("Pool bar");
        saved.setHost("192.168.1.50");
        saved.setPort(80);
        when(devices.findById("d1")).thenReturn(Optional.of(saved));
        when(devices.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(devices.findAll()).thenReturn(List.of(saved));
    }

    @Test
    void status_andList_sayNotConfigured_withoutTouchingAnything() {
        assertThat(lightingService.status().getConfigured()).isFalse();

        var list = lightingService.list(true);

        assertThat(list.getConfigured()).isFalse();
        assertThat(list.getDevices()).isEmpty();
        verifyNoInteractions(wled);
        verify(devices, never()).findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Test
    void commandsAndAddressWork_are409_andSendNothing() {
        assertThatThrownBy(() -> lightingService.setState("d1", new LightingStateInput().isOn(true)))
                .isInstanceOf(ConflictException.class).hasMessage(MESSAGE);
        assertThatThrownBy(() -> settings.create(new LightingDeviceCreateInput("New", "192.168.1.51")))
                .isInstanceOf(ConflictException.class).hasMessage(MESSAGE);
        assertThatThrownBy(() -> settings.check(new LightingDeviceCheckInput("192.168.1.51")))
                .isInstanceOf(ConflictException.class).hasMessage(MESSAGE);
        assertThatThrownBy(() -> settings.update("d1", new LightingDeviceUpdateInput().host("192.168.1.99")))
                .isInstanceOf(ConflictException.class).hasMessage(MESSAGE);
        assertThatThrownBy(() -> settings.update("d1", new LightingDeviceUpdateInput().port(8080)))
                .isInstanceOf(ConflictException.class).hasMessage(MESSAGE);

        verifyNoInteractions(wled);
        verify(devices, never()).saveAndFlush(any());
        assertThat(saved.getHost()).isEqualTo("192.168.1.50");
        assertThat(saved.getPort()).isEqualTo(80);
    }

    @Test
    void housekeepingThatSendsNothing_keepsWorking() {
        // a form that re-sends the unchanged host and port while renaming must not trip over the missing setting
        var renamed = settings.update("d1", new LightingDeviceUpdateInput().name("Pool terrace").host("192.168.1.50").port(80));
        assertThat(renamed.getName()).isEqualTo("Pool terrace");

        assertThat(settings.update("d1", new LightingDeviceUpdateInput().active(false)).getActive()).isFalse();
        assertThat(settings.update("d1", new LightingDeviceUpdateInput().location("Roof")).getLocation().get()).isEqualTo("Roof");

        saved.setSortOrder(0);
        when(devices.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(saved));
        assertThat(settings.list()).hasSize(1);
        settings.delete("d1");
        verify(devices).delete(saved);
        verifyNoInteractions(wled);
        verify(wled, never()).info(anyString(), anyInt());
    }
}
