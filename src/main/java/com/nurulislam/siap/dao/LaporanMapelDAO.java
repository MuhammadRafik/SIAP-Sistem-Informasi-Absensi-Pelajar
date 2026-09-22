package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.AbsensiMapel;
import com.nurulislam.siap.model.StatusAbsensi;
import com.nurulislam.siap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object untuk halaman "Laporan" (rekap absensi mata pelajaran)
 * pada akun Guru. Semua query di sini selalu dibatasi {@code guru_id} lewat
 * JOIN ke tb_jadwal_mengajar, sehingga seorang Guru hanya bisa melihat rekap
 * absensi dari kelas/mata pelajaran yang benar-benar ia ajar.
 */
public class LaporanMapelDAO {

    /** Daftar mata pelajaran yang diajar guru ini, untuk mengisi ComboBox filter. Key = mapel_id. */
    public Map<Integer, String> ambilDaftarMapelGuru(int guruId) throws SQLException {
        String sql = "SELECT DISTINCT mp.mapel_id, mp.nama_mapel FROM tb_jadwal_mengajar j "
                + "JOIN tb_mata_pelajaran mp ON j.mapel_id = mp.mapel_id "
                + "WHERE j.guru_id = ? ORDER BY mp.nama_mapel ASC";
        return ambilPasanganIdNama(sql, guruId);
    }

    /** Daftar kelas yang diajar guru ini, untuk mengisi ComboBox filter. Key = kelas_id. */
    public Map<Integer, String> ambilDaftarKelasGuru(int guruId) throws SQLException {
        String sql = "SELECT DISTINCT k.kelas_id, k.nama_kelas FROM tb_jadwal_mengajar j "
                + "JOIN tb_kelas k ON j.kelas_id = k.kelas_id "
                + "WHERE j.guru_id = ? ORDER BY k.nama_kelas ASC";
        return ambilPasanganIdNama(sql, guruId);
    }

    private Map<Integer, String> ambilPasanganIdNama(String sql, int guruId) throws SQLException {
        Map<Integer, String> hasil = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guruId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasil.put(rs.getInt(1), rs.getString(2));
                }
            }
        }
        return hasil;
    }

    /**
     * Mencari baris rekapitulasi absensi mata pelajaran milik guru ini sesuai filter.
     * Semua parameter filter (selain guruId, dari, sampai) boleh null berarti "semua".
     */
    public List<AbsensiMapel> cariRekap(int guruId, LocalDate dari, LocalDate sampai,
                                        Integer mapelId, Integer kelasId, StatusAbsensi status,
                                        String kataKunci) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT am.*, m.nama AS nama_murid, m.nis AS nis, "
                        + "k.nama_kelas AS nama_kelas, mp.nama_mapel AS nama_mapel "
                        + "FROM tb_absensi_mapel am "
                        + "JOIN tb_murid m ON am.murid_id = m.murid_id "
                        + "JOIN tb_jadwal_mengajar j ON am.jadwal_id = j.jadwal_id "
                        + "JOIN tb_kelas k ON j.kelas_id = k.kelas_id "
                        + "JOIN tb_mata_pelajaran mp ON j.mapel_id = mp.mapel_id "
                        + "WHERE j.guru_id = ? AND am.tanggal BETWEEN ? AND ? ");

        if (mapelId != null) {
            sql.append("AND j.mapel_id = ? ");
        }
        if (kelasId != null) {
            sql.append("AND j.kelas_id = ? ");
        }
        if (status != null) {
            sql.append("AND am.status = ? ");
        }
        if (kataKunci != null && !kataKunci.isBlank()) {
            sql.append("AND (m.nama LIKE ? OR m.nis LIKE ?) ");
        }
        sql.append("ORDER BY am.tanggal DESC, am.waktu_scan DESC");

        List<AbsensiMapel> hasil = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            ps.setInt(idx++, guruId);
            ps.setDate(idx++, Date.valueOf(dari));
            ps.setDate(idx++, Date.valueOf(sampai));
            if (mapelId != null) {
                ps.setInt(idx++, mapelId);
            }
            if (kelasId != null) {
                ps.setInt(idx++, kelasId);
            }
            if (status != null) {
                ps.setString(idx++, status.name());
            }
            if (kataKunci != null && !kataKunci.isBlank()) {
                String like = "%" + kataKunci.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasil.add(mapRow(rs));
                }
            }
        }
        return hasil;
    }

    /**
     * Rekap jumlah per status untuk guru ini sesuai filter tanggal/mapel/kelas
     * (TIDAK ikut memfilter status, karena kartu rekap harus menampilkan
     * pemecahan seluruh status pada rentang yang dipilih).
     */
    public Map<StatusAbsensi, Integer> hitungRekapStatus(int guruId, LocalDate dari, LocalDate sampai,
                                                          Integer mapelId, Integer kelasId) throws SQLException {
        Map<StatusAbsensi, Integer> rekap = new EnumMap<>(StatusAbsensi.class);
        for (StatusAbsensi s : StatusAbsensi.values()) {
            rekap.put(s, 0);
        }

        StringBuilder sql = new StringBuilder(
                "SELECT am.status, COUNT(*) AS jumlah FROM tb_absensi_mapel am "
                        + "JOIN tb_jadwal_mengajar j ON am.jadwal_id = j.jadwal_id "
                        + "WHERE j.guru_id = ? AND am.tanggal BETWEEN ? AND ? ");
        if (mapelId != null) {
            sql.append("AND j.mapel_id = ? ");
        }
        if (kelasId != null) {
            sql.append("AND j.kelas_id = ? ");
        }
        sql.append("GROUP BY am.status");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            ps.setInt(idx++, guruId);
            ps.setDate(idx++, Date.valueOf(dari));
            ps.setDate(idx++, Date.valueOf(sampai));
            if (mapelId != null) {
                ps.setInt(idx++, mapelId);
            }
            if (kelasId != null) {
                ps.setInt(idx++, kelasId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rekap.put(StatusAbsensi.valueOf(rs.getString("status")), rs.getInt("jumlah"));
                }
            }
        }
        return rekap;
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
        a.setNamaKelas(rs.getString("nama_kelas"));
        a.setNamaMapel(rs.getString("nama_mapel"));
        return a;
    }
}