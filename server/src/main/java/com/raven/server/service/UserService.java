package com.raven.server.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.raven.server.repository.UserRepository;
import com.raven.shared.dto.LoginRequest;
import com.raven.shared.dto.RegisterRequest;
import com.raven.shared.dto.ServiceResponse;
import com.raven.shared.dto.UserAccountDto;
import com.raven.shared.validation.InputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ServiceResponse register(RegisterRequest request) {
        try {
            InputValidator.validateUserName(request.getUserName());
            InputValidator.validatePassword(request.getPassword());

            if (userRepository.existsByUserName(request.getUserName())) {
                log.warn("Registration failed: username '{}' already exists", request.getUserName());
                return ServiceResponse.failure("User Already Exists");
            }

            // Secure BCrypt password hashing
            String hashedPassword = BCrypt.withDefaults().hashToString(12, request.getPassword().toCharArray());

            int userID = userRepository.insertUser(request.getUserName(), hashedPassword);
            userRepository.insertUserAccount(userID, request.getUserName());

            log.info("User registered successfully: id={}, name='{}'", userID, request.getUserName());
            UserAccountDto account = new UserAccountDto(userID, request.getUserName(), "", "", true);
            return ServiceResponse.success("Ok", account);

        } catch (IllegalArgumentException e) {
            log.warn("Registration validation failed for user '{}': {}", request.getUserName(), e.getMessage());
            return ServiceResponse.failure(e.getMessage());
        } catch (SQLException e) {
            log.error("Database error during registration of user '{}'", request.getUserName(), e);
            return ServiceResponse.failure("Server Error");
        }
    }

    public Optional<UserAccountDto> login(LoginRequest request) {
        try {
            Optional<UserRepository.UserRecord> userOpt = userRepository.findByUserName(request.getUserName());
            if (userOpt.isPresent()) {
                UserRepository.UserRecord record = userOpt.get();
                BCrypt.Result result = BCrypt.verifyer().verify(request.getPassword().toCharArray(), record.getPasswordHash());
                if (result.verified) {
                    log.info("Successful login for user id={}, name='{}'", record.getAccount().getUserID(), request.getUserName());
                    return Optional.of(record.getAccount());
                } else {
                    log.warn("Login failed: incorrect password for user '{}'", request.getUserName());
                }
            } else {
                log.warn("Login failed: user '{}' not found", request.getUserName());
            }
        } catch (SQLException e) {
            log.error("Database error during login of user '{}'", request.getUserName(), e);
        }
        return Optional.empty();
    }

    public List<UserAccountDto> getUsers(int excludeUserID, SessionManager sessionManager) throws SQLException {
        List<UserAccountDto> list = userRepository.findAllExcept(excludeUserID);
        // Map real-time online status from SessionManager
        for (UserAccountDto account : list) {
            account.setStatus(sessionManager.isOnline(account.getUserID()));
        }
        return list;
    }
}
