package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.TahunAjaran;
import com.nurulislam.siap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk tabel tb_tahun_ajaran.
 * Modul Data Master (Kelas, Mata Pelajaran & Jadwal) selalu mengaitkan data
 * ke tahun ajaran yang sedang AKTIF (status_aktif = TRUE), sesuai seed awal
 * pada siap_schema.sql.
 */
public class TahunAjaranDAO {

    public List<TahunAjaran> findAll() throws SQLException {
        String sql = "SELECT * FROM tb_tahun_ajaran ORDER BY tahun DESC, semester ASC";
        List<TahunAjaran> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    /** Tahun ajaran yang sedang berjalan (status_aktif = TRUE). Dipakai sebagai default saat menambah data baru. */
    public Optional<TahunAjaran> findAktif() throws SQLException {
        String sql = "SELECT * FROM tb_tahun_ajaran WHERE status_aktif = TRUE LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        }
    }

    public Optional<TahunAjaran> findById(int tahunAjaranId) throws SQLException {
        String sql = "SELECT * FROM tb_tahun_ajaran WHERE tahun_ajaran_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tahunAjaranId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private TahunAjaran mapRow(ResultSet rs) throws SQLException {
        return new TahunAjaran(
                rs.getInt("tahun_ajaran_id"),
                rs.getString("tahun"),
                rs.getString("semester"),
                rs.getBoolean("status_aktif")
        );
    }
}