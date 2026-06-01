package com.raven.client.ui.chat;

import com.raven.client.ui.theme.AppTheme;
import com.raven.shared.dto.UserAccountDto;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ChatPanel extends JPanel {
    private final ChatHeaderPanel headerPanel;
    private final ChatBodyPanel bodyPanel;
    private final ChatInputPanel inputPanel;
    private final JPanel pnlChat;
    
    private UserAccountDto currentUser;
    
    public ChatPanel(Consumer<String> onSendMessage) {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PRIMARY);
        
        headerPanel = new ChatHeaderPanel();
        bodyPanel = new ChatBodyPanel();
        inputPanel = new ChatInputPanel(onSendMessage);
        
        pnlChat = new JPanel(new BorderLayout());
        pnlChat.setBackground(AppTheme.BG_PRIMARY);
        pnlChat.add(headerPanel, BorderLayout.NORTH);
        pnlChat.add(bodyPanel, BorderLayout.CENTER);
        pnlChat.add(inputPanel, BorderLayout.SOUTH);
        pnlChat.setVisible(false);
        
        JPanel pnlPlaceholder = new JPanel(new BorderLayout());
        pnlPlaceholder.setBackground(AppTheme.BG_PRIMARY);
        JLabel lblPlaceholder = new JLabel("Select a chat to start messaging", SwingConstants.CENTER);
        lblPlaceholder.setFont(AppTheme.FONT_TITLE);
        lblPlaceholder.setForeground(AppTheme.TEXT_SECONDARY);
        pnlPlaceholder.add(lblPlaceholder, BorderLayout.CENTER);
        
        add(pnlPlaceholder, BorderLayout.CENTER);
    }
    
    public void setUser(UserAccountDto user) {
        this.currentUser = user;
        removeAll();
        if (user != null) {
            headerPanel.setUser(user);
            bodyPanel.clear();
            add(pnlChat, BorderLayout.CENTER);
            pnlChat.setVisible(true);
        } else {
            JPanel pnlPlaceholder = new JPanel(new BorderLayout());
            pnlPlaceholder.setBackground(AppTheme.BG_PRIMARY);
            JLabel lblPlaceholder = new JLabel("Select a chat to start messaging", SwingConstants.CENTER);
            lblPlaceholder.setFont(AppTheme.FONT_TITLE);
            lblPlaceholder.setForeground(AppTheme.TEXT_SECONDARY);
            pnlPlaceholder.add(lblPlaceholder, BorderLayout.CENTER);
            add(pnlPlaceholder, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }
    
    public UserAccountDto getCurrentUser() {
        return currentUser;
    }
    
    public ChatHeaderPanel getHeader() { return headerPanel; }
    public ChatBodyPanel getBody() { return bodyPanel; }
    public ChatInputPanel getInput() { return inputPanel; }
}
