package com.raven.shared.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @Test
    void testSanitizeFileExtension_Valid() {
        assertEquals(".jpg", InputValidator.sanitizeFileExtension(".jpg"));
        assertEquals(".png", InputValidator.sanitizeFileExtension(".png"));
    }

    @Test
    void testSanitizeFileExtension_PathTraversalAttempt() {
        assertEquals(".jpg", InputValidator.sanitizeFileExtension("../../../secret.jpg"));
        assertEquals(".png", InputValidator.sanitizeFileExtension("..\\..\\secret.png"));
    }

    @Test
    void testSanitizeFileExtension_NoDot() {
        assertEquals(".jpg", InputValidator.sanitizeFileExtension("jpg"));
    }

    @Test
    void testSanitizeFileExtension_Disallowed() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            InputValidator.sanitizeFileExtension(".exe");
        });
        assertTrue(exception.getMessage().contains("File extension not allowed"));
    }

    @Test
    void testValidateUserName_Valid() {
        assertDoesNotThrow(() -> InputValidator.validateUserName("JohnDoe"));
        assertDoesNotThrow(() -> InputValidator.validateUserName("Alice_123"));
    }

    @Test
    void testValidateUserName_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validateUserName(null));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validateUserName(""));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validateUserName("A"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validateUserName("John@Doe"));
    }
}
