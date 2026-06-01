package com.raven.client.ui.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.geom.Arc2D;
import com.raven.client.ui.theme.AppTheme;

public class LoadingOverlay extends JPanel {
    private double angle = 0;
    private final Timer timer;

    public LoadingOverlay() {
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 120));
        
        // Block mouse events
        addMouseListener(new MouseAdapter() {});
        addMouseMotionListener(new MouseAdapter() {});
        
        timer = new Timer(30, e -> {
            angle += 10;
            if (angle >= 360) angle = 0;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Paint semi-transparent background
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int size = 50;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(AppTheme.ACCENT);
        
        Arc2D.Double arc = new Arc2D.Double(x, y, size, size, angle, 270, Arc2D.OPEN);
        g2.draw(arc);
        g2.dispose();
    }
    
    public void start() {
        setVisible(true);
        timer.start();
    }
    
    public void stop() {
        setVisible(false);
        timer.stop();
    }
}
