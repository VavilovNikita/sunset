package com.sunsetbeach.repository;

import com.sunsetbeach.entity.RestaurantMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantMapRepository extends JpaRepository<RestaurantMapEntity, String> {
}
