package com.nurulislam.siap.dao;

import com.nurulislam.siap.model.Sesiabsensi;
import com.nurulislam.siap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object untuk tabel tb_sesi_absensi.
 */
public class SesiAbsensiDAO {

    public List<Sesiabsensi> findAll() throws SQLException {
        String sql = "SELECT * FROM tb_sesi_absensi ORDER BY jam_masuk ASC";
        List<Sesiabsensi> result = new ArrayList<>();
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
     * Menentukan apakah absensi harian sedang berada dalam jendela waktu
     * yang diizinkan: 06:30 sampai 08:00.
     *
     * <p>Jika dipanggil sebelum 06:30 atau setelah 08:00, method
     * mengembalikan Optional.empty() sehingga scan tidak dapat disimpan.</p>
     */
    public Optional<Sesiabsensi> findSesiUntukWaktu(LocalTime waktuSekarang) throws SQLException {
        LocalTime mulaiAbsensi = LocalTime.of(6, 30);
        LocalTime selesaiAbsensi = LocalTime.of(8, 0);

        if (waktuSekarang.isBefore(mulaiAbsensi) || waktuSekarang.isAfter(selesaiAbsensi)) {
            return Optional.empty();
        }

        List<Sesiabsensi> semua = findAll();
        if (semua.isEmpty()) {
            return Optional.empty();
        }

        // Absensi harian menggunakan jendela tetap 06:30-08:00.
        // Sesi pertama dipakai sebagai sesi pencatatan harian.
        return Optional.of(semua.get(0));
    }

    private Sesiabsensi mapRow(ResultSet rs) throws SQLException {
        return new Sesiabsensi(
                rs.getInt("sesi_absensi_id"),
                rs.getString("nama_sesi"),
                rs.getTime("jam_masuk").toLocalTime(),
                rs.getTime("batas_terlambat").toLocalTime()
        );
    }
}
