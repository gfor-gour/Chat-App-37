package com.raven.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    TEXT(1),
    EMOJI(2),
    FILE(3),
    IMAGE(4);

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static MessageType fromValue(int value) {
        for (MessageType type : MessageType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type value: " + value);
    }
}
