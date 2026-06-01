package com.raven.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.raven.shared.enums.MessageType;

public class SendMessageRequest {
    @JsonProperty("messageType")
    private MessageType messageType;

    @JsonProperty("fromUserID")
    private int fromUserID;

    @JsonProperty("toUserID")
    private int toUserID;

    @JsonProperty("text")
    private String text;

    public SendMessageRequest() {
    }

    public SendMessageRequest(MessageType messageType, int fromUserID, int toUserID, String text) {
        this.messageType = messageType;
        this.fromUserID = fromUserID;
        this.toUserID = toUserID;
        this.text = text;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public int getFromUserID() {
        return fromUserID;
    }

    public void setFromUserID(int fromUserID) {
        this.fromUserID = fromUserID;
    }

    public int getToUserID() {
        return toUserID;
    }

    public void setToUserID(int toUserID) {
        this.toUserID = toUserID;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "SendMessageRequest{" +
                "messageType=" + messageType +
                ", fromUserID=" + fromUserID +
                ", toUserID=" + toUserID +
                ", text='" + text + '\'' +
                '}';
    }
}
