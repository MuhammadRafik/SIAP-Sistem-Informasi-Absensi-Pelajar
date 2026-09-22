package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.Murid;
import com.nurulislam.siap.model.Statusmurid;
import com.nurulislam.siap.util.DatabaseConnection;
import com.nurulislam.siap.util.TokenUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk tabel tb_murid.
 * Query SELECT selalu JOIN ke tb_kelas agar nama_kelas siap dipakai
 * langsung oleh layar (Scan QR, Data Murid, dst) tanpa query kedua.
 */
public class MuridDAO {

    private static final int PANJANG_QR_TOKEN = 32;

    private static final String SELECT_JOIN_KELAS =
            "SELECT m.*, k.nama_kelas AS nama_kelas_join "
                    + "FROM tb_murid m JOIN tb_kelas k ON m.kelas_id = k.kelas_id ";

    /** Dipakai halaman Scan QR: mencari murid berdasarkan token unik pada QR Code. */
    public Optional<Murid> findByQrToken(String qrToken) throws SQLException {
        String sql = SELECT_JOIN_KELAS + "WHERE m.qr_token = ? AND m.status = 'AKTIF'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, qrToken);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Murid> findByNis(String nis) throws SQLException {
        String sql = SELECT_JOIN_KELAS + "WHERE m.nis = ? AND m.status = 'AKTIF'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nis);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Murid> findById(int muridId) throws SQLException {
        String sql = SELECT_JOIN_KELAS + "WHERE m.murid_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, muridId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Murid> findAll() throws SQLException {
        String sql = SELECT_JOIN_KELAS + "ORDER BY m.nama ASC";
        List<Murid> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<Murid> findByKelas(int kelasId) throws SQLException {
        String sql = SELECT_JOIN_KELAS + "WHERE m.kelas_id = ? ORDER BY m.nama ASC";
        List<Murid> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kelasId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    public boolean existsByNis(String nis, Integer excludeMuridId) throws SQLException {
        String sql = "SELECT 1 FROM tb_murid WHERE nis = ?" + (excludeMuridId != null ? " AND murid_id <> ?" : "");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nis);
            if (excludeMuridId != null) {
                ps.setInt(2, excludeMuridId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Menyimpan murid baru. qr_token dibuat otomatis secara acak di sini (bukan
     * diinput manual dari form) supaya token selalu unik dan tidak bisa ditebak
     * dari NIS - lihat catatan keamanan pada TokenUtil#generateAlphanumericToken.
     */
    public Murid insert(Murid murid) throws SQLException {
        String sql = "INSERT INTO tb_murid (nis, nama, jenis_kelamin, tanggal_lahir, foto, kelas_id, qr_token, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        String qrToken = TokenUtil.generateAlphanumericToken(PANJANG_QR_TOKEN);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, murid.getNis());
            ps.setString(2, murid.getNama());
            ps.setString(3, murid.getJenisKelamin());
            ps.setDate(4, murid.getTanggalLahir() != null ? Date.valueOf(murid.getTanggalLahir()) : null);
            ps.setString(5, murid.getFoto());
            ps.setInt(6, murid.getKelasId());
            ps.setString(7, qrToken);
            ps.setString(8, murid.getStatus().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    murid.setMuridId(keys.getInt(1));
                }
            }
        }
        murid.setQrToken(qrToken);
        return murid;
    }

    /** Data inti murid diubah lewat form; qr_token TIDAK ikut diubah di sini (lihat regenerateQrToken). */
    public boolean update(Murid murid) throws SQLException {
        String sql = "UPDATE tb_murid SET nis = ?, nama = ?, jenis_kelamin = ?, tanggal_lahir = ?, "
                + "foto = ?, kelas_id = ?, status = ? WHERE murid_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, murid.getNis());
            ps.setString(2, murid.getNama());
            ps.setString(3, murid.getJenisKelamin());
            ps.setDate(4, murid.getTanggalLahir() != null ? Date.valueOf(murid.getTanggalLahir()) : null);
            ps.setString(5, murid.getFoto());
            ps.setInt(6, murid.getKelasId());
            ps.setString(7, murid.getStatus().name());
            ps.setInt(8, murid.getMuridId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Membuat ulang qr_token murid (mis. jika kartu pelajar hilang/rusak dan perlu
     * dicetak ulang dengan kode baru, supaya kartu lama otomatis tidak berlaku lagi).
     */
    public String regenerateQrToken(int muridId) throws SQLException {
        String qrTokenBaru = TokenUtil.generateAlphanumericToken(PANJANG_QR_TOKEN);
        String sql = "UPDATE tb_murid SET qr_token = ? WHERE murid_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, qrTokenBaru);
            ps.setInt(2, muridId);
            ps.executeUpdate();
        }
        return qrTokenBaru;
    }

    /**
     * Mengecek apakah murid sudah memiliki riwayat absensi.
     * Jika sudah memiliki histori, murid tidak boleh dihapus secara permanen.
     */
    public boolean hasAbsensiHistory(int muridId) throws SQLException {
        String sql = "SELECT 1 FROM tb_absensi WHERE murid_id = ? LIMIT 1";
        String sqlMapel = "SELECT 1 FROM tb_absensi_mapel WHERE murid_id = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, muridId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlMapel)) {
            ps.setInt(1, muridId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Menonaktifkan murid tanpa menghapus data. Histori absensi tetap aman.
     */
    public boolean archive(int muridId) throws SQLException {
        String sql = "UPDATE tb_murid SET status = 'NONAKTIF' WHERE murid_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, muridId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int muridId) throws SQLException {
        String sql = "DELETE FROM tb_murid WHERE murid_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, muridId);
            return ps.executeUpdate() > 0;
        }
    }

    private Murid mapRow(ResultSet rs) throws SQLException {
        Date tglLahirSql = rs.getDate("tanggal_lahir");
        Murid murid = new Murid(
                rs.getInt("murid_id"),
                rs.getString("nis"),
                rs.getString("nama"),
                rs.getString("jenis_kelamin"),
                tglLahirSql != null ? tglLahirSql.toLocalDate() : null,
                rs.getString("foto"),
                rs.getInt("kelas_id"),
                rs.getString("qr_token"),
                Statusmurid.valueOf(rs.getString("status"))
        );
        murid.setNamaKelas(rs.getString("nama_kelas_join"));
        return murid;
    }
}