package com.nurulislam.siap.controller;

import com.nurulislam.siap.util.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller sementara untuk layar "Starting" (fondasi project).
 * Fungsinya hanya membuktikan bahwa:
 *  1. JavaFX + FXML sudah berjalan dengan benar.
 *  2. Koneksi ke database MySQL berhasil dibuat lewat DatabaseConnection.
 *
 * Kelas ini akan digantikan oleh LoginController pada tahap berikutnya.
 */
public class StartingController {

    @FXML
    private Label statusDbLabel;

    @FXML
    public void initialize() {
        boolean terhubung = DatabaseConnection.testConnection();
        if (terhubung) {
            statusDbLabel.setText("Status database: TERHUBUNG");
            statusDbLabel.getStyleClass().add("status-ok");
        } else {
            statusDbLabel.setText("Status database: GAGAL TERHUBUNG (cek db.properties)");
            statusDbLabel.getStyleClass().add("status-error");
        }
    }
}
