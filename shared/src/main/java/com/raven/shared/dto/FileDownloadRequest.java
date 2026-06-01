package com.raven.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileDownloadRequest {
    @JsonProperty("fileID")
    private int fileID;

    @JsonProperty("currentLength")
    private long currentLength;

    public FileDownloadRequest() {
    }

    public FileDownloadRequest(int fileID, long currentLength) {
        this.fileID = fileID;
        this.currentLength = currentLength;
    }

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public long getCurrentLength() {
        return currentLength;
    }

    public void setCurrentLength(long currentLength) {
        this.currentLength = currentLength;
    }

    @Override
    public String toString() {
        return "FileDownloadRequest{" +
                "fileID=" + fileID +
                ", currentLength=" + currentLength +
                '}';
    }
}
