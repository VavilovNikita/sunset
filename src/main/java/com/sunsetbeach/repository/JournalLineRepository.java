package com.sunsetbeach.repository;

import com.sunsetbeach.entity.JournalLineEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalLineRepository extends JpaRepository<JournalLineEntity, String> {

    List<JournalLineEntity> findByEntryIdInOrderByEntryIdAscLineOrderAsc(Collection<String> entryIds);

    /** [accountCode, Σ debit, Σ credit] over every line of every entry dated on or before {@code asOf}. */
    @Query("""
            select l.accountCode, sum(l.debit), sum(l.credit)
            from JournalLineEntity l, JournalEntryEntity e
            where l.entryId = e.id and e.entryDate <= :asOf
            group by l.accountCode
            """)
    List<Object[]> sumByAccountAsOf(@Param("asOf") LocalDate asOf);
}
