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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Halaman "Profil Saya" untuk Staf TU dan Guru.
 * - Menampilkan & mengubah foto profil, nama, dan email.
 * - Mengubah password.
 * - Keluar (logout) dari aplikasi.
 */
public class PengaturanAkunController {

    @FXML private ImageView imgFoto;
    @FXML private StackPane fotoBox;
    @FXML private Label labelInisialFoto;
    @FXML private Label labelNamaBesar;
    @FXML private Label labelEmailBesar;
    @FXML private Button btnPilihFoto;
    @FXML private Button btnHapusFoto;

    @FXML private TextField fieldNama;
    @FXML private TextField fieldEmail;
    @FXML private Label labelRole;
    @FXML private Label labelStatusAkun;
    @FXML private Label labelProfilError;
    @FXML private Button btnSimpanProfil;

    @FXML private PasswordField fieldPasswordLama;
    @FXML private PasswordField fieldPasswordBaru;
    @FXML private PasswordField fieldKonfirmasi;
    @FXML private Label labelError;
    @FXML private Button btnSimpanPassword;

    @FXML private Button btnKembali;
    @FXML private Button btnLogout;

    private static final int MIN_PASSWORD = 6;
    private static final long MAKS_UKURAN_FOTO = 2L * 1024 * 1024; // 2 MB

    private final PenggunaDAO penggunaDAO = new PenggunaDAO();

    /** File foto yang baru dipilih (belum tersimpan sampai "Simpan Perubahan" ditekan). */
    private File fotoBaru;
    /** true jika pengguna menekan "Hapus Foto". */
    private boolean hapusFotoDiminta;

    @FXML
    public void initialize() {
        labelError.setText("");
        labelProfilError.setText("");

        // Kunci ukuran bingkai: foto dipaksa 96x96 di kode (bukan hanya FXML)
        // dan bingkai diberi klip keras 96x96 agar tidak ada piksel yang bisa
        // meluber keluar card (StackPane tidak memotong anaknya secara bawaan).
        if (imgFoto != null) {
            imgFoto.setFitWidth(96);
            imgFoto.setFitHeight(96);
            imgFoto.setPreserveRatio(false);
            imgFoto.setClip(new Circle(48, 48, 48));
        }
        if (fotoBox != null) {
            fotoBox.setClip(new Rectangle(96, 96));
        }

        muatDataAkun();

        btnSimpanProfil.setOnAction(e -> simpanProfil());
        btnSimpanPassword.setOnAction(e -> ubahPassword());
        btnPilihFoto.setOnAction(e -> pilihFoto());
        btnHapusFoto.setOnAction(e -> hapusFoto());
        btnKembali.setOnAction(e -> kembali());
        btnLogout.setOnAction(e -> handleLogout());
    }

    // ================= Profil =================

    private void muatDataAkun() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();

        if (pengguna == null || pengguna.getPenggunaId() == null) {
            labelProfilError.setText("Sesi akun tidak valid. Silakan login kembali.");
            btnSimpanProfil.setDisable(true);
            btnSimpanPassword.setDisable(true);
            return;
        }

        fieldNama.setText(pengguna.getNama() == null ? "" : pengguna.getNama());
        fieldEmail.setText(pengguna.getEmail() == null ? "" : pengguna.getEmail());
        labelRole.setText(pengguna.getRole() == null ? "-" : pengguna.getRole().getLabel());
        labelStatusAkun.setText(pengguna.getStatusAkun() == null ? "-" : humanis(pengguna.getStatusAkun().name()));

        labelNamaBesar.setText(pengguna.getNama() == null ? "-" : pengguna.getNama());
        labelEmailBesar.setText(pengguna.getEmail() == null ? "-" : pengguna.getEmail());

        fotoBaru = null;
        hapusFotoDiminta = false;
        tampilkanFoto(pengguna.getFoto());
    }

    private void simpanProfil() {
        labelProfilError.setText("");

        Pengguna pengguna = SessionManager.getPenggunaAktif();
        if (pengguna == null || pengguna.getPenggunaId() == null) {
            labelProfilError.setText("Sesi akun tidak valid. Silakan login kembali.");
            return;
        }

        String nama = safe(fieldNama.getText());
        String email = safe(fieldEmail.getText());

        if (nama.isEmpty()) {
            labelProfilError.setText("Nama lengkap wajib diisi.");
            return;
        }
        if (email.isEmpty()) {
            labelProfilError.setText("Email wajib diisi.");
            return;
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            labelProfilError.setText("Format email tidak valid.");
            return;
        }

        try {
            if (!email.equalsIgnoreCase(pengguna.getEmail())
                    && penggunaDAO.existsByEmailExcept(email, pengguna.getPenggunaId())) {
                labelProfilError.setText("Email ini sudah dipakai akun lain.");
                return;
            }

            String pathFoto = pengguna.getFoto();
            if (hapusFotoDiminta) {
                hapusFileFotoLama(pathFoto);
                pathFoto = null;
            } else if (fotoBaru != null) {
                hapusFileFotoLama(pathFoto);
                pathFoto = salinFotoKePenyimpanan(pengguna.getPenggunaId(), fotoBaru);
            }

            if (!penggunaDAO.updateProfil(pengguna.getPenggunaId(), nama, email, pathFoto)) {
                labelProfilError.setText("Perubahan gagal disimpan.");
                return;
            }

            pengguna.setNama(nama);
            pengguna.setEmail(email);
            pengguna.setFoto(pathFoto);

            fotoBaru = null;
            hapusFotoDiminta = false;
            labelNamaBesar.setText(nama);
            labelEmailBesar.setText(email);
            tampilkanFoto(pathFoto);
            labelProfilError.setText("");
            info("Profil Saya", "Perubahan profil berhasil disimpan.");
        } catch (SQLException e) {
            labelProfilError.setText("Database tidak dapat dihubungi. Coba lagi.");
            System.err.println("[PengaturanAkunController] SQLException (profil): " + e.getMessage());
        } catch (IOException e) {
            labelProfilError.setText("Gagal menyimpan file foto. Coba lagi.");
            System.err.println("[PengaturanAkunController] IOException (foto): " + e.getMessage());
        }
    }

    // ================= Foto =================

    private void pilihFoto() {
        FileChooser pemilih = new FileChooser();
        pemilih.setTitle("Pilih Foto Profil");
        pemilih.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Gambar", "*.png", "*.jpg", "*.jpeg")
        );
        File berkas = pemilih.showOpenDialog(btnPilihFoto.getScene().getWindow());
        if (berkas == null) {
            return;
        }

        String namaBerkas = berkas.getName().toLowerCase();
        if (!(namaBerkas.endsWith(".png") || namaBerkas.endsWith(".jpg") || namaBerkas.endsWith(".jpeg"))) {
            labelProfilError.setText("Format foto harus PNG atau JPG.");
            return;
        }
        if (berkas.length() > MAKS_UKURAN_FOTO) {
            labelProfilError.setText("Ukuran foto maksimal 2 MB.");
            return;
        }

        fotoBaru = berkas;
        hapusFotoDiminta = false;
        tampilkanFotoPratinjau(berkas);
        labelProfilError.setText("Foto dipilih. Tekan \"Simpan Perubahan\" untuk menyimpan.");
    }

    private void hapusFoto() {
        fotoBaru = null;
        hapusFotoDiminta = true;
        tampilkanFoto(null);
        labelProfilError.setText("Foto akan dihapus saat \"Simpan Perubahan\" ditekan.");
    }

    private void tampilkanFoto(String pathFoto) {
        if (pathFoto == null || pathFoto.isBlank()) {
            imgFoto.setImage(null);
            imgFoto.setViewport(null);
            labelInisialFoto.setVisible(true);
            perbaruiInisial();
            return;
        }
        try {
            File berkas = new File(pathFoto);
            if (!berkas.exists()) {
                imgFoto.setImage(null);
                imgFoto.setViewport(null);
                labelInisialFoto.setVisible(true);
                perbaruiInisial();
                return;
            }
            pasangFoto(new Image(berkas.toURI().toString()));
        } catch (RuntimeException e) {
            imgFoto.setImage(null);
            imgFoto.setViewport(null);
            labelInisialFoto.setVisible(true);
            perbaruiInisial();
            System.err.println("[PengaturanAkunController] Gagal memuat foto: " + e.getMessage());
        }
    }

    private void tampilkanFotoPratinjau(File berkas) {
        try {
            pasangFoto(new Image(berkas.toURI().toString()));
        } catch (RuntimeException e) {
            labelProfilError.setText("File gambar tidak dapat dibaca.");
            System.err.println("[PengaturanAkunController] Gagal membaca foto: " + e.getMessage());
        }
    }

    /**
     * Memasang foto ke bingkai 96x96 dengan crop persegi dari tengah
     * (cover), sehingga foto apa pun menyesuaikan bingkai tanpa melar
     * dan tanpa meluber keluar lingkaran.
     */
    private void pasangFoto(Image gambar) {
        if (gambar == null || gambar.isError()) {
            imgFoto.setImage(null);
            imgFoto.setViewport(null);
            labelInisialFoto.setVisible(true);
            perbaruiInisial();
            return;
        }
        double lebar = gambar.getWidth();
        double tinggi = gambar.getHeight();
        if (lebar > 0 && tinggi > 0) {
            double sisi = Math.min(lebar, tinggi);
            imgFoto.setViewport(new Rectangle2D(
                    (lebar - sisi) / 2, (tinggi - sisi) / 2, sisi, sisi));
        } else {
            imgFoto.setViewport(null);
        }
        imgFoto.setImage(gambar);
        labelInisialFoto.setVisible(false);
    }

    private void perbaruiInisial() {
        String nama = safe(fieldNama.getText());
        if (nama.isEmpty()) {
            Pengguna pengguna = SessionManager.getPenggunaAktif();
            nama = pengguna != null && pengguna.getNama() != null ? pengguna.getNama() : "";
        }
        String[] bagian = nama.trim().split("\\s+");
        StringBuilder inisial = new StringBuilder();
        for (int i = 0; i < Math.min(2, bagian.length); i++) {
            if (!bagian[i].isEmpty()) {
                inisial.append(Character.toUpperCase(bagian[i].charAt(0)));
            }
        }
        labelInisialFoto.setText(inisial.length() > 0 ? inisial.toString() : "?");
    }

    private String salinFotoKePenyimpanan(int penggunaId, File sumber) throws IOException {
        String namaSumber = sumber.getName().toLowerCase();
        String ekstensi = namaSumber.endsWith(".png") ? ".png" : ".jpg";
        Path folder = Path.of(System.getProperty("user.home"), ".siap", "foto-profil");
        Files.createDirectories(folder);
        // Hapus varian ekstensi lama agar tidak menumpuk.
        Files.deleteIfExists(folder.resolve("pengguna-" + penggunaId + ".png"));
        Files.deleteIfExists(folder.resolve("pengguna-" + penggunaId + ".jpg"));
        Path tujuan = folder.resolve("pengguna-" + penggunaId + ekstensi);
        Files.copy(sumber.toPath(), tujuan, StandardCopyOption.REPLACE_EXISTING);
        return tujuan.toString();
    }

    private void hapusFileFotoLama(String pathFoto) {
        if (pathFoto == null || pathFoto.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(pathFoto));
        } catch (IOException | RuntimeException e) {
            System.err.println("[PengaturanAkunController] Gagal menghapus foto lama: " + e.getMessage());
        }
    }

    // ================= Password =================

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

            info("Profil Saya", "Password baru sudah tersimpan. Gunakan password baru saat login berikutnya.");
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

    // ================= Navigasi =================

    private void kembali() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();

        String tujuan = pengguna != null && pengguna.isGuru()
                ? "/com/nurulislam/siap/fxml/DashboardGuru.fxml"
                : "/com/nurulislam/siap/fxml/Dashboard.fxml";

        String judul = pengguna != null && pengguna.isGuru()
                ? "Beranda"
                : "Dashboard";

        try {
            SceneManager.switchTo(tujuan, Main.APP_TITLE + " - " + judul);
        } catch (IOException e) {
            labelError.setText("Gagal kembali ke dashboard.");
            System.err.println(
                    "[PengaturanAkunController] IOException: " + e.getMessage()
            );
        }
    }

    private void handleLogout() {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Logout");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Keluar dari aplikasi dan kembali ke halaman masuk?");
        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isEmpty() || jawaban.get() != ButtonType.OK) {
            return;
        }
        SessionManager.logout();
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", Main.APP_TITLE + " - Masuk");
        } catch (IOException e) {
            labelError.setText("Gagal membuka halaman Login.");
            System.err.println("[PengaturanAkunController] IOException: " + e.getMessage());
        }
    }

    private void info(String judul, String pesan) {
        Alert sukses = new Alert(Alert.AlertType.INFORMATION);
        sukses.setTitle(judul);
        sukses.setHeaderText(null);
        sukses.setContentText(pesan);
        sukses.showAndWait();
    }

    private String humanis(String teksEnum) {
        if (teksEnum == null || teksEnum.isEmpty()) {
            return "-";
        }
        String[] kata = teksEnum.split("_");
        StringBuilder hasil = new StringBuilder();
        for (String k : kata) {
            if (k.isEmpty()) {
                continue;
            }
            if (hasil.length() > 0) {
                hasil.append(' ');
            }
            hasil.append(k.charAt(0)).append(k.substring(1).toLowerCase());
        }
        return hasil.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
