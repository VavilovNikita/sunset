package com.sunsetbeach.repository;

import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.model.Zone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableRepository extends JpaRepository<TableEntity, String> {

    List<TableEntity> findByZone(Zone zone);
}
