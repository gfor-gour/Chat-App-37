package com.raven.shared.validation;

import java.util.Set;

public class InputValidator {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".pdf", ".zip", ".doc", ".docx", ".txt", ".mp3", ".mp4"
    );

    /**
     * Sanitizes file extensions to prevent path traversal (A5) and limit allowed extensions.
     */
    public static String sanitizeFileExtension(String extension) {
        if (extension == null) {
            return "";
        }
        // Normalize: strip whitespace, convert to lower case
        String clean = extension.trim().toLowerCase();
        
        // Extract the actual extension if a path or filename is passed
        int dot = clean.lastIndexOf('.');
        if (dot >= 0) {
            clean = clean.substring(dot);
        }
        
        // Remove any path traversal/separators (e.g. /, \)
        clean = clean.replaceAll("[\\\\/]", "");
        
        // Restore leading dot if missing
        if (!clean.startsWith(".") && !clean.isEmpty()) {
            clean = "." + clean;
        }

        // Validate against whitelist
        if (!ALLOWED_EXTENSIONS.contains(clean)) {
            throw new IllegalArgumentException("Unsupported file extension: " + extension);
        }
        
        return clean;
    }

    /**
     * Validates a username (A4). Throws IllegalArgumentException if invalid.
     */
    public static void validateUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        String trimmed = userName.trim();
        if (trimmed.length() < 3 || trimmed.length() > 30) {
            throw new IllegalArgumentException("Username must be between 3 and 30 characters");
        }
        // Only allow alphanumeric, underscores, hyphens
        if (!trimmed.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, hyphens, and underscores");
        }
    }

    /**
     * Validates a password. Throws IllegalArgumentException if invalid.
     */
    public static void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (password.length() < 3) {
            throw new IllegalArgumentException("Password must be at least 3 characters long");
        }
    }

    /**
     * Validates message text.
     */
    public static void validateMessageText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (text.length() > 5000) {
            throw new IllegalArgumentException("Message too long (max 5000 characters)");
        }
    }
}
