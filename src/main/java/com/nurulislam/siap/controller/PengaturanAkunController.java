package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.PenggunaDAO;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.util.PasswordUtil;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Halaman Pengaturan Akun dengan pola navigasi seperti WhatsApp Desktop:
 * daftar pengaturan di kiri dan detail pengaturan di kanan.
 *
 * Fitur aktif saat ini:
 * - Profil: nama, email, dan foto profil.
 * - Akun & Keamanan: ubah password.
 * - Sesi: kembali ke dashboard atau logout.
 */
public class PengaturanAkunController {

    // ===== Sidebar pengaturan =====
    @FXML private Button btnMenuProfil;
    @FXML private Button btnMenuAkun;
    @FXML private Button btnMenuSesi;
    @FXML private Button btnKembali;
    @FXML private Button btnLogout;

    @FXML private ImageView imgFotoSidebar;
    @FXML private Label labelInisialSidebar;
    @FXML private Label labelNamaSidebar;
    @FXML private Label labelEmailSidebar;

    // ===== Panel Profil =====
    @FXML private VBox panelProfil;
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

    // ===== Panel Akun & Keamanan =====
    @FXML private VBox panelAkun;
    @FXML private PasswordField fieldPasswordLama;
    @FXML private PasswordField fieldPasswordBaru;
    @FXML private PasswordField fieldKonfirmasi;
    @FXML private Label labelError;
    @FXML private Button btnSimpanPassword;

    // ===== Panel Sesi =====
    @FXML private VBox panelSesi;
    @FXML private Label labelNamaSesi;
    @FXML private Label labelEmailSesi;
    @FXML private Label labelRoleSesi;
    @FXML private Label labelStatusSesi;
    @FXML private Button btnSesiKembali;
    @FXML private Button btnSesiLogout;

    private static final int MIN_PASSWORD = 6;
    private static final long MAKS_UKURAN_FOTO = 2L * 1024 * 1024; // 2 MB
    private static final int UKURAN_FOTO_UTAMA = 170;
    private static final int UKURAN_FOTO_SIDEBAR = 64;

    private final PenggunaDAO penggunaDAO = new PenggunaDAO();

    /** File foto yang baru dipilih; belum tersimpan sampai tombol Simpan ditekan. */
    private File fotoBaru;
    /** true jika tombol Hapus Foto dipilih; perubahan diterapkan saat Simpan ditekan. */
    private boolean hapusFotoDiminta;

    @FXML
    public void initialize() {
        kosongkanPesan();
        siapkanPanelNavigasi();
        siapkanTampilanFoto();
        muatDataAkun();

        btnSimpanProfil.setOnAction(e -> simpanProfil());
        btnSimpanPassword.setOnAction(e -> ubahPassword());
        btnPilihFoto.setOnAction(e -> pilihFoto());
        btnHapusFoto.setOnAction(e -> hapusFoto());
        btnKembali.setOnAction(e -> kembali());
        btnSesiKembali.setOnAction(e -> kembali());
        btnLogout.setOnAction(e -> handleLogout());
        btnSesiLogout.setOnAction(e -> handleLogout());
    }

    private void kosongkanPesan() {
        if (labelError != null) labelError.setText("");
        if (labelProfilError != null) labelProfilError.setText("");
    }

    private void siapkanPanelNavigasi() {
        btnMenuProfil.setOnAction(e -> tampilkanPanel(panelProfil, btnMenuProfil));
        btnMenuAkun.setOnAction(e -> tampilkanPanel(panelAkun, btnMenuAkun));
        btnMenuSesi.setOnAction(e -> tampilkanPanel(panelSesi, btnMenuSesi));
        tampilkanPanel(panelProfil, btnMenuProfil);
    }

    private void tampilkanPanel(VBox panel, Button menuAktif) {
        panelProfil.setVisible(panel == panelProfil);
        panelProfil.setManaged(panel == panelProfil);
        panelAkun.setVisible(panel == panelAkun);
        panelAkun.setManaged(panel == panelAkun);
        panelSesi.setVisible(panel == panelSesi);
        panelSesi.setManaged(panel == panelSesi);

        for (Button b : new Button[]{btnMenuProfil, btnMenuAkun, btnMenuSesi}) {
            b.getStyleClass().remove("settings-menu-active");
        }
        menuAktif.getStyleClass().add("settings-menu-active");
    }

    private void siapkanTampilanFoto() {
        // Tidak lagi menggunakan viewport/crop persegi dari tengah.
        // ImageView memakai preserveRatio=true agar seluruh foto tetap terlihat.
        if (imgFoto != null) {
            imgFoto.setFitWidth(UKURAN_FOTO_UTAMA - 10);
            imgFoto.setFitHeight(UKURAN_FOTO_UTAMA - 10);
            imgFoto.setPreserveRatio(true);
            imgFoto.setSmooth(true);
        }
        if (imgFotoSidebar != null) {
            imgFotoSidebar.setFitWidth(UKURAN_FOTO_SIDEBAR - 8);
            imgFotoSidebar.setFitHeight(UKURAN_FOTO_SIDEBAR - 8);
            imgFotoSidebar.setPreserveRatio(true);
            imgFotoSidebar.setSmooth(true);
        }
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

        String nama = safe(pengguna.getNama());
        String email = safe(pengguna.getEmail());

        fieldNama.setText(nama);
        fieldEmail.setText(email);
        labelRole.setText(pengguna.getRole() == null ? "-" : pengguna.getRole().getLabel());
        labelStatusAkun.setText(pengguna.getStatusAkun() == null
                ? "-"
                : humanis(pengguna.getStatusAkun().name()));

        labelNamaBesar.setText(nama.isEmpty() ? "-" : nama);
        labelEmailBesar.setText(email.isEmpty() ? "-" : email);

        labelNamaSidebar.setText(nama.isEmpty() ? "Pengguna" : nama);
        labelEmailSidebar.setText(email.isEmpty() ? "-" : email);

        labelNamaSesi.setText(nama.isEmpty() ? "Pengguna" : nama);
        labelEmailSesi.setText(email.isEmpty() ? "-" : email);
        labelRoleSesi.setText(pengguna.getRole() == null ? "-" : pengguna.getRole().getLabel());
        labelStatusSesi.setText(pengguna.getStatusAkun() == null
                ? "-"
                : humanis(pengguna.getStatusAkun().name()));

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

        String fotoLama = pengguna.getFoto();
        Path fotoBaruTersimpan = null;
        boolean dbBerhasil = false;

        try {
            if (!email.equalsIgnoreCase(safe(pengguna.getEmail()))
                    && penggunaDAO.existsByEmailExcept(email, pengguna.getPenggunaId())) {
                labelProfilError.setText("Email ini sudah dipakai akun lain.");
                return;
            }

            String pathFoto = fotoLama;

            if (hapusFotoDiminta) {
                pathFoto = null;
            } else if (fotoBaru != null) {
                Path hasilSalin = salinFotoKePenyimpanan(pengguna.getPenggunaId(), fotoBaru);
                fotoBaruTersimpan = hasilSalin;
                pathFoto = hasilSalin.toString();
            }

            if (!penggunaDAO.updateProfil(pengguna.getPenggunaId(), nama, email, pathFoto)) {
                labelProfilError.setText("Perubahan gagal disimpan.");
                return;
            }
            dbBerhasil = true;

            // Database sudah menyimpan path baru. Setelah itu baru bersihkan file lama.
            if ((hapusFotoDiminta || fotoBaru != null)
                    && fotoLama != null
                    && !fotoLama.isBlank()
                    && (pathFoto == null || !Path.of(fotoLama).equals(Path.of(pathFoto)))) {
                hapusFileFotoLama(fotoLama);
            }

            pengguna.setNama(nama);
            pengguna.setEmail(email);
            pengguna.setFoto(pathFoto);

            fotoBaru = null;
            hapusFotoDiminta = false;
            labelNamaBesar.setText(nama);
            labelEmailBesar.setText(email);
            labelNamaSidebar.setText(nama);
            labelEmailSidebar.setText(email);
            labelNamaSesi.setText(nama);
            labelEmailSesi.setText(email);
            tampilkanFoto(pathFoto);
            labelProfilError.setText("");
            info("Profil Saya", "Perubahan profil berhasil disimpan.");
        } catch (SQLException e) {
            // Bila DB gagal setelah file baru dibuat, hapus file baru agar tidak menjadi orphan.
            if (!dbBerhasil && fotoBaruTersimpan != null) {
                try {
                    Files.deleteIfExists(fotoBaruTersimpan);
                } catch (IOException ignored) {
                }
            }
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
        pemilih.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Gambar PNG atau JPG", "*.png", "*.jpg", "*.jpeg"));

        if (btnPilihFoto.getScene() == null) {
            return;
        }

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

        try {
            Image gambar = new Image(berkas.toURI().toString(), 0, 0, true, true);
            if (gambar.isError()) {
                labelProfilError.setText("File gambar tidak dapat dibaca.");
                return;
            }

            fotoBaru = berkas;
            hapusFotoDiminta = false;
            pasangFoto(gambar);
            labelProfilError.setText("Foto dipilih. Tekan \"Simpan Perubahan\" untuk menyimpan.");
        } catch (RuntimeException e) {
            labelProfilError.setText("File gambar tidak dapat dibaca.");
            System.err.println("[PengaturanAkunController] Gagal membaca foto: " + e.getMessage());
        }
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
            imgFotoSidebar.setImage(null);
            labelInisialFoto.setVisible(true);
            labelInisialSidebar.setVisible(true);
            perbaruiInisial();
            return;
        }

        try {
            File berkas = new File(pathFoto);
            if (!berkas.exists() || !berkas.isFile()) {
                imgFoto.setImage(null);
                imgFotoSidebar.setImage(null);
                labelInisialFoto.setVisible(true);
                labelInisialSidebar.setVisible(true);
                perbaruiInisial();
                return;
            }

            Image gambar = new Image(berkas.toURI().toString(), 0, 0, true, true);
            pasangFoto(gambar);
        } catch (RuntimeException e) {
            imgFoto.setImage(null);
            imgFotoSidebar.setImage(null);
            labelInisialFoto.setVisible(true);
            labelInisialSidebar.setVisible(true);
            perbaruiInisial();
            System.err.println("[PengaturanAkunController] Gagal memuat foto: " + e.getMessage());
        }
    }

    private void pasangFoto(Image gambar) {
        if (gambar == null || gambar.isError()) {
            imgFoto.setImage(null);
            imgFotoSidebar.setImage(null);
            labelInisialFoto.setVisible(true);
            labelInisialSidebar.setVisible(true);
            perbaruiInisial();
            return;
        }

        // Tanpa viewport: tidak ada crop. PreserveRatio menjaga proporsi asli foto.
        imgFoto.setImage(gambar);
        imgFotoSidebar.setImage(gambar);
        labelInisialFoto.setVisible(false);
        labelInisialSidebar.setVisible(false);
    }

    private void perbaruiInisial() {
        String nama = safe(fieldNama.getText());
        if (nama.isEmpty()) {
            Pengguna pengguna = SessionManager.getPenggunaAktif();
            nama = pengguna != null ? safe(pengguna.getNama()) : "";
        }
        String nilai = buatInisial(nama);
        labelInisialFoto.setText(nilai);
        labelInisialSidebar.setText(nilai);
    }

    private String buatInisial(String nama) {
        String[] bagian = nama.trim().split("\\s+");
        StringBuilder hasil = new StringBuilder();
        for (int i = 0; i < Math.min(2, bagian.length); i++) {
            if (!bagian[i].isEmpty()) {
                hasil.append(Character.toUpperCase(bagian[i].charAt(0)));
            }
        }
        return hasil.length() > 0 ? hasil.toString() : "?";
    }

    private Path salinFotoKePenyimpanan(int penggunaId, File sumber) throws IOException {
        String namaSumber = sumber.getName().toLowerCase();
        String ekstensi;
        if (namaSumber.endsWith(".png")) {
            ekstensi = ".png";
        } else {
            ekstensi = ".jpg";
        }

        Path folder = Path.of(System.getProperty("user.home"), ".siap", "foto-profil");
        Files.createDirectories(folder);

        // Simpan dengan nama konsisten, tetapi jangan menghapus foto lama sebelum DB sukses.
        Path tujuan = folder.resolve("pengguna-" + penggunaId + ekstensi);
        Files.copy(sumber.toPath(), tujuan, StandardCopyOption.REPLACE_EXISTING);

        // Bersihkan varian ekstensi lain hanya setelah salinan baru tersedia.
        if (".png".equals(ekstensi)) {
            Files.deleteIfExists(folder.resolve("pengguna-" + penggunaId + ".jpg"));
        } else {
            Files.deleteIfExists(folder.resolve("pengguna-" + penggunaId + ".png"));
        }

        return tujuan;
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
            Pengguna terbaru = penggunaDAO.findById(pengguna.getPenggunaId()).orElse(null);
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
            if (!penggunaDAO.updatePassword(pengguna.getPenggunaId(), hashBaru)) {
                labelError.setText("Password gagal diperbarui.");
                return;
            }

            pengguna.setPasswordHash(hashBaru);
            fieldPasswordLama.clear();
            fieldPasswordBaru.clear();
            fieldKonfirmasi.clear();
            info("Akun & Keamanan", "Password baru sudah tersimpan. Gunakan password baru saat login berikutnya.");
        } catch (SQLException e) {
            labelError.setText("Database tidak dapat dihubungi. Coba lagi.");
            System.err.println("[PengaturanAkunController] SQLException (password): " + e.getMessage());
        } catch (RuntimeException e) {
            labelError.setText("Terjadi kesalahan saat mengubah password.");
            System.err.println("[PengaturanAkunController] Runtime error: " + e.getMessage());
        }
    }

    // ================= Navigasi =================

    private void kembali() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();
        String tujuan = pengguna != null && pengguna.isGuru()
                ? "/com/nurulislam/siap/fxml/DashboardGuru.fxml"
                : "/com/nurulislam/siap/fxml/Dashboard.fxml";
        String judul = pengguna != null && pengguna.isGuru() ? "Beranda" : "Dashboard";

        try {
            SceneManager.switchTo(tujuan, Main.APP_TITLE + " - " + judul);
        } catch (IOException e) {
            labelError.setText("Gagal kembali ke dashboard.");
            System.err.println("[PengaturanAkunController] IOException: " + e.getMessage());
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
            if (k.isEmpty()) continue;
            if (hasil.length() > 0) hasil.append(' ');
            hasil.append(Character.toUpperCase(k.charAt(0)));
            if (k.length() > 1) hasil.append(k.substring(1).toLowerCase());
        }
        return hasil.toString();
    }

    private String safe(String nilai) {
        return nilai == null ? "" : nilai.trim();
    }
}
