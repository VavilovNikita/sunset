package com.sunsetbeach.repository;

import com.sunsetbeach.entity.OverstayChargeEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OverstayChargeRepository extends JpaRepository<OverstayChargeEntity, String> {

    List<OverstayChargeEntity> findByBookingIdOrderByDateAsc(String bookingId);

    List<OverstayChargeEntity> findByBookingIdAndVoidedAtIsNull(String bookingId);

    List<OverstayChargeEntity> findByBookingIdInAndVoidedAtIsNull(Collection<String> bookingIds);

    List<OverstayChargeEntity> findByVoidedAtIsNullAndDateBetween(LocalDate from, LocalDate to);

    /** Bookings whose guest has not left and that still carry an active charge - the ones whose stay may have changed under it. */
    @Query("""
        SELECT DISTINCT c.bookingId FROM OverstayChargeEntity c, BookingEntity b
        WHERE c.bookingId = b.id AND c.voidedAt IS NULL AND b.occupancyStatus <> :checkedOut
        """)
    List<String> findBookingIdsWithActiveChargeNotCheckedOut(@org.springframework.data.repository.query.Param("checkedOut") com.sunsetbeach.model.OccupancyStatus checkedOut);
}
