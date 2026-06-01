package com.raven.server.handler;

import com.corundumstudio.socketio.SocketIOServer;
import com.raven.server.service.FileService;
import com.raven.server.service.SessionManager;
import com.raven.server.service.UserService;
import com.raven.shared.dto.*;
import com.raven.shared.enums.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageEventHandler {
    private static final Logger log = LoggerFactory.getLogger(MessageEventHandler.class);
    private final UserService userService;
    private final FileService fileService;
    private final SessionManager sessionManager;

    public MessageEventHandler(UserService userService, FileService fileService, SessionManager sessionManager) {
        this.userService = userService;
        this.fileService = fileService;
        this.sessionManager = sessionManager;
    }

    public void registerListeners(SocketIOServer server) {
        // ── LIST USER EVENT ─────────────────────────────────────────────────
        server.addEventListener("list_user", Integer.class, (client, userID, ack) -> {
            log.debug("Fetch user list requested by user id={}", userID);
            try {
                List<UserAccountDto> list = userService.getUsers(userID, sessionManager);
                client.sendEvent("list_user", list.toArray());
            } catch (Exception e) {
                log.error("Failed to fetch user list for user id={}", userID, e);
            }
        });

        // ── SEND TO USER EVENT (Core messaging handler) ──────────────────────
        server.addEventListener("send_to_user", SendMessageRequest.class, (client, data, ack) -> {
            log.info("Message event: from={}, to={}, type={}", data.getFromUserID(), data.getToUserID(), data.getMessageType());
            try {
                if (data.getMessageType() == MessageType.FILE || data.getMessageType() == MessageType.IMAGE) {
                    // Pre-upload handshake for files/images
                    FileMetadata metadata = fileService.registerFileReceiver(data.getText());
                    fileService.initReceiver(metadata, data);
                    
                    // Return generated fileID back to sender as ACK
                    ack.sendAckData(metadata.getFileID());
                } else {
                    // Standard Text or Emoji message delivery
                    ReceiveMessageResponse response = new ReceiveMessageResponse(
                            data.getMessageType(),
                            data.getFromUserID(),
                            data.getText(),
                            null
                    );
                    
                    // Multi-session routing (delivers to all sessions of receiver - fixes C6)
                    sessionManager.sendToUser(data.getToUserID(), "receive_ms", response);
                }
            } catch (Exception e) {
                log.error("Failed to route message from={} to={}", data.getFromUserID(), data.getToUserID(), e);
            }
        });
    }
}
