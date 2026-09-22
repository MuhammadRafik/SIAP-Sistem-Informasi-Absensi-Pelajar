package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.AbsensiMapel;
import com.nurulislam.siap.model.StatusAbsensi;
import com.nurulislam.siap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object untuk tabel tb_absensi_mapel (absensi per mata pelajaran,
 * dicatat oleh Guru lewat halaman "Absensi Mata Pelajaran").
 * Query SELECT JOIN ke tb_murid agar nama & NIS siap dipakai langsung oleh
 * tabel "Absen Masuk" tanpa query kedua.
 */
public class AbsensiMapelDAO {

    private static final String SELECT_JOIN =
            "SELECT am.*, m.nama AS nama_murid, m.nis AS nis, m.status AS status_murid "
                    + "FROM tb_absensi_mapel am "
                    + "JOIN tb_murid m ON am.murid_id = m.murid_id ";

    /**
     * JOIN lengkap sampai ke kelas, mata pelajaran, dan guru pengajar - dipakai
     * khusus oleh halaman Laporan Kehadiran (Staf TU) supaya Admin bisa melihat
     * rekap absensi mata pelajaran dari SEMUA guru & kelas, bukan hanya milik
     * satu guru pada satu jadwal seperti SELECT_JOIN di atas.
     */
    private static final String SELECT_JOIN_LAPORAN =
            "SELECT am.*, m.nama AS nama_murid, m.nis AS nis, k.nama_kelas AS nama_kelas, m.status AS status_murid, "
                    + "mp.nama_mapel AS nama_mapel, g.nama AS nama_guru "
                    + "FROM tb_absensi_mapel am "
                    + "JOIN tb_murid m ON am.murid_id = m.murid_id "
                    + "JOIN tb_jadwal_mengajar j ON am.jadwal_id = j.jadwal_id "
                    + "JOIN tb_kelas k ON j.kelas_id = k.kelas_id "
                    + "JOIN tb_mata_pelajaran mp ON j.mapel_id = mp.mapel_id "
                    + "JOIN tb_pengguna g ON j.guru_id = g.pengguna_id ";

    /**
     * Menyimpan satu baris absensi mata pelajaran hasil scan QR / input manual.
     *
     * @throws AbsensiSudahAdaException jika murid tersebut sudah absen pada jadwal & tanggal yang sama
     */
    public AbsensiMapel insert(AbsensiMapel absensi) throws SQLException, AbsensiSudahAdaException {
        String sql = "INSERT INTO tb_absensi_mapel (murid_id, jadwal_id, pengguna_id, tanggal, waktu_scan, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, absensi.getMuridId());
            ps.setInt(2, absensi.getJadwalId());
            ps.setInt(3, absensi.getPenggunaId());
            ps.setDate(4, Date.valueOf(absensi.getTanggal()));
            ps.setTime(5, Time.valueOf(absensi.getWaktuScan()));
            ps.setString(6, absensi.getStatus().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    absensi.setAbsensiMapelId(keys.getInt(1));
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new AbsensiSudahAdaException("Murid ini sudah tercatat absen untuk mata pelajaran ini hari ini.");
        }
        return absensi;
    }

    /** Daftar "Absen Masuk" untuk satu sesi jadwal & tanggal, terbaru di atas. */
    public List<AbsensiMapel> findByJadwalTanggal(int jadwalId, LocalDate tanggal) throws SQLException {
        String sql = SELECT_JOIN + "WHERE am.jadwal_id = ? AND am.tanggal = ? "
                + "ORDER BY am.waktu_scan DESC, am.absensi_mapel_id DESC";
        List<AbsensiMapel> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jadwalId);
            ps.setDate(2, Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /** Rekap jumlah per status untuk satu sesi jadwal & tanggal, dipakai kartu rekap. */
    public Map<StatusAbsensi, Integer> hitungRekap(int jadwalId, LocalDate tanggal) throws SQLException {
        Map<StatusAbsensi, Integer> rekap = new EnumMap<>(StatusAbsensi.class);
        for (StatusAbsensi s : StatusAbsensi.values()) {
            rekap.put(s, 0);
        }
        String sql = "SELECT status, COUNT(*) AS jumlah FROM tb_absensi_mapel "
                + "WHERE jadwal_id = ? AND tanggal = ? GROUP BY status";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jadwalId);
            ps.setDate(2, Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rekap.put(StatusAbsensi.valueOf(rs.getString("status")), rs.getInt("jumlah"));
                }
            }
        }
        return rekap;
    }

    /**
     * Daftar absensi mata pelajaran lintas kelas/guru sesuai filter - dipakai
     * tab "Absensi Mata Pelajaran" pada halaman Laporan Kehadiran (Staf TU).
     * Semua parameter filter bersifat opsional (null berarti tidak difilter).
     *
     * @param tglMulai rentang tanggal awal (inklusif), wajib diisi
     * @param tglAkhir rentang tanggal akhir (inklusif), wajib diisi
     * @param kelasId  filter kelas tertentu (lewat jadwal), null berarti semua kelas
     * @param mapelId  filter mata pelajaran tertentu, null berarti semua mata pelajaran
     * @param status   filter status tertentu, null berarti semua status
     * @param kataKunci filter nama murid/NIS (LIKE, case-insensitive), null/kosong berarti tidak difilter
     */
    public List<AbsensiMapel> findFiltered(LocalDate tglMulai, LocalDate tglAkhir, Integer kelasId,
                                            Integer mapelId, StatusAbsensi status, String kataKunci) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_JOIN_LAPORAN)
                .append("WHERE am.tanggal BETWEEN ? AND ? ");
        if (kelasId != null) {
            sql.append("AND k.kelas_id = ? ");
        }
        if (mapelId != null) {
            sql.append("AND mp.mapel_id = ? ");
        }
        if (status != null) {
            sql.append("AND am.status = ? ");
        }
        if (kataKunci != null && !kataKunci.isBlank()) {
            sql.append("AND (m.nama LIKE ? OR m.nis LIKE ?) ");
        }
        sql.append("ORDER BY am.tanggal DESC, am.waktu_scan DESC, am.absensi_mapel_id DESC");

        List<AbsensiMapel> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(tglMulai));
            ps.setDate(idx++, Date.valueOf(tglAkhir));
            if (kelasId != null) {
                ps.setInt(idx++, kelasId);
            }
            if (mapelId != null) {
                ps.setInt(idx++, mapelId);
            }
            if (status != null) {
                ps.setString(idx++, status.name());
            }
            if (kataKunci != null && !kataKunci.isBlank()) {
                String pola = "%" + kataKunci.trim() + "%";
                ps.setString(idx++, pola);
                ps.setString(idx, pola);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRowLaporan(rs));
                }
            }
        }
        return result;
    }

    /**
     * Rekap jumlah per status pada rentang tanggal (kelas & mata pelajaran opsional)
     * lintas kelas/guru - dipakai kartu ringkasan tab "Absensi Mata Pelajaran".
     */
    public Map<StatusAbsensi, Integer> hitungRekap(LocalDate tglMulai, LocalDate tglAkhir,
                                                     Integer kelasId, Integer mapelId) throws SQLException {
        Map<StatusAbsensi, Integer> rekap = new EnumMap<>(StatusAbsensi.class);
        for (StatusAbsensi s : StatusAbsensi.values()) {
            rekap.put(s, 0);
        }
        StringBuilder sql = new StringBuilder(
                "SELECT am.status, COUNT(*) AS jumlah FROM tb_absensi_mapel am "
                        + "JOIN tb_jadwal_mengajar j ON am.jadwal_id = j.jadwal_id "
                        + "WHERE am.tanggal BETWEEN ? AND ? ");
        if (kelasId != null) {
            sql.append("AND j.kelas_id = ? ");
        }
        if (mapelId != null) {
            sql.append("AND j.mapel_id = ? ");
        }
        sql.append("GROUP BY am.status");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(tglMulai));
            ps.setDate(idx++, Date.valueOf(tglAkhir));
            if (kelasId != null) {
                ps.setInt(idx++, kelasId);
            }
            if (mapelId != null) {
                ps.setInt(idx, mapelId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rekap.put(StatusAbsensi.valueOf(rs.getString("status")), rs.getInt("jumlah"));
                }
            }
        }
        return rekap;
    }

    /**
     * Versi "milik satu guru" dari {@link #findFiltered(LocalDate, LocalDate, Integer, Integer, StatusAbsensi, String)},
     * dipakai halaman Laporan Kehadiran versi Guru - guru hanya boleh melihat
     * rekap absensi dari jadwal mengajarnya sendiri, tidak seperti Admin yang
     * bisa melihat lintas guru.
     *
     * @param guruId wajib diisi (pengguna_id guru yang sedang login)
     */
    public List<AbsensiMapel> findFiltered(LocalDate tglMulai, LocalDate tglAkhir, Integer kelasId,
                                            Integer mapelId, int guruId, StatusAbsensi status,
                                            String kataKunci) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_JOIN_LAPORAN)
                .append("WHERE am.tanggal BETWEEN ? AND ? AND j.guru_id = ? ");
        if (kelasId != null) {
            sql.append("AND k.kelas_id = ? ");
        }
        if (mapelId != null) {
            sql.append("AND mp.mapel_id = ? ");
        }
        if (status != null) {
            sql.append("AND am.status = ? ");
        }
        if (kataKunci != null && !kataKunci.isBlank()) {
            sql.append("AND (m.nama LIKE ? OR m.nis LIKE ?) ");
        }
        sql.append("ORDER BY am.tanggal DESC, am.waktu_scan DESC, am.absensi_mapel_id DESC");

        List<AbsensiMapel> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(tglMulai));
            ps.setDate(idx++, Date.valueOf(tglAkhir));
            ps.setInt(idx++, guruId);
            if (kelasId != null) {
                ps.setInt(idx++, kelasId);
            }
            if (mapelId != null) {
                ps.setInt(idx++, mapelId);
            }
            if (status != null) {
                ps.setString(idx++, status.name());
            }
            if (kataKunci != null && !kataKunci.isBlank()) {
                String pola = "%" + kataKunci.trim() + "%";
                ps.setString(idx++, pola);
                ps.setString(idx, pola);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRowLaporan(rs));
                }
            }
        }
        return result;
    }

    /** Versi "milik satu guru" dari {@link #hitungRekap(LocalDate, LocalDate, Integer, Integer)}. */
    public Map<StatusAbsensi, Integer> hitungRekap(LocalDate tglMulai, LocalDate tglAkhir, Integer kelasId,
                                                     Integer mapelId, int guruId) throws SQLException {
        Map<StatusAbsensi, Integer> rekap = new EnumMap<>(StatusAbsensi.class);
        for (StatusAbsensi s : StatusAbsensi.values()) {
            rekap.put(s, 0);
        }
        StringBuilder sql = new StringBuilder(
                "SELECT am.status, COUNT(*) AS jumlah FROM tb_absensi_mapel am "
                        + "JOIN tb_jadwal_mengajar j ON am.jadwal_id = j.jadwal_id "
                        + "WHERE am.tanggal BETWEEN ? AND ? AND j.guru_id = ? ");
        if (kelasId != null) {
            sql.append("AND j.kelas_id = ? ");
        }
        if (mapelId != null) {
            sql.append("AND j.mapel_id = ? ");
        }
        sql.append("GROUP BY am.status");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setDate(idx++, Date.valueOf(tglMulai));
            ps.setDate(idx++, Date.valueOf(tglAkhir));
            ps.setInt(idx++, guruId);
            if (kelasId != null) {
                ps.setInt(idx++, kelasId);
            }
            if (mapelId != null) {
                ps.setInt(idx, mapelId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rekap.put(StatusAbsensi.valueOf(rs.getString("status")), rs.getInt("jumlah"));
                }
            }
        }
        return rekap;
    }

    /** Memperbarui status/waktu absensi mata pelajaran dari halaman laporan. */
    public void updateFromLaporan(AbsensiMapel absensi, int penggunaId) throws SQLException {
        String sql = "UPDATE tb_absensi_mapel SET waktu_scan = ?, status = ?, pengguna_id = ? WHERE absensi_mapel_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (absensi.getWaktuScan() == null) {
                ps.setNull(1, java.sql.Types.TIME);
            } else {
                ps.setTime(1, Time.valueOf(absensi.getWaktuScan()));
            }
            ps.setString(2, absensi.getStatus().name());
            ps.setInt(3, penggunaId);
            ps.setInt(4, absensi.getAbsensiMapelId());
            ps.executeUpdate();
        }
    }

    /** Menghapus satu record absensi mata pelajaran dari halaman laporan. */
    public void deleteById(int absensiMapelId) throws SQLException {
        String sql = "DELETE FROM tb_absensi_mapel WHERE absensi_mapel_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, absensiMapelId);
            ps.executeUpdate();
        }
    }

    private AbsensiMapel mapRowLaporan(ResultSet rs) throws SQLException {
        AbsensiMapel a = mapRow(rs);
        a.setNamaKelas(rs.getString("nama_kelas"));
        a.setNamaMapel(rs.getString("nama_mapel"));
        a.setNamaGuru(rs.getString("nama_guru"));
        a.setStatusMurid(rs.getString("status_murid"));
        return a;
    }

    private AbsensiMapel mapRow(ResultSet rs) throws SQLException {
        AbsensiMapel a = new AbsensiMapel();
        a.setAbsensiMapelId(rs.getInt("absensi_mapel_id"));
        a.setMuridId(rs.getInt("murid_id"));
        a.setJadwalId(rs.getInt("jadwal_id"));
        a.setPenggunaId(rs.getInt("pengguna_id"));
        a.setTanggal(rs.getDate("tanggal").toLocalDate());
        a.setWaktuScan(rs.getTime("waktu_scan") != null ? rs.getTime("waktu_scan").toLocalTime() : null);
        a.setStatus(StatusAbsensi.valueOf(rs.getString("status")));
        a.setNamaMurid(rs.getString("nama_murid"));
        a.setNis(rs.getString("nis"));
        return a;
    }

    public int hitungJumlahSiswaAktifUntukJadwal(int jadwalId) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}