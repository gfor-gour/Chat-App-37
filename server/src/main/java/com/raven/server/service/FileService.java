package com.raven.server.service;

import com.raven.server.config.AppConfig;
import com.raven.server.repository.FileRepository;
import com.raven.shared.dto.*;
import com.raven.shared.enums.MessageType;
import com.raven.shared.validation.InputValidator;
import com.raven.swing.blurHash.BlurHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private final FileRepository fileRepository;
    private final String storagePath;
    
    private final ConcurrentHashMap<Integer, FileReceiver> activeReceivers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, FileSender> activeSenders = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cacheEvictor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "file-cache-evictor");
        t.setDaemon(true);
        return t;
    });

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
        this.storagePath = AppConfig.getInstance().getProperty("file.storage.path", "server_data/");
        
        // Ensure storage directory exists
        File dir = new File(storagePath);
        if (!dir.exists() && dir.mkdirs()) {
            log.info("Created file storage directory at: {}", storagePath);
        }

        // Periodically evict inactive file handles after 5 minutes of inactivity (E4)
        cacheEvictor.scheduleAtFixedRate(this::evictInactiveSessions, 1, 1, TimeUnit.MINUTES);
    }

    public FileMetadata registerFileReceiver(String originalFileName) throws SQLException {
        // Extract file extension and sanitize
        String ext = "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalFileName.substring(dot);
        }
        String cleanExt = InputValidator.sanitizeFileExtension(ext);
        
        FileMetadata metadata = fileRepository.insertFile(cleanExt);
        log.info("Registered file receiver: id={}, extension='{}'", metadata.getFileID(), cleanExt);
        return metadata;
    }

    public void initReceiver(FileMetadata metadata, SendMessageRequest message) throws IOException {
        File file = new File(storagePath + metadata.getFileID() + metadata.getFileExtension());
        FileReceiver receiver = new FileReceiver(message, file);
        activeReceivers.put(metadata.getFileID(), receiver);
    }

    public void receiveFileChunk(FileChunkPacket chunk) throws IOException {
        FileReceiver receiver = activeReceivers.get(chunk.getFileID());
        if (receiver == null) {
            throw new IOException("No active receiver found for file ID: " + chunk.getFileID());
        }

        if (!chunk.isFinish()) {
            receiver.write(chunk.getData());
        } else {
            receiver.close();
            log.info("Finished writing file: id={}", chunk.getFileID());
        }
    }

    public SendMessageRequest closeFileReceiver(ImagePreviewData dataImage) throws IOException, SQLException {
        FileReceiver receiver = activeReceivers.get(dataImage.getFileID());
        if (receiver == null) {
            throw new IOException("No active receiver session for file: " + dataImage.getFileID());
        }

        SendMessageRequest message = receiver.getMessage();
        if (message.getMessageType() == MessageType.IMAGE) {
            message.setText(""); // Image files do not use text field
            String blurhash = generateBlurHash(receiver.getFile(), dataImage);
            fileRepository.updateBlurHashDone(dataImage.getFileID(), blurhash);
            log.info("Image closed. Generated BlurHash: {}, file={}", blurhash, dataImage.getFileID());
        } else {
            fileRepository.updateDone(dataImage.getFileID());
            log.info("Non-image file closed: file={}", dataImage.getFileID());
        }
        
        activeReceivers.remove(dataImage.getFileID());
        return message;
    }

    public synchronized FileMetadata initSender(int fileID) throws IOException, SQLException {
        FileSender sender = activeSenders.get(fileID);
        if (sender == null) {
            Optional<FileMetadata> opt = fileRepository.findById(fileID);
            if (opt.isEmpty()) {
                throw new SQLException("File metadata not found in database: " + fileID);
            }
            FileMetadata fileMeta = opt.get();
            File physicalFile = new File(storagePath + fileID + fileMeta.getFileExtension());
            if (!physicalFile.exists()) {
                throw new IOException("Physical file does not exist: " + physicalFile.getAbsolutePath());
            }
            sender = new FileSender(fileMeta, physicalFile);
            activeSenders.put(fileID, sender);
            log.info("Initialized file sender session: id={}, size={}", fileID, sender.getFileSize());
        }
        return sender.getMetadata();
    }

    public byte[] getFileChunk(int fileID, long currentLength) throws IOException, SQLException {
        initSender(fileID);
        FileSender sender = activeSenders.get(fileID);
        if (sender == null) {
            throw new IOException("Sender not initialized for file: " + fileID);
        }
        return sender.read(currentLength);
    }

    public long getFileSize(int fileID) throws IOException {
        FileSender sender = activeSenders.get(fileID);
        if (sender == null) {
            throw new IOException("Sender not active for file: " + fileID);
        }
        return sender.getFileSize();
    }

    private String generateBlurHash(File file, ImagePreviewData dataImage) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Failed to parse image from file: " + file.getAbsolutePath());
        }
        Dimension size = getAutoSize(new Dimension(img.getWidth(), img.getHeight()), new Dimension(200, 200));
        
        BufferedImage newImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(img, 0, 0, size.width, size.height, null);
        g2.dispose();
        
        String blurhash = BlurHash.encode(newImage);
        dataImage.setWidth(size.width);
        dataImage.setHeight(size.height);
        dataImage.setImage(blurhash);
        return blurhash;
    }

    private Dimension getAutoSize(Dimension fromSize, Dimension toSize) {
        int w = toSize.width;
        int h = toSize.height;
        int iw = fromSize.width;
        int ih = fromSize.height;
        double xScale = (double) w / iw;
        double yScale = (double) h / ih;
        double scale = Math.min(xScale, yScale);
        return new Dimension((int) (scale * iw), (int) (scale * ih));
    }

    private synchronized void evictInactiveSessions() {
        long now = System.currentTimeMillis();
        long maxIdleMs = TimeUnit.MINUTES.toMillis(5);

        activeReceivers.entrySet().removeIf(entry -> {
            FileReceiver receiver = entry.getValue();
            if (now - receiver.getLastAccessed() > maxIdleMs) {
                log.warn("Evicting inactive file receiver session: fileID={}", entry.getKey());
                receiver.cleanupQuietly();
                return true;
            }
            return false;
        });

        activeSenders.entrySet().removeIf(entry -> {
            FileSender sender = entry.getValue();
            if (now - sender.getLastAccessed() > maxIdleMs) {
                log.info("Evicting inactive file sender session: fileID={}", entry.getKey());
                sender.cleanupQuietly();
                return true;
            }
            return false;
        });
    }

    public void shutdown() {
        log.info("Shutting down FileService and evicting all active file handlers...");
        cacheEvictor.shutdownNow();
        activeReceivers.values().forEach(FileReceiver::cleanupQuietly);
        activeReceivers.clear();
        activeSenders.values().forEach(FileSender::cleanupQuietly);
        activeSenders.clear();
    }

    // ── INNER CLASSES ────────────────────────────────────────────────────────

    private static class FileReceiver {
        private final SendMessageRequest message;
        private final File file;
        private final RandomAccessFile randomAccessFile;
        private volatile long lastAccessed;

        public FileReceiver(SendMessageRequest message, File file) throws IOException {
            this.message = message;
            this.file = file;
            this.randomAccessFile = new RandomAccessFile(file, "rw");
            this.lastAccessed = System.currentTimeMillis();
        }

        public SendMessageRequest getMessage() {
            this.lastAccessed = System.currentTimeMillis();
            return message;
        }

        public File getFile() {
            this.lastAccessed = System.currentTimeMillis();
            return file;
        }

        public long getLastAccessed() {
            return lastAccessed;
        }

        public synchronized void write(byte[] data) throws IOException {
            this.lastAccessed = System.currentTimeMillis();
            randomAccessFile.seek(randomAccessFile.length());
            randomAccessFile.write(data);
        }

        public synchronized void close() throws IOException {
            randomAccessFile.close();
        }

        public void cleanupQuietly() {
            try {
                randomAccessFile.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static class FileSender {
        private final FileMetadata metadata;
        private final RandomAccessFile randomAccessFile;
        private final long fileSize;
        private volatile long lastAccessed;

        public FileSender(FileMetadata metadata, File file) throws IOException {
            this.metadata = metadata;
            this.randomAccessFile = new RandomAccessFile(file, "r");
            this.fileSize = randomAccessFile.length();
            this.lastAccessed = System.currentTimeMillis();
        }

        public FileMetadata getMetadata() {
            this.lastAccessed = System.currentTimeMillis();
            return metadata;
        }

        public long getFileSize() {
            this.lastAccessed = System.currentTimeMillis();
            return fileSize;
        }

        public long getLastAccessed() {
            return lastAccessed;
        }

        public synchronized byte[] read(long currentLength) throws IOException {
            this.lastAccessed = System.currentTimeMillis();
            if (currentLength >= fileSize) {
                return null;
            }
            randomAccessFile.seek(currentLength);
            int maxChunkSize = 2000;
            long remaining = fileSize - currentLength;
            int length = (int) Math.min(maxChunkSize, remaining);
            byte[] buffer = new byte[length];
            randomAccessFile.read(buffer);
            return buffer;
        }

        public void cleanupQuietly() {
            try {
                randomAccessFile.close();
            } catch (Exception ignored) {
            }
        }
    }
}
