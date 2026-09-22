package com.nurulislam.siap.dao;

/**
 * Dilempar KelasDAO#delete ketika kelas masih direferensikan oleh data lain
 * (tb_murid, tb_jadwal_mengajar, dsb - semuanya ON DELETE RESTRICT pada
 * siap_schema.sql). Dipakai controller Manajemen Data Kelas untuk menampilkan
 * pesan yang jelas alih-alih error SQL mentah.
 */
public class KelasMasihDipakaiException extends Exception {
    public KelasMasihDipakaiException(String message) {
        super(message);
    }
}