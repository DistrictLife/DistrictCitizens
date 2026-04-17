package dev.districtlife.citizens.database.dao;

import dev.districtlife.citizens.model.Citizen;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class CitizenDAO {

    private final Connection connection;

    public CitizenDAO(Connection connection) {
        this.connection = connection;
    }

    public void insert(Citizen citizen) throws SQLException {
        String sql = "INSERT INTO citizens (uuid, minecraft_name, first_name, last_name, birth_date, registered_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, citizen.getUuid());
            ps.setString(2, citizen.getMinecraftName());
            ps.setString(3, citizen.getFirstName());
            ps.setString(4, citizen.getLastName());
            ps.setString(5, citizen.getBirthDate());
            ps.setLong(6, citizen.getRegisteredAt());
            ps.executeUpdate();
        }
    }

    public Optional<Citizen> findByUuid(UUID uuid) throws SQLException {
        String sql = "SELECT * FROM citizens WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Citizen> findByFullName(String firstName, String lastName) throws SQLException {
        String sql = "SELECT * FROM citizens WHERE LOWER(first_name) = LOWER(?) AND LOWER(last_name) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean existsByFullName(String firstName, String lastName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM citizens WHERE LOWER(first_name) = LOWER(?) AND LOWER(last_name) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void updateFirstName(UUID uuid, String firstName) throws SQLException {
        String sql = "UPDATE citizens SET first_name = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void updateLastName(UUID uuid, String lastName) throws SQLException {
        String sql = "UPDATE citizens SET last_name = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, lastName);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void updateBirthDate(UUID uuid, String birthDate) throws SQLException {
        String sql = "UPDATE citizens SET birth_date = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, birthDate);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void deleteByUuid(UUID uuid) throws SQLException {
        String sql = "DELETE FROM citizens WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public int countAll() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM citizens")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Citizen mapRow(ResultSet rs) throws SQLException {
        return new Citizen(
            rs.getString("uuid"),
            rs.getString("minecraft_name"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("birth_date"),
            rs.getLong("registered_at")
        );
    }
}
