package com.raven.server.handler;

import com.corundumstudio.socketio.SocketIOServer;
import com.raven.server.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class ConnectionEventHandler {
    private static final Logger log = LoggerFactory.getLogger(ConnectionEventHandler.class);
    private final SessionManager sessionManager;
    private final JTextArea consoleTextArea;

    public ConnectionEventHandler(SessionManager sessionManager, JTextArea consoleTextArea) {
        this.sessionManager = sessionManager;
        this.consoleTextArea = consoleTextArea;
    }

    public void registerListeners(SocketIOServer server) {
        // ── ON CONNECT LISTENER ─────────────────────────────────────────────
        server.addConnectListener(client -> {
            String logMsg = "Client connected: sessionId=" + client.getSessionId() + ", address=" + client.getRemoteAddress();
            log.info(logMsg);
            appendConsole(logMsg);
        });

        // ── ON DISCONNECT LISTENER ──────────────────────────────────────────
        server.addDisconnectListener(client -> {
            String logMsg = "Client disconnected: sessionId=" + client.getSessionId();
            log.info(logMsg);
            appendConsole(logMsg);

            try {
                int offlineUserID = sessionManager.removeSession(client);
                if (offlineUserID != -1) {
                    // User has no other active sessions, broadcast offline status (fixes C6)
                    server.getBroadcastOperations().sendEvent("user_status", offlineUserID, false);
                    String offlineMsg = "User id=" + offlineUserID + " is now completely OFFLINE.";
                    log.info(offlineMsg);
                    appendConsole(offlineMsg);
                }
            } catch (Exception e) {
                log.error("Error during client disconnection processing", e);
            }
        });
    }

    private void appendConsole(String msg) {
        if (consoleTextArea != null) {
            SwingUtilities.invokeLater(() -> {
                consoleTextArea.append(msg + "\n");
                // Auto-scroll to bottom of console
                consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
            });
        }
    }
}
