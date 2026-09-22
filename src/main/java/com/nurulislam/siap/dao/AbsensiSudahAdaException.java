package com.nurulislam.siap.dao;

/**
 * Dilempar AbsensiDAO#insert ketika murid sudah tercatat absen pada
 * sesi & tanggal yang sama (melanggar UNIQUE KEY uq_absensi_harian).
 * Dipakai controller Scan QR untuk menampilkan pesan "Sudah absen hari ini"
 * alih-alih error SQL mentah.
 */
public class AbsensiSudahAdaException extends Exception {
    public AbsensiSudahAdaException(String message) {
        super(message);
    }
}
