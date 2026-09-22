package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.Kelas;
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
 * Data Access Object untuk tabel tb_kelas.
 * Dipakai antara lain oleh layar Manajemen Data Kelas (CRUD, khusus Staf TU).
 */
public class KelasDAO {

    /** Query dasar (tanpa JOIN), dipakai DAO lain yang hanya butuh data mentah tb_kelas. */
    public Optional<Kelas> findById(int kelasId) throws SQLException {
        String sql = "SELECT * FROM tb_kelas WHERE kelas_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kelasId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Kelas> findAll() throws SQLException {
        String sql = "SELECT * FROM tb_kelas ORDER BY tingkat ASC, nama_kelas ASC";
        List<Kelas> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    /**
     * Daftar kelas lengkap dengan nama wali kelas, jumlah murid AKTIF, dan label
     * tahun ajaran - khusus untuk ditampilkan di tabel layar Manajemen Data Kelas
     * (menghindari query N+1 per baris).
     */
    public List<Kelas> findAllDetail() throws SQLException {
        String sql = "SELECT k.*, "
                + "w.nama AS wali_nama, "
                + "ta.tahun AS ta_tahun, ta.semester AS ta_semester, "
                + "COALESCE(jm.jumlah, 0) AS jumlah_murid "
                + "FROM tb_kelas k "
                + "LEFT JOIN tb_pengguna w ON k.wali_kelas_id = w.pengguna_id "
                + "LEFT JOIN tb_tahun_ajaran ta ON k.tahun_ajaran_id = ta.tahun_ajaran_id "
                + "LEFT JOIN (SELECT kelas_id, COUNT(*) AS jumlah FROM tb_murid "
                + "           WHERE status = 'AKTIF' GROUP BY kelas_id) jm ON jm.kelas_id = k.kelas_id "
                + "ORDER BY k.tingkat ASC, k.nama_kelas ASC";

        List<Kelas> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Kelas kelas = mapRow(rs);
                kelas.setWaliKelasNama(rs.getString("wali_nama") != null ? rs.getString("wali_nama") : "-");
                kelas.setJumlahMurid(rs.getInt("jumlah_murid"));
                String tahun = rs.getString("ta_tahun");
                String semester = rs.getString("ta_semester");
                if (tahun != null) {
                    kelas.setTahunAjaranLabel(tahun + " - " + ("GENAP".equals(semester) ? "Genap" : "Ganjil"));
                } else {
                    kelas.setTahunAjaranLabel("-");
                }
                result.add(kelas);
            }
        }
        return result;
    }

    /** Dipakai validasi form: cegah nama kelas ganda pada tahun ajaran yang sama. */
    public boolean existsByNamaDanTahun(String namaKelas, int tahunAjaranId, Integer excludeKelasId) throws SQLException {
        String sql = "SELECT 1 FROM tb_kelas WHERE nama_kelas = ? AND tahun_ajaran_id = ?"
                + (excludeKelasId != null ? " AND kelas_id <> ?" : "");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaKelas);
            ps.setInt(2, tahunAjaranId);
            if (excludeKelasId != null) {
                ps.setInt(3, excludeKelasId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Kelas insert(Kelas kelas) throws SQLException {
        String sql = "INSERT INTO tb_kelas (nama_kelas, tingkat, jurusan, wali_kelas_id, tahun_ajaran_id) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            isiParameter(ps, kelas);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    kelas.setKelasId(keys.getInt(1));
                }
            }
        }
        return kelas;
    }

    public boolean update(Kelas kelas) throws SQLException {
        String sql = "UPDATE tb_kelas SET nama_kelas = ?, tingkat = ?, jurusan = ?, "
                + "wali_kelas_id = ?, tahun_ajaran_id = ? WHERE kelas_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            isiParameter(ps, kelas);
            ps.setInt(6, kelas.getKelasId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * @throws KelasMasihDipakaiException jika kelas masih direferensikan murid/jadwal
     *                                     (FK ON DELETE RESTRICT pada siap_schema.sql).
     */
    public boolean delete(int kelasId) throws SQLException, KelasMasihDipakaiException {
        String sql = "DELETE FROM tb_kelas WHERE kelas_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kelasId);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new KelasMasihDipakaiException(
                    "Kelas tidak dapat dihapus karena masih memiliki murid dan/atau jadwal mengajar terkait.");
        }
    }

    private void isiParameter(PreparedStatement ps, Kelas kelas) throws SQLException {
        ps.setString(1, kelas.getNamaKelas());
        ps.setString(2, kelas.getTingkat());
        ps.setString(3, kelas.getJurusan());
        if (kelas.getWaliKelasId() != null) {
            ps.setInt(4, kelas.getWaliKelasId());
        } else {
            ps.setNull(4, java.sql.Types.INTEGER);
        }
        ps.setInt(5, kelas.getTahunAjaranId());
    }

    private Kelas mapRow(ResultSet rs) throws SQLException {
        int waliRaw = rs.getInt("wali_kelas_id");
        Integer waliKelasId = rs.wasNull() ? null : waliRaw;
        return new Kelas(
                rs.getInt("kelas_id"),
                rs.getString("nama_kelas"),
                rs.getString("tingkat"),
                rs.getString("jurusan"),
                waliKelasId,
                rs.getInt("tahun_ajaran_id")
        );
    }
}