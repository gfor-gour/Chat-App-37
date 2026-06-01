package com.raven.server.handler;

import com.corundumstudio.socketio.SocketIOServer;
import com.raven.server.service.SessionManager;
import com.raven.server.service.UserService;
import com.raven.shared.dto.LoginRequest;
import com.raven.shared.dto.RegisterRequest;
import com.raven.shared.dto.ServiceResponse;
import com.raven.shared.dto.UserAccountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthEventHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthEventHandler.class);
    private final UserService userService;
    private final SessionManager sessionManager;

    public AuthEventHandler(UserService userService, SessionManager sessionManager) {
        this.userService = userService;
        this.sessionManager = sessionManager;
    }

    public void registerListeners(SocketIOServer server) {
        // ── REGISTER EVENT ──────────────────────────────────────────────────
        server.addEventListener("register", RegisterRequest.class, (client, data, ack) -> {
            log.info("Registration request received: user='{}'", data.getUserName());
            try {
                ServiceResponse response = userService.register(data);
                if (response.isAction()) {
                    UserAccountDto registeredUser = (UserAccountDto) response.getData();
                    // Send ACK back to registering client
                    ack.sendAckData(true, response.getMessage(), registeredUser);
                    
                    // Broadcast the new user to all currently connected clients
                    server.getBroadcastOperations().sendEvent("list_user", registeredUser);
                    
                    // Automatically add the new user session
                    sessionManager.addSession(registeredUser.getUserID(), client);
                } else {
                    ack.sendAckData(false, response.getMessage(), null);
                }
            } catch (Exception e) {
                log.error("Error during user registration", e);
                ack.sendAckData(false, "Server Error", null);
            }
        });

        // ── LOGIN EVENT ─────────────────────────────────────────────────────
        server.addEventListener("login", LoginRequest.class, (client, data, ack) -> {
            log.info("Login request received: user='{}'", data.getUserName());
            try {
                Optional<UserAccountDto> userOpt = userService.login(data);
                if (userOpt.isPresent()) {
                    UserAccountDto user = userOpt.get();
                    ack.sendAckData(true, user);
                    
                    // Add the session
                    sessionManager.addSession(user.getUserID(), client);
                    
                    // Broadcast status change to everyone
                    server.getBroadcastOperations().sendEvent("user_status", user.getUserID(), true);
                } else {
                    ack.sendAckData(false);
                }
            } catch (Exception e) {
                log.error("Error during user login", e);
                ack.sendAckData(false);
            }
        });
    }
}
