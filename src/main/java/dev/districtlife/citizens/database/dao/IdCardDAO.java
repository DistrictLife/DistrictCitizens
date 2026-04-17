package dev.districtlife.citizens.database.dao;

import dev.districtlife.citizens.model.IdCard;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class IdCardDAO {

    private final Connection connection;

    public IdCardDAO(Connection connection) {
        this.connection = connection;
    }

    public void insert(IdCard idCard) throws SQLException {
        String sql = "INSERT INTO id_cards (serial, owner_uuid, issued_at, reissue_count) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, idCard.getSerial());
            ps.setString(2, idCard.getOwnerUuid());
            ps.setLong(3, idCard.getIssuedAt());
            ps.setInt(4, idCard.getReissueCount());
            ps.executeUpdate();
        }
    }

    public Optional<IdCard> findBySerial(String serial) throws SQLException {
        String sql = "SELECT * FROM id_cards WHERE serial = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, serial);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<IdCard> findByOwnerUuid(UUID ownerUuid) throws SQLException {
        String sql = "SELECT * FROM id_cards WHERE owner_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void incrementReissueCount(String serial) throws SQLException {
        String sql = "UPDATE id_cards SET reissue_count = reissue_count + 1 WHERE serial = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, serial);
            ps.executeUpdate();
        }
    }

    private IdCard mapRow(ResultSet rs) throws SQLException {
        return new IdCard(
            rs.getString("serial"),
            rs.getString("owner_uuid"),
            rs.getLong("issued_at"),
            rs.getInt("reissue_count")
        );
    }
}
