package com.raven.client.ui.chat;

import com.raven.client.ui.theme.AppTheme;
import com.raven.client.util.SwingUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatBodyPanel extends JPanel {
    private final JPanel pnlMessages;
    private final JScrollPane scrollPane;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

    public ChatBodyPanel() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PRIMARY);
        
        pnlMessages = new JPanel(new MigLayout("fillx, wrap", "[fill]"));
        pnlMessages.setBackground(AppTheme.BG_PRIMARY);
        
        scrollPane = new JScrollPane(pnlMessages);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void addMessage(String text, MessageBubble.Alignment align) {
        MessageBubble bubble = new MessageBubble(text, align, timeFormat.format(new Date()));
        pnlMessages.add(bubble, "wrap");
        pnlMessages.revalidate();
        pnlMessages.repaint();
        scrollToBottom();
    }
    
    public void clear() {
        pnlMessages.removeAll();
        pnlMessages.revalidate();
        pnlMessages.repaint();
    }
    
    private void scrollToBottom() {
        SwingUtils.runOnEDT(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
}
