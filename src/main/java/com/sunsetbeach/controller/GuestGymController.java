package com.sunsetbeach.controller;

import com.sunsetbeach.api.GuestGymApi;
import com.sunsetbeach.model.GymGuestInfo;
import com.sunsetbeach.model.GymGuestReservation;
import com.sunsetbeach.model.GymGuestReservationInput;
import com.sunsetbeach.model.GymSlotDay;
import com.sunsetbeach.security.CurrentGuest;
import com.sunsetbeach.service.GymReservationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** The guest's own gym booking under {@code /guest/gym/**} (ROLE_GUEST, see SecurityConfig). */
@RestController
public class GuestGymController implements GuestGymApi {

    private final GymReservationService reservations;

    public GuestGymController(GymReservationService reservations) {
        this.reservations = reservations;
    }

    @Override
    public ResponseEntity<GymGuestInfo> getGuestGymInfo() {
        return ResponseEntity.ok(reservations.guestInfo(CurrentGuest.id()));
    }

    @Override
    public ResponseEntity<GymSlotDay> getGuestGymSlots(String date, String bookingId) {
        return ResponseEntity.ok(reservations.guestSlots(CurrentGuest.id(), date, bookingId));
    }

    @Override
    public ResponseEntity<List<GymGuestReservation>> listGuestGymReservations() {
        return ResponseEntity.ok(reservations.listForGuest(CurrentGuest.id()));
    }

    @Override
    public ResponseEntity<GymGuestReservation> createGuestGymReservation(GymGuestReservationInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservations.createForGuest(CurrentGuest.id(), input));
    }

    @Override
    public ResponseEntity<GymGuestReservation> cancelGuestGymReservation(String id) {
        return ResponseEntity.ok(reservations.cancelForGuest(CurrentGuest.id(), id));
    }
}
