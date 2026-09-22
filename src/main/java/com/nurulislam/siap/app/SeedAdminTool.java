package com.nurulislam.siap.app;

import com.nurulislam.siap.util.PasswordUtil;

/**
 * Tool bantu (bukan bagian dari aplikasi utama) untuk men-generate hash BCrypt
 * langsung memakai library jBCrypt yang sama persis dengan yang dipakai
 * aplikasi saat login. Ini menghindari masalah "hash tidak cocok" akibat
 * karakter tersembunyi saat copy-paste hash dari sumber lain (browser/chat).
 *
 * CARA PAKAI (di NetBeans):
 *   1. Buka file ini.
 *   2. Klik kanan di area kode -> "Run File" (atau klik kanan file di project tree -> Run File).
 *   3. Lihat Output/console: akan tercetak query SQL siap-pakai.
 *   4. Copy-paste query tersebut langsung ke phpMyAdmin / MySQL client.
 *
 * File ini boleh dihapus/diabaikan setelah modul "Daftar Akun" selesai dibuat,
 * karena nantinya pembuatan akun akan otomatis lewat aplikasi.
 */
public class SeedAdminTool {

    public static void main(String[] args) {
        String email = "admin@nurulislam.sch.id";
        String plainPassword = "admin123";

        String hash = PasswordUtil.hash(plainPassword);

        System.out.println("========================================================");
        System.out.println("Email     : " + email);
        System.out.println("Password  : " + plainPassword);
        System.out.println("Hash      : " + hash);
        System.out.println("Panjang   : " + hash.length() + " karakter (harus 60)");
        System.out.println("========================================================");
        System.out.println("Copy query di bawah ini APA ADANYA ke phpMyAdmin / MySQL:");
        System.out.println();
        System.out.println("UPDATE tb_pengguna SET password_hash = '" + hash
                + "', status_akun = 'AKTIF' WHERE email = '" + email + "';");
        System.out.println();

        // Verifikasi langsung di sini juga, supaya kita yakin hash yang
        // dicetak di atas memang valid sebelum sempat dipakai di database.
        boolean cocok = PasswordUtil.matches(plainPassword, hash);
        System.out.println("Verifikasi mandiri (harus TRUE): " + cocok);
    }
}
