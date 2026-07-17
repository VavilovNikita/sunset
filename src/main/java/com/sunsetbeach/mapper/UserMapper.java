package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDto(UserEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getRole(), TimestampFormat.toUtc(entity.getCreatedAt()));
    }
}
