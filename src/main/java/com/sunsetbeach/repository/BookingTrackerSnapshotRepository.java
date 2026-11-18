package com.sunsetbeach.repository;

import com.sunsetbeach.entity.BookingTrackerSnapshotEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookingTrackerSnapshotRepository extends JpaRepository<BookingTrackerSnapshotEntity, String> {

    List<BookingTrackerSnapshotEntity> findByStayMonthIn(Collection<String> stayMonths);

    boolean existsBySnapshotDateAndStayMonth(LocalDate snapshotDate, String stayMonth);

    /**
     * Insert-if-absent on the unique {@code (snapshotDate, stayMonth)}: a repeated or racing run, or a catch-up after
     * downtime, writes nothing twice - the database decides, not a check two threads can both pass. Returns 1 when
     * this call wrote the row.
     */
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO \"BookingTrackerSnapshot\" (\"id\", \"snapshotDate\", \"stayMonth\", \"late\", \"roomNights\","
            + " \"netRoomRevenue\", \"occupancyPercent\") VALUES (:id, :snapshotDate, :stayMonth, :late, :roomNights,"
            + " :netRoomRevenue, :occupancyPercent) ON CONFLICT (\"snapshotDate\", \"stayMonth\") DO NOTHING", nativeQuery = true)
    int insertIfAbsent(
            @Param("id") String id,
            @Param("snapshotDate") LocalDate snapshotDate,
            @Param("stayMonth") String stayMonth,
            @Param("late") boolean late,
            @Param("roomNights") int roomNights,
            @Param("netRoomRevenue") BigDecimal netRoomRevenue,
            @Param("occupancyPercent") BigDecimal occupancyPercent);
}
