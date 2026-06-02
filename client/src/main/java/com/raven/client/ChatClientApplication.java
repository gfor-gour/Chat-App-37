package com.raven.client;

import com.formdev.flatlaf.FlatDarkLaf;
import com.raven.client.connection.ConnectionManager;
import com.raven.client.ui.MainFrame;

import javax.swing.*;

public class ChatClientApplication {
    
    public static void main(String[] args) {
        // Setup modern FlatLaf theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        
        SwingUtilities.invokeLater(() -> {
            // Establish connection
            ConnectionManager connectionManager = ConnectionManager.getInstance();
            connectionManager.init("localhost", 9999);
            connectionManager.connect();
            
            // Launch main frame
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
