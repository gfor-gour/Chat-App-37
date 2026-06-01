package com.raven.client.ui;

import com.raven.client.connection.ConnectionManager;
import com.raven.client.service.AuthService;
import com.raven.client.service.ChatService;
import com.raven.client.ui.auth.LoginPanel;
import com.raven.client.ui.auth.RegisterPanel;
import com.raven.client.ui.chat.ChatPanel;
import com.raven.client.ui.chat.MessageBubble;
import com.raven.client.ui.common.ConnectionStatusBar;
import com.raven.client.ui.common.ToastNotification;
import com.raven.client.ui.sidebar.SidebarPanel;
import com.raven.client.ui.theme.AppTheme;
import com.raven.event.EventChat;
import com.raven.event.EventMenuLeft;
import com.raven.event.PublicEvent;
import com.raven.shared.dto.ReceiveMessageResponse;
import com.raven.shared.dto.SendMessageRequest;
import com.raven.shared.dto.UserAccountDto;
import com.raven.shared.enums.MessageType;
import com.raven.client.util.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {
    private final ConnectionManager connectionManager;
    private final AuthService authService;
    private final ChatService chatService;

    private final JPanel pnlRoot;
    private final CardLayout cardLayout;
    
    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    
    private JPanel pnlApp;
    private SidebarPanel sidebarPanel;
    private ChatPanel chatPanel;
    private ConnectionStatusBar statusBar;
    private ToastNotification toast;

    public MainFrame() {
        this.connectionManager = ConnectionManager.getInstance();
        this.authService = AuthService.getInstance();
        this.chatService = ChatService.getInstance();

        setTitle("Chat Application");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(AppTheme.BG_PRIMARY);
        
        cardLayout = new CardLayout();
        pnlRoot = new JPanel(cardLayout);
        pnlRoot.setBackground(AppTheme.BG_PRIMARY);
        setContentPane(pnlRoot);
        
        toast = new ToastNotification(this);
        
        initAuthPanels();
        initAppPanels();
        
        pnlRoot.add(createCenteredPanel(loginPanel), "login");
        pnlRoot.add(createCenteredPanel(registerPanel), "register");
        pnlRoot.add(pnlApp, "app");
        
        cardLayout.show(pnlRoot, "login");
        
        registerEvents();
    }
    
    private JPanel createCenteredPanel(JPanel panel) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(AppTheme.BG_PRIMARY);
        wrapper.add(panel);
        return wrapper;
    }

    private void initAuthPanels() {
        loginPanel = new LoginPanel(req -> {
            loginPanel.setInputsEnabled(false);
            authService.login(req, user -> {
                loginPanel.setInputsEnabled(true);
                toast.showToast("Login successful", ToastNotification.Type.SUCCESS);
                onLoginSuccess(user);
            }, error -> {
                loginPanel.setInputsEnabled(true);
                loginPanel.showError(error);
            });
        }, () -> cardLayout.show(pnlRoot, "register"));
        
        registerPanel = new RegisterPanel(req -> {
            registerPanel.setInputsEnabled(false);
            authService.register(req, user -> {
                registerPanel.setInputsEnabled(true);
                toast.showToast("Registration successful", ToastNotification.Type.SUCCESS);
                onLoginSuccess(user);
            }, error -> {
                registerPanel.setInputsEnabled(true);
                registerPanel.showError(error);
            });
        }, () -> cardLayout.show(pnlRoot, "login"));
    }

    private void initAppPanels() {
        pnlApp = new JPanel(new BorderLayout());
        pnlApp.setBackground(AppTheme.BG_PRIMARY);
        
        statusBar = new ConnectionStatusBar();
        connectionManager.addStateListener(statusBar::updateState);
        
        chatPanel = new ChatPanel(text -> {
            UserAccountDto current = chatPanel.getCurrentUser();
            if (current != null) {
                SendMessageRequest req = new SendMessageRequest();
                req.setToUserId(current.getUserID());
                req.setText(text);
                req.setMessageType(MessageType.TEXT);
                
                chatService.sendMessage(req);
                chatPanel.getBody().addMessage(text, MessageBubble.Alignment.RIGHT);
            }
        });
        
        sidebarPanel = new SidebarPanel(user -> {
            chatPanel.setUser(user);
        });
        
        pnlApp.add(statusBar, BorderLayout.NORTH);
        pnlApp.add(sidebarPanel, BorderLayout.WEST);
        pnlApp.add(chatPanel, BorderLayout.CENTER);
    }
    
    private void registerEvents() {
        PublicEvent.getInstance().addEventMenuLeft(new EventMenuLeft() {
            @Override
            public void newUser(List<UserAccountDto> users) {
                sidebarPanel.setUsers(users);
            }
            @Override
            public void userConnect(int userID) {
                sidebarPanel.updateUserStatus(userID, true);
                if (chatPanel.getCurrentUser() != null && chatPanel.getCurrentUser().getUserID() == userID) {
                    chatPanel.getHeader().updateUserStatus(true);
                }
            }
            @Override
            public void userDisconnect(int userID) {
                sidebarPanel.updateUserStatus(userID, false);
                if (chatPanel.getCurrentUser() != null && chatPanel.getCurrentUser().getUserID() == userID) {
                    chatPanel.getHeader().updateUserStatus(false);
                }
            }
        });
        
        PublicEvent.getInstance().addEventChat(new EventChat() {
            @Override
            public void sendMessage(SendMessageRequest data) {
            }
            @Override
            public void receiveMessage(ReceiveMessageResponse data) {
                if (chatPanel.getCurrentUser() != null && chatPanel.getCurrentUser().getUserID() == data.getFromUserId()) {
                    chatPanel.getBody().addMessage(data.getText(), MessageBubble.Alignment.LEFT);
                } else {
                    toast.showToast("New message received", ToastNotification.Type.SUCCESS);
                }
            }
        });
    }

    private void onLoginSuccess(UserAccountDto user) {
        cardLayout.show(pnlRoot, "app");
        chatService.requestUserList();
    }
}
