package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.KelasDAO;
import com.nurulislam.siap.dao.MuridDAO;
import com.nurulislam.siap.dao.TahunAjaranDAO;
import com.nurulislam.siap.model.Kelas;
import com.nurulislam.siap.model.Murid;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.Role;
import com.nurulislam.siap.model.TahunAjaran;
import com.nurulislam.siap.util.KartuPelajarView;
import com.nurulislam.siap.util.QrCodeUtil;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import com.nurulislam.siap.util.ProfileMenu;
import com.nurulislam.siap.util.AvatarUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller halaman "Cetak Kartu Pelajar", khusus Staf TU.
 * <p>
 * QR Code pada kartu memuat tb_murid.qr_token (BUKAN NIS secara langsung -
 * lihat catatan keamanan di TokenUtil), lalu dibaca kembali oleh kamera pada
 * halaman Scan QR / Absensi Mata Pelajaran.
 * <p>
 * Pencetakan memakai API cetak bawaan JavaFX ({@link PrinterJob}), BUKAN
 * library PDF tambahan seperti iText/PDFBox - pengguna tinggal memilih
 * printer "Microsoft Print to PDF" (sudah tersedia bawaan di Windows) pada
 * dialog cetak jika ingin hasil berupa file PDF, tanpa menambah dependency
 * baru ke project. Tersedia juga opsi "Simpan sebagai PNG" untuk ekspor
 * gambar langsung tanpa perlu dialog printer.
 */
public class ManajemenCetakKartuController {

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
    @FXML private ImageView imgAvatar;

    // --- Top bar ---
    @FXML private Label labelNamaUser;
    @FXML private Label labelRoleUser;
    @FXML private Label labelInisialUser;

    // --- Filter & tabel murid ---
    @FXML private ComboBox<Kelas> comboFilterKelas;
    @FXML private TextField fieldPencarian;
    @FXML private Label labelJumlahTerpilih;
    @FXML private TableView<Murid> tabelMurid;
    @FXML private TableColumn<Murid, String> kolomNis;
    @FXML private TableColumn<Murid, String> kolomNama;
    @FXML private TableColumn<Murid, String> kolomKelas;

    @FXML private Label errorLabel;
    @FXML private Button btnCetak;
    @FXML private Button btnSimpanPng;
    @FXML private Button btnRegenerateToken;

    // --- Pratinjau kartu ---
    @FXML private ToggleButton tabKartuDepan;
    @FXML private ToggleButton tabKartuBelakang;
    @FXML private Pane previewHolder;

    private final MuridDAO muridDAO = new MuridDAO();
    private final KelasDAO kelasDAO = new KelasDAO();
    private final TahunAjaranDAO tahunAjaranDAO = new TahunAjaranDAO();

    private final ObservableList<Murid> semuaMurid = FXCollections.observableArrayList();
    private final ObservableList<Murid> daftarTampil = FXCollections.observableArrayList();

    /** Kartu sisi depan & belakang milik murid yang sedang dipratinjau. */
    private Pane kartuDepan;
    private Pane kartuBelakang;
    private ToggleGroup grupKartu;
    private Image logoResmi;

    /** Ukuran holder pratinjau di kolom kanan. */
    private static final double PREVIEW_LEBAR = 240;
    private static final double PREVIEW_TINGGI = 300;

    private static final DateTimeFormatter FORMAT_TTL =
            DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("id", "ID"));

    @FXML
    public void initialize() {
        if (!pastikanAksesTU()) return;
        errorLabel.setText("");

        isiInfoPengguna();
        siapkanSidebar();
        siapkanFilter();
        siapkanTabel();
        siapkanPratinjauKartu();
        logoResmi = KartuPelajarView.muatLogoResmi();

        fieldPencarian.textProperty().addListener((obs, lama, baru) -> terapkanFilter());
        comboFilterKelas.valueProperty().addListener((obs, lama, baru) -> terapkanFilter());

        tabelMurid.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabelMurid.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<Murid>) c -> perbaruiSetelahSeleksiBerubah());

        btnCetak.setOnAction(e -> handleCetak());
        btnSimpanPng.setOnAction(e -> handleSimpanPng());
        btnRegenerateToken.setOnAction(e -> handleRegenerateToken());
        ProfileMenu.pasang(avatarBox, this::bukaProfil, this::handleLogout);
        AvatarUtil.tampilkan(avatarBox, imgAvatar, labelInisialUser);

        muatDaftarKelasUntukFilter();
        muatDaftarMurid();
        tampilkanKartuKosong();
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
        btnNavDashboard.setOnAction(e -> bukaHalaman("/com/nurulislam/siap/fxml/Dashboard.fxml", "Dashboard"));
        btnNavVerifikasi.setOnAction(e -> bukaHalaman("/com/nurulislam/siap/fxml/VerifikasiAkun.fxml", "Verifikasi Akun"));
        btnNavScanQr.setOnAction(e -> bukaHalaman("/com/nurulislam/siap/fxml/ScanQr.fxml", "Scan QR"));
        btnNavDataMurid.setOnAction(e -> bukaHalaman("/com/nurulislam/siap/fxml/ManajemenDataMurid.fxml", "Data Murid"));
        btnNavDataKelas.setOnAction(e -> bukaHalaman("/com/nurulislam/siap/fxml/ManajemenDataKelas.fxml", "Data Kelas"));
        btnNavMataPelajaran.setOnAction(e -> bukaHalaman("/com/nurulislam/siap/fxml/ManajemenMataPelajaran.fxml", "Mata Pelajaran"));
        btnNavCetakKartu.setOnAction(e -> { /* sudah di halaman Cetak Kartu Pelajar */ });
        btnNavLaporan.setOnAction(e -> bukaHalaman("/com/nurulislam/siap/fxml/LaporanKehadiran.fxml", "Laporan"));
    }

    private void bukaHalaman(String fxml, String judul) {
        try {
            SceneManager.switchTo(fxml, Main.APP_TITLE + " - " + judul);
        } catch (IOException e) {
            System.err.println("[ManajemenCetakKartuController] Gagal membuka halaman " + judul + ": " + e.getMessage());
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
            System.err.println("[ManajemenCetakKartuController] Gagal membuka Profil Saya: " + e.getMessage());
        }
    }

    private void handleLogout() {
        SessionManager.logout();
        bukaHalaman("/com/nurulislam/siap/fxml/Login.fxml", "Masuk");
    }

    // ================= Filter & tabel =================

    private void siapkanFilter() {
        comboFilterKelas.setConverter(new StringConverter<>() {
            @Override
            public String toString(Kelas k) {
                return k == null ? "Semua Kelas" : k.getNamaKelas();
            }

            @Override
            public Kelas fromString(String s) {
                return null;
            }
        });
    }

    private void muatDaftarKelasUntukFilter() {
        try {
            ObservableList<Kelas> daftar = FXCollections.observableArrayList();
            daftar.add(null); // opsi "Semua Kelas"
            daftar.addAll(kelasDAO.findAll());
            comboFilterKelas.setItems(daftar);
            comboFilterKelas.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            errorLabel.setText("Gagal memuat daftar kelas untuk filter.");
            System.err.println("[ManajemenCetakKartuController] SQLException (kelas): " + e.getMessage());
        }
    }

    private void siapkanTabel() {
        kolomNis.setCellValueFactory(new PropertyValueFactory<>("nis"));
        kolomNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        kolomKelas.setCellValueFactory(new PropertyValueFactory<>("namaKelas"));
        tabelMurid.setItems(daftarTampil);
        tabelMurid.setPlaceholder(new Label("Belum ada data murid."));
    }

    private void muatDaftarMurid() {
        try {
            semuaMurid.setAll(muridDAO.findAll());
            terapkanFilter();
            errorLabel.setText("");
        } catch (SQLException e) {
            errorLabel.setText("Gagal memuat daftar murid. Coba lagi.");
            System.err.println("[ManajemenCetakKartuController] SQLException (murid): " + e.getMessage());
        }
    }

    private void terapkanFilter() {
        String kataKunci = fieldPencarian.getText() == null ? "" : fieldPencarian.getText().trim().toLowerCase();
        Kelas kelasDipilih = comboFilterKelas.getSelectionModel().getSelectedItem();

        daftarTampil.setAll(semuaMurid.stream()
                .filter(m -> kelasDipilih == null || m.getKelasId() == kelasDipilih.getKelasId())
                .filter(m -> kataKunci.isEmpty()
                        || m.getNama().toLowerCase().contains(kataKunci)
                        || m.getNis().toLowerCase().contains(kataKunci))
                .toList());
    }

    // ================= Pratinjau kartu =================

    private void siapkanPratinjauKartu() {
        grupKartu = new ToggleGroup();
        tabKartuDepan.setToggleGroup(grupKartu);
        tabKartuBelakang.setToggleGroup(grupKartu);
        tabKartuDepan.setSelected(true);
        grupKartu.selectedToggleProperty().addListener((obs, lama, baru) -> {
            if (baru == null) {
                grupKartu.selectToggle(lama != null ? lama : tabKartuDepan);
                return;
            }
            perbaruiGayaTabKartu();
            tampilkanSisiAktif();
        });
        perbaruiGayaTabKartu();
    }

    private void perbaruiGayaTabKartu() {
        for (ToggleButton tab : List.of(tabKartuDepan, tabKartuBelakang)) {
            tab.getStyleClass().remove("filter-tab-active");
            if (tab.isSelected()) {
                tab.getStyleClass().add("filter-tab-active");
            }
        }
    }

    /** Menampilkan sisi kartu (depan/belakang) sesuai toggle aktif. */
    private void tampilkanSisiAktif() {
        previewHolder.getChildren().clear();
        boolean belakang = grupKartu.getSelectedToggle() == tabKartuBelakang;
        Pane sisi = belakang ? kartuBelakang : kartuDepan;
        double lebarAsli = belakang
                ? KartuPelajarView.LEBAR_BELAKANG : KartuPelajarView.LEBAR_DEPAN;
        double tinggiAsli = belakang
                ? KartuPelajarView.TINGGI_BELAKANG : KartuPelajarView.TINGGI_DEPAN;
        if (sisi == null) {
            Label kosong = new Label("Pilih murid di tabel");
            kosong.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7076;");
            kosong.setLayoutX(63);
            kosong.setLayoutY(140);
            previewHolder.getChildren().add(kosong);
            return;
        }
        // Skala pas + tengahkan dalam holder.
        double skala = Math.min(PREVIEW_LEBAR / lebarAsli, PREVIEW_TINGGI / tinggiAsli);
        sisi.getTransforms().clear();
        sisi.getTransforms().add(new Scale(skala, skala, 0, 0));
        sisi.setLayoutX((PREVIEW_LEBAR - lebarAsli * skala) / 2);
        sisi.setLayoutY((PREVIEW_TINGGI - tinggiAsli * skala) / 2);
        Pane bungkus = new Pane();
        bungkus.setPrefSize(PREVIEW_LEBAR, PREVIEW_TINGGI);
        bungkus.getChildren().add(sisi);
        previewHolder.getChildren().add(bungkus);
    }

    /** Pasangan kartu depan-belakang yang baru dibangun (belum diskala). */
    private record Kartu(Pane depan, Pane belakang) {
    }

    private void perbaruiSetelahSeleksiBerubah() {
        List<Murid> terpilih = tabelMurid.getSelectionModel().getSelectedItems();
        labelJumlahTerpilih.setText(terpilih.size() + " murid terpilih");

        boolean adaTerpilih = !terpilih.isEmpty();
        btnCetak.setDisable(!adaTerpilih);
        btnSimpanPng.setDisable(!adaTerpilih);
        btnRegenerateToken.setDisable(terpilih.size() != 1);

        if (terpilih.size() == 1) {
            tampilkanKartu(terpilih.get(0));
        } else if (terpilih.isEmpty()) {
            tampilkanKartuKosong();
        } else {
            // Lebih dari satu terpilih: tampilkan kartu murid pertama sebagai contoh pratinjau.
            tampilkanKartu(terpilih.get(0));
        }
    }

    private void tampilkanKartuKosong() {
        kartuDepan = null;
        kartuBelakang = null;
        tampilkanSisiAktif();
        btnCetak.setDisable(true);
        btnSimpanPng.setDisable(true);
        btnRegenerateToken.setDisable(true);
        labelJumlahTerpilih.setText("0 murid terpilih");
    }

    private void tampilkanKartu(Murid murid) {
        try {
            Kartu kartu = bangunKartu(murid);
            kartuDepan = kartu.depan();
            kartuBelakang = kartu.belakang();
            tampilkanSisiAktif();
        } catch (Exception e) {
            errorLabel.setText("Gagal membuat pratinjau kartu untuk " + murid.getNama() + ".");
            System.err.println("[ManajemenCetakKartuController] Gagal bangun kartu: " + e.getMessage());
        }
    }

    /**
     * Membangun pasangan kartu depan-belakang dari data murid + foto + QR.
     * Dipakai pratinjau, cetak, dan ekspor PNG (selalu instance baru).
     */
    private Kartu bangunKartu(Murid murid) throws Exception {
        String nama = murid.getNama() != null ? murid.getNama() : "-";
        String nis = murid.getNis() != null ? murid.getNis() : "-";
        String ttl = murid.getTanggalLahir() != null
                ? murid.getTanggalLahir().format(FORMAT_TTL) : "-";

        String jurusan = "-";
        String tahunMasuk = "-";
        try {
            Optional<Kelas> kelas = kelasDAO.findById(murid.getKelasId());
            if (kelas.isPresent()) {
                if (kelas.get().getJurusan() != null && !kelas.get().getJurusan().isBlank()) {
                    jurusan = kelas.get().getJurusan();
                }
                Optional<TahunAjaran> tahun = tahunAjaranDAO.findById(kelas.get().getTahunAjaranId());
                if (tahun.isPresent() && tahun.get().getTahun() != null) {
                    tahunMasuk = tahun.get().getTahun().split("/")[0];
                }
            }
        } catch (SQLException e) {
            System.err.println("[ManajemenCetakKartuController] Gagal muat kelas/tahun: " + e.getMessage());
        }

        Image foto = muatFotoMurid(murid.getFoto());
        Image qr = QrCodeUtil.buatGambarQr(murid.getQrToken(), 300);

        Pane depan = KartuPelajarView.buatDepan(nama, nis, ttl, jurusan, tahunMasuk, foto, logoResmi);
        Pane belakang = KartuPelajarView.buatBelakang(qr, logoResmi);
        return new Kartu(depan, belakang);
    }

    private Image muatFotoMurid(String path) {
        try {
            if (path != null && !path.isBlank() && Files.exists(Paths.get(path))) {
                BufferedImage buffered = ImageIO.read(new File(path));
                if (buffered != null) {
                    return SwingFXUtils.toFXImage(buffered, null);
                }
            }
        } catch (IOException e) {
            System.err.println("[ManajemenCetakKartuController] Gagal memuat foto: " + e.getMessage());
        }
        return null;
    }

    // ================= Cetak & ekspor =================

    private void handleCetak() {
        List<Murid> terpilih = tabelMurid.getSelectionModel().getSelectedItems();
        if (terpilih.isEmpty()) {
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            errorLabel.setText("Tidak ada printer yang terdeteksi di komputer ini.");
            return;
        }

        boolean lanjut = job.showPrintDialog(btnCetak.getScene().getWindow());
        if (!lanjut) {
            return;
        }

        Printer printer = job.getPrinter();
        PageLayout layout = printer.createPageLayout(Paper.A5, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);

        try {
            for (Murid murid : terpilih) {
                try {
                    Kartu kartu = bangunKartu(murid);
                    cetakSisi(job, layout, kartu.depan());
                    cetakSisi(job, layout, kartu.belakang());
                } catch (Exception e) {
                    System.err.println("[ManajemenCetakKartuController] Lewati " + murid.getNama()
                            + ": " + e.getMessage());
                }
            }
            job.endJob();
            errorLabel.setText("");
        } catch (Exception e) {
            errorLabel.setText("Gagal mencetak kartu. Coba lagi.");
            System.err.println("[ManajemenCetakKartuController] Gagal mencetak: " + e.getMessage());
        } finally {
            // Kembalikan pratinjau ke murid terakhir yang benar-benar dipilih di tabel.
            if (!terpilih.isEmpty()) {
                tampilkanKartu(terpilih.get(0));
            }
        }
    }

    /** Mencetak satu sisi kartu (node baru) diskala pas area cetak. */
    private void cetakSisi(PrinterJob job, PageLayout layout, Pane sisi) {
        double skala = Math.min(1.0, Math.min(
                layout.getPrintableWidth() / sisi.getPrefWidth(),
                layout.getPrintableHeight() / sisi.getPrefHeight()));
        javafx.scene.Group bungkus = new javafx.scene.Group(sisi);
        bungkus.getTransforms().add(new Scale(skala, skala, 0, 0));
        bungkus.applyCss();
        bungkus.layout();
        job.printPage(layout, bungkus);
    }

    private void handleSimpanPng() {
        List<Murid> terpilih = tabelMurid.getSelectionModel().getSelectedItems();
        if (terpilih.isEmpty()) {
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Pilih Folder Simpan Kartu Pelajar (PNG)");
        File folderTujuan = chooser.showDialog(btnSimpanPng.getScene().getWindow());
        if (folderTujuan == null) {
            return;
        }

        int berhasil = 0;
        String dasarNama = "";
        for (Murid murid : terpilih) {
            try {
                Kartu kartu = bangunKartu(murid);
                dasarNama = murid.getNis() + "_" + slugify(murid.getNama());
                simpanPng(kartu.depan(), new File(folderTujuan, dasarNama + "_depan.png"));
                simpanPng(kartu.belakang(), new File(folderTujuan, dasarNama + "_belakang.png"));
                berhasil++;
            } catch (Exception e) {
                System.err.println("[ManajemenCetakKartuController] Gagal simpan PNG untuk "
                        + murid.getNama() + ": " + e.getMessage());
            }
        }

        if (!terpilih.isEmpty()) {
            tampilkanKartu(terpilih.get(0));
        }

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Simpan Kartu Pelajar");
        info.setHeaderText(null);
        info.setContentText(berhasil + " dari " + terpilih.size() + " kartu berhasil disimpan ke:\n"
                + folderTujuan.getAbsolutePath());
        info.showAndWait();
    }

    /** Snapshot satu sisi kartu (node baru) ke file PNG. */
    private void simpanPng(Pane sisi, File tujuan) throws IOException {
        // Node dibangun baru khusus ekspor (tidak tampil di scene) sehingga
        // applyCss() + layout() wajib manual sebelum snapshot.
        sisi.applyCss();
        sisi.layout();
        Image snapshot = sisi.snapshot(new javafx.scene.SnapshotParameters(), null);
        BufferedImage buffered = SwingFXUtils.fromFXImage(snapshot, null);
        ImageIO.write(buffered, "PNG", tujuan);
    }

    private void handleRegenerateToken() {
        Murid terpilih = tabelMurid.getSelectionModel().getSelectedItem();
        if (terpilih == null) {
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Buat Ulang Kode QR");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Kartu pelajar LAMA milik \"" + terpilih.getNama()
                + "\" tidak akan berlaku lagi setelah ini (jika sudah pernah dicetak).\n"
                + "Kartu baru dengan kode QR baru perlu dicetak ulang. Lanjutkan?");

        var jawaban = konfirmasi.showAndWait();
        if (jawaban.isPresent() && jawaban.get() == ButtonType.OK) {
            try {
                muridDAO.regenerateQrToken(terpilih.getMuridId());
                muatDaftarMurid();
                errorLabel.setText("");
            } catch (SQLException e) {
                errorLabel.setText("Gagal membuat ulang kode QR. Coba lagi.");
                System.err.println("[ManajemenCetakKartuController] SQLException (regenerate): " + e.getMessage());
            }
        }
    }

    private String slugify(String nama) {
        return nama == null ? "" : nama.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }
    /** Memastikan halaman ini hanya dapat diakses oleh Staf TU. */
    private boolean pastikanAksesTU() {
        Pengguna sesi = SessionManager.getPenggunaAktif();
        if (sesi != null && sesi.getRole() == Role.TU) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Akses Ditolak");
        alert.setHeaderText("Halaman khusus Staf TU");
        alert.setContentText("Akun Guru tidak memiliki hak akses ke halaman ini.");
        alert.showAndWait();
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/DashboardGuru.fxml", Main.APP_TITLE + " - Beranda");
        } catch (IOException e) {
            System.err.println("[AccessGuard] Gagal kembali ke Dashboard Guru: " + e.getMessage());
        }
        return false;
    }

}