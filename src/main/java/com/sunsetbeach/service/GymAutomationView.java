package com.sunsetbeach.service;

import com.sunsetbeach.entity.GymReservationActionEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.GymAutomation;
import com.sunsetbeach.model.GymAutomationDeviceResult;
import com.sunsetbeach.model.GymAutomationPhase;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Turns the stored per-device actions of a reservation into the {@link GymAutomation} the API returns. */
final class GymAutomationView {

    private GymAutomationView() {}

    static GymAutomation of(List<GymReservationActionEntity> actions) {
        GymAutomation automation = new GymAutomation();
        GymAutomationPhase on = phase(actions, GymReservationActionEntity.ON);
        GymAutomationPhase off = phase(actions, GymReservationActionEntity.OFF);
        if (on != null) {
            automation.switchOn(on);
        }
        if (off != null) {
            automation.switchOff(off);
        }
        return automation;
    }

    /** One phase's rows reduced to a state; null while the automation has not reached the phase. */
    static GymAutomationPhase phase(List<GymReservationActionEntity> all, String phase) {
        List<GymReservationActionEntity> rows = all.stream()
                .filter(a -> phase.equals(a.getPhase()))
                .sorted(Comparator.comparing(GymReservationActionEntity::getDeviceName))
                .toList();
        if (rows.isEmpty()) {
            return null;
        }
        boolean pending = rows.stream().anyMatch(GymReservationActionEntity::isPending);
        boolean failed = rows.stream().anyMatch(a -> GymReservationActionEntity.FAILED.equals(a.getStatus()));
        boolean worked = rows.stream().anyMatch(a -> GymReservationActionEntity.OK.equals(a.getStatus()) || covered(a));
        boolean skippedForReal = rows.stream().anyMatch(a -> GymReservationActionEntity.SKIPPED.equals(a.getStatus()) && !covered(a));

        GymAutomationPhase.StateEnum state;
        if (pending) {
            state = GymAutomationPhase.StateEnum.PENDING;
        } else if (failed && worked) {
            state = GymAutomationPhase.StateEnum.PARTIAL;
        } else if (failed) {
            state = GymAutomationPhase.StateEnum.FAILED;
        } else if (skippedForReal && !worked) {
            state = GymAutomationPhase.StateEnum.SKIPPED;
        } else {
            state = GymAutomationPhase.StateEnum.OK;
        }

        List<GymAutomationDeviceResult> results = rows.stream().map(GymAutomationView::result).toList();
        GymAutomationPhase dto = new GymAutomationPhase(state, results);
        if (!pending) {
            rows.stream()
                    .map(GymReservationActionEntity::getCompletedAt)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .ifPresent(t -> dto.at(TimestampFormat.toUtc(t)));
        }
        return dto;
    }

    private static boolean covered(GymReservationActionEntity a) {
        return GymReservationActionEntity.SKIPPED.equals(a.getStatus()) && GymReservationActionEntity.COVERED_BY_PREVIOUS.equals(a.getSkipReason());
    }

    private static GymAutomationDeviceResult result(GymReservationActionEntity a) {
        GymAutomationDeviceResult r = new GymAutomationDeviceResult(
                a.getDeviceName(), GymAutomationDeviceResult.StatusEnum.fromValue(a.getStatus()), a.getAttempts());
        if (a.getSkipReason() != null) {
            r.skipReason(GymAutomationDeviceResult.SkipReasonEnum.fromValue(a.getSkipReason()));
        }
        if (a.getLastError() != null && !GymReservationActionEntity.OK.equals(a.getStatus())) {
            r.error(a.getLastError());
        }
        return r;
    }
}
