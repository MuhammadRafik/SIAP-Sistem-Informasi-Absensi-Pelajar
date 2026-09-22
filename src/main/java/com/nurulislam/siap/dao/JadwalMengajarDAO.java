package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.HariMengajar;
import com.nurulislam.siap.model.JadwalMengajar;
import com.nurulislam.siap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk tabel tb_jadwal_mengajar.
 * Query SELECT selalu JOIN ke tb_mata_pelajaran, tb_kelas, dan tb_pengguna (guru)
 * agar layar Absensi Mata Pelajaran dan Manajemen Mata Pelajaran & Jadwal siap
 * tampil tanpa query tambahan.
 */
public class JadwalMengajarDAO {

    private static final String SELECT_JOIN =
            "SELECT j.*, mp.nama_mapel AS nama_mapel, mp.kode_mapel AS kode_mapel, "
                    + "k.nama_kelas AS nama_kelas, g.nama AS nama_guru "
                    + "FROM tb_jadwal_mengajar j "
                    + "JOIN tb_mata_pelajaran mp ON j.mapel_id = mp.mapel_id "
                    + "JOIN tb_kelas k ON j.kelas_id = k.kelas_id "
                    + "JOIN tb_pengguna g ON j.guru_id = g.pengguna_id "
                    + "JOIN tb_tahun_ajaran ta ON j.tahun_ajaran_id = ta.tahun_ajaran_id ";

    /** Jadwal mengajar guru tertentu pada hari ini, diurutkan berdasarkan jam mulai. */
    public List<JadwalMengajar> findHariIniByGuru(int guruId, HariMengajar hari) throws SQLException {
        String sql = SELECT_JOIN + "WHERE j.guru_id = ? AND j.hari = ? AND ta.status_aktif = TRUE ORDER BY j.jam_mulai ASC";
        List<JadwalMengajar> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guruId);
            ps.setString(2, hari.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /**
     * Seluruh jadwal mengajar milik satu guru, lintas hari - dipakai untuk mengisi
     * pilihan filter Kelas & Mata Pelajaran pada halaman Laporan Kehadiran versi
     * Guru, supaya guru hanya bisa memfilter kelas/mapel yang benar-benar diajarnya.
     */
    public List<JadwalMengajar> findAllByGuru(int guruId) throws SQLException {
        String sql = SELECT_JOIN + "WHERE j.guru_id = ? AND ta.status_aktif = TRUE "
                + "ORDER BY FIELD(j.hari,'SENIN','SELASA','RABU','KAMIS','JUMAT','SABTU'), j.jam_mulai ASC";
        List<JadwalMengajar> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guruId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /** Seluruh jadwal mengajar hari ini (semua guru) - dipakai Staf TU untuk memantau. */
    public List<JadwalMengajar> findHariIniSemua(HariMengajar hari) throws SQLException {
        String sql = SELECT_JOIN + "WHERE j.hari = ? ORDER BY j.jam_mulai ASC, k.nama_kelas ASC";
        List<JadwalMengajar> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hari.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /** Seluruh jadwal mengajar (lintas hari) - dipakai layar Manajemen Mata Pelajaran & Jadwal. */
    public List<JadwalMengajar> findAllDetail() throws SQLException {
        String sql = SELECT_JOIN + "ORDER BY FIELD(j.hari,'SENIN','SELASA','RABU','KAMIS','JUMAT','SABTU'), "
                + "j.jam_mulai ASC";
        List<JadwalMengajar> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public Optional<JadwalMengajar> findById(int jadwalId) throws SQLException {
        String sql = SELECT_JOIN + "WHERE j.jadwal_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jadwalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Mengecek apakah ada jadwal LAIN pada hari &amp; rentang jam yang beririsan,
     * untuk guru yang sama ATAU kelas yang sama (keduanya tidak boleh mengajar/
     * diajar dua mata pelajaran sekaligus). Dipakai validasi form sebelum simpan.
     *
     * @param excludeJadwalId saat mode ubah, ID jadwal yang sedang diedit (diabaikan
     *                        dari pengecekan); null saat mode tambah.
     */
    public boolean adaBentrok(int guruId, int kelasId, HariMengajar hari, LocalTime jamMulai, LocalTime jamSelesai,
                               Integer excludeJadwalId) throws SQLException {
        String sql = "SELECT 1 FROM tb_jadwal_mengajar "
                + "WHERE hari = ? AND jam_mulai < ? AND jam_selesai > ? AND (guru_id = ? OR kelas_id = ?)"
                + (excludeJadwalId != null ? " AND jadwal_id <> ?" : "");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hari.name());
            ps.setTime(2, Time.valueOf(jamSelesai));
            ps.setTime(3, Time.valueOf(jamMulai));
            ps.setInt(4, guruId);
            ps.setInt(5, kelasId);
            if (excludeJadwalId != null) {
                ps.setInt(6, excludeJadwalId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public JadwalMengajar insert(JadwalMengajar jadwal) throws SQLException {
        String sql = "INSERT INTO tb_jadwal_mengajar "
                + "(mapel_id, kelas_id, guru_id, tahun_ajaran_id, hari, jam_mulai, jam_selesai) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            isiParameter(ps, jadwal);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    jadwal.setJadwalId(keys.getInt(1));
                }
            }
        }
        return jadwal;
    }

    public boolean update(JadwalMengajar jadwal) throws SQLException {
        String sql = "UPDATE tb_jadwal_mengajar SET mapel_id = ?, kelas_id = ?, guru_id = ?, "
                + "tahun_ajaran_id = ?, hari = ?, jam_mulai = ?, jam_selesai = ? WHERE jadwal_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            isiParameter(ps, jadwal);
            ps.setInt(8, jadwal.getJadwalId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * @throws JadwalMasihDipakaiException jika jadwal masih memiliki riwayat absensi
     *                                      mata pelajaran terkait (FK ON DELETE RESTRICT).
     */
    public boolean delete(int jadwalId) throws SQLException, JadwalMasihDipakaiException {
        String sql = "DELETE FROM tb_jadwal_mengajar WHERE jadwal_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jadwalId);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new JadwalMasihDipakaiException(
                    "Jadwal tidak dapat dihapus karena sudah memiliki riwayat absensi mata pelajaran.");
        }
    }

    private void isiParameter(PreparedStatement ps, JadwalMengajar jadwal) throws SQLException {
        ps.setInt(1, jadwal.getMapelId());
        ps.setInt(2, jadwal.getKelasId());
        ps.setInt(3, jadwal.getGuruId());
        ps.setInt(4, jadwal.getTahunAjaranId());
        ps.setString(5, jadwal.getHari().name());
        ps.setTime(6, Time.valueOf(jadwal.getJamMulai()));
        ps.setTime(7, Time.valueOf(jadwal.getJamSelesai()));
    }

    private JadwalMengajar mapRow(ResultSet rs) throws SQLException {
        JadwalMengajar j = new JadwalMengajar();
        j.setJadwalId(rs.getInt("jadwal_id"));
        j.setMapelId(rs.getInt("mapel_id"));
        j.setKelasId(rs.getInt("kelas_id"));
        j.setGuruId(rs.getInt("guru_id"));
        j.setTahunAjaranId(rs.getInt("tahun_ajaran_id"));
        j.setHari(HariMengajar.valueOf(rs.getString("hari")));
        j.setJamMulai(rs.getTime("jam_mulai").toLocalTime());
        j.setJamSelesai(rs.getTime("jam_selesai").toLocalTime());
        j.setNamaMapel(rs.getString("nama_mapel"));
        j.setKodeMapel(rs.getString("kode_mapel"));
        j.setNamaKelas(rs.getString("nama_kelas"));
        j.setNamaGuru(rs.getString("nama_guru"));
        return j;
    }
}