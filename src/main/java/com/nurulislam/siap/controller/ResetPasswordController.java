package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.PenggunaDAO;
import com.nurulislam.siap.dao.ResetPasswordTokenDAO;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.ResetPasswordToken;
import com.nurulislam.siap.util.PasswordUtil;
import com.nurulislam.siap.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller untuk halaman "Reset Password" (langkah kedua alur reset password).
 * Diakses setelah pengguna meminta kode reset lewat halaman "Lupa Password"
 * (lihat LupaPasswordController), yang memanggil {@link #setEmail(String)}
 * untuk mengisi email secara otomatis.
 *
 * Alur:
 *   1. Pengguna memasukkan kode reset (6 digit) & password baru + konfirmasi.
 *   2. Sistem mencari token (ResetPasswordTokenDAO.findByToken), memastikan
 *      token memang milik akun dengan email tersebut & belum kedaluwarsa.
 *   3. Jika valid: password diganti (hash BCrypt baru), token dihapus supaya
 *      tidak bisa dipakai ulang.
 */
public class ResetPasswordController {

    @FXML private Label emailLabel;
    @FXML private TextField tokenField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private PasswordField konfirmasiPasswordField;
    @FXML private TextField konfirmasiPasswordVisibleField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label errorLabel;
    @FXML private Button btnReset;
    @FXML private Hyperlink linkMintaKodeBaru;
    @FXML private Hyperlink linkKembaliLogin;

    private static final int PANJANG_PASSWORD_MINIMAL = 6;

    private final PenggunaDAO penggunaDAO = new PenggunaDAO();
    private final ResetPasswordTokenDAO resetPasswordTokenDAO = new ResetPasswordTokenDAO();

    /** Email pemilik akun yang sedang melakukan reset password, diisi dari LupaPasswordController. */
    private String email;

    @FXML
    public void initialize() {
        errorLabel.setText("");

        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        konfirmasiPasswordVisibleField.setManaged(false);
        konfirmasiPasswordVisibleField.setVisible(false);
        konfirmasiPasswordVisibleField.textProperty().bindBidirectional(konfirmasiPasswordField.textProperty());

        showPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            passwordVisibleField.setVisible(isSelected);
            passwordVisibleField.setManaged(isSelected);
            passwordField.setVisible(!isSelected);
            passwordField.setManaged(!isSelected);

            konfirmasiPasswordVisibleField.setVisible(isSelected);
            konfirmasiPasswordVisibleField.setManaged(isSelected);
            konfirmasiPasswordField.setVisible(!isSelected);
            konfirmasiPasswordField.setManaged(!isSelected);
        });

        btnReset.setOnAction(e -> handleReset());
        linkMintaKodeBaru.setOnAction(e -> goToLupaPassword());
        linkKembaliLogin.setOnAction(e -> goToLogin());
    }

    /** Dipanggil oleh LupaPasswordController setelah kode reset dibuat & ditampilkan. */
    public void setEmail(String email) {
        this.email = email;
        if (emailLabel != null) {
            emailLabel.setText(email);
        }
    }

    private void handleReset() {
        if (email == null || email.isEmpty()) {
            showError("Sesi reset password tidak valid. Mulai ulang dari halaman Lupa Password.");
            return;
        }

        String kode = tokenField.getText() == null ? "" : tokenField.getText().trim();
        String password = passwordField.getText();
        String konfirmasi = konfirmasiPasswordField.getText();

        if (kode.isEmpty() || password == null || password.isEmpty()
                || konfirmasi == null || konfirmasi.isEmpty()) {
            showError("Semua kolom wajib diisi.");
            return;
        }
        if (password.length() < PANJANG_PASSWORD_MINIMAL) {
            showError("Kata sandi minimal " + PANJANG_PASSWORD_MINIMAL + " karakter.");
            return;
        }
        if (!password.equals(konfirmasi)) {
            showError("Konfirmasi kata sandi tidak cocok.");
            return;
        }

        try {
            Optional<Pengguna> penggunaOpt = penggunaDAO.findByEmail(email);
            if (penggunaOpt.isEmpty()) {
                showError("Akun tidak ditemukan. Mulai ulang dari halaman Lupa Password.");
                return;
            }
            Pengguna pengguna = penggunaOpt.get();

            Optional<ResetPasswordToken> tokenOpt = resetPasswordTokenDAO.findByToken(kode);
            if (tokenOpt.isEmpty() || !tokenOpt.get().getPenggunaId().equals(pengguna.getPenggunaId())) {
                showError("Kode reset tidak valid.");
                return;
            }

            ResetPasswordToken token = tokenOpt.get();
            if (token.isExpired()) {
                // Token kedaluwarsa: bersihkan supaya tidak menumpuk, minta pengguna mulai ulang
                resetPasswordTokenDAO.deleteByPenggunaId(pengguna.getPenggunaId());
                showError("Kode reset sudah kedaluwarsa. Silakan minta kode baru.");
                return;
            }

            String hashBaru = PasswordUtil.hash(password);
            penggunaDAO.updatePassword(pengguna.getPenggunaId(), hashBaru);
            resetPasswordTokenDAO.deleteByPenggunaId(pengguna.getPenggunaId());

            tampilkanSuksesLaluKembaliKeLogin();

        } catch (SQLException e) {
            showError("Gagal terhubung ke database. Coba lagi.");
            System.err.println("[ResetPasswordController] SQLException: " + e.getMessage());
        } catch (RuntimeException e) {
            showError("Terjadi kesalahan tak terduga.");
            System.err.println("[ResetPasswordController] Unexpected error: " + e);
            e.printStackTrace();
        }
    }

    private void tampilkanSuksesLaluKembaliKeLogin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reset Password Berhasil");
        alert.setHeaderText(null);
        alert.setContentText("Kata sandi Anda berhasil diganti. Silakan masuk dengan kata sandi baru.");
        alert.showAndWait();

        goToLogin();
    }

    private void goToLupaPassword() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/LupaPassword.fxml", Main.APP_TITLE + " - Lupa Password");
        } catch (IOException e) {
            showError("Gagal membuka halaman Lupa Password.");
            System.err.println("[ResetPasswordController] IOException: " + e.getMessage());
        }
    }

    private void goToLogin() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", Main.APP_TITLE + " - Masuk");
        } catch (IOException e) {
            showError("Gagal membuka halaman Login.");
            System.err.println("[ResetPasswordController] IOException: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}