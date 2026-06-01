package com.raven.server.handler;

import com.corundumstudio.socketio.SocketIOServer;
import com.raven.server.service.FileService;
import com.raven.server.service.SessionManager;
import com.raven.shared.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileEventHandler {
    private static final Logger log = LoggerFactory.getLogger(FileEventHandler.class);
    private final FileService fileService;
    private final SessionManager sessionManager;

    public FileEventHandler(FileService fileService, SessionManager sessionManager) {
        this.fileService = fileService;
        this.sessionManager = sessionManager;
    }

    public void registerListeners(SocketIOServer server) {
        // ── SEND FILE CHUNK EVENT (Uploads) ──────────────────────────────────
        server.addEventListener("send_file", FileChunkPacket.class, (client, data, ack) -> {
            try {
                fileService.receiveFileChunk(data);
                
                if (data.isFinish()) {
                    // File fully uploaded. Finalize metadata and trigger delivery
                    ack.sendAckData(true);
                    
                    ImagePreviewData preview = new ImagePreviewData();
                    preview.setFileID(data.getFileID());
                    
                    SendMessageRequest origMsg = fileService.closeFileReceiver(preview);
                    
                    // Route the finalized message (with preview attachment) to the recipient
                    ReceiveMessageResponse response = new ReceiveMessageResponse(
                            origMsg.getMessageType(),
                            origMsg.getFromUserID(),
                            origMsg.getText(),
                            preview
                    );
                    
                    sessionManager.sendToUser(origMsg.getToUserID(), "receive_ms", response);
                } else {
                    ack.sendAckData(true);
                }
            } catch (Exception e) {
                log.error("Failed to receive chunk for file ID: {}", data.getFileID(), e);
                ack.sendAckData(false);
            }
        });

        // ── GET FILE METADATA EVENT (Download initiation) ─────────────────────
        server.addEventListener("get_file", Integer.class, (client, fileID, ack) -> {
            log.info("Download metadata requested for file: {}", fileID);
            try {
                FileMetadata metadata = fileService.initSender(fileID);
                long size = fileService.getFileSize(fileID);
                ack.sendAckData(metadata.getFileExtension(), size);
            } catch (Exception e) {
                log.error("Failed to initialize download for file: {}", fileID, e);
                ack.sendAckData(null, 0L);
            }
        });

        // ── REQUEST FILE CHUNK EVENT (Downloading chunks) ────────────────────
        server.addEventListener("reques_file", FileDownloadRequest.class, (client, data, ack) -> {
            try {
                byte[] chunk = fileService.getFileChunk(data.getFileID(), data.getCurrentLength());
                if (chunk != null) {
                    ack.sendAckData((Object) chunk);
                } else {
                    ack.sendAckData();
                }
            } catch (Exception e) {
                log.error("Failed to read chunk for file: {}, at length: {}", data.getFileID(), data.getCurrentLength(), e);
                ack.sendAckData();
            }
        });
    }
}
