package com.raven.client.connection;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    private static ConnectionManager instance;

    private Socket socket;
    private String host = "localhost";
    private int port = 9999;
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private final List<Consumer<ConnectionState>> stateListeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "socket-reconnector");
        t.setDaemon(true);
        return t;
    });

    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_DELAY_SECONDS = 30;

    private ConnectionManager() {
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public void init(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public synchronized void connect() {
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
            return;
        }

        setState(ConnectionState.CONNECTING);
        try {
            log.info("Connecting to server at http://{}:{}", host, port);
            IO.Options opts = new IO.Options();
            opts.reconnection = false; // We handle reconnection ourselves for fine-grained backoff control
            opts.timeout = 10000;      // 10 second timeout

            socket = IO.socket("http://" + host + ":" + port, opts);

            // Register core connection event handlers
            socket.on(Socket.EVENT_CONNECT, args -> {
                log.info("Socket connected successfully.");
                reconnectAttempts = 0;
                setState(ConnectionState.CONNECTED);
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                log.warn("Socket disconnected.");
                if (state != ConnectionState.DISCONNECTED) {
                    setState(ConnectionState.RECONNECTING);
                    scheduleReconnect();
                }
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                String error = (args.length > 0) ? args[0].toString() : "unknown error";
                log.error("Socket connection error: {}", error);
                if (state == ConnectionState.CONNECTING || state == ConnectionState.RECONNECTING) {
                    setState(ConnectionState.RECONNECTING);
                    scheduleReconnect();
                }
            });

            socket.connect();
        } catch (Exception e) {
            log.error("Exception during socket connection initialization", e);
            setState(ConnectionState.RECONNECTING);
            scheduleReconnect();
        }
    }

    private synchronized void scheduleReconnect() {
        if (state != ConnectionState.RECONNECTING) {
            return;
        }

        // Exponential backoff logic: 2^attempts (e.g. 1s, 2s, 4s, 8s, 16s... up to MAX_RECONNECT_DELAY_SECONDS)
        int delay = Math.min((int) Math.pow(2, reconnectAttempts), MAX_RECONNECT_DELAY_SECONDS);
        reconnectAttempts++;
        log.info("Scheduling reconnection attempt #{} in {} seconds...", reconnectAttempts, delay);

        reconnectScheduler.schedule(() -> {
            synchronized (ConnectionManager.this) {
                if (state == ConnectionState.RECONNECTING) {
                    log.info("Executing reconnection attempt #{}...", reconnectAttempts);
                    if (socket != null) {
                        socket.connect();
                    }
                }
            }
        }, delay, TimeUnit.SECONDS);
    }

    public synchronized void disconnect() {
        log.info("Disconnecting manually from Socket.IO Server...");
        setState(ConnectionState.DISCONNECTED);
        reconnectAttempts = 0;
        if (socket != null) {
            socket.disconnect();
            socket.close();
            socket = null;
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public ConnectionState getState() {
        return state;
    }

    public void addStateListener(Consumer<ConnectionState> listener) {
        stateListeners.add(listener);
        // Proactively notify of current state
        listener.accept(state);
    }

    public void removeStateListener(Consumer<ConnectionState> listener) {
        stateListeners.remove(listener);
    }

    private void setState(ConnectionState newState) {
        if (this.state != newState) {
            ConnectionState oldState = this.state;
            this.state = newState;
            log.info("Connection state changed: {} -> {}", oldState, newState);
            stateListeners.forEach(listener -> {
                try {
                    listener.accept(newState);
                } catch (Exception e) {
                    log.error("Exception in ConnectionState listener", e);
                }
            });
        }
    }

    public void shutdown() {
        disconnect();
        reconnectScheduler.shutdownNow();
    }
}
