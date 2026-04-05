package com.finance.service;

import com.finance.dto.request.CreateUserRequest;
import com.finance.dto.request.UpdateUserRequest;
import com.finance.dto.response.UserResponse;
import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.exception.DuplicateResourceException;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.UserRepository;
import com.finance.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .password("encoded_password")
                .role(Role.ANALYST)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- createUser ---

    @Test
    @DisplayName("createUser: should create user successfully")
    void createUser_success() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole(Role.ANALYST);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRole()).isEqualTo(Role.ANALYST);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser: should throw DuplicateResourceException when email already exists")
    void createUser_duplicateEmail_throwsException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole(Role.VIEWER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any());
    }

    // --- getUserById ---

    @Test
    @DisplayName("getUserById: should return user when found")
    void getUserById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("getUserById: should throw ResourceNotFoundException when not found")
    void getUserById_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    // --- getAllUsers ---

    @Test
    @DisplayName("getAllUsers: should return all users")
    void getAllUsers_returnsList() {
        User admin = User.builder()
                .id(2L).fullName("Admin").email("admin@finance.com")
                .role(Role.ADMIN).active(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findAll()).thenReturn(List.of(sampleUser, admin));

        List<UserResponse> users = userService.getAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserResponse::getEmail)
                .containsExactlyInAnyOrder("john@example.com", "admin@finance.com");
    }

    // --- updateUser ---

    @Test
    @DisplayName("updateUser: should update fields correctly")
    void updateUser_success() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Jane Doe");
        request.setRole(Role.ADMIN);
        request.setActive(false);

        User updated = User.builder()
                .id(1L).fullName("Jane Doe").email("john@example.com")
                .role(Role.ADMIN).active(false)
                .createdAt(sampleUser.getCreatedAt()).updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(updated);

        UserResponse response = userService.updateUser(1L, request);

        assertThat(response.getFullName()).isEqualTo("Jane Doe");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.isActive()).isFalse();
    }

    @Test
    @DisplayName("updateUser: should only update provided fields (partial update)")
    void updateUser_partialUpdate() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        // role and active left null — should not be changed

        User updated = User.builder()
                .id(1L).fullName("Updated Name").email("john@example.com")
                .role(Role.ANALYST).active(true)
                .createdAt(sampleUser.getCreatedAt()).updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(updated);

        UserResponse response = userService.updateUser(1L, request);

        assertThat(response.getFullName()).isEqualTo("Updated Name");
        assertThat(response.getRole()).isEqualTo(Role.ANALYST);
        assertThat(response.isActive()).isTrue();
    }

    // --- deleteUser ---

    @Test
    @DisplayName("deleteUser: should soft-delete (deactivate) user")
    void deleteUser_softDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.deleteUser(1L);

        assertThat(sampleUser.isActive()).isFalse();
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("deleteUser: should throw ResourceNotFoundException when user not found")
    void deleteUser_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }
}
