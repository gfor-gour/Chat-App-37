package com.raven.client.ui.common;

import com.raven.client.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ToastNotification extends JWindow {
    
    public enum Type {
        SUCCESS(AppTheme.SUCCESS),
        WARNING(AppTheme.WARNING),
        ERROR(AppTheme.DANGER);
        
        private final Color color;
        Type(Color color) { this.color = color; }
    }

    private final JLabel label;
    private final Timer timer;

    public ToastNotification(JFrame owner) {
        super(owner);
        setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.BG_SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.DIVIDER, 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        label = new JLabel();
        label.setFont(AppTheme.FONT_BODY);
        
        panel.add(label, BorderLayout.CENTER);
        add(panel);
        setAlwaysOnTop(true);
        
        timer = new Timer(3000, (ActionEvent e) -> setVisible(false));
        timer.setRepeats(false);
    }
    
    public void showToast(String message, Type type) {
        label.setText(message);
        label.setForeground(type.color);
        pack();
        
        if (getOwner() != null) {
            Point p = getOwner().getLocationOnScreen();
            setLocation(p.x + getOwner().getWidth() / 2 - getWidth() / 2, p.y + 50);
        }
        
        setVisible(true);
        timer.restart();
    }
}
