package com.sunsetbeach.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;

@Entity
@Table(name = "Booking")
public class BookingEntity {

    @Id
    @UuidGenerator
    private String id;

    private String roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId", insertable = false, updatable = false)
    private RoomEntity room;

    private String roomUnitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomUnitId", insertable = false, updatable = false)
    private RoomUnitEntity roomUnit;

    private String guestName;

    private String guestEmail;

    private String guestPhone;

    private LocalDate checkIn;

    private LocalDate checkOut;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BookingStatus status;

    // Never serialized to the API - see BookingSource's javadoc.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BookingSource source = BookingSource.PUBLIC;

    private String paymentNote;

    // Set once BookingExpiryService has warned staff this booking is about to auto-cancel -
    // prevents the reminder sweep from re-sending the same nudge on every 15-minute pass.
    private boolean expiryReminderSent = false;

    // Physical occupancy - deliberately separate from `status` (commercial only) - see
    // V23__booking_occupancy.sql and BookingOccupancyService's class javadoc. One value per
    // booking, not per segment: a relocation mid-stay never touches this.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OccupancyStatus occupancyStatus = OccupancyStatus.EXPECTED;

    private LocalDateTime checkedInAt;

    private LocalDateTime checkedOutAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
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

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getPaymentNote() {
        return paymentNote;
    }

    public void setPaymentNote(String paymentNote) {
        this.paymentNote = paymentNote;
    }

    public BookingSource getSource() {
        return source;
    }

    public void setSource(BookingSource source) {
        this.source = source;
    }

    public boolean isExpiryReminderSent() {
        return expiryReminderSent;
    }

    public void setExpiryReminderSent(boolean expiryReminderSent) {
        this.expiryReminderSent = expiryReminderSent;
    }

    public OccupancyStatus getOccupancyStatus() {
        return occupancyStatus;
    }

    public void setOccupancyStatus(OccupancyStatus occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public LocalDateTime getCheckedOutAt() {
        return checkedOutAt;
    }

    public void setCheckedOutAt(LocalDateTime checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
