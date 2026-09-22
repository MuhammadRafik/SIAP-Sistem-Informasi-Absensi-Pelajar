package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.PenggunaDAO;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.Role;
import com.nurulislam.siap.model.StatusAkun;
import com.nurulislam.siap.util.PasswordUtil;
import com.nurulislam.siap.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller untuk halaman "Daftar Akun".
 * Dipakai oleh calon pengguna Guru untuk
 * membuat akun sendiri lewat aplikasi (tanpa perlu insert manual lewat SQL).
 *
 * Alur:
 *   1. Validasi input (semua wajib diisi, format email, panjang password,
 *      konfirmasi password cocok).
 *   2. Cek email belum terdaftar (PenggunaDAO.existsByEmail).
 *   3. Hash password dengan BCrypt (PasswordUtil.hash) -> TIDAK PERNAH
 *      disimpan dalam bentuk plain text.
 *   4. Simpan akun baru dengan status MENUNGGU_VERIFIKASI.
 *   5. Akun baru belum bisa dipakai login sampai diverifikasi oleh Staf TU
 *      lewat halaman "Verifikasi Akun" (lihat VerifikasiAkunController).
 */
public class DaftarAkunController {

    @FXML private TextField namaField;
    @FXML private TextField emailField;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private PasswordField konfirmasiPasswordField;
    @FXML private TextField konfirmasiPasswordVisibleField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label errorLabel;
    @FXML private Button btnDaftar;
    @FXML private Hyperlink linkMasuk;

    private static final int PANJANG_PASSWORD_MINIMAL = 6;

    private final PenggunaDAO penggunaDAO = new PenggunaDAO();

    @FXML
    public void initialize() {
        errorLabel.setText("");

        // Isi pilihan role dengan label yang enak dibaca (bukan nama enum mentah)
        roleComboBox.getItems().setAll(Role.GURU);
        roleComboBox.setConverter(new StringConverter<Role>() {
            @Override
            public String toString(Role role) {
                return role == null ? "" : role.getLabel();
            }

            @Override
            public Role fromString(String string) {
                return null; // tidak dipakai, ComboBox ini non-editable
            }
        });

        // Toggle tampilkan/sembunyikan password, sama seperti di halaman Login,
        // diterapkan ke dua field password (password & konfirmasi)
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

        btnDaftar.setOnAction(e -> handleDaftar());
        linkMasuk.setOnAction(e -> handleKembaliKeLogin());
    }

    private void handleDaftar() {
        String nama = namaField.getText() == null ? "" : namaField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        Role role = roleComboBox.getValue();
        String password = passwordField.getText();
        String konfirmasi = konfirmasiPasswordField.getText();

        if (nama.isEmpty() || email.isEmpty() || role == null
                || password == null || password.isEmpty()
                || konfirmasi == null || konfirmasi.isEmpty()) {
            showError("Semua kolom wajib diisi.");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}$")) {
            showError("Format email tidak valid.");
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
            if (penggunaDAO.existsByEmail(email)) {
                showError("Email ini sudah terdaftar. Silakan masuk atau gunakan email lain.");
                return;
            }

            String passwordHash = PasswordUtil.hash(password);
            Pengguna penggunaBaru = new Pengguna(email, passwordHash, nama, role, StatusAkun.MENUNGGU_VERIFIKASI);
            penggunaDAO.insert(penggunaBaru);

            tampilkanSuksesLaluKembaliKeLogin();

        } catch (SQLException e) {
            showError("Gagal terhubung ke database. Coba lagi.");
            System.err.println("[DaftarAkunController] SQLException: " + e.getMessage());
        } catch (RuntimeException e) {
            showError("Terjadi kesalahan tak terduga saat mendaftar.");
            System.err.println("[DaftarAkunController] Unexpected error: " + e);
            e.printStackTrace();
        }
    }

    private void tampilkanSuksesLaluKembaliKeLogin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pendaftaran Berhasil");
        alert.setHeaderText(null);
        alert.setContentText("Akun Anda berhasil didaftarkan dan sedang menunggu verifikasi oleh Staf TU.\n"
                + "Anda akan bisa masuk setelah akun diverifikasi.");
        alert.showAndWait();

        goToLogin();
    }

    private void handleKembaliKeLogin() {
        goToLogin();
    }

    private void goToLogin() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", Main.APP_TITLE + " - Masuk");
        } catch (IOException e) {
            showError("Gagal membuka halaman Login.");
            System.err.println("[DaftarAkunController] IOException: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}