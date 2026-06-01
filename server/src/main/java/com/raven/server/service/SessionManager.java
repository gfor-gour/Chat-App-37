package com.raven.server.service;

import com.corundumstudio.socketio.SocketIOClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    
    // Maps UserID -> Thread-safe Set of SocketIOClient connections
    private final ConcurrentHashMap<Integer, Set<SocketIOClient>> sessions = new ConcurrentHashMap<>();
    
    // Maps Socket.IO Session ID -> UserID (for quick disconnect lookup)
    private final ConcurrentHashMap<UUID, Integer> sessionToUser = new ConcurrentHashMap<>();

    public void addSession(int userID, SocketIOClient client) {
        sessions.computeIfAbsent(userID, k -> ConcurrentHashMap.newKeySet()).add(client);
        sessionToUser.put(client.getSessionId(), userID);
        log.info("Registered session: user={}, totalActiveSessions={}", userID, sessions.get(userID).size());
    }

    /**
     * Removes a session. Returns the UserID if the user has no more active sessions, or -1 if the user remains online.
     */
    public int removeSession(SocketIOClient client) {
        UUID sessionId = client.getSessionId();
        Integer userID = sessionToUser.remove(sessionId);
        if (userID != null) {
            Set<SocketIOClient> userSessions = sessions.get(userID);
            if (userSessions != null) {
                userSessions.remove(client);
                log.info("Deregistered session: user={}, remainingSessions={}", userID, userSessions.size());
                if (userSessions.isEmpty()) {
                    sessions.remove(userID);
                    return userID; // User is now fully offline
                }
            }
        }
        return -1;
    }

    public void sendToUser(int userID, String event, Object data) {
        Set<SocketIOClient> userSessions = sessions.get(userID);
        if (userSessions != null && !userSessions.isEmpty()) {
            log.debug("Delivering event '{}' to user={} across {} sessions", event, userID, userSessions.size());
            for (SocketIOClient client : userSessions) {
                try {
                    client.sendEvent(event, data);
                } catch (Exception e) {
                    log.error("Failed to send event to client session user={}, session={}", userID, client.getSessionId(), e);
                }
            }
        } else {
            log.warn("Attempted to send to offline user={}", userID);
        }
    }

    public boolean isOnline(int userID) {
        Set<SocketIOClient> userSessions = sessions.get(userID);
        return userSessions != null && !userSessions.isEmpty();
    }

    public Set<SocketIOClient> getSessions(int userID) {
        return sessions.getOrDefault(userID, Collections.emptySet());
    }

    public int getActiveUserCount() {
        return sessions.size();
    }
}
