package com.raven.shared.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageTypeTest {

    @Test
    void testFromValue_Valid() {
        assertEquals(MessageType.TEXT, MessageType.fromValue(1));
        assertEquals(MessageType.EMOJI, MessageType.fromValue(2));
        assertEquals(MessageType.FILE, MessageType.fromValue(3));
        assertEquals(MessageType.IMAGE, MessageType.fromValue(4));
    }

    @Test
    void testFromValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            MessageType.fromValue(99);
        });
    }

    @Test
    void testGetValue() {
        assertEquals(1, MessageType.TEXT.getValue());
        assertEquals(4, MessageType.IMAGE.getValue());
    }
}
