package com.raven.client.ui.chat;

import com.raven.client.ui.theme.AppTheme;
import com.raven.shared.dto.UserAccountDto;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class ChatHeaderPanel extends JPanel {
    private final JLabel lblName;
    private final JLabel lblStatus;
    private final JLabel lblAvatar;

    public ChatHeaderPanel() {
        setLayout(new MigLayout("fillx, insets 10 20 10 20", "[42!]15[fill]"));
        setBackground(AppTheme.BG_PRIMARY);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.DIVIDER));
        
        lblAvatar = new JLabel("", SwingConstants.CENTER);
        lblAvatar.setOpaque(true);
        lblAvatar.setBackground(AppTheme.ACCENT);
        lblAvatar.setForeground(Color.WHITE);
        lblAvatar.setFont(AppTheme.FONT_TITLE);
        lblAvatar.setPreferredSize(new Dimension(AppTheme.AVATAR_SIZE, AppTheme.AVATAR_SIZE));
        
        lblName = new JLabel("Select a chat");
        lblName.setFont(AppTheme.FONT_HEADING);
        lblName.setForeground(AppTheme.TEXT_PRIMARY);
        
        lblStatus = new JLabel("");
        lblStatus.setFont(AppTheme.FONT_CAPTION);
        lblStatus.setForeground(AppTheme.TEXT_SECONDARY);
        
        JPanel pnlText = new JPanel(new MigLayout("insets 0, gap 0", "[fill]"));
        pnlText.setOpaque(false);
        pnlText.add(lblName, "wrap");
        pnlText.add(lblStatus);
        
        add(lblAvatar);
        add(pnlText);
        
        setVisible(false);
    }
    
    public void setUser(UserAccountDto user) {
        if (user == null) {
            setVisible(false);
            return;
        }
        lblName.setText(user.getUserName());
        lblStatus.setText(user.isStatus() ? "Online" : "Offline");
        lblAvatar.setText(user.getUserName().substring(0, 1).toUpperCase());
        setVisible(true);
    }
    
    public void updateUserStatus(boolean isOnline) {
        lblStatus.setText(isOnline ? "Online" : "Offline");
    }
}
