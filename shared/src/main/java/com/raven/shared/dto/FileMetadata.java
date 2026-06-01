package com.raven.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileMetadata {
    @JsonProperty("fileID")
    private int fileID;

    @JsonProperty("fileExtension")
    private String fileExtension;

    public FileMetadata() {
    }

    public FileMetadata(int fileID, String fileExtension) {
        this.fileID = fileID;
        this.fileExtension = fileExtension;
    }

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "fileID=" + fileID +
                ", fileExtension='" + fileExtension + '\'' +
                '}';
    }
}
