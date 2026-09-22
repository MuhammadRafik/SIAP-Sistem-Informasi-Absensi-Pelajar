package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.PenggunaDAO;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.util.PasswordUtil;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Fix 25:
 * Pengaturan akun Guru untuk melihat data akun dan mengganti password.
 */
public class PengaturanAkunController {

    @FXML private TextField fieldNama;
    @FXML private TextField fieldEmail;
    @FXML private Label labelRole;
    @FXML private PasswordField fieldPasswordLama;
    @FXML private PasswordField fieldPasswordBaru;
    @FXML private PasswordField fieldKonfirmasi;
    @FXML private Label labelError;
    @FXML private Button btnSimpanPassword;
    @FXML private Button btnKembali;

    private static final int MIN_PASSWORD = 6;

    private final PenggunaDAO penggunaDAO = new PenggunaDAO();

    @FXML
    public void initialize() {
        labelError.setText("");
        muatDataAkun();

        btnSimpanPassword.setOnAction(e -> ubahPassword());
        btnKembali.setOnAction(e -> kembali());
    }

    private void muatDataAkun() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();

        if (pengguna == null || pengguna.getPenggunaId() == null) {
            labelError.setText("Sesi akun tidak valid. Silakan login kembali.");
            btnSimpanPassword.setDisable(true);
            return;
        }

        fieldNama.setText(pengguna.getNama() == null ? "" : pengguna.getNama());
        fieldEmail.setText(pengguna.getEmail() == null ? "" : pengguna.getEmail());
        labelRole.setText(
                pengguna.getRole() == null ? "" : pengguna.getRole().getLabel()
        );

        // Nama dan email hanya ditampilkan pada Fix 25.
        // Pengubahan identitas tetap dikelola melalui TU agar tidak
        // mengganggu keterhubungan akun dengan data jadwal.
        fieldNama.setEditable(false);
        fieldEmail.setEditable(false);
    }

    private void ubahPassword() {
        labelError.setText("");

        Pengguna pengguna = SessionManager.getPenggunaAktif();
        if (pengguna == null || pengguna.getPenggunaId() == null) {
            labelError.setText("Sesi akun tidak valid. Silakan login kembali.");
            return;
        }

        String lama = safe(fieldPasswordLama.getText());
        String baru = safe(fieldPasswordBaru.getText());
        String konfirmasi = safe(fieldKonfirmasi.getText());

        if (lama.isEmpty() || baru.isEmpty() || konfirmasi.isEmpty()) {
            labelError.setText("Semua kolom password wajib diisi.");
            return;
        }

        if (baru.length() < MIN_PASSWORD) {
            labelError.setText("Password baru minimal " + MIN_PASSWORD + " karakter.");
            return;
        }

        if (!baru.equals(konfirmasi)) {
            labelError.setText("Konfirmasi password tidak cocok.");
            return;
        }

        try {
            Pengguna terbaru = penggunaDAO.findById(pengguna.getPenggunaId())
                    .orElse(null);

            if (terbaru == null) {
                labelError.setText("Akun tidak ditemukan.");
                return;
            }

            if (!PasswordUtil.matches(lama, terbaru.getPasswordHash())) {
                labelError.setText("Password lama salah.");
                return;
            }

            if (PasswordUtil.matches(baru, terbaru.getPasswordHash())) {
                labelError.setText("Password baru harus berbeda dari password lama.");
                return;
            }

            String hashBaru = PasswordUtil.hash(baru);

            if (!penggunaDAO.updatePassword(
                    pengguna.getPenggunaId(), hashBaru)) {
                labelError.setText("Password gagal diperbarui.");
                return;
            }

            // Sinkronkan hash di session agar objek session tetap konsisten.
            pengguna.setPasswordHash(hashBaru);

            fieldPasswordLama.clear();
            fieldPasswordBaru.clear();
            fieldKonfirmasi.clear();

            Alert sukses = new Alert(Alert.AlertType.INFORMATION);
            sukses.setTitle("Pengaturan Akun");
            sukses.setHeaderText("Password berhasil diubah");
            sukses.setContentText(
                    "Password baru sudah tersimpan. Gunakan password baru saat login berikutnya."
            );
            sukses.showAndWait();

        } catch (SQLException e) {
            labelError.setText("Database tidak dapat dihubungi. Coba lagi.");
            System.err.println(
                    "[PengaturanAkunController] SQLException: " + e.getMessage()
            );
        } catch (RuntimeException e) {
            labelError.setText("Terjadi kesalahan saat mengubah password.");
            System.err.println(
                    "[PengaturanAkunController] Runtime error: " + e.getMessage()
            );
        }
    }

    private void kembali() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();

        String tujuan = pengguna != null && pengguna.isGuru()
                ? "/com/nurulislam/siap/fxml/DashboardGuru.fxml"
                : "/com/nurulislam/siap/fxml/Dashboard.fxml";

        String judul = pengguna != null && pengguna.isGuru()
                ? "Dashboard Guru"
                : "Dashboard TU";

        try {
            SceneManager.switchTo(tujuan, Main.APP_TITLE + " - " + judul);
        } catch (IOException e) {
            labelError.setText("Gagal kembali ke dashboard.");
            System.err.println(
                    "[PengaturanAkunController] IOException: " + e.getMessage()
            );
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
