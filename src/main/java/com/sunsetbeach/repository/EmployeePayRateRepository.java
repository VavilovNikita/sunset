package com.sunsetbeach.repository;

import com.sunsetbeach.entity.EmployeePayRateEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePayRateRepository extends JpaRepository<EmployeePayRateEntity, String> {

    List<EmployeePayRateEntity> findByEmployeeUserIdOrderByEffectiveFrom(String employeeUserId);

    List<EmployeePayRateEntity> findByEmployeeUserIdInOrderByEffectiveFrom(java.util.Collection<String> employeeUserIds);
}
