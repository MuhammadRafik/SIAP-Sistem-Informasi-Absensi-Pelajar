package com.nurulislam.siap.dao;

/**
 * Dilempar MataPelajaranDAO#delete ketika mata pelajaran masih dipakai oleh
 * jadwal mengajar (FK ON DELETE RESTRICT pada tb_jadwal_mengajar, lihat
 * siap_schema.sql). Dipakai controller Manajemen Mata Pelajaran untuk
 * menampilkan pesan yang jelas alih-alih error SQL mentah.
 */
public class MataPelajaranMasihDipakaiException extends Exception {
    public MataPelajaranMasihDipakaiException(String message) {
        super(message);
    }
}