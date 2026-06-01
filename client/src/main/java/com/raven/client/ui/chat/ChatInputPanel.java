package com.raven.client.ui.chat;

import com.raven.client.ui.theme.AppTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class ChatInputPanel extends JPanel {
    private final JTextField txtInput;
    private final JButton btnSend;

    public ChatInputPanel(Consumer<String> onSend) {
        setLayout(new MigLayout("fillx, insets 15", "[fill]10[40!]"));
        setBackground(AppTheme.BG_PRIMARY);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.DIVIDER));
        
        txtInput = new JTextField();
        txtInput.putClientProperty("JTextField.placeholderText", "Type a message...");
        txtInput.putClientProperty("FlatLaf.style", "arc: 20; margin: 5,15,5,15");
        txtInput.setFont(AppTheme.FONT_BODY);
        
        btnSend = new JButton("►");
        btnSend.setBackground(AppTheme.ACCENT);
        btnSend.setForeground(AppTheme.BG_PRIMARY);
        btnSend.setFocusPainted(false);
        btnSend.putClientProperty("FlatLaf.style", "arc: 999");
        
        add(txtInput, "height 40!");
        add(btnSend, "height 40!");
        
        Runnable sendAction = () -> {
            String text = txtInput.getText().trim();
            if (!text.isEmpty()) {
                onSend.accept(text);
                txtInput.setText("");
            }
        };
        
        btnSend.addActionListener(e -> sendAction.run());
        txtInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendAction.run();
                }
            }
        });
    }
}
