package com.sunsetbeach.repository;

import com.sunsetbeach.entity.JournalEntryEntity;
import com.sunsetbeach.model.JournalSourceType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, String> {

    List<JournalEntryEntity> findByEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(LocalDate from, LocalDate to);

    List<JournalEntryEntity> findBySourceTypeAndSourceIdOrderByCreatedAtAsc(JournalSourceType sourceType, String sourceId);

    boolean existsByReversesEntryId(String reversesEntryId);

    List<JournalEntryEntity> findByReversesEntryIdIn(Collection<String> reversesEntryIds);
}
