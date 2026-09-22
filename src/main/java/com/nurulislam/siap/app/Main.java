package com.nurulislam.siap.app;

import com.nurulislam.siap.util.DatabaseConnection;
import com.nurulislam.siap.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Entry point aplikasi SIAP (Sistem Informasi Absensi Pelajar) MA Nurul Islam.
 *
 * Layar awal aplikasi adalah halaman Login (sesuai desain login_siap).
 * Perpindahan antar layar berikutnya (Login -> Dashboard, dst) ditangani
 * oleh SceneManager agar tidak perlu membuat Stage baru di setiap controller.
 *
 * @author Muhammad Rafik - 24302037
 */
public class Main extends Application {

    public static final String APP_TITLE = "SIAP - Absensi Siswa MA Nurul Islam";

    @Override
    public void start(Stage primaryStage) throws IOException {
        SceneManager.setPrimaryStage(primaryStage);
        SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", APP_TITLE + " - Masuk");

        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(560);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Uji koneksi database lebih dulu, hasilnya ditampilkan di layar Starting
        boolean terhubung = DatabaseConnection.testConnection();
        System.out.println(terhubung
                ? "[SIAP] Koneksi database berhasil."
                : "[SIAP] Koneksi database GAGAL. Cek db.properties & pastikan MySQL menyala.");

        launch(args);
    }
}
