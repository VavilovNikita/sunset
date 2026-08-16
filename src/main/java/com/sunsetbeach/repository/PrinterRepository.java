package com.sunsetbeach.repository;

import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.model.PrinterDepartment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrinterRepository extends JpaRepository<PrinterEntity, String> {

    /** At most one row can ever match, per the {@code Printer_one_active_per_department} unique index. */
    Optional<PrinterEntity> findByDepartmentAndIsActiveTrue(PrinterDepartment department);
}
