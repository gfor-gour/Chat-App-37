package com.raven.client.ui.common;

import com.raven.client.connection.ConnectionState;
import com.raven.client.ui.theme.AppTheme;
import com.raven.client.util.SwingUtils;

import javax.swing.*;
import java.awt.*;

public class ConnectionStatusBar extends JPanel {
    private final JLabel statusText;
    private final StatusIcon statusIcon;
    private final Timer hideTimer;

    public ConnectionStatusBar() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));
        setBackground(AppTheme.BG_SURFACE);
        
        statusIcon = new StatusIcon();
        statusText = new JLabel("Disconnected");
        statusText.setFont(AppTheme.FONT_CAPTION);
        statusText.setForeground(AppTheme.TEXT_PRIMARY);
        
        add(statusIcon);
        add(statusText);
        
        hideTimer = new Timer(3000, e -> setVisible(false));
        hideTimer.setRepeats(false);
        
        // Initial state
        updateState(ConnectionState.DISCONNECTED);
    }
    
    public void updateState(ConnectionState state) {
        SwingUtils.runOnEDT(() -> {
            setVisible(true);
            hideTimer.stop();
            
            switch (state) {
                case CONNECTED:
                    statusText.setText("Connected");
                    statusIcon.setColor(AppTheme.SUCCESS);
                    hideTimer.start();
                    break;
                case CONNECTING:
                    statusText.setText("Connecting...");
                    statusIcon.setColor(AppTheme.WARNING);
                    break;
                case RECONNECTING:
                    statusText.setText("Reconnecting...");
                    statusIcon.setColor(AppTheme.WARNING);
                    break;
                case DISCONNECTED:
                default:
                    statusText.setText("Disconnected");
                    statusIcon.setColor(AppTheme.DANGER);
                    break;
            }
        });
    }

    private static class StatusIcon extends JPanel {
        private Color color = AppTheme.DANGER;

        public StatusIcon() {
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
