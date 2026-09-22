package com.nurulislam.siap.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitas hashing & verifikasi password menggunakan BCrypt.
 * Password TIDAK PERNAH disimpan dalam bentuk plain text di database
 * (lihat kolom tb_pengguna.password_hash).
 */
public class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * Menghasilkan hash BCrypt dari password mentah.
     * Dipanggil saat pendaftaran akun baru atau reset password.
     */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Mencocokkan password yang diketik pengguna saat login
     * dengan hash yang tersimpan di database.
     */
    public static boolean matches(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
