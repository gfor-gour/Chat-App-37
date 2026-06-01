package com.raven.client.ui.sidebar;

import com.raven.client.ui.theme.AppTheme;
import com.raven.shared.dto.UserAccountDto;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class UserItemPanel extends JPanel {
    private final UserAccountDto user;
    private final JLabel lblName;
    private final JLabel lblStatusText;
    private final StatusDot statusDot;
    private boolean isSelected = false;

    public UserItemPanel(UserAccountDto user, Consumer<UserAccountDto> onClick) {
        this.user = user;
        setLayout(new MigLayout("fillx, insets 10", "[42!]10[fill]"));
        setBackground(AppTheme.BG_SECONDARY);
        
        JLabel lblAvatar = new JLabel(user.getUserName().substring(0, 1).toUpperCase(), SwingConstants.CENTER);
        lblAvatar.setOpaque(true);
        lblAvatar.setBackground(AppTheme.ACCENT);
        lblAvatar.setForeground(Color.WHITE);
        lblAvatar.setFont(AppTheme.FONT_TITLE);
        lblAvatar.setPreferredSize(new Dimension(AppTheme.AVATAR_SIZE, AppTheme.AVATAR_SIZE));
        
        lblName = new JLabel(user.getUserName());
        lblName.setFont(AppTheme.FONT_BODY);
        lblName.setForeground(AppTheme.TEXT_PRIMARY);
        
        lblStatusText = new JLabel(user.isStatus() ? "Online" : "Offline");
        lblStatusText.setFont(AppTheme.FONT_CAPTION);
        lblStatusText.setForeground(AppTheme.TEXT_SECONDARY);
        
        statusDot = new StatusDot(user.isStatus() ? AppTheme.SUCCESS : AppTheme.TEXT_SECONDARY);
        
        JPanel pnlText = new JPanel(new MigLayout("insets 0, gap 0", "[fill]"));
        pnlText.setOpaque(false);
        pnlText.add(lblName, "wrap");
        
        JPanel pnlStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pnlStatus.setOpaque(false);
        pnlStatus.add(statusDot);
        pnlStatus.add(lblStatusText);
        
        pnlText.add(pnlStatus);
        
        add(lblAvatar);
        add(pnlText);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) setBackground(AppTheme.BG_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected) setBackground(AppTheme.BG_SECONDARY);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                onClick.accept(user);
            }
        });
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        setBackground(selected ? AppTheme.BG_HOVER : AppTheme.BG_SECONDARY);
    }
    
    public void updateStatus(boolean isOnline) {
        user.setStatus(isOnline);
        lblStatusText.setText(isOnline ? "Online" : "Offline");
        statusDot.setColor(isOnline ? AppTheme.SUCCESS : AppTheme.TEXT_SECONDARY);
    }
    
    public UserAccountDto getUser() { return user; }

    private static class StatusDot extends JPanel {
        private Color color;

        public StatusDot(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(8, 8));
            setOpaque(false);
        }

        public void setColor(Color color) {
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
