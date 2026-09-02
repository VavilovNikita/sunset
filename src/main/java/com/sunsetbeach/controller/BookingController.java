package com.sunsetbeach.controller;

import com.sunsetbeach.api.BookingsApi;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCalendarResponse;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.BookingPosOrder;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.CheckInResult;
import com.sunsetbeach.model.CheckOutResult;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RelocationUndoInput;
import com.sunsetbeach.model.RepriceInput;
import com.sunsetbeach.model.RepriceQuote;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.TodayBoard;
import com.sunsetbeach.security.BookingRateLimiter;
import com.sunsetbeach.security.ClientIpResolver;
import com.sunsetbeach.service.BookingCalendarService;
import com.sunsetbeach.service.BookingOccupancyService;
import com.sunsetbeach.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
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
    private final BookingCalendarService bookingCalendarService;
    private final BookingOccupancyService bookingOccupancyService;
    private final BookingRateLimiter bookingRateLimiter;
    private final HttpServletRequest request;

    public BookingController(
            BookingService bookingService,
            BookingCalendarService bookingCalendarService,
            BookingOccupancyService bookingOccupancyService,
            BookingRateLimiter bookingRateLimiter,
            HttpServletRequest request) {
        this.bookingService = bookingService;
        this.bookingCalendarService = bookingCalendarService;
        this.bookingOccupancyService = bookingOccupancyService;
        this.bookingRateLimiter = bookingRateLimiter;
        this.request = request;
    }

    @Override
    public ResponseEntity<List<Booking>> listBookings(String from, String to, BookingStatus status, String guestName) {
        return ResponseEntity.ok(bookingService.list(from, to, status, guestName));
    }

    @Override
    public ResponseEntity<Booking> getBooking(String id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @Override
    public ResponseEntity<Booking> createBooking(BookingCreateInput bookingCreateInput) {
        // This is the one public, unauthenticated write in the whole API - see
        // BookingRateLimiter's javadoc for what this does and doesn't protect against.
        bookingRateLimiter.checkAllowedAndRecord(ClientIpResolver.resolve(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(bookingCreateInput));
    }

    @Override
    public ResponseEntity<Booking> updateBookingStatus(String id, BookingStatusInput bookingStatusInput) {
        return ResponseEntity.ok(bookingService.updateStatus(id, bookingStatusInput));
    }

    @Override
    public ResponseEntity<Booking> assignBookingRoomUnit(String id, RoomUnitAssignmentInput roomUnitAssignmentInput) {
        return ResponseEntity.ok(bookingService.assignRoomUnit(id, roomUnitAssignmentInput));
    }

    @Override
    public ResponseEntity<Booking> createStaffBooking(StaffBookingCreateInput staffBookingCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createStaffBooking(staffBookingCreateInput));
    }

    @Override
    public ResponseEntity<BookingCalendarResponse> getBookingsCalendar(String from, String to) {
        return ResponseEntity.ok(bookingCalendarService.getCalendar(LocalDate.parse(from), LocalDate.parse(to)));
    }

    @Override
    public ResponseEntity<Booking> updateBookingSchedule(String id, BookingScheduleInput bookingScheduleInput) {
        return ResponseEntity.ok(bookingService.updateSchedule(id, bookingScheduleInput));
    }

    @Override
    public ResponseEntity<BookingScheduleQuote> quoteBookingSchedule(String id, BookingScheduleInput bookingScheduleInput) {
        return ResponseEntity.ok(bookingService.quoteSchedule(id, bookingScheduleInput));
    }

    @Override
    public ResponseEntity<Booking> relocateBooking(String id, RelocationInput relocationInput) {
        return ResponseEntity.ok(bookingService.relocate(id, relocationInput));
    }

    @Override
    public ResponseEntity<BookingScheduleQuote> quoteBookingRelocation(String id, RelocationInput relocationInput) {
        return ResponseEntity.ok(bookingService.quoteRelocation(id, relocationInput));
    }

    @Override
    public ResponseEntity<Booking> undoBookingRelocation(String id, RelocationUndoInput relocationUndoInput) {
        return ResponseEntity.ok(bookingService.undoRelocation(id, relocationUndoInput));
    }

    @Override
    public ResponseEntity<Booking> repriceBooking(String id, RepriceInput repriceInput) {
        return ResponseEntity.ok(bookingService.reprice(id, repriceInput));
    }

    @Override
    public ResponseEntity<RepriceQuote> quoteBookingReprice(String id, RepriceInput repriceInput) {
        return ResponseEntity.ok(bookingService.quoteReprice(id, repriceInput));
    }

    @Override
    public ResponseEntity<TodayBoard> getTodayBoard() {
        return ResponseEntity.ok(bookingOccupancyService.getTodayBoard());
    }

    @Override
    public ResponseEntity<CheckInResult> checkInBooking(String id) {
        return ResponseEntity.ok(bookingOccupancyService.checkIn(id));
    }

    @Override
    public ResponseEntity<CheckOutResult> checkOutBooking(String id) {
        return ResponseEntity.ok(bookingOccupancyService.checkOut(id));
    }

    @Override
    public ResponseEntity<Booking> markBookingNoShow(String id) {
        return ResponseEntity.ok(bookingOccupancyService.markNoShow(id));
    }

    @Override
    public ResponseEntity<List<BookingPosOrder>> listBookingPosOrders(String id) {
        return ResponseEntity.ok(bookingService.listPosOrders(id));
    }

    @Override
    public ResponseEntity<BookingFolio> getBookingFolio(String id) {
        return ResponseEntity.ok(bookingService.getFolio(id));
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
