package com.raven.client.ui.theme;

import java.awt.Color;
import java.awt.Font;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;

public final class AppTheme {
    // ── Color Palette ──────────────────────────────
    public static final Color BG_PRIMARY    = new Color(30, 30, 46);     
    public static final Color BG_SECONDARY  = new Color(36, 39, 58);     
    public static final Color BG_SURFACE    = new Color(49, 50, 68);     
    public static final Color BG_HOVER      = new Color(69, 71, 90);     
    public static final Color TEXT_PRIMARY   = new Color(205, 214, 244); 
    public static final Color TEXT_SECONDARY = new Color(147, 153, 178); 
    public static final Color ACCENT         = new Color(137, 180, 250); 
    public static final Color ACCENT_HOVER   = new Color(116, 160, 230); 
    public static final Color SUCCESS        = new Color(166, 227, 161); 
    public static final Color WARNING        = new Color(249, 226, 175); 
    public static final Color DANGER         = new Color(243, 139, 168); 
    public static final Color BUBBLE_SENT    = new Color(137, 180, 250); 
    public static final Color BUBBLE_RECV    = new Color(49, 50, 68);    
    public static final Color DIVIDER        = new Color(69, 71, 90);    
    
    // ── Typography ─────────────────────────────────
    public static final Font FONT_HEADING  = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY     = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_CAPTION  = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL    = new Font("Segoe UI", Font.PLAIN, 11);
    
    // ── Spacing & Sizing ───────────────────────────
    public static final int SIDEBAR_WIDTH = 280;
    public static final int CORNER_RADIUS = 12;
    public static final int PADDING_SM = 6;
    public static final int PADDING_MD = 12;
    public static final int PADDING_LG = 20;
    public static final int AVATAR_SIZE = 42;
    
    // ── Borders ────────────────────────────────────
    public static Border paddedBorder(int size) { return new EmptyBorder(size, size, size, size); }
    public static Border roundedBorder() { return BorderFactory.createLineBorder(DIVIDER, 1, true); }
}
