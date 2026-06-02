package com.raven.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileChunkPacket {
    @JsonProperty("fileID")
    private int fileID;

    @JsonProperty("data")
    private byte[] data;

    @JsonProperty("finish")
    private boolean finish;

    public FileChunkPacket() {
    }

    public FileChunkPacket(int fileID, byte[] data, boolean finish) {
        this.fileID = fileID;
        this.data = data;
        this.finish = finish;
    }

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    @Override
    public String toString() {
        return "FileChunkPacket{" +
                "fileID=" + fileID +
                ", dataLength=" + (data != null ? data.length : 0) +
                ", finish=" + finish +
                '}';
    }
}
