package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.PenggunaDAO;
import com.nurulislam.siap.dao.ResetPasswordTokenDAO;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.ResetPasswordToken;
import com.nurulislam.siap.model.StatusAkun;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.TokenUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Controller untuk halaman "Lupa Password" (langkah pertama alur reset password).
 *
 * Alur:
 *   1. Pengguna memasukkan email akunnya.
 *   2. Sistem mencari akun & memastikan statusnya AKTIF (akun yang belum
 *      diverifikasi atau nonaktif tidak bisa reset password).
 *   3. Sistem membuat kode reset acak (6 digit) dengan masa berlaku 15 menit,
 *      menyimpannya ke tb_reset_password_token (ResetPasswordTokenDAO).
 *   4. Karena aplikasi ini belum punya modul pengiriman email/SMTP, kode
 *      tersebut DITAMPILKAN LANGSUNG lewat dialog (bukan dikirim ke email).
 *      Catatan: pada implementasi produksi, langkah ini digantikan dengan
 *      mengirim kode ke email pengguna, bukan menampilkannya di layar.
 *   5. Pengguna diarahkan ke halaman "Reset Password" untuk memasukkan kode
 *      tersebut beserta password baru (lihat ResetPasswordController).
 */
public class LupaPasswordController {

    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Button btnKirim;
    @FXML private Hyperlink linkKembali;

    private static final int PANJANG_KODE = 6;
    private static final long MASA_BERLAKU_MENIT = 15;

    private final PenggunaDAO penggunaDAO = new PenggunaDAO();
    private final ResetPasswordTokenDAO resetPasswordTokenDAO = new ResetPasswordTokenDAO();

    @FXML
    public void initialize() {
        errorLabel.setText("");
        btnKirim.setOnAction(e -> handleKirimKode());
        linkKembali.setOnAction(e -> goToLogin());
    }

    private void handleKirimKode() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Email wajib diisi.");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}$")) {
            showError("Format email tidak valid.");
            return;
        }

        try {
            Optional<Pengguna> hasil = penggunaDAO.findByEmail(email);
            if (hasil.isEmpty()) {
                showError("Email tidak terdaftar.");
                return;
            }

            Pengguna pengguna = hasil.get();

            if (pengguna.getStatusAkun() == StatusAkun.MENUNGGU_VERIFIKASI) {
                showError("Akun ini belum diverifikasi oleh Staf TU.");
                return;
            }
            if (pengguna.getStatusAkun() == StatusAkun.NONAKTIF) {
                showError("Akun ini nonaktif. Hubungi Staf TU.");
                return;
            }

            // Hapus kode lama (jika ada) supaya hanya satu kode aktif per akun
            resetPasswordTokenDAO.deleteByPenggunaId(pengguna.getPenggunaId());

            String kode = TokenUtil.generateNumericCode(PANJANG_KODE);
            ResetPasswordToken token = new ResetPasswordToken(
                    pengguna.getPenggunaId(), kode, LocalDateTime.now().plusMinutes(MASA_BERLAKU_MENIT));
            resetPasswordTokenDAO.insert(token);

            tampilkanKodeLaluLanjutKeResetPassword(pengguna, kode);

        } catch (SQLException e) {
            showError("Gagal terhubung ke database. Coba lagi.");
            System.err.println("[LupaPasswordController] SQLException: " + e.getMessage());
        } catch (RuntimeException e) {
            showError("Terjadi kesalahan tak terduga.");
            System.err.println("[LupaPasswordController] Unexpected error: " + e);
            e.printStackTrace();
        }
    }

    private void tampilkanKodeLaluLanjutKeResetPassword(Pengguna pengguna, String kode) {
        // Sementara (belum ada modul email): kode ditampilkan langsung ke pengguna.
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kode Reset Password");
        alert.setHeaderText(null);
        alert.setContentText("Kode reset password Anda: " + kode + "\n\n"
                + "Kode berlaku selama " + MASA_BERLAKU_MENIT + " menit.\n"
                + "(Catatan: pada versi produksi, kode ini akan dikirim ke email Anda, "
                + "bukan ditampilkan di layar.)");
        alert.showAndWait();

        try {
            ResetPasswordController controller = SceneManager.switchToWithController(
                    "/com/nurulislam/siap/fxml/ResetPassword.fxml", Main.APP_TITLE + " - Reset Password");
            controller.setEmail(pengguna.getEmail());
        } catch (IOException e) {
            showError("Gagal membuka halaman Reset Password.");
            System.err.println("[LupaPasswordController] IOException: " + e.getMessage());
        }
    }

    private void goToLogin() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", Main.APP_TITLE + " - Masuk");
        } catch (IOException e) {
            showError("Gagal membuka halaman Login.");
            System.err.println("[LupaPasswordController] IOException: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}