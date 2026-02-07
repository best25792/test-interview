package com.example.userservice.service;

import com.example.userservice.dto.request.CreateUserRequest;
import com.example.userservice.entity.User;
import com.example.userservice.exception.PaymentException;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest createUserRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("testuser");
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setPhoneNumber("1234567890");
        createUserRequest.setIsActive(true);
        createUserRequest.setIsVerified(false);

        savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .phoneNumber("1234567890")
                .isActive(true)
                .isVerified(false)
                .build();
    }

    @Test
    void testCreateUser_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser(createUserRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_UsernameAlreadyExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            userService.createUser(createUserRequest);
        });

        assertEquals("Username already exists: testuser", exception.getMessage());
        verify(userRepository).findByUsername("testuser");
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_EmailAlreadyExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedUser));

        // When & Then
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            userService.createUser(createUserRequest);
        });

        assertEquals("Email already exists: test@example.com", exception.getMessage());
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_WithDefaultValues() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPhoneNumber("9876543210");
        // isActive and isVerified are null

        User userWithDefaults = User.builder()
                .id(2L)
                .username("newuser")
                .email("new@example.com")
                .phoneNumber("9876543210")
                .isActive(true)  // Default value
                .isVerified(false)  // Default value
                .build();

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(userWithDefaults);

        // When
        User result = userService.createUser(request);

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertTrue(result.getIsActive());
        assertFalse(result.getIsVerified());
    }
}
