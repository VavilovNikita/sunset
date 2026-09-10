package com.sunsetbeach.controller;

import com.sunsetbeach.api.GuestsApi;
import com.sunsetbeach.model.Guest;
import com.sunsetbeach.model.GuestCreateInput;
import com.sunsetbeach.model.GuestDetail;
import com.sunsetbeach.model.GuestUpdateInput;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.service.GuestService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuestController implements GuestsApi {

    private final GuestService guestService;

    public GuestController(GuestService guestService) {
        this.guestService = guestService;
    }

    @Override
    public ResponseEntity<List<Guest>> searchGuests(String q) {
        return ResponseEntity.ok(guestService.search(q));
    }

    @Override
    public ResponseEntity<Guest> createGuest(GuestCreateInput guestCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestService.create(guestCreateInput));
    }

    @Override
    public ResponseEntity<GuestDetail> getGuest(String id) {
        return ResponseEntity.ok(guestService.getDetail(id));
    }

    @Override
    public ResponseEntity<Guest> updateGuest(String id, GuestUpdateInput guestUpdateInput) {
        return ResponseEntity.ok(guestService.update(id, guestUpdateInput));
    }

    @Override
    public ResponseEntity<OkTrue> deleteGuest(String id) {
        guestService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }
}
