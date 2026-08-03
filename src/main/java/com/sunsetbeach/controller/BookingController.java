package com.sunsetbeach.controller;

import com.sunsetbeach.api.BookingsApi;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.service.BookingService;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController implements BookingsApi {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public ResponseEntity<List<Booking>> listBookings(String from, String to, BookingStatus status) {
        return ResponseEntity.ok(bookingService.list(from, to, status));
    }

    @Override
    public ResponseEntity<Booking> getBooking(String id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @Override
    public ResponseEntity<Booking> createBooking(BookingCreateInput bookingCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(bookingCreateInput));
    }

    @Override
    public ResponseEntity<Booking> updateBookingStatus(String id, BookingStatusInput bookingStatusInput) {
        return ResponseEntity.ok(bookingService.updateStatus(id, bookingStatusInput));
    }

    @Override
    public ResponseEntity<String> exportBookings(String from, String to, BookingStatus status) {
        String csv = bookingService.exportCsv(from, to, status);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("bookings.csv").build().toString())
                .body(csv);
    }
}
