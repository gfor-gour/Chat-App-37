package com.raven.server.service;

import com.raven.server.repository.UserRepository;
import com.raven.shared.dto.LoginRequest;
import com.raven.shared.dto.RegisterRequest;
import com.raven.shared.dto.ServiceResponse;
import com.raven.shared.dto.UserAccountDto;
import at.favre.lib.crypto.bcrypt.BCrypt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void testRegister_Success() throws SQLException {
        RegisterRequest req = new RegisterRequest();
        req.setUserName("ValidUser");
        req.setPassword("Password123");

        when(userRepository.existsByUserName("ValidUser")).thenReturn(false);
        when(userRepository.insertUser(eq("ValidUser"), anyString())).thenReturn(1);
        
        ServiceResponse response = userService.register(req);
        
        assertTrue(response.isSuccess());
        UserAccountDto user = (UserAccountDto) response.getData();
        assertEquals(1, user.getUserID());
        assertEquals("ValidUser", user.getUserName());
        
        verify(userRepository, times(1)).insertUserAccount(1, "ValidUser");
    }

    @Test
    void testRegister_UsernameExists() throws SQLException {
        RegisterRequest req = new RegisterRequest();
        req.setUserName("ExistingUser");
        req.setPassword("Password123");

        when(userRepository.existsByUserName("ExistingUser")).thenReturn(true);
        
        ServiceResponse response = userService.register(req);
        
        assertFalse(response.isSuccess());
        assertEquals("User Already Exists", response.getMessage());
        
        verify(userRepository, never()).insertUser(anyString(), anyString());
    }

    @Test
    void testLogin_Success() throws SQLException {
        LoginRequest req = new LoginRequest();
        req.setUserName("ValidUser");
        req.setPassword("Password123");
        
        String hashed = BCrypt.withDefaults().hashToString(12, "Password123".toCharArray());
        UserAccountDto mockUser = new UserAccountDto(1, "ValidUser", "", "", true);
        UserRepository.UserRecord record = new UserRepository.UserRecord(mockUser, hashed);
        
        when(userRepository.findByUserName("ValidUser")).thenReturn(Optional.of(record));
        
        Optional<UserAccountDto> result = userService.login(req);
        
        assertTrue(result.isPresent());
        assertEquals("ValidUser", result.get().getUserName());
    }

    @Test
    void testLogin_InvalidPassword() throws SQLException {
        LoginRequest req = new LoginRequest();
        req.setUserName("ValidUser");
        req.setPassword("WrongPassword");
        
        String hashed = BCrypt.withDefaults().hashToString(12, "Password123".toCharArray());
        UserAccountDto mockUser = new UserAccountDto(1, "ValidUser", "", "", true);
        UserRepository.UserRecord record = new UserRepository.UserRecord(mockUser, hashed);
        
        when(userRepository.findByUserName("ValidUser")).thenReturn(Optional.of(record));
        
        Optional<UserAccountDto> result = userService.login(req);
        
        assertFalse(result.isPresent());
    }
}
