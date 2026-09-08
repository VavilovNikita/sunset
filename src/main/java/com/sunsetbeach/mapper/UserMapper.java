package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.User;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDto(UserEntity entity) {
        List<JobFunction> functions = Arrays.stream(entity.getJobFunctions()).map(JobFunction::fromValue).toList();
        return new User(entity.getId(), entity.getEmail(), entity.getRole(), entity.isActive(), functions, TimestampFormat.toUtc(entity.getCreatedAt()));
    }
}
