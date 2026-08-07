package com.sunsetbeach.repository;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByRoleIn(Collection<Role> roles);
}
