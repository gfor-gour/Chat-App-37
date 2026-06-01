package com.raven.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.raven.shared.enums.MessageType;

public class ReceiveMessageResponse {
    @JsonProperty("messageType")
    private MessageType messageType;

    @JsonProperty("fromUserID")
    private int fromUserID;

    @JsonProperty("text")
    private String text;

    @JsonProperty("dataImage")
    private ImagePreviewData dataImage;

    public ReceiveMessageResponse() {
    }

    public ReceiveMessageResponse(MessageType messageType, int fromUserID, String text, ImagePreviewData dataImage) {
        this.messageType = messageType;
        this.fromUserID = fromUserID;
        this.text = text;
        this.dataImage = dataImage;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ImagePreviewData getDataImage() {
        return dataImage;
    }

    public void setDataImage(ImagePreviewData dataImage) {
        this.dataImage = dataImage;
    }

    @Override
    public String toString() {
        return "ReceiveMessageResponse{" +
                "messageType=" + messageType +
                ", fromUserID=" + fromUserID +
                ", text='" + text + '\'' +
                ", dataImage=" + dataImage +
                '}';
    }
}
