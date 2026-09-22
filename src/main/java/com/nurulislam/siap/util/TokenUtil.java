package com.nurulislam.siap.util;

import java.security.SecureRandom;

/**
 * Utilitas untuk membuat kode acak: token reset password (numerik, mirip OTP)
 * dan token QR Code murid (alfanumerik, dipakai pada tb_murid.qr_token).
 */
public class TokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String KARAKTER_ALFANUMERIK =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private TokenUtil() {
    }

    /**
     * Menghasilkan kode numerik acak sepanjang {@code panjang} digit,
     * boleh diawali angka 0 (contoh: "004821"). Dipakai untuk reset password.
     */
    public static String generateNumericCode(int panjang) {
        StringBuilder sb = new StringBuilder(panjang);
        for (int i = 0; i < panjang; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Menghasilkan token alfanumerik acak sepanjang {@code panjang} karakter.
     * Dipakai sebagai isi QR Code kartu pelajar (tb_murid.qr_token) - SENGAJA
     * bukan NIS murid secara langsung, supaya kartu tidak mudah dipalsukan/
     * diprediksi hanya dari mengetahui NIS (lihat batasan masalah di proposal).
     */
    public static String generateAlphanumericToken(int panjang) {
        StringBuilder sb = new StringBuilder(panjang);
        for (int i = 0; i < panjang; i++) {
            sb.append(KARAKTER_ALFANUMERIK.charAt(RANDOM.nextInt(KARAKTER_ALFANUMERIK.length())));
        }
        return sb.toString();
    }
}