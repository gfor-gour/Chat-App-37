package com.raven.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImagePreviewData {
    @JsonProperty("fileID")
    private int fileID;

    @JsonProperty("image")
    private String image; // Contains the BlurHash string

    @JsonProperty("width")
    private int width;

    @JsonProperty("height")
    private int height;

    public ImagePreviewData() {
    }

    public ImagePreviewData(int fileID, String image, int width, int height) {
        this.fileID = fileID;
        this.image = image;
        this.width = width;
        this.height = height;
    }

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "ImagePreviewData{" +
                "fileID=" + fileID +
                ", image='" + image + '\'' +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
