package com.sunsetbeach.repository;

import com.sunsetbeach.entity.EmployeePatternEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePatternRepository extends JpaRepository<EmployeePatternEntity, String> {

    List<EmployeePatternEntity> findByDefaultShiftCodeIdIn(Collection<String> shiftCodeIds);
}
