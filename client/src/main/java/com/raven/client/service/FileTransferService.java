package com.raven.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raven.client.connection.ConnectionManager;
import com.raven.client.util.SwingUtils;
import com.raven.event.EventFileReceiver;
import com.raven.event.EventFileSender;
import com.raven.shared.dto.*;
import io.socket.client.Ack;
import io.socket.client.Socket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FileTransferService {
    private static final Logger log = LoggerFactory.getLogger(FileTransferService.class);
    private static FileTransferService instance;
    private final String clientDataDir = "client_data/";

    private final ConcurrentLinkedQueue<FileSender> sendQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<FileReceiver> receiveQueue = new ConcurrentLinkedQueue<>();

    private FileTransferService() {
        File dir = new File(clientDataDir);
        if (!dir.exists() && dir.mkdirs()) {
            log.info("Created client data directory at: {}", clientDataDir);
        }
    }

    public static synchronized FileTransferService getInstance() {
        if (instance == null) {
            instance = new FileTransferService();
        }
        return instance;
    }

    // ── FILE UPLOAD QUEUE MANAGEMENT ──────────────────────────────────────────

    public synchronized FileSender addFileToSend(File file, SendMessageRequest message, EventFileSender event) throws IOException {
        FileSender sender = new FileSender(file, message, event);
        sendQueue.add(sender);
        log.info("Added file to upload queue: filename='{}', queueSize={}", file.getName(), sendQueue.size());
        
        // Start transferring if it's the only one in the queue
        if (sendQueue.size() == 1) {
            sender.initiateSend();
        }
        return sender;
    }

    private synchronized void onFileSendFinish(FileSender sender) {
        sendQueue.remove(sender);
        log.info("File upload finished. Remaining in queue: {}", sendQueue.size());
        FileSender next = sendQueue.peek();
        if (next != null) {
            try {
                next.initiateSend();
            } catch (IOException e) {
                log.error("Failed to start next file upload in queue", e);
                onFileSendFinish(next); // Evict failed sender and proceed
            }
        }
    }

    // ── FILE DOWNLOAD QUEUE MANAGEMENT ────────────────────────────────────────

    public synchronized void addFileToReceive(int fileID, EventFileReceiver event) {
        FileReceiver receiver = new FileReceiver(fileID, event);
        receiveQueue.add(receiver);
        log.info("Added file to download queue: fileID={}, queueSize={}", fileID, receiveQueue.size());

        if (receiveQueue.size() == 1) {
            receiver.initiateReceive();
        }
    }

    private synchronized void onFileReceiveFinish(FileReceiver receiver) {
        receiveQueue.remove(receiver);
        log.info("File download finished. Remaining in queue: {}", receiveQueue.size());
        FileReceiver next = receiveQueue.peek();
        if (next != null) {
            next.initiateReceive();
        }
    }

    // ── CLIENT FILE SENDER INNER CLASS ───────────────────────────────────────

    public class FileSender {
        private final File file;
        private final SendMessageRequest message;
        private final EventFileSender event;
        private final long fileSize;
        private RandomAccessFile randomAccessFile;
        private int fileID;

        public FileSender(File file, SendMessageRequest message, EventFileSender event) throws IOException {
            this.file = file;
            this.message = message;
            this.event = event;
            this.randomAccessFile = new RandomAccessFile(file, "r");
            this.fileSize = randomAccessFile.length();
        }

        public File getFile() {
            return file;
        }

        public SendMessageRequest getMessage() {
            return message;
        }

        public int getFileID() {
            return fileID;
        }

        public long getFileSize() {
            return fileSize;
        }

        private synchronized byte[] readNextChunk() throws IOException {
            long filePointer = randomAccessFile.getFilePointer();
            if (filePointer >= fileSize) {
                return null;
            }
            int maxChunkSize = 2000;
            long remaining = fileSize - filePointer;
            int length = (int) Math.min(maxChunkSize, remaining);
            byte[] data = new byte[length];
            randomAccessFile.read(data);
            return data;
        }

        public void initiateSend() throws IOException {
            Socket socket = ConnectionManager.getInstance().getSocket();
            if (socket == null) {
                cleanupQuietly();
                throw new IOException("Socket is disconnected");
            }

            ObjectMapper mapper = new ObjectMapper();
            try {
                JSONObject json = new JSONObject(mapper.writeValueAsString(message));
                log.info("Initiating upload handshake for: {}", file.getName());
                
                socket.emit("send_to_user", json, (Ack) args -> {
                    if (args.length > 0) {
                        this.fileID = (int) args[0];
                        log.info("Handshake ACK received. Generated FileID: {}", fileID);
                        
                        SwingUtils.runOnEDT(() -> {
                            if (event != null) event.onStartSending();
                        });
                        
                        try {
                            sendNextChunkRecursive();
                        } catch (IOException e) {
                            log.error("Failed to start chunk upload", e);
                            cleanupQuietly();
                            FileTransferService.this.onFileSendFinish(this);
                        }
                    }
                });
            } catch (Exception e) {
                cleanupQuietly();
                throw new IOException("Serialization failed for upload handshake", e);
            }
        }

        private void sendNextChunkRecursive() throws IOException {
            Socket socket = ConnectionManager.getInstance().getSocket();
            if (socket == null) {
                cleanupQuietly();
                FileTransferService.this.onFileSendFinish(this);
                return;
            }

            FileChunkPacket packet = new FileChunkPacket();
            packet.setFileID(fileID);
            
            byte[] bytes = readNextChunk();
            if (bytes != null) {
                packet.setData(bytes);
                packet.setFinish(false);
            } else {
                packet.setFinish(true);
                cleanupQuietly();
            }

            ObjectMapper mapper = new ObjectMapper();
            try {
                JSONObject json = new JSONObject(mapper.writeValueAsString(packet));
                socket.emit("send_file", json, (Ack) args -> {
                    if (args.length > 0 && (boolean) args[0]) {
                        try {
                            if (!packet.isFinish()) {
                                double progress = getProgressPercentage();
                                SwingUtils.runOnEDT(() -> {
                                    if (event != null) event.onSending(progress);
                                });
                                sendNextChunkRecursive();
                            } else {
                                log.info("File upload completed: fileID={}", fileID);
                                FileTransferService.this.onFileSendFinish(this);
                                SwingUtils.runOnEDT(() -> {
                                    if (event != null) event.onFinish();
                                });
                            }
                        } catch (IOException e) {
                            log.error("Error reading file pointer for progress", e);
                            cleanupQuietly();
                            FileTransferService.this.onFileSendFinish(this);
                        }
                    } else {
                        log.error("Server NACK'd file chunk for fileID: {}", fileID);
                        cleanupQuietly();
                        FileTransferService.this.onFileSendFinish(this);
                    }
                });
            } catch (Exception e) {
                log.error("Jackson serialization failed in sendNextChunk", e);
                cleanupQuietly();
                FileTransferService.this.onFileSendFinish(this);
            }
        }

        private double getProgressPercentage() throws IOException {
            if (fileSize == 0) return 100.0;
            return (double) randomAccessFile.getFilePointer() * 100.0 / fileSize;
        }

        private void cleanupQuietly() {
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    // ── CLIENT FILE RECEIVER INNER CLASS ─────────────────────────────────────

    public class FileReceiver {
        private final int fileID;
        private final EventFileReceiver event;
        private String fileExtension;
        private long fileSize;
        private File outputFile;
        private RandomAccessFile randomAccessFile;

        public FileReceiver(int fileID, EventFileReceiver event) {
            this.fileID = fileID;
            this.event = event;
        }

        public void initiateReceive() {
            Socket socket = ConnectionManager.getInstance().getSocket();
            if (socket == null) {
                FileTransferService.this.onFileReceiveFinish(this);
                return;
            }

            log.info("Requesting file metadata for fileID: {}", fileID);
            socket.emit("get_file", fileID, (Ack) args -> {
                if (args.length >= 2 && args[0] != null) {
                    try {
                        fileExtension = args[0].toString();
                        fileSize = ((Number) args[1]).longValue();
                        
                        outputFile = new File(clientDataDir + fileID + fileExtension);
                        randomAccessFile = new RandomAccessFile(outputFile, "rw");
                        
                        log.info("Metadata received: extension='{}', size={} bytes. Downloading to: {}", fileExtension, fileSize, outputFile.getName());

                        SwingUtils.runOnEDT(() -> {
                            if (event != null) event.onStartReceiving();
                        });

                        requestNextChunkRecursive();
                    } catch (Exception e) {
                        log.error("Failed to initialize file download", e);
                        cleanupQuietly();
                        FileTransferService.this.onFileReceiveFinish(this);
                    }
                } else {
                    log.error("Server did not return valid file metadata for download: id={}", fileID);
                    FileTransferService.this.onFileReceiveFinish(this);
                }
            });
        }

        private void requestNextChunkRecursive() throws IOException {
            Socket socket = ConnectionManager.getInstance().getSocket();
            if (socket == null) {
                cleanupQuietly();
                FileTransferService.this.onFileReceiveFinish(this);
                return;
            }

            FileDownloadRequest request = new FileDownloadRequest(fileID, randomAccessFile.length());
            ObjectMapper mapper = new ObjectMapper();
            try {
                JSONObject json = new JSONObject(mapper.writeValueAsString(request));
                socket.emit("reques_file", json, (Ack) args -> {
                    try {
                        if (args.length > 0 && args[0] != null) {
                            byte[] chunk = (byte[]) args[0];
                            writeChunk(chunk);
                            
                            double progress = getProgressPercentage();
                            SwingUtils.runOnEDT(() -> {
                                if (event != null) event.onReceiving(progress);
                            });

                            requestNextChunkRecursive();
                        } else {
                            // No more chunks: download complete
                            cleanupQuietly();
                            log.info("Download completed successfully for file: {}", outputFile.getName());
                            FileTransferService.this.onFileReceiveFinish(this);
                            
                            SwingUtils.runOnEDT(() -> {
                                if (event != null) event.onFinish(outputFile);
                            });
                        }
                    } catch (Exception e) {
                        log.error("Failed to request chunk in download recursive loop", e);
                        cleanupQuietly();
                        FileTransferService.this.onFileReceiveFinish(this);
                    }
                });
            } catch (Exception e) {
                cleanupQuietly();
                throw new IOException("Serialization failed in requestNextChunkRecursive", e);
            }
        }

        private synchronized void writeChunk(byte[] chunk) throws IOException {
            randomAccessFile.seek(randomAccessFile.length());
            randomAccessFile.write(chunk);
        }

        private double getProgressPercentage() throws IOException {
            if (fileSize == 0) return 100.0;
            return (double) randomAccessFile.getFilePointer() * 100.0 / fileSize;
        }

        private void cleanupQuietly() {
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
