package com.sunsetbeach.service;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.UserMapper;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.model.UserRoleUpdateInput;
import com.sunsetbeach.repository.UserRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public User getById(String id) {
        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toDto(entity);
    }

    @Transactional
    public User create(UserCreateInput input) {
        UserEntity entity = new UserEntity();
        entity.setEmail(input.getEmail().trim());
        entity.setPasswordHash(passwordEncoder.encode(input.getPassword()));
        entity.setRole(input.getRole() != null ? input.getRole() : Role.MANAGER);

        try {
            return userMapper.toDto(userRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A user with that email already exists");
        }
    }

    @Transactional
    public User updateRole(String id, String callerId, UserRoleUpdateInput input) {
        if (id.equals(callerId)) {
            throw new BadRequestException("You can't change your own role");
        }

        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        entity.setRole(input.getRole());
        return userMapper.toDto(userRepository.save(entity));
    }
}
