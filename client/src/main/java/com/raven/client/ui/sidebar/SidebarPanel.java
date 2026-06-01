package com.raven.client.ui.sidebar;

import com.raven.client.ui.theme.AppTheme;
import com.raven.shared.dto.UserAccountDto;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarPanel extends JPanel {
    private final JPanel pnlUsers;
    private final Map<Integer, UserItemPanel> userPanels = new HashMap<>();
    private UserItemPanel selectedPanel = null;
    private final Consumer<UserAccountDto> onUserSelected;

    public SidebarPanel(Consumer<UserAccountDto> onUserSelected) {
        this.onUserSelected = onUserSelected;
        
        setLayout(new MigLayout("fill, insets 0", "[fill]"));
        setBackground(AppTheme.BG_SECONDARY);
        setPreferredSize(new Dimension(AppTheme.SIDEBAR_WIDTH, 0));
        
        JPanel pnlHeader = new JPanel(new MigLayout("fillx, insets 15", "[fill]"));
        pnlHeader.setBackground(AppTheme.BG_SECONDARY);
        pnlHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.DIVIDER));
        
        JLabel lblTitle = new JLabel("Chats");
        lblTitle.setFont(AppTheme.FONT_HEADING);
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        
        JTextField txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Search users...");
        txtSearch.putClientProperty("FlatLaf.style", "arc: 15");
        
        pnlHeader.add(lblTitle, "wrap, gapy 0 10");
        pnlHeader.add(txtSearch, "height 30!");
        
        pnlUsers = new JPanel(new MigLayout("fillx, insets 0, gap 0", "[fill]"));
        pnlUsers.setBackground(AppTheme.BG_SECONDARY);
        
        JScrollPane scrollPane = new JScrollPane(pnlUsers);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(pnlHeader, "wrap");
        add(scrollPane, "push, grow");
    }
    
    public void setUsers(List<UserAccountDto> users) {
        pnlUsers.removeAll();
        userPanels.clear();
        for (UserAccountDto user : users) {
            addUser(user);
        }
        pnlUsers.revalidate();
        pnlUsers.repaint();
    }
    
    public void addUser(UserAccountDto user) {
        UserItemPanel panel = new UserItemPanel(user, this::selectUser);
        userPanels.put(user.getUserID(), panel);
        pnlUsers.add(panel, "wrap");
        pnlUsers.revalidate();
        pnlUsers.repaint();
    }
    
    public void updateUserStatus(int userId, boolean isOnline) {
        UserItemPanel panel = userPanels.get(userId);
        if (panel != null) {
            panel.updateStatus(isOnline);
        }
    }
    
    private void selectUser(UserAccountDto user) {
        if (selectedPanel != null) {
            selectedPanel.setSelected(false);
        }
        selectedPanel = userPanels.get(user.getUserID());
        if (selectedPanel != null) {
            selectedPanel.setSelected(true);
        }
        onUserSelected.accept(user);
    }
}
