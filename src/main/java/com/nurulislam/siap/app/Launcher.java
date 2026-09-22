package com.nurulislam.siap.app;

/**
 * Kelas Launcher terpisah dari Main (yang extends javafx.application.Application).
 * Ini praktik standar JavaFX agar aplikasi tetap bisa dijalankan sebagai
 * jar biasa (java -jar) tanpa error "JavaFX runtime components are missing".
 *
 * Jalankan project dari kelas INI jika suatu saat run langsung dari Main
 * bermasalah di NetBeans/terminal.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
