package com.raven.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raven.client.connection.ConnectionManager;
import com.raven.client.util.SwingUtils;
import com.raven.shared.dto.LoginRequest;
import com.raven.shared.dto.RegisterRequest;
import com.raven.shared.dto.UserAccountDto;
import io.socket.client.Ack;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static AuthService instance;
    private final ObjectMapper mapper = new ObjectMapper();
    private UserAccountDto currentUser;

    private AuthService() {
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public void login(LoginRequest request, Consumer<UserAccountDto> onSuccess, Consumer<String> onError) {
        Socket socket = ConnectionManager.getInstance().getSocket();
        if (socket == null) {
            SwingUtils.runOnEDT(() -> onError.accept("Not connected to server"));
            return;
        }

        try {
            // Serialize using Jackson DTO, wrap in org.json.JSONObject for socket.io-client
            JSONObject json = new JSONObject(mapper.writeValueAsString(request));
            log.info("Sending login request for user: {}", request.getUserName());

            socket.emit("login", json, (Ack) args -> {
                try {
                    if (args.length > 0 && (boolean) args[0]) {
                        // Success
                        UserAccountDto user = mapper.readValue(args[1].toString(), UserAccountDto.class);
                        currentUser = user;
                        log.info("Login successful for: {}", user.getUserName());
                        SwingUtils.runOnEDT(() -> onSuccess.accept(user));
                    } else {
                        log.warn("Login failed: invalid credentials.");
                        SwingUtils.runOnEDT(() -> onError.accept("Invalid Username or Password"));
                    }
                } catch (Exception e) {
                    log.error("Failed to parse login response", e);
                    SwingUtils.runOnEDT(() -> onError.accept("Server response error"));
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize login request", e);
            SwingUtils.runOnEDT(() -> onError.accept("Serialization error"));
        }
    }

    public void register(RegisterRequest request, Consumer<UserAccountDto> onSuccess, Consumer<String> onError) {
        Socket socket = ConnectionManager.getInstance().getSocket();
        if (socket == null) {
            SwingUtils.runOnEDT(() -> onError.accept("Not connected to server"));
            return;
        }

        try {
            JSONObject json = new JSONObject(mapper.writeValueAsString(request));
            log.info("Sending registration request for user: {}", request.getUserName());

            socket.emit("register", json, (Ack) args -> {
                try {
                    if (args.length > 0 && (boolean) args[0]) {
                        // Success (ACK: success, message, userAccount)
                        UserAccountDto user = mapper.readValue(args[2].toString(), UserAccountDto.class);
                        currentUser = user;
                        log.info("Registration successful for: {}", user.getUserName());
                        SwingUtils.runOnEDT(() -> onSuccess.accept(user));
                    } else {
                        String errMsg = (args.length > 1) ? args[1].toString() : "Registration failed";
                        log.warn("Registration failed: {}", errMsg);
                        SwingUtils.runOnEDT(() -> onError.accept(errMsg));
                    }
                } catch (Exception e) {
                    log.error("Failed to parse registration response", e);
                    SwingUtils.runOnEDT(() -> onError.accept("Server response error"));
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize registration request", e);
            SwingUtils.runOnEDT(() -> onError.accept("Serialization error"));
        }
    }

    public UserAccountDto getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserAccountDto currentUser) {
        this.currentUser = currentUser;
    }
}
