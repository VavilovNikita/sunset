package com.sunsetbeach.controller;

import com.sunsetbeach.api.GymScheduleApi;
import com.sunsetbeach.model.GymHallSettings;
import com.sunsetbeach.model.GymReservation;
import com.sunsetbeach.model.GymReservationCancelInput;
import com.sunsetbeach.model.GymReservationCreateInput;
import com.sunsetbeach.model.GymReservationScheduleInput;
import com.sunsetbeach.model.GymSchedule;
import com.sunsetbeach.security.CurrentUser;
import com.sunsetbeach.service.GymHallSettingsService;
import com.sunsetbeach.service.GymReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** The staff side of gym reservations: schedule, create for a guest, move, cancel, and the ADMIN hall settings. */
@RestController
public class GymReservationController implements GymScheduleApi {

    private final GymReservationService reservations;
    private final GymHallSettingsService hallSettings;

    public GymReservationController(GymReservationService reservations, GymHallSettingsService hallSettings) {
        this.reservations = reservations;
        this.hallSettings = hallSettings;
    }

    @Override
    public ResponseEntity<GymSchedule> listGymReservations(String from, String to, Boolean includeCancelled) {
        return ResponseEntity.ok(reservations.schedule(from, to, Boolean.TRUE.equals(includeCancelled)));
    }

    @Override
    public ResponseEntity<GymReservation> createGymReservation(GymReservationCreateInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservations.createForStaff(input, CurrentUser.id()));
    }

    @Override
    public ResponseEntity<GymReservation> updateGymReservationSchedule(String id, GymReservationScheduleInput input) {
        return ResponseEntity.ok(reservations.move(id, input, CurrentUser.id()));
    }

    @Override
    public ResponseEntity<GymReservation> cancelGymReservation(String id, GymReservationCancelInput input) {
        return ResponseEntity.ok(reservations.cancelByStaff(id, input == null ? null : input.getReason(), CurrentUser.id()));
    }

    @Override
    public ResponseEntity<GymHallSettings> getGymHallSettings() {
        return ResponseEntity.ok(hallSettings.get());
    }

    @Override
    public ResponseEntity<GymHallSettings> updateGymHallSettings(GymHallSettings input) {
        return ResponseEntity.ok(hallSettings.update(input));
    }
}
