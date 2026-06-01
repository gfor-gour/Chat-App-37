package com.raven.client.ui.chat;

import com.raven.client.ui.theme.AppTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class MessageBubble extends JPanel {
    
    public enum Alignment { LEFT, RIGHT }
    
    public MessageBubble(String text, Alignment align, String time) {
        setLayout(new MigLayout("insets 5, fillx", align == Alignment.LEFT ? "[left]" : "[right]"));
        setOpaque(false);
        
        JTextArea txtMsg = new JTextArea(text);
        txtMsg.setEditable(false);
        txtMsg.setOpaque(false);
        txtMsg.setLineWrap(true);
        txtMsg.setWrapStyleWord(true);
        txtMsg.setFont(AppTheme.FONT_BODY);
        
        boolean isMine = (align == Alignment.RIGHT);
        txtMsg.setForeground(isMine ? Color.WHITE : AppTheme.TEXT_PRIMARY);
        
        JPanel bubble = new JPanel(new MigLayout("insets 10 15 10 15"));
        bubble.setBackground(isMine ? AppTheme.BUBBLE_SENT : AppTheme.BUBBLE_RECV);
        bubble.putClientProperty("FlatLaf.style", "arc: 16");
        bubble.add(txtMsg, "width ::300"); // max width 300
        
        JLabel lblTime = new JLabel(time);
        lblTime.setFont(AppTheme.FONT_SMALL);
        lblTime.setForeground(AppTheme.TEXT_SECONDARY);
        
        JPanel wrapper = new JPanel(new MigLayout("insets 0, gap 0", "[fill]"));
        wrapper.setOpaque(false);
        
        if (align == Alignment.LEFT) {
            wrapper.add(bubble, "wrap");
            wrapper.add(lblTime, "gapleft 5");
        } else {
            wrapper.add(bubble, "wrap, align right");
            wrapper.add(lblTime, "align right, gapright 5");
        }
        
        add(wrapper, align == Alignment.LEFT ? "left" : "right");
    }
}
