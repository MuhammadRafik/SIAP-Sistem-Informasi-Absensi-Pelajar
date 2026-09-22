package com.nurulislam.siap.util;

import com.nurulislam.siap.model.Pengguna;

/**
 * Menyimpan data pengguna yang sedang login selama aplikasi berjalan
 * (session sederhana, cukup untuk aplikasi desktop single-user).
 * Dipakai oleh Dashboard & semua controller untuk mengetahui:
 *   - siapa yang sedang login (untuk mencatat pengguna_id di tb_absensi/tb_absensi_mapel)
 *   - apa role-nya (untuk menampilkan menu TU vs GURU)
 */
public class SessionManager {

    private static Pengguna pengunaAktif;

    private SessionManager() {
    }

    public static void login(Pengguna pengguna) {
        pengunaAktif = pengguna;
    }

    public static void logout() {
        pengunaAktif = null;
    }

    public static Pengguna getPenggunaAktif() {
        return pengunaAktif;
    }

    public static boolean isLoggedIn() {
        return pengunaAktif != null;
    }
}
