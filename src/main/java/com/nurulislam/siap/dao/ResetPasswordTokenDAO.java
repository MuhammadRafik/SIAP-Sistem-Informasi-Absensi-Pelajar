package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.ResetPasswordToken;
import com.nurulislam.siap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Data Access Object untuk tabel tb_reset_password_token.
 * Dipakai pada alur "Lupa Password" di halaman Login:
 *   1. Sistem membuat token acak + waktu kedaluwarsa -> insert()
 *   2. Token dikirim/ditampilkan ke pengguna
 *   3. Saat pengguna submit token, sistem cek -> findByToken() + isExpired()
 *   4. Setelah dipakai, token dihapus -> deleteByPenggunaId()
 */
public class ResetPasswordTokenDAO {

    public ResetPasswordToken insert(ResetPasswordToken token) throws SQLException {
        String sql = "INSERT INTO tb_reset_password_token (pengguna_id, token, expired_at) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, token.getPenggunaId());
            ps.setString(2, token.getToken());
            ps.setTimestamp(3, Timestamp.valueOf(token.getExpiredAt()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    token.setTokenId(keys.getInt(1));
                }
            }
        }
        return token;
    }

    public Optional<ResetPasswordToken> findByToken(String tokenValue) throws SQLException {
        String sql = "SELECT * FROM tb_reset_password_token WHERE token = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenValue);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Hapus semua token milik satu pengguna (dipanggil setelah token berhasil dipakai). */
    public void deleteByPenggunaId(int penggunaId) throws SQLException {
        String sql = "DELETE FROM tb_reset_password_token WHERE pengguna_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            ps.executeUpdate();
        }
    }

    /** Bersihkan token yang sudah kedaluwarsa (bisa dijalankan berkala saat aplikasi start). */
    public void deleteExpired() throws SQLException {
        String sql = "DELETE FROM tb_reset_password_token WHERE expired_at < NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private ResetPasswordToken mapRow(ResultSet rs) throws SQLException {
        return new ResetPasswordToken(
                rs.getInt("token_id"),
                rs.getInt("pengguna_id"),
                rs.getString("token"),
                rs.getTimestamp("expired_at").toLocalDateTime()
        );
    }
}
