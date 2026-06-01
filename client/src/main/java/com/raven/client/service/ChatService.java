package com.raven.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raven.client.connection.ConnectionManager;
import com.raven.client.connection.ConnectionState;
import com.raven.client.util.SwingUtils;
import com.raven.event.PublicEvent;
import com.raven.shared.dto.ReceiveMessageResponse;
import com.raven.shared.dto.SendMessageRequest;
import com.raven.shared.dto.UserAccountDto;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static ChatService instance;
    private final ObjectMapper mapper = new ObjectMapper();

    private ChatService() {
        // Register connection state listener to automatically register socket listeners on connect
        ConnectionManager.getInstance().addStateListener(state -> {
            if (state == ConnectionState.CONNECTED) {
                registerSocketListeners();
            }
        });
    }

    public static synchronized ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }
        return instance;
    }

    public void registerSocketListeners() {
        Socket socket = ConnectionManager.getInstance().getSocket();
        if (socket == null) {
            return;
        }

        log.info("Registering chat socket listeners...");

        // ── LIST USER EVENT ─────────────────────────────────────────────────
        socket.off("list_user"); // Remove stale listener
        socket.on("list_user", args -> {
            log.info("Received 'list_user' broadcast from server, count={}", args.length);
            List<UserAccountDto> users = new ArrayList<>();
            for (Object arg : args) {
                try {
                    UserAccountDto u = mapper.readValue(arg.toString(), UserAccountDto.class);
                    // Filter out current user from the list
                    UserAccountDto currentUser = AuthService.getInstance().getCurrentUser();
                    if (currentUser == null || u.getUserID() != currentUser.getUserID()) {
                        users.add(u);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse user in list_user", e);
                }
            }
            // Execute Swing UI update on the Event Dispatch Thread (fixes G1)
            SwingUtils.runOnEDT(() -> {
                PublicEvent.getInstance().getEventMenuLeft().newUser(users);
            });
        });

        // ── USER STATUS BROADCAST EVENT ──────────────────────────────────────
        socket.off("user_status");
        socket.on("user_status", args -> {
            if (args.length >= 2) {
                int userID = (Integer) args[0];
                boolean status = (Boolean) args[1];
                log.info("User status update: userID={}, online={}", userID, status);
                
                SwingUtils.runOnEDT(() -> {
                    if (status) {
                        PublicEvent.getInstance().getEventMenuLeft().userConnect(userID);
                    } else {
                        PublicEvent.getInstance().getEventMenuLeft().userDisconnect(userID);
                    }
                });
            }
        });

        // ── RECEIVE MESSAGE EVENT ───────────────────────────────────────────
        socket.off("receive_ms");
        socket.on("receive_ms", args -> {
            if (args.length > 0) {
                log.info("Received new message packet: {}", args[0]);
                try {
                    ReceiveMessageResponse message = mapper.readValue(args[0].toString(), ReceiveMessageResponse.class);
                    SwingUtils.runOnEDT(() -> {
                        PublicEvent.getInstance().getEventChat().receiveMessage(message);
                    });
                } catch (Exception e) {
                    log.error("Failed to deserialize received message packet", e);
                }
            }
        });
    }

    public void requestUserList() {
        Socket socket = ConnectionManager.getInstance().getSocket();
        UserAccountDto currentUser = AuthService.getInstance().getCurrentUser();
        if (socket != null && currentUser != null) {
            log.info("Requesting active user list for user id={}", currentUser.getUserID());
            socket.emit("list_user", currentUser.getUserID());
        }
    }

    public void sendMessage(SendMessageRequest request) {
        Socket socket = ConnectionManager.getInstance().getSocket();
        if (socket != null) {
            try {
                JSONObject json = new JSONObject(mapper.writeValueAsString(request));
                log.info("Emitting send_to_user event: {}", request);
                socket.emit("send_to_user", json);
            } catch (Exception e) {
                log.error("Failed to serialize message request", e);
            }
        }
    }
}
