package com.nurulislam.siap.model;

/**
 * Status akun pengguna (tb_pengguna.status_akun).
 * Akun baru dari halaman "Daftar Akun" masuk sebagai MENUNGGU_VERIFIKASI,
 * lalu diverifikasi oleh Staf TU lewat halaman "Verifikasi Akun"
 * sebelum bisa dipakai login (menjadi AKTIF).
 */
public enum StatusAkun {
    MENUNGGU_VERIFIKASI("Menunggu Verifikasi"),
    AKTIF("Aktif"),
    NONAKTIF("Nonaktif");

    private final String label;

    StatusAkun(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
