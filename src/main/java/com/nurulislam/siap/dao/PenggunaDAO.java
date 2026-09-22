package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.Role;
import com.nurulislam.siap.model.StatusAkun;
import com.nurulislam.siap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk tabel tb_pengguna.
 * Menangani seluruh operasi database terkait akun (Staf TU & Guru):
 * pendaftaran akun, login, verifikasi akun oleh TU, dan reset password.
 */
public class PenggunaDAO {

    /** Menyimpan akun baru (hasil "Daftar Akun"). Status awal ditentukan oleh pemanggil. */
    public Pengguna insert(Pengguna pengguna) throws SQLException {
        String sql = "INSERT INTO tb_pengguna (email, password_hash, nama, role, status_akun) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, pengguna.getEmail());
            ps.setString(2, pengguna.getPasswordHash());
            ps.setString(3, pengguna.getNama());
            ps.setString(4, pengguna.getRole().name());
            ps.setString(5, pengguna.getStatusAkun().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    pengguna.setPenggunaId(keys.getInt(1));
                }
            }
        }
        return pengguna;
    }

    public boolean update(Pengguna pengguna) throws SQLException {
        String sql = "UPDATE tb_pengguna SET email = ?, nama = ?, role = ?, status_akun = ? "
                + "WHERE pengguna_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pengguna.getEmail());
            ps.setString(2, pengguna.getNama());
            ps.setString(3, pengguna.getRole().name());
            ps.setString(4, pengguna.getStatusAkun().name());
            ps.setInt(5, pengguna.getPenggunaId());

            return ps.executeUpdate() > 0;
        }
    }

    /** Mengganti password (dipakai saat proses "Lupa Password" / reset). */
    public boolean updatePassword(int penggunaId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE tb_pengguna SET password_hash = ? WHERE pengguna_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, penggunaId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Mengubah status akun, dipakai oleh Staf TU di halaman "Verifikasi Akun". */
    public boolean updateStatusAkun(int penggunaId, StatusAkun statusAkun) throws SQLException {
        String sql = "UPDATE tb_pengguna SET status_akun = ? WHERE pengguna_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusAkun.name());
            ps.setInt(2, penggunaId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int penggunaId) throws SQLException {
        String sql = "DELETE FROM tb_pengguna WHERE pengguna_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<Pengguna> findById(int penggunaId) throws SQLException {
        String sql = "SELECT * FROM tb_pengguna WHERE pengguna_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, penggunaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Dipakai saat login: cari akun berdasarkan email yang diinput pengguna. */
    public Optional<Pengguna> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM tb_pengguna WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM tb_pengguna WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Pengguna> findAll() throws SQLException {
        String sql = "SELECT * FROM tb_pengguna ORDER BY nama ASC";
        List<Pengguna> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<Pengguna> findByRole(Role role) throws SQLException {
        String sql = "SELECT * FROM tb_pengguna WHERE role = ? ORDER BY nama ASC";
        List<Pengguna> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /** Daftar akun yang menunggu diverifikasi TU, untuk halaman "Verifikasi Akun". */
    public List<Pengguna> findByStatus(StatusAkun statusAkun) throws SQLException {
        String sql = "SELECT * FROM tb_pengguna WHERE status_akun = ? ORDER BY created_at ASC";
        List<Pengguna> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusAkun.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    private Pengguna mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        return new Pengguna(
                rs.getInt("pengguna_id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("nama"),
                Role.valueOf(rs.getString("role")),
                StatusAkun.valueOf(rs.getString("status_akun")),
                createdAtTs != null ? createdAtTs.toLocalDateTime() : null
        );
    }
}
