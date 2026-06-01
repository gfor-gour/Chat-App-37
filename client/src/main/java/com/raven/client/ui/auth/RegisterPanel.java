package com.raven.client.ui.auth;

import com.raven.client.ui.theme.AppTheme;
import com.raven.shared.dto.RegisterRequest;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class RegisterPanel extends JPanel {
    private final JTextField txtUser;
    private final JPasswordField txtPass;
    private final JPasswordField txtConfirmPass;
    private final JButton btnRegister;
    private final JButton btnGoLogin;
    private final JLabel lblError;
    
    public RegisterPanel(Consumer<RegisterRequest> onRegister, Runnable onGoLogin) {
        setLayout(new MigLayout("wrap, fillx, insets 35 45 30 45", "[fill, 250]"));
        setBackground(AppTheme.BG_SURFACE);
        putClientProperty("FlatLaf.style", "arc: 20");
        
        JLabel lblTitle = new JLabel("Register", SwingConstants.CENTER);
        lblTitle.setFont(AppTheme.FONT_HEADING);
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        
        txtUser = new JTextField();
        txtUser.putClientProperty("JTextField.placeholderText", "Username");
        
        txtPass = new JPasswordField();
        txtPass.putClientProperty("JTextField.placeholderText", "Password");
        
        txtConfirmPass = new JPasswordField();
        txtConfirmPass.putClientProperty("JTextField.placeholderText", "Confirm Password");
        
        btnRegister = new JButton("Register");
        btnRegister.setBackground(AppTheme.ACCENT);
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFocusPainted(false);
        
        btnGoLogin = new JButton("Already have an account? Login");
        btnGoLogin.setContentAreaFilled(false);
        btnGoLogin.setBorderPainted(false);
        btnGoLogin.setForeground(AppTheme.TEXT_SECONDARY);
        btnGoLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        lblError = new JLabel("");
        lblError.setForeground(AppTheme.DANGER);
        lblError.setFont(AppTheme.FONT_SMALL);
        lblError.setVisible(false);
        
        add(lblTitle, "wrap, pady 0 20");
        add(new JLabel("Username", SwingConstants.LEFT), "wrap, gapy 10");
        add(txtUser, "height 35!");
        add(new JLabel("Password", SwingConstants.LEFT), "wrap, gapy 10");
        add(txtPass, "height 35!");
        add(new JLabel("Confirm Password", SwingConstants.LEFT), "wrap, gapy 10");
        add(txtConfirmPass, "height 35!");
        add(lblError, "wrap");
        add(btnRegister, "height 40!, gapy 20");
        add(btnGoLogin, "gapy 10");
        
        btnRegister.addActionListener(e -> {
            String user = txtUser.getText();
            String pass = new String(txtPass.getPassword());
            String confirmPass = new String(txtConfirmPass.getPassword());
            
            if (user.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                showError("Please fill all fields");
                return;
            }
            if (!pass.equals(confirmPass)) {
                showError("Passwords do not match");
                return;
            }
            
            lblError.setVisible(false);
            RegisterRequest req = new RegisterRequest();
            req.setUserName(user);
            req.setPassword(pass);
            onRegister.accept(req);
        });
        
        btnGoLogin.addActionListener(e -> onGoLogin.run());
    }
    
    public void showError(String error) {
        lblError.setText(error);
        lblError.setVisible(true);
    }
    
    public void setInputsEnabled(boolean enabled) {
        txtUser.setEnabled(enabled);
        txtPass.setEnabled(enabled);
        txtConfirmPass.setEnabled(enabled);
        btnRegister.setEnabled(enabled);
        btnGoLogin.setEnabled(enabled);
    }
}
