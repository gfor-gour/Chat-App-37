package com.raven.server.repository;

import com.raven.shared.dto.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class FileRepository {
    private static final Logger log = LoggerFactory.getLogger(FileRepository.class);
    private final DataSource dataSource;

    public FileRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public FileMetadata insertFile(String fileExtension) throws SQLException {
        String sql = "INSERT INTO files (FileExtension) VALUES (?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fileExtension);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int fileID = rs.getInt(1);
                    return new FileMetadata(fileID, fileExtension);
                } else {
                    throw new SQLException("Inserting file failed, no ID obtained.");
                }
            }
        }
    }

    public void updateBlurHashDone(int fileID, String blurHash) throws SQLException {
        String sql = "UPDATE files SET BlurHash = ?, Status = '1' WHERE FileID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, blurHash);
            ps.setInt(2, fileID);
            ps.executeUpdate();
        }
    }

    public void updateDone(int fileID) throws SQLException {
        String sql = "UPDATE files SET Status = '1' WHERE FileID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fileID);
            ps.executeUpdate();
        }
    }

    public Optional<FileMetadata> findById(int fileID) throws SQLException {
        String sql = "SELECT FileExtension FROM files WHERE FileID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fileID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String extension = rs.getString(1);
                    return Optional.of(new FileMetadata(fileID, extension));
                }
            }
        }
        return Optional.empty();
    }
}
