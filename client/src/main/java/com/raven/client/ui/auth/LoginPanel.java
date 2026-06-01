package com.raven.client.ui.auth;

import com.raven.client.ui.theme.AppTheme;
import com.raven.shared.dto.LoginRequest;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class LoginPanel extends JPanel {
    private final JTextField txtUser;
    private final JPasswordField txtPass;
    private final JButton btnLogin;
    private final JButton btnGoRegister;
    private final JLabel lblError;
    
    public LoginPanel(Consumer<LoginRequest> onLogin, Runnable onGoRegister) {
        setLayout(new MigLayout("wrap, fillx, insets 35 45 30 45", "[fill, 250]"));
        setBackground(AppTheme.BG_SURFACE);
        putClientProperty("FlatLaf.style", "arc: 20");
        
        JLabel lblTitle = new JLabel("Login", SwingConstants.CENTER);
        lblTitle.setFont(AppTheme.FONT_HEADING);
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        
        txtUser = new JTextField();
        txtUser.putClientProperty("JTextField.placeholderText", "Username");
        
        txtPass = new JPasswordField();
        txtPass.putClientProperty("JTextField.placeholderText", "Password");
        
        btnLogin = new JButton("Login");
        btnLogin.setBackground(AppTheme.ACCENT);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        
        btnGoRegister = new JButton("Don't have an account? Register");
        btnGoRegister.setContentAreaFilled(false);
        btnGoRegister.setBorderPainted(false);
        btnGoRegister.setForeground(AppTheme.TEXT_SECONDARY);
        btnGoRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        lblError = new JLabel("");
        lblError.setForeground(AppTheme.DANGER);
        lblError.setFont(AppTheme.FONT_SMALL);
        lblError.setVisible(false);
        
        add(lblTitle, "wrap, pady 0 20");
        add(new JLabel("Username", SwingConstants.LEFT), "wrap, gapy 10");
        add(txtUser, "height 35!");
        add(new JLabel("Password", SwingConstants.LEFT), "wrap, gapy 10");
        add(txtPass, "height 35!");
        add(lblError, "wrap");
        add(btnLogin, "height 40!, gapy 20");
        add(btnGoRegister, "gapy 10");
        
        btnLogin.addActionListener(e -> {
            String user = txtUser.getText();
            String pass = new String(txtPass.getPassword());
            if (user.isEmpty() || pass.isEmpty()) {
                showError("Please fill all fields");
                return;
            }
            lblError.setVisible(false);
            LoginRequest req = new LoginRequest();
            req.setUserName(user);
            req.setPassword(pass);
            onLogin.accept(req);
        });
        
        btnGoRegister.addActionListener(e -> onGoRegister.run());
    }
    
    public void showError(String error) {
        lblError.setText(error);
        lblError.setVisible(true);
    }
    
    public void setInputsEnabled(boolean enabled) {
        txtUser.setEnabled(enabled);
        txtPass.setEnabled(enabled);
        btnLogin.setEnabled(enabled);
        btnGoRegister.setEnabled(enabled);
    }
}
