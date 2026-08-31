package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * One "room X from date A to date B" leg of a booking's stay - see V18__booking_segments.sql's
 * comment for the full model rationale. {@code booking} is a lazy {@code @ManyToOne} (not the
 * owning side of a collection on {@link BookingEntity}) purely so Spring Data query derivation
 * can traverse {@code Booking_status} in {@link com.sunsetbeach.repository.BookingSegmentRepository}
 * - segments are always loaded/queried directly by {@code bookingId}, never through a lazy
 * collection on the booking itself.
 */
@Entity
@Table(name = "BookingSegment")
public class BookingSegmentEntity {

    @Id
    @UuidGenerator
    private String id;

    private String bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId", insertable = false, updatable = false)
    private BookingEntity booking;

    private String roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId", insertable = false, updatable = false)
    private RoomEntity room;

    private String roomUnitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomUnitId", insertable = false, updatable = false)
    private RoomUnitEntity roomUnit;

    private LocalDate checkIn;

    private LocalDate checkOut;

    private BigDecimal totalPrice;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public BookingEntity getBooking() {
        return booking;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public String getRoomUnitId() {
        return roomUnitId;
    }

    public void setRoomUnitId(String roomUnitId) {
        this.roomUnitId = roomUnitId;
    }

    public RoomUnitEntity getRoomUnit() {
        return roomUnit;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
