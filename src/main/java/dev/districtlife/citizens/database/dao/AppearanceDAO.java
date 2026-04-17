package dev.districtlife.citizens.database.dao;

import dev.districtlife.citizens.model.Appearance;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class AppearanceDAO {

    private final Connection connection;

    public AppearanceDAO(Connection connection) {
        this.connection = connection;
    }

    public void insert(Appearance appearance) throws SQLException {
        String sql = "INSERT INTO appearances (uuid, skin_tone, eye_color, hair_style, hair_color) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, appearance.getUuid());
            ps.setInt(2, appearance.getSkinTone());
            ps.setInt(3, appearance.getEyeColor());
            ps.setInt(4, appearance.getHairStyle());
            ps.setInt(5, appearance.getHairColor());
            ps.executeUpdate();
        }
    }

    public Optional<Appearance> findByUuid(UUID uuid) throws SQLException {
        String sql = "SELECT * FROM appearances WHERE uuid = ?";
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

    public void updateByUuid(Appearance appearance) throws SQLException {
        String sql = "UPDATE appearances SET skin_tone = ?, eye_color = ?, hair_style = ?, hair_color = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, appearance.getSkinTone());
            ps.setInt(2, appearance.getEyeColor());
            ps.setInt(3, appearance.getHairStyle());
            ps.setInt(4, appearance.getHairColor());
            ps.setString(5, appearance.getUuid());
            ps.executeUpdate();
        }
    }

    private Appearance mapRow(ResultSet rs) throws SQLException {
        return new Appearance(
            rs.getString("uuid"),
            rs.getInt("skin_tone"),
            rs.getInt("eye_color"),
            rs.getInt("hair_style"),
            rs.getInt("hair_color")
        );
    }
}
