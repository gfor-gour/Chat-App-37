package com.raven.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceResponse {
    @JsonProperty("action")
    private boolean action;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data;

    public ServiceResponse() {
    }

    public ServiceResponse(boolean action, String message, Object data) {
        this.action = action;
        this.message = message;
        this.data = data;
    }

    public static ServiceResponse success(String message, Object data) {
        return new ServiceResponse(true, message, data);
    }

    public static ServiceResponse success(Object data) {
        return new ServiceResponse(true, "Ok", data);
    }

    public static ServiceResponse failure(String message) {
        return new ServiceResponse(false, message, null);
    }

    public boolean isAction() {
        return action;
    }

    public void setAction(boolean action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ServiceResponse{" +
                "action=" + action +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
