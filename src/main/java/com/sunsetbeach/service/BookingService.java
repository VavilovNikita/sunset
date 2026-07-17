package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.BookingMapper;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import jakarta.persistence.criteria.Predicate;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final String SERIALIZATION_FAILURE_SQLSTATE = "40001";

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingWriter bookingWriter;
    private final BookingMapper bookingMapper;
    private final EmailService emailService;

    public BookingService(
            RoomRepository roomRepository,
            BookingRepository bookingRepository,
            BookingWriter bookingWriter,
            BookingMapper bookingMapper,
            EmailService emailService) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.bookingWriter = bookingWriter;
        this.bookingMapper = bookingMapper;
        this.emailService = emailService;
    }

    public Booking createBooking(BookingCreateInput input) {
        LocalDate checkIn = LocalDate.parse(input.getCheckIn());
        LocalDate checkOut = LocalDate.parse(input.getCheckOut());
        if (!checkIn.isBefore(checkOut)) {
            throw ValidationException.field("checkOut", "checkIn must be before checkOut");
        }

        RoomEntity room = roomRepository.findById(input.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));

        BookingEntity saved;
        try {
            saved = bookingWriter.insert(
                    room, input.getGuestName(), input.getGuestEmail(), input.getGuestPhone(), checkIn, checkOut);
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just booked these dates — please try again.");
            }
            throw e;
        }

        emailService.sendNewBookingEmail(saved, room);
        return bookingMapper.toDto(saved, room);
    }

    @Transactional
    public Booking updateStatus(String id, BookingStatusInput input) {
        BookingEntity booking = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        booking.setStatus(input.getStatus());
        // paymentNote is optional+nullable: an omitted field leaves the existing value alone
        // (matches Prisma skipping `undefined` update data), an explicit null clears it.
        if (input.getPaymentNote().isPresent()) {
            String note = input.getPaymentNote().get();
            booking.setPaymentNote(note != null ? note.trim() : null);
        }
        // flush so @UpdateTimestamp (regenerated on every save) is on the object before mapping
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        RoomEntity room = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        emailService.sendGuestStatusEmail(saved, room);
        return bookingMapper.toDto(saved, room);
    }

    @Transactional(readOnly = true)
    public String exportCsv(String from, String to, BookingStatus status) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null ? LocalDate.parse(to) : null;

        Specification<BookingEntity> spec = (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("room");
            }
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThan(root.get("checkOut"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThan(root.get("checkIn"), toDate));
            }
            query.orderBy(cb.asc(root.get("checkIn")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<BookingEntity> bookings = bookingRepository.findAll(spec);
        return buildCsv(bookings);
    }

    private static String buildCsv(List<BookingEntity> bookings) {
        String[] header = {
            "ID", "Room", "Guest", "Email", "Phone", "Check-in", "Check-out", "Total", "Status", "Payment note", "Created at"
        };
        StringBuilder csv = new StringBuilder();
        appendRow(csv, header);
        for (BookingEntity b : bookings) {
            appendRow(
                    csv,
                    new String[] {
                        b.getId(),
                        b.getRoom().getName(),
                        b.getGuestName(),
                        b.getGuestEmail(),
                        b.getGuestPhone(),
                        b.getCheckIn().toString(),
                        b.getCheckOut().toString(),
                        b.getTotalPrice().setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString(),
                        b.getStatus().getValue(),
                        b.getPaymentNote() != null ? b.getPaymentNote() : "",
                        DateRangeUtil.formatIsoInstant(b.getCreatedAt())
                    });
        }
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, String[] fields) {
        if (!csv.isEmpty()) {
            csv.append("\r\n");
        }
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvEscape(fields[i]));
        }
    }

    private static String csvEscape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static boolean isSerializationFailure(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                    && SERIALIZATION_FAILURE_SQLSTATE.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
