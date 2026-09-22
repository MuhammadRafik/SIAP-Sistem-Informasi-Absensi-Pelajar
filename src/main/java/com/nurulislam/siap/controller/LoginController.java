package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.PenggunaDAO;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.StatusAkun;
import com.nurulislam.siap.util.PasswordUtil;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller untuk halaman Login (sesuai desain "login_siap").
 * Alur:
 *   1. Validasi input tidak kosong & format email sederhana
 *   2. Cari akun berdasarkan email (PenggunaDAO.findByEmail)
 *   3. Cek status akun (harus AKTIF, bukan menunggu verifikasi / nonaktif)
 *   4. Cocokkan password dengan hash BCrypt (PasswordUtil.matches)
 *   5. Jika berhasil -> simpan ke SessionManager, lanjut ke Dashboard
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label errorLabel;
    @FXML private Button btnMasuk;
    @FXML private Hyperlink linkLupaPassword;
    @FXML private Hyperlink linkDaftar;

    private final PenggunaDAO penggunaDAO = new PenggunaDAO();

    @FXML
    public void initialize() {
        errorLabel.setText("");

        // Toggle tampilkan/sembunyikan password: dua field saling menimpa,
        // hanya salah satu yang terlihat & aktif pada satu waktu.
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            passwordVisibleField.setVisible(isSelected);
            passwordVisibleField.setManaged(isSelected);
            passwordField.setVisible(!isSelected);
            passwordField.setManaged(!isSelected);
        });

        btnMasuk.setOnAction(e -> handleLogin());
        linkLupaPassword.setOnAction(e -> handleLupaPassword());
        linkDaftar.setOnAction(e -> handleDaftar());
    }

    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password == null || password.isEmpty()) {
            showError("Email dan kata sandi wajib diisi.");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}$")) {
            showError("Format email tidak valid.");
            return;
        }

        try {
            Optional<Pengguna> hasil = penggunaDAO.findByEmail(email);

            if (hasil.isEmpty()) {
                showError("Email atau kata sandi salah.");
                return;
            }

            Pengguna pengguna = hasil.get();

            if (pengguna.getStatusAkun() == StatusAkun.MENUNGGU_VERIFIKASI) {
                showError("Akun Anda belum diverifikasi oleh Staf TU.");
                return;
            }
            if (pengguna.getStatusAkun() == StatusAkun.NONAKTIF) {
                showError("Akun Anda nonaktif. Hubungi Staf TU.");
                return;
            }

            boolean cocok;
            try {
                cocok = PasswordUtil.matches(password, pengguna.getPasswordHash());
            } catch (IllegalArgumentException e) {
                // password_hash di database bukan hash BCrypt yang valid
                // (misalnya masih nilai placeholder dari data seed).
                showError("Data akun tidak valid (hash password rusak). Hubungi Staf TU / cek database.");
                System.err.println("[LoginController] Hash password tidak valid untuk email " + email
                        + ": " + e.getMessage());
                return;
            }

            if (!cocok) {
                showError("Email atau kata sandi salah.");
                return;
            }

            // Login berhasil
            SessionManager.login(pengguna);
            goToDashboard(pengguna);

        } catch (SQLException e) {
            showError("Gagal terhubung ke database. Coba lagi.");
            System.err.println("[LoginController] SQLException: " + e.getMessage());
        } catch (RuntimeException e) {
            // Jaring pengaman terakhir: pastikan error apa pun tetap terlihat oleh
            // pengguna (bukan hilang diam-diam), sambil dicatat ke console untuk debugging.
            showError("Terjadi kesalahan tak terduga saat login.");
            System.err.println("[LoginController] Unexpected error: " + e);
            e.printStackTrace();
        }
    }

    private void goToDashboard(Pengguna pengguna) {
        // TU  -> Dashboard.fxml (menu admin lengkap, lihat DashboardController)
        // GURU -> DashboardGuru.fxml (Beranda guru, lihat DashboardGuruController)
        try {
            if (pengguna.isTU()) {
                SceneManager.switchTo("/com/nurulislam/siap/fxml/Dashboard.fxml",
                        Main.APP_TITLE + " - Dashboard");
            } else {
                SceneManager.switchTo("/com/nurulislam/siap/fxml/DashboardGuru.fxml",
                        Main.APP_TITLE + " - Beranda");
            }
        } catch (IOException e) {
            showError("Gagal membuka halaman Dashboard.");
            System.err.println("[LoginController] IOException: " + e.getMessage());
        }
    }

    private void handleLupaPassword() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/LupaPassword.fxml", Main.APP_TITLE + " - Lupa Password");
        } catch (IOException e) {
            showError("Gagal membuka halaman Lupa Password.");
            System.err.println("[LoginController] IOException: " + e.getMessage());
        }
    }

    private void handleDaftar() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/DaftarAkun.fxml", Main.APP_TITLE + " - Daftar Akun");
        } catch (IOException e) {
            showError("Gagal membuka halaman Daftar Akun.");
            System.err.println("[LoginController] IOException: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}