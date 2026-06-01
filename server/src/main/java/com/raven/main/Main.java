package com.raven.main;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.raven.server.config.AppConfig;
import com.raven.server.handler.AuthEventHandler;
import com.raven.server.handler.ConnectionEventHandler;
import com.raven.server.handler.FileEventHandler;
import com.raven.server.handler.MessageEventHandler;
import com.raven.server.repository.FileRepository;
import com.raven.server.repository.UserRepository;
import com.raven.server.service.FileService;
import com.raven.server.service.SessionManager;
import com.raven.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private JScrollPane jScrollPane1;
    private JTextArea txt;

    private SocketIOServer server;
    private HikariDataSource dataSource;
    private FileService fileService;

    public Main() {
        initComponents();
        setupShutdownHook();
    }

    private void initComponents() {
        jScrollPane1 = new JScrollPane();
        txt = new JTextArea();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Socket.IO Chat Server (PostgreSQL)");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent evt) {
                formWindowOpened(evt);
            }
            @Override
            public void windowClosing(WindowEvent evt) {
                shutdownAll();
            }
        });

        txt.setEditable(false);
        txt.setColumns(20);
        txt.setRows(5);
        jScrollPane1.setViewportView(txt);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 879, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }

    private void formWindowOpened(WindowEvent evt) {
        try {
            txt.append("Bootstrapping Server components...\n");
            log.info("Bootstrapping Server components...");

            // 1. Load configuration
            AppConfig config = AppConfig.getInstance();

            // 2. Initialize HikariCP PostgreSQL Connection Pool
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(config.getProperty("db.driver", "org.postgresql.Driver"));
            hikariConfig.setJdbcUrl(config.getProperty("db.url", "jdbc:postgresql://localhost:5432/chat_application"));
            hikariConfig.setUsername(config.getProperty("db.username", "postgres"));
            hikariConfig.setPassword(config.getProperty("db.password", "postgres"));
            hikariConfig.setMaximumPoolSize(config.getInt("db.pool.size", 10));
            
            // Standard performance settings for Postgres
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(hikariConfig);
            txt.append("HikariCP Connection Pool Initialized.\n");
            log.info("HikariCP Connection Pool Initialized.");

            // 3. Construct repositories and services
            UserRepository userRepository = new UserRepository(dataSource);
            FileRepository fileRepository = new FileRepository(dataSource);

            SessionManager sessionManager = new SessionManager();
            UserService userService = new UserService(userRepository);
            fileService = new FileService(fileRepository);

            // 4. Configure Netty-SocketIO server
            Configuration socketConfig = new Configuration();
            socketConfig.setHostname(config.getProperty("server.host", "0.0.0.0"));
            socketConfig.setPort(config.getInt("server.port", 9999));
            socketConfig.setPingInterval(config.getInt("socketio.ping.interval", 25000));
            socketConfig.setPingTimeout(config.getInt("socketio.ping.timeout", 60000));

            // Increase Max Frame Payload size to support file transfers up to configured MB limit
            int maxPayloadSize = config.getInt("file.max.size.mb", 50) * 1024 * 1024;
            socketConfig.setMaxFramePayloadLength(maxPayloadSize);
            socketConfig.setMaxHttpContentLength(maxPayloadSize);

            server = new SocketIOServer(socketConfig);

            // 5. Wire event handlers (separated concerns)
            new AuthEventHandler(userService, sessionManager).registerListeners(server);
            new MessageEventHandler(userService, fileService, sessionManager).registerListeners(server);
            new FileEventHandler(fileService, sessionManager).registerListeners(server);
            new ConnectionEventHandler(sessionManager, txt).registerListeners(server);

            // 6. Start the server
            server.start();
            
            String successMsg = "Socket.IO Chat Server started on port " + socketConfig.getPort() + "\n";
            txt.append(successMsg);
            log.info(successMsg.trim());

        } catch (Exception e) {
            String errMsg = "Critical Server Startup Exception: " + e.getMessage() + "\n";
            txt.append(errMsg);
            log.error("Critical Server Startup Exception", e);
        }
    }

    private synchronized void shutdownAll() {
        log.info("Initiating graceful shutdown...");
        if (server != null) {
            try {
                server.stop();
                log.info("Socket.IO Server stopped.");
            } catch (Exception e) {
                log.error("Error stopping Socket.IO server", e);
            }
            server = null;
        }
        if (fileService != null) {
            try {
                fileService.shutdown();
            } catch (Exception e) {
                log.error("Error shutting down FileService", e);
            }
            fileService = null;
        }
        if (dataSource != null) {
            try {
                dataSource.close();
                log.info("HikariCP Data Source closed.");
            } catch (Exception e) {
                log.error("Error closing Data Source", e);
            }
            dataSource = null;
        }
        log.info("Shutdown completed successfully.");
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownAll, "shutdown-hook"));
    }

    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception ex) {
            log.warn("FlatLaf theme failed to load, falling back to Nimbus", ex);
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        java.awt.EventQueue.invokeLater(() -> new Main().setVisible(true));
    }
}
