package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.KelasDAO;
import com.nurulislam.siap.dao.MuridDAO;
import com.nurulislam.siap.model.Kelas;
import com.nurulislam.siap.model.Murid;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.Statusmurid;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.DatabaseConnection;
import com.nurulislam.siap.util.SessionManager;
import com.nurulislam.siap.util.ProfileMenu;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller untuk halaman "Manajemen Data Murid", khusus diakses oleh Staf TU.
 * Pola tabel + form kanan mengikuti {@link ManajemenDataKelasController} agar
 * konsisten secara pengalaman pengguna di seluruh aplikasi.
 * <p>
 * qr_token murid dibuat OTOMATIS oleh {@link MuridDAO#insert} saat murid baru
 * disimpan - tidak ada field input untuk itu di form, karena kode ini yang
 * nantinya dicetak sebagai QR Code di kartu pelajar (lihat tahap "Cetak Kartu
 * Pelajar" berikutnya).
 */
public class ManajemenDataMuridController {

    // --- Sidebar ---
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavVerifikasi;
    @FXML private Button btnNavScanQr;
    @FXML private Button btnNavDataMurid;
    @FXML private Button btnNavDataKelas;
    @FXML private Button btnNavMataPelajaran;
    @FXML private Button btnNavCetakKartu;
    @FXML private Button btnNavLaporan;
    @FXML private StackPane avatarBox;

    // --- Top bar ---
    @FXML private Label labelNamaUser;
    @FXML private Label labelRoleUser;
    @FXML private Label labelInisialUser;

    // --- Ringkasan ---
    @FXML private Label labelTotalMurid;
    @FXML private Label labelMuridAktif;
    @FXML private Label labelLakiLaki;
    @FXML private Label labelPerempuan;

    @FXML private Label errorLabel;
    @FXML private TextField fieldPencarian;
    @FXML private Button btnRefresh;
    @FXML private Button btnTambahMurid;

    // --- Tabel ---
    @FXML private TableView<Murid> tabelMurid;
    @FXML private TableColumn<Murid, String> kolomNis;
    @FXML private TableColumn<Murid, String> kolomNama;
    @FXML private TableColumn<Murid, String> kolomKelas;
    @FXML private TableColumn<Murid, String> kolomJenisKelamin;
    @FXML private TableColumn<Murid, String> kolomStatus;

    @FXML private Button btnUbah;
    @FXML private Button btnHapus;

    // --- Form ---
    @FXML private Label labelJudulForm;
    @FXML private ImageView imageViewFoto;
    @FXML private Button btnPilihFoto;
    @FXML private TextField fieldNis;
    @FXML private TextField fieldNama;
    @FXML private ComboBox<String> comboJenisKelamin;
    @FXML private DatePicker datePickerTanggalLahir;
    @FXML private ComboBox<Kelas> comboKelas;
    @FXML private ComboBox<Statusmurid> comboStatus;
    @FXML private Label formErrorLabel;
    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;

    private final MuridDAO muridDAO = new MuridDAO();
    private final KelasDAO kelasDAO = new KelasDAO();

    /** Seluruh murid dari database (belum difilter pencarian). */
    private final ObservableList<Murid> semuaMurid = FXCollections.observableArrayList();
    /** Daftar yang benar-benar ditampilkan di tabel, hasil filter pencarian aktif. */
    private final ObservableList<Murid> daftarTampil = FXCollections.observableArrayList();

    /** Murid yang sedang diedit lewat form; null berarti form dalam mode "Tambah". */
    private Murid muridSedangDiedit;
    /** Path foto yang baru dipilih lewat file chooser, menunggu disimpan saat form di-submit. */
    private String pathFotoDipilih;

    /** Mencegah klik Simpan berulang selama satu transaksi sedang berjalan. */
    private boolean prosesSimpanBerjalan = false;

    // Context navigasi dari halaman Data Kelas per kelas.
    private Integer kelasDefaultId;
    private Integer muridDefaultId;
    private static Integer kelasAwalId;
    private static String kelasAwalNama;
    private static Integer muridAwalId;

    /**
     * Dipakai halaman detail kelas agar Data Murid langsung fokus pada kelas tertentu.
     */
    public static void setKelasAwal(Integer kelasId, String namaKelas) {
        kelasAwalId = kelasId;
        kelasAwalNama = namaKelas;
        muridAwalId = null;
    }

    /**
     * Dipakai tombol Edit pada halaman detail kelas.
     */
    public static void setMuridAwal(Integer muridId, Integer kelasId, String namaKelas) {
        kelasAwalId = kelasId;
        kelasAwalNama = namaKelas;
        muridAwalId = muridId;
    }

    private static final String PLACEHOLDER_FOTO = "/com/nurulislam/siap/images/placeholder-foto.png";

    @FXML
    public void initialize() {
        errorLabel.setText("");
        formErrorLabel.setText("");

        isiInfoPengguna();
        siapkanSidebar();
        siapkanTabel();
        siapkanForm();

        fieldPencarian.textProperty().addListener((obs, lama, baru) -> terapkanFilter());

        btnUbah.setDisable(true);
        btnHapus.setDisable(true);
        tabelMurid.getSelectionModel().selectedItemProperty().addListener((obs, lama, baru) -> {
            boolean adaTerpilih = baru != null;
            btnUbah.setDisable(!adaTerpilih);
            btnHapus.setDisable(!adaTerpilih);
        });

        btnRefresh.setOnAction(e -> muatDaftarMurid());
        btnTambahMurid.setOnAction(e -> mulaiModeTambah());
        btnUbah.setOnAction(e -> mulaiModeUbah());
        btnHapus.setOnAction(e -> handleHapus());
        btnSimpan.setOnAction(e -> handleSimpan());
        btnBatal.setOnAction(e -> resetForm());
        btnPilihFoto.setOnAction(e -> handlePilihFoto());
        ProfileMenu.pasang(avatarBox, this::bukaProfil, this::handleLogout);

        muatDaftarKelasUntukCombo();
        muatDaftarMurid();

        // Tangkap context navigasi sekali, lalu kosongkan static state.
        kelasDefaultId = kelasAwalId;
        muridDefaultId = muridAwalId;
        String namaKelasAwal = kelasAwalNama;
        kelasAwalId = null;
        kelasAwalNama = null;
        muridAwalId = null;

        if (namaKelasAwal != null && !namaKelasAwal.isBlank()) {
            fieldPencarian.setText(namaKelasAwal);
        }

        resetForm();

        if (muridDefaultId != null) {
            javafx.application.Platform.runLater(() -> pilihDanEditMurid(muridDefaultId));
        }
    }

    // ================= Info pengguna & sidebar =================

    private void isiInfoPengguna() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();
        String nama = (pengguna != null && pengguna.getNama() != null) ? pengguna.getNama() : "Pengguna";
        String peran = (pengguna != null && pengguna.getRole() != null) ? pengguna.getRole().getLabel() : "";

        labelNamaUser.setText(nama);
        labelRoleUser.setText(peran);

        String[] bagian = nama.trim().split("\\s+");
        StringBuilder inisial = new StringBuilder();
        for (int i = 0; i < Math.min(2, bagian.length); i++) {
            if (!bagian[i].isEmpty()) {
                inisial.append(Character.toUpperCase(bagian[i].charAt(0)));
            }
        }
        labelInisialUser.setText(inisial.length() > 0 ? inisial.toString() : "?");
    }

    private void siapkanSidebar() {
        btnNavDashboard.setOnAction(e -> bukaDashboard());
        btnNavVerifikasi.setOnAction(e -> bukaVerifikasiAkun());
        btnNavScanQr.setOnAction(e -> bukaScanQr());
        btnNavDataMurid.setOnAction(e -> { /* sudah di halaman Data Murid */ });
        btnNavDataKelas.setOnAction(e -> bukaDataKelas());
        btnNavMataPelajaran.setOnAction(e -> bukaMataPelajaran());
        btnNavCetakKartu.setOnAction(e -> bukaCetakKartu());
        btnNavLaporan.setOnAction(e -> bukaLaporan());
    }

    // ================= Tabel =================

    private void siapkanTabel() {
        kolomNis.setCellValueFactory(new PropertyValueFactory<>("nis"));
        kolomNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        kolomKelas.setCellValueFactory(new PropertyValueFactory<>("namaKelas"));
        kolomJenisKelamin.setCellValueFactory(data -> {
            String jk = data.getValue().getJenisKelamin();
            return new javafx.beans.property.SimpleStringProperty(
                    "L".equals(jk) ? "Laki-laki" : "P".equals(jk) ? "Perempuan" : "-");
        });
        kolomStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getStatus() != null ? data.getValue().getStatus().name() : "-"));

        tabelMurid.setItems(daftarTampil);
        tabelMurid.setPlaceholder(new Label("Belum ada data murid."));
    }

    private void muatDaftarMurid() {
        try {
            semuaMurid.setAll(muridDAO.findAll());
            terapkanFilter();
            perbaruiRingkasan();
            errorLabel.setText("");
        } catch (SQLException e) {
            errorLabel.setText("Gagal memuat daftar murid. Coba lagi.");
            System.err.println("[ManajemenDataMuridController] SQLException: " + e.getMessage());
        }
    }

    private void terapkanFilter() {
        String kataKunci = fieldPencarian.getText() == null ? "" : fieldPencarian.getText().trim().toLowerCase();
        if (kataKunci.isEmpty()) {
            daftarTampil.setAll(semuaMurid);
        } else {
            daftarTampil.setAll(semuaMurid.stream()
                    .filter(m -> m.getNama().toLowerCase().contains(kataKunci)
                            || m.getNis().toLowerCase().contains(kataKunci)
                            || (m.getNamaKelas() != null && m.getNamaKelas().toLowerCase().contains(kataKunci)))
                    .toList());
        }
    }

    private void perbaruiRingkasan() {
        labelTotalMurid.setText(String.valueOf(semuaMurid.size()));
        long aktif = semuaMurid.stream().filter(m -> m.getStatus() == Statusmurid.AKTIF).count();
        labelMuridAktif.setText(String.valueOf(aktif));
        long lakiLaki = semuaMurid.stream().filter(m -> "L".equals(m.getJenisKelamin())).count();
        labelLakiLaki.setText(String.valueOf(lakiLaki));
        long perempuan = semuaMurid.stream().filter(m -> "P".equals(m.getJenisKelamin())).count();
        labelPerempuan.setText(String.valueOf(perempuan));
    }

    // ================= Form =================

    private void siapkanForm() {
        comboJenisKelamin.setItems(FXCollections.observableArrayList("L", "P"));
        comboJenisKelamin.setConverter(new StringConverter<>() {
            @Override
            public String toString(String kode) {
                return "L".equals(kode) ? "Laki-laki" : "P".equals(kode) ? "Perempuan" : "";
            }

            @Override
            public String fromString(String s) {
                return s;
            }
        });

        comboKelas.setConverter(new StringConverter<>() {
            @Override
            public String toString(Kelas k) {
                return k == null ? "" : k.getNamaKelas();
            }

            @Override
            public Kelas fromString(String s) {
                return null;
            }
        });

        comboStatus.setItems(FXCollections.observableArrayList(Statusmurid.values()));

        datePickerTanggalLahir.setValue(null);
    }

    private void muatDaftarKelasUntukCombo() {
        try {
            comboKelas.setItems(FXCollections.observableArrayList(kelasDAO.findAll()));
        } catch (SQLException e) {
            formErrorLabel.setText("Gagal memuat daftar kelas.");
            System.err.println("[ManajemenDataMuridController] SQLException (findAll kelas): " + e.getMessage());
        }
    }

    private void mulaiModeTambah() {
        resetForm();
        fieldNis.requestFocus();
    }

    private void mulaiModeUbah() {
        Murid terpilih = tabelMurid.getSelectionModel().getSelectedItem();
        if (terpilih == null) {
            return;
        }
        muridSedangDiedit = terpilih;
        pathFotoDipilih = null;
        labelJudulForm.setText("Ubah Murid");
        fieldNis.setText(terpilih.getNis());
        fieldNama.setText(terpilih.getNama());
        comboJenisKelamin.getSelectionModel().select(terpilih.getJenisKelamin());
        datePickerTanggalLahir.setValue(terpilih.getTanggalLahir());

        comboKelas.getItems().stream()
                .filter(k -> k.getKelasId() == terpilih.getKelasId())
                .findFirst()
                .ifPresent(comboKelas.getSelectionModel()::select);

        comboStatus.getSelectionModel().select(terpilih.getStatus());
        tampilkanFoto(terpilih.getFoto());

        formErrorLabel.setText("");
    }

    private void resetForm() {
        muridSedangDiedit = null;
        pathFotoDipilih = null;
        labelJudulForm.setText("Tambah Murid");
        fieldNis.clear();
        fieldNama.clear();
        comboJenisKelamin.getSelectionModel().clearSelection();
        datePickerTanggalLahir.setValue(null);
        comboKelas.getSelectionModel().clearSelection();
        if (kelasDefaultId != null) {
            comboKelas.getItems().stream()
                    .filter(k -> k.getKelasId() == kelasDefaultId)
                    .findFirst()
                    .ifPresent(comboKelas.getSelectionModel()::select);
        }

        comboStatus.getSelectionModel().select(Statusmurid.AKTIF);
        tampilkanFoto(null);
        formErrorLabel.setText("");
        tabelMurid.getSelectionModel().clearSelection();
    }

    private void pilihDanEditMurid(Integer muridId) {
        if (muridId == null) {
            return;
        }

        Murid target = tabelMurid.getItems().stream()
                .filter(m -> m.getMuridId() == muridId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            // Tabel bisa sedang terfilter nama kelas; cari dari semua data sebagai fallback.
            target = semuaMurid.stream()
                    .filter(m -> m.getMuridId() == muridId)
                    .findFirst()
                    .orElse(null);
        }

        if (target == null) {
            return;
        }

        tabelMurid.getSelectionModel().select(target);
        tabelMurid.scrollTo(target);
        mulaiModeUbah();
    }

    private void handlePilihFoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Pilih Foto Murid");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Gambar (*.jpg, *.jpeg, *.png)", "*.jpg", "*.jpeg", "*.png"));

        File dipilih = chooser.showOpenDialog(btnPilihFoto.getScene().getWindow());
        if (dipilih == null) {
            return;
        }

        try {
            String namaFileBaru = UUID.randomUUID() + "-" + dipilih.getName();
            Path folderTujuan = folderPenyimpananFoto();
            Files.createDirectories(folderTujuan);
            Path tujuan = folderTujuan.resolve(namaFileBaru);
            Files.copy(dipilih.toPath(), tujuan, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            pathFotoDipilih = tujuan.toAbsolutePath().toString();
            tampilkanFoto(pathFotoDipilih);
        } catch (IOException e) {
            formErrorLabel.setText("Gagal menyalin file foto. Coba pilih file lain.");
            System.err.println("[ManajemenDataMuridController] IOException (foto): " + e.getMessage());
        }
    }

    /**
     * Foto murid disalin ke folder data aplikasi (bukan disimpan sebagai BLOB di
     * database) supaya ukuran database tetap ringan; hanya PATH file yang disimpan
     * di kolom tb_murid.foto. Folder ini persisten di luar folder project, sehingga
     * tidak ikut terhapus saat "Clean and Build" di NetBeans.
     */
    private Path folderPenyimpananFoto() {
        return Paths.get(System.getProperty("user.home"), ".siap-nurulislam", "foto_murid");
    }

    private void tampilkanFoto(String path) {
        try {
            if (path != null && !path.isBlank() && Files.exists(Paths.get(path))) {
                BufferedImage buffered = ImageIO.read(new File(path));
                if (buffered != null) {
                    imageViewFoto.setImage(SwingFXUtils.toFXImage(buffered, null));
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("[ManajemenDataMuridController] Gagal memuat foto: " + e.getMessage());
        }

        // Fallback: placeholder jika belum ada foto atau file tidak ditemukan/rusak.
        java.io.InputStream placeholder = getClass().getResourceAsStream(PLACEHOLDER_FOTO);
        imageViewFoto.setImage(placeholder != null ? new Image(placeholder) : null);
    }

    private void handleSimpan() {
        if (prosesSimpanBerjalan) {
            return;
        }

        prosesSimpanBerjalan = true;
        btnSimpan.setDisable(true);
        formErrorLabel.setText("");

        try {
            handleSimpanInternal();
        } finally {
            prosesSimpanBerjalan = false;
            btnSimpan.setDisable(false);
        }
    }

    private void handleSimpanInternal() {
        formErrorLabel.setText("");

        String nis = fieldNis.getText() == null ? "" : fieldNis.getText().trim();
        String nama = fieldNama.getText() == null ? "" : fieldNama.getText().trim();
        String jenisKelamin = comboJenisKelamin.getSelectionModel().getSelectedItem();
        LocalDate tanggalLahir = datePickerTanggalLahir.getValue();
        Kelas kelasDipilih = comboKelas.getSelectionModel().getSelectedItem();
        Statusmurid statusDipilih = comboStatus.getSelectionModel().getSelectedItem();

        if (nis.isEmpty()) {
            formErrorLabel.setText("NIS wajib diisi.");
            return;
        }
        if (nama.isEmpty()) {
            formErrorLabel.setText("Nama wajib diisi.");
            return;
        }
        if (!nis.matches("\\d+")) {
            formErrorLabel.setText("NIS hanya boleh berisi angka.");
            return;
        }
        if (nis.length() > 20) {
            formErrorLabel.setText("NIS terlalu panjang (maksimal 20 angka).");
            return;
        }
        if (!nama.matches("[\\p{L} .,'-]+")) {
            formErrorLabel.setText("Nama hanya boleh berisi huruf, spasi, titik, koma, apostrof, atau tanda hubung.");
            return;
        }
        if (tanggalLahir != null && tanggalLahir.isAfter(LocalDate.now())) {
            formErrorLabel.setText("Tanggal lahir tidak boleh lebih dari hari ini.");
            return;
        }
        if (jenisKelamin == null) {
            formErrorLabel.setText("Jenis kelamin wajib dipilih.");
            return;
        }
        if (kelasDipilih == null) {
            formErrorLabel.setText("Kelas wajib dipilih.");
            return;
        }
        if (statusDipilih == null) {
            formErrorLabel.setText("Status wajib dipilih.");
            return;
        }

        // Cek koneksi sebelum melakukan proses simpan agar operator mendapat
        // informasi yang jelas ketika MySQL/XAMPP sedang mati.
        if (!DatabaseConnection.isDatabaseAvailable()) {
            formErrorLabel.setText("Database tidak terhubung. Nyalakan MySQL/XAMPP lalu coba simpan lagi.");
            return;
        }

        try {
            Integer excludeId = muridSedangDiedit != null ? muridSedangDiedit.getMuridId() : null;
            if (muridDAO.existsByNis(nis, excludeId)) {
                formErrorLabel.setText("NIS ini sudah dipakai murid lain.");
                return;
            }

            // Konfirmasi perubahan status agar murid tidak sengaja diaktifkan kembali
            // atau dinonaktifkan karena salah klik.
            if (muridSedangDiedit != null) {
                Statusmurid statusLama = muridSedangDiedit.getStatus();

                if (statusLama != statusDipilih) {
                    String pesan;

                    if (statusDipilih == Statusmurid.AKTIF
                            && statusLama != Statusmurid.AKTIF) {
                        pesan = "Murid \"" + muridSedangDiedit.getNama()
                                + "\" saat ini berstatus " + statusLama
                                + ".\n\nAktifkan kembali murid ini?\n"
                                + "Setelah aktif, murid dapat melakukan absensi kembali.";
                    } else if (statusDipilih != Statusmurid.AKTIF
                            && statusLama == Statusmurid.AKTIF) {
                        pesan = "Ubah status murid \"" + muridSedangDiedit.getNama()
                                + "\" dari AKTIF menjadi " + statusDipilih + "?\n\n"
                                + "Murid dengan status ini tidak dapat melakukan absensi.";
                    } else {
                        pesan = "Ubah status murid \"" + muridSedangDiedit.getNama()
                                + "\" dari " + statusLama + " menjadi " + statusDipilih + "?";
                    }

                    if (!konfirmasiPerubahanStatus(pesan)) {
                        formErrorLabel.setText("Perubahan status dibatalkan.");
                        return;
                    }
                }
            }

            Murid murid = muridSedangDiedit != null ? muridSedangDiedit : new Murid();
            murid.setNis(nis);
            murid.setNama(nama);
            murid.setJenisKelamin(jenisKelamin);
            murid.setTanggalLahir(tanggalLahir);
            murid.setKelasId(kelasDipilih.getKelasId());
            murid.setStatus(statusDipilih);
            // Foto: pakai yang baru dipilih jika ada; kalau mode ubah dan tidak ganti foto, pertahankan yang lama.
            if (pathFotoDipilih != null) {
                murid.setFoto(pathFotoDipilih);
            }

            if (muridSedangDiedit != null) {
                muridDAO.update(murid);
            } else {
                muridDAO.insert(murid);
            }

            resetForm();
            muatDaftarMurid();
        } catch (SQLException e) {
            String pesan = DatabaseConnection.getUserFriendlyMessage(e);
            formErrorLabel.setText(pesan);
            System.err.println("[ManajemenDataMuridController] SQLException (simpan): " + e.getMessage());
        }
    }

    /**
     * Meminta konfirmasi sebelum status murid diubah.
     */
    private boolean konfirmasiPerubahanStatus(String pesan) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Perubahan Status");
        alert.setHeaderText("Status murid akan diubah");
        alert.setContentText(pesan);

        Optional<ButtonType> jawaban = alert.showAndWait();
        return jawaban.isPresent() && jawaban.get() == ButtonType.OK;
    }

    private void handleHapus() {
        Murid terpilih = tabelMurid.getSelectionModel().getSelectedItem();
        if (terpilih == null) {
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Hapus Murid");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Hapus murid \"" + terpilih.getNama() + "\" (NIS " + terpilih.getNis() + ")?\n"
                + "Jika murid memiliki riwayat absensi, data akan dipertahankan dan statusnya dapat dinonaktifkan. Lanjutkan?");

        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isPresent() && jawaban.get() == ButtonType.OK) {
            if (!DatabaseConnection.isDatabaseAvailable()) {
                errorLabel.setText("Database tidak terhubung. Nyalakan MySQL/XAMPP lalu coba lagi.");
                return;
            }
            try {
                boolean memilikiRiwayat = muridDAO.hasAbsensiHistory(terpilih.getMuridId());
                if (memilikiRiwayat) {
                    muridDAO.archive(terpilih.getMuridId());
                    errorLabel.setText("Murid memiliki riwayat absensi, sehingga data dipertahankan dan status diubah menjadi NONAKTIF.");
                } else {
                    muridDAO.delete(terpilih.getMuridId());
                    errorLabel.setText("");
                }
                if (muridSedangDiedit != null && muridSedangDiedit.getMuridId() == terpilih.getMuridId()) {
                    resetForm();
                }
                muatDaftarMurid();
                errorLabel.setText("");
            } catch (SQLException e) {
                errorLabel.setText(DatabaseConnection.getUserFriendlyMessage(e));
                System.err.println("[ManajemenDataMuridController] SQLException (hapus): " + e.getMessage());
            }
        }
    }

    // ================= Navigasi =================

    private void bukaDashboard() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Dashboard.fxml", Main.APP_TITLE + " - Dashboard");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Dashboard.");
            System.err.println("[ManajemenDataMuridController] IOException: " + e.getMessage());
        }
    }

    private void bukaVerifikasiAkun() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/VerifikasiAkun.fxml",
                    Main.APP_TITLE + " - Verifikasi Akun");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Verifikasi Akun.");
            System.err.println("[ManajemenDataMuridController] IOException: " + e.getMessage());
        }
    }

    private void bukaScanQr() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ScanQr.fxml", Main.APP_TITLE + " - Scan QR");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Scan QR.");
            System.err.println("[ManajemenDataMuridController] IOException: " + e.getMessage());
        }
    }

    private void bukaLaporan() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/LaporanKehadiran.fxml",
                    Main.APP_TITLE + " - Laporan");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Laporan.");
            System.err.println("[ManajemenDataMuridController] IOException: " + e.getMessage());
        }
    }

    private void bukaCetakKartu() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenCetakKartu.fxml",
                    Main.APP_TITLE + " - Cetak Kartu Pelajar");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Cetak Kartu Pelajar.");
            System.err.println("[ManajemenDataMuridController] IOException: " + e.getMessage());
        }
    }

    private void bukaDataKelas() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenDataKelas.fxml",
                    Main.APP_TITLE + " - Data Kelas");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Data Kelas.");
            System.err.println("[ManajemenDataMuridController] IOException: " + e.getMessage());
        }
    }

    private void bukaMataPelajaran() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenMataPelajaran.fxml",
                    Main.APP_TITLE + " - Mata Pelajaran");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Mata Pelajaran.");
            System.err.println("[ManajemenDataMuridController] IOException: " + e.getMessage());
        }
    }

    private void tampilkanBelumTersedia(String namaFitur) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(namaFitur);
        alert.setHeaderText(null);
        alert.setContentText("Fitur \"" + namaFitur + "\" akan dibuat pada tahap pengembangan berikutnya.");
        alert.showAndWait();
    }

    /** Membuka halaman Profil Saya (diakses dari menu avatar kanan atas). */
    private void bukaProfil() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/PengaturanAkun.fxml",
                    Main.APP_TITLE + " - Profil Saya");
        } catch (IOException e) {
            System.err.println("[ManajemenDataMuridController] Gagal membuka Profil Saya: " + e.getMessage());
        }
    }

    private void handleLogout() {
        SessionManager.logout();
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", Main.APP_TITLE + " - Masuk");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Login.");
            System.err.println("[ManajemenDataMuridController] IOException: " + e.getMessage());
        }
    }
}