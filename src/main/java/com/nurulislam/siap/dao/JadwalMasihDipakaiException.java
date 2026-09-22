package com.nurulislam.siap.dao;

/**
 * Dilempar JadwalMengajarDAO#delete ketika jadwal mengajar masih memiliki
 * riwayat absensi mata pelajaran terkait (FK ON DELETE RESTRICT pada
 * tb_absensi_mapel, lihat siap_schema.sql).
 */
public class JadwalMasihDipakaiException extends Exception {
    public JadwalMasihDipakaiException(String message) {
        super(message);
    }
}