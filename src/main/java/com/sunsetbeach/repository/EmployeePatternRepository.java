package com.sunsetbeach.repository;

import com.sunsetbeach.entity.EmployeePatternEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePatternRepository extends JpaRepository<EmployeePatternEntity, String> {
}
