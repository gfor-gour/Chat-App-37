package com.raven.server.repository;

import com.raven.shared.dto.UserAccountDto;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean existsByUserName(String userName) throws SQLException {
        String sql = "SELECT 1 FROM \"user\" WHERE UserName = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertUser(String userName, String passwordHash) throws SQLException {
        String sql = "INSERT INTO \"user\" (UserName, Password) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, userName);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Inserting user failed, no ID obtained.");
                }
            }
        }
    }

    public void insertUserAccount(int userID, String userName) throws SQLException {
        String sql = "INSERT INTO user_account (UserID, UserName, Gender, ImageString, Status) VALUES (?, ?, '', '', '1')";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setString(2, userName);
            ps.executeUpdate();
        }
    }

    /**
     * Finds a user's BCrypt password hash and account details by username.
     */
    public Optional<UserRecord> findByUserName(String userName) throws SQLException {
        String sql = "SELECT UserID, user_account.UserName, Gender, ImageString, Password " +
                     "FROM \"user\" JOIN user_account USING (UserID) " +
                     "WHERE \"user\".UserName = ? AND user_account.Status = '1'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userID = rs.getInt(1);
                    String name = rs.getString(2);
                    String gender = rs.getString(3);
                    String image = rs.getString(4);
                    String passwordHash = rs.getString(5);
                    UserAccountDto account = new UserAccountDto(userID, name, gender, image, true);
                    return Optional.of(new UserRecord(account, passwordHash));
                }
            }
        }
        return Optional.empty();
    }

    public List<UserAccountDto> findAllExcept(int excludeUserID) throws SQLException {
        List<UserAccountDto> list = new ArrayList<>();
        String sql = "SELECT UserID, UserName, Gender, ImageString FROM user_account WHERE Status = '1' AND UserID <> ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, excludeUserID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int userID = rs.getInt(1);
                    String userName = rs.getString(2);
                    String gender = rs.getString(3);
                    String image = rs.getString(4);
                    list.add(new UserAccountDto(userID, userName, gender, image, false));
                }
            }
        }
        return list;
    }

    /**
     * Helper record to hold DTO and corresponding BCrypt password hash.
     */
    public static class UserRecord {
        private final UserAccountDto account;
        private final String passwordHash;

        public UserRecord(UserAccountDto account, String passwordHash) {
            this.account = account;
            this.passwordHash = passwordHash;
        }

        public UserAccountDto getAccount() {
            return account;
        }

        public String getPasswordHash() {
            return passwordHash;
        }
    }
}
