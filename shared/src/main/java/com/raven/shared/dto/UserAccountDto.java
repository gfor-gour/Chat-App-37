package com.raven.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserAccountDto {
    @JsonProperty("userID")
    private int userID;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("image")
    private String image;

    @JsonProperty("status")
    private boolean status; // Online status

    public UserAccountDto() {
    }

    public UserAccountDto(int userID, String userName, String gender, String image, boolean status) {
        this.userID = userID;
        this.userName = userName;
        this.gender = gender;
        this.image = image;
        this.status = status;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "UserAccountDto{" +
                "userID=" + userID +
                ", userName='" + userName + '\'' +
                ", gender='" + gender + '\'' +
                ", status=" + status +
                '}';
    }
}
