package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.MataPelajaran;
import com.nurulislam.siap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk tabel tb_mata_pelajaran.
 * Dipakai layar Manajemen Mata Pelajaran & Jadwal Mengajar (khusus Staf TU).
 */
public class MataPelajaranDAO {

    public List<MataPelajaran> findAll() throws SQLException {
        String sql = "SELECT * FROM tb_mata_pelajaran ORDER BY nama_mapel ASC";
        List<MataPelajaran> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public Optional<MataPelajaran> findById(int mapelId) throws SQLException {
        String sql = "SELECT * FROM tb_mata_pelajaran WHERE mapel_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mapelId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public boolean existsByKode(String kodeMapel, Integer excludeMapelId) throws SQLException {
        String sql = "SELECT 1 FROM tb_mata_pelajaran WHERE kode_mapel = ?"
                + (excludeMapelId != null ? " AND mapel_id <> ?" : "");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kodeMapel);
            if (excludeMapelId != null) {
                ps.setInt(2, excludeMapelId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public MataPelajaran insert(MataPelajaran mapel) throws SQLException {
        String sql = "INSERT INTO tb_mata_pelajaran (kode_mapel, nama_mapel) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, mapel.getKodeMapel());
            ps.setString(2, mapel.getNamaMapel());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    mapel.setMapelId(keys.getInt(1));
                }
            }
        }
        return mapel;
    }

    public boolean update(MataPelajaran mapel) throws SQLException {
        String sql = "UPDATE tb_mata_pelajaran SET kode_mapel = ?, nama_mapel = ? WHERE mapel_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mapel.getKodeMapel());
            ps.setString(2, mapel.getNamaMapel());
            ps.setInt(3, mapel.getMapelId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * @throws MataPelajaranMasihDipakaiException jika mata pelajaran masih dipakai
     *                                             pada jadwal mengajar (FK ON DELETE RESTRICT).
     */
    public boolean delete(int mapelId) throws SQLException, MataPelajaranMasihDipakaiException {
        String sql = "DELETE FROM tb_mata_pelajaran WHERE mapel_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mapelId);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new MataPelajaranMasihDipakaiException(
                    "Mata pelajaran tidak dapat dihapus karena masih dipakai pada jadwal mengajar. "
                            + "Hapus atau ubah jadwal terkait terlebih dahulu.");
        }
    }

    private MataPelajaran mapRow(ResultSet rs) throws SQLException {
        return new MataPelajaran(
                rs.getInt("mapel_id"),
                rs.getString("kode_mapel"),
                rs.getString("nama_mapel")
        );
    }
}