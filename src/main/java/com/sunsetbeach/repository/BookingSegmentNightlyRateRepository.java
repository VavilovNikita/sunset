package com.sunsetbeach.repository;

import com.sunsetbeach.entity.BookingSegmentNightlyRateEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSegmentNightlyRateRepository extends JpaRepository<BookingSegmentNightlyRateEntity, String> {

    List<BookingSegmentNightlyRateEntity> findBySegmentIdOrderByDateAsc(String segmentId);

    /** The frozen nightly prices of many segments at once - the booking tracker prices every sold night from them. */
    List<BookingSegmentNightlyRateEntity> findBySegmentIdIn(java.util.Collection<String> segmentIds);

    /** Drops specific nights' rows - a schedule change shrinking a segment, or a reprice's before-state cleanup. */
    void deleteBySegmentIdAndDateIn(String segmentId, List<LocalDate> dates);
}
