package com.finance.service;

import com.finance.dto.request.CreateUserRequest;
import com.finance.dto.request.UpdateUserRequest;
import com.finance.dto.response.UserResponse;
import com.finance.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
    User getCurrentUser();
}
