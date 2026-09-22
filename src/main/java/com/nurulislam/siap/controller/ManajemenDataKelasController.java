package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.KelasDAO;
import com.nurulislam.siap.dao.KelasMasihDipakaiException;
import com.nurulislam.siap.dao.PenggunaDAO;
import com.nurulislam.siap.dao.TahunAjaranDAO;
import com.nurulislam.siap.model.Kelas;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.Role;
import com.nurulislam.siap.model.TahunAjaran;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import com.nurulislam.siap.util.ProfileMenu;
import com.nurulislam.siap.util.AvatarUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.image.ImageView;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller untuk halaman "Manajemen Data Kelas", khusus diakses oleh Staf TU.
 * Sesuai desain "manajemen_data_kelas_siap": tabel daftar kelas di kolom kiri,
 * form tambah/ubah di panel kanan.
 * <p>
 * Kelas selalu dikaitkan ke tahun ajaran yang sedang AKTIF (lihat
 * TahunAjaranDAO#findAktif); daftar tahun ajaran lampau ditampilkan sebagai
 * referensi saja pada combo box.
 */
public class ManajemenDataKelasController {

    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavVerifikasi;
    @FXML private Button btnNavScanQr;
    @FXML private Button btnNavDataMurid;
    @FXML private Button btnNavDataKelas;
    @FXML private VBox boxSubKelas;
    @FXML private Button btnNavMataPelajaran;
    @FXML private Button btnNavCetakKartu;
    @FXML private Button btnNavLaporan;

    // Shortcut enam kelas aktif: X IPA, X IPS, XI IPA, XI IPS, XII IPA, XII IPS.
    @FXML private Button btnKelasXIPA;
    @FXML private Button btnKelasXIPS;
    @FXML private Button btnKelasXIIPA;
    @FXML private Button btnKelasXIIPS;
    @FXML private Button btnKelasXIIIPA;
    @FXML private Button btnKelasXIIIPS;

    @FXML private StackPane avatarBox;
    @FXML private ImageView imgAvatar;

    @FXML private Label labelNamaUser;
    @FXML private Label labelRoleUser;
    @FXML private Label labelInisialUser;

    @FXML private Label labelTotalKelas;
    @FXML private Label labelTotalSiswa;
    @FXML private Label labelTanpaWali;

    @FXML private Label errorLabel;
    @FXML private TextField fieldPencarian;
    @FXML private Button btnRefresh;
    @FXML private Button btnTambahKelas;

    @FXML private TableView<Kelas> tabelKelas;
    @FXML private TableColumn<Kelas, String> kolomNamaKelas;
    @FXML private TableColumn<Kelas, String> kolomTingkatJurusan;
    @FXML private TableColumn<Kelas, String> kolomWaliKelas;
    @FXML private TableColumn<Kelas, Number> kolomJumlahMurid;
    @FXML private TableColumn<Kelas, String> kolomTahunAjaran;

    @FXML private Button btnUbah;
    @FXML private Button btnHapus;

    @FXML private Label labelJudulForm;
    @FXML private TextField fieldNamaKelas;
    @FXML private ComboBox<String> comboTingkat;
    @FXML private TextField fieldJurusan;
    @FXML private ComboBox<Pengguna> comboWaliKelas;
    @FXML private ComboBox<TahunAjaran> comboTahunAjaran;
    @FXML private Label formErrorLabel;
    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;

    private final KelasDAO kelasDAO = new KelasDAO();
    private final PenggunaDAO penggunaDAO = new PenggunaDAO();
    private final TahunAjaranDAO tahunAjaranDAO = new TahunAjaranDAO();

    /** Seluruh kelas dari database (belum difilter pencarian). */
    private final ObservableList<Kelas> semuaKelas = FXCollections.observableArrayList();
    /** Daftar yang benar-benar ditampilkan di tabel, hasil filter pencarian aktif. */
    private final ObservableList<Kelas> daftarTampil = FXCollections.observableArrayList();

    /** Kelas yang sedang diedit lewat form; null berarti form dalam mode "Tambah". */
    private Kelas kelasSedangDiedit;
    private boolean subKelasTerbuka = true;

    @FXML
    public void initialize() {
        if (!pastikanAksesTU()) return;
        errorLabel.setText("");
        formErrorLabel.setText("");

        isiInfoPengguna();
        siapkanSidebar();
        siapkanTabel();
        siapkanForm();
        setSubKelasTerbuka(true);

        fieldPencarian.textProperty().addListener((obs, lama, baru) -> terapkanFilter());

        btnUbah.setDisable(true);
        btnHapus.setDisable(true);
        tabelKelas.getSelectionModel().selectedItemProperty().addListener((obs, lama, baru) -> {
            boolean adaTerpilih = baru != null;
            btnUbah.setDisable(!adaTerpilih);
            btnHapus.setDisable(!adaTerpilih);
        });

        btnRefresh.setOnAction(e -> muatDaftarKelas());
        btnTambahKelas.setOnAction(e -> mulaiModeTambah());
        btnUbah.setOnAction(e -> mulaiModeUbah());
        btnHapus.setOnAction(e -> handleHapus());
        btnSimpan.setOnAction(e -> handleSimpan());
        btnBatal.setOnAction(e -> resetForm());
        ProfileMenu.pasang(avatarBox, this::bukaProfil, this::handleLogout);
        AvatarUtil.tampilkan(avatarBox, imgAvatar, labelInisialUser);

        muatDaftarPendukung();
        muatDaftarKelas();
        resetForm();
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

    private void setSubKelasTerbuka(boolean terbuka) {
        subKelasTerbuka = terbuka;
        boxSubKelas.setVisible(terbuka);
        boxSubKelas.setManaged(terbuka);
        btnNavDataKelas.setText(terbuka ? "Data Kelas   ⌄" : "Data Kelas   ›");
    }

    private void toggleSubKelas() {
        setSubKelasTerbuka(!subKelasTerbuka);
    }

    private void siapkanSidebar() {
        btnNavDashboard.setOnAction(e -> bukaDashboard());
        btnNavVerifikasi.setOnAction(e -> bukaVerifikasiAkun());
        btnNavScanQr.setOnAction(e -> bukaScanQr());
        btnNavDataMurid.setOnAction(e -> bukaDataMurid());
        btnNavDataKelas.setOnAction(e -> toggleSubKelas());
        btnNavMataPelajaran.setOnAction(e -> bukaMataPelajaran());
        btnNavCetakKartu.setOnAction(e -> bukaCetakKartu());
        btnNavLaporan.setOnAction(e -> bukaLaporan());

        siapkanNavigasiKelas();
    }

    /**
     * Shortcut kelas dibuat langsung dari sidebar agar Staf TU tidak perlu
     * membuka Data Murid lalu mencari kelas secara manual.
     */
    private void siapkanNavigasiKelas() {
        btnKelasXIPA.setOnAction(e -> bukaDetailKelas("X IPA", btnKelasXIPA));
        btnKelasXIPS.setOnAction(e -> bukaDetailKelas("X IPS", btnKelasXIPS));
        btnKelasXIIPA.setOnAction(e -> bukaDetailKelas("XI IPA", btnKelasXIIPA));
        btnKelasXIIPS.setOnAction(e -> bukaDetailKelas("XI IPS", btnKelasXIIPS));
        btnKelasXIIIPA.setOnAction(e -> bukaDetailKelas("XII IPA", btnKelasXIIIPA));
        btnKelasXIIIPS.setOnAction(e -> bukaDetailKelas("XII IPS", btnKelasXIIIPS));
    }

    private void bukaDetailKelas(String namaKelas, Button tombol) {
        DataKelasDetailController.setKelasTarget(namaKelas);
        try {
            SceneManager.switchTo(
                    "/com/nurulislam/siap/fxml/DataKelasDetail.fxml",
                    Main.APP_TITLE + " - " + namaKelas
            );
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka data " + namaKelas + ".");
            System.err.println("[ManajemenDataKelasController] IOException: " + e.getMessage());
        }
    }

    // ================= Tabel =================

    private void siapkanTabel() {
        kolomNamaKelas.setCellValueFactory(new PropertyValueFactory<>("namaKelas"));
        kolomTingkatJurusan.setCellValueFactory(new PropertyValueFactory<>("tingkatJurusan"));
        kolomWaliKelas.setCellValueFactory(new PropertyValueFactory<>("waliKelasNama"));
        kolomJumlahMurid.setCellValueFactory(new PropertyValueFactory<>("jumlahMurid"));
        kolomTahunAjaran.setCellValueFactory(new PropertyValueFactory<>("tahunAjaranLabel"));

        tabelKelas.setItems(daftarTampil);
        tabelKelas.setPlaceholder(new Label("Belum ada data kelas."));
    }

    private void muatDaftarKelas() {
        try {
            semuaKelas.setAll(kelasDAO.findAllDetail());
            terapkanFilter();
            perbaruiRingkasan();
            errorLabel.setText("");
        } catch (SQLException e) {
            errorLabel.setText("Gagal memuat daftar kelas. Coba lagi.");
            System.err.println("[ManajemenDataKelasController] SQLException: " + e.getMessage());
        }
    }

    private void terapkanFilter() {
        String kataKunci = fieldPencarian.getText() == null ? "" : fieldPencarian.getText().trim().toLowerCase();
        if (kataKunci.isEmpty()) {
            daftarTampil.setAll(semuaKelas);
        } else {
            daftarTampil.setAll(semuaKelas.stream()
                    .filter(k -> k.getNamaKelas().toLowerCase().contains(kataKunci)
                            || (k.getWaliKelasNama() != null && k.getWaliKelasNama().toLowerCase().contains(kataKunci)))
                    .toList());
        }
    }

    private void perbaruiRingkasan() {
        labelTotalKelas.setText(String.valueOf(semuaKelas.size()));
        int totalSiswa = semuaKelas.stream().mapToInt(Kelas::getJumlahMurid).sum();
        labelTotalSiswa.setText(String.valueOf(totalSiswa));
        long tanpaWali = semuaKelas.stream().filter(k -> k.getWaliKelasId() == null).count();
        labelTanpaWali.setText(String.valueOf(tanpaWali));
    }

    // ================= Form =================

    private void siapkanForm() {
        comboTingkat.setItems(FXCollections.observableArrayList("X", "XI", "XII"));

        comboWaliKelas.setConverter(new StringConverter<>() {
            @Override
            public String toString(Pengguna p) {
                return p == null ? "" : p.getNama();
            }

            @Override
            public Pengguna fromString(String s) {
                return null; // tidak dipakai, combo box bersifat read-only (bukan editable)
            }
        });

        comboTahunAjaran.setConverter(new StringConverter<>() {
            @Override
            public String toString(TahunAjaran ta) {
                return ta == null ? "" : ta.getLabel();
            }

            @Override
            public TahunAjaran fromString(String s) {
                return null;
            }
        });
    }

    private void muatDaftarPendukung() {
        try {
            comboWaliKelas.setItems(FXCollections.observableArrayList(penggunaDAO.findByRole(Role.GURU)));
        } catch (SQLException e) {
            formErrorLabel.setText("Gagal memuat daftar guru untuk wali kelas.");
            System.err.println("[ManajemenDataKelasController] SQLException (findByRole): " + e.getMessage());
        }

        try {
            ObservableList<TahunAjaran> daftarTahun = FXCollections.observableArrayList(tahunAjaranDAO.findAll());
            comboTahunAjaran.setItems(daftarTahun);
            tahunAjaranDAO.findAktif().ifPresent(comboTahunAjaran.getSelectionModel()::select);
        } catch (SQLException e) {
            formErrorLabel.setText("Gagal memuat daftar tahun ajaran.");
            System.err.println("[ManajemenDataKelasController] SQLException (tahunAjaran): " + e.getMessage());
        }
    }

    private void mulaiModeTambah() {
        resetForm();
        fieldNamaKelas.requestFocus();
    }

    private void mulaiModeUbah() {
        Kelas terpilih = tabelKelas.getSelectionModel().getSelectedItem();
        if (terpilih == null) {
            return;
        }
        kelasSedangDiedit = terpilih;
        labelJudulForm.setText("Ubah Kelas");
        fieldNamaKelas.setText(terpilih.getNamaKelas());
        comboTingkat.getSelectionModel().select(terpilih.getTingkat());
        fieldJurusan.setText(terpilih.getJurusan());

        if (terpilih.getWaliKelasId() != null) {
            comboWaliKelas.getItems().stream()
                    .filter(p -> p.getPenggunaId() == terpilih.getWaliKelasId())
                    .findFirst()
                    .ifPresent(comboWaliKelas.getSelectionModel()::select);
        } else {
            comboWaliKelas.getSelectionModel().clearSelection();
        }

        comboTahunAjaran.getItems().stream()
                .filter(ta -> ta.getTahunAjaranId() == terpilih.getTahunAjaranId())
                .findFirst()
                .ifPresent(comboTahunAjaran.getSelectionModel()::select);

        formErrorLabel.setText("");
    }

    private void resetForm() {
        kelasSedangDiedit = null;
        labelJudulForm.setText("Tambah Kelas");
        fieldNamaKelas.clear();
        comboTingkat.getSelectionModel().clearSelection();
        fieldJurusan.clear();
        comboWaliKelas.getSelectionModel().clearSelection();
        tahunAjaranDAO_findAktifAman();
        formErrorLabel.setText("");
        tabelKelas.getSelectionModel().clearSelection();
    }

    /** Memilih ulang tahun ajaran aktif di combo box tanpa membiarkan exception mengganggu reset form. */
    private void tahunAjaranDAO_findAktifAman() {
        try {
            Optional<TahunAjaran> aktif = tahunAjaranDAO.findAktif();
            if (aktif.isPresent()) {
                comboTahunAjaran.getItems().stream()
                        .filter(ta -> ta.getTahunAjaranId() == aktif.get().getTahunAjaranId())
                        .findFirst()
                        .ifPresent(comboTahunAjaran.getSelectionModel()::select);
            } else {
                comboTahunAjaran.getSelectionModel().clearSelection();
            }
        } catch (SQLException e) {
            comboTahunAjaran.getSelectionModel().clearSelection();
        }
    }

    private void handleSimpan() {
        formErrorLabel.setText("");

        String namaKelas = fieldNamaKelas.getText() == null ? "" : fieldNamaKelas.getText().trim();
        String tingkat = comboTingkat.getSelectionModel().getSelectedItem();
        String jurusan = fieldJurusan.getText() == null ? null : fieldJurusan.getText().trim();
        Pengguna waliDipilih = comboWaliKelas.getSelectionModel().getSelectedItem();
        TahunAjaran tahunDipilih = comboTahunAjaran.getSelectionModel().getSelectedItem();

        if (namaKelas.isEmpty()) {
            formErrorLabel.setText("Nama kelas wajib diisi.");
            return;
        }
        if (tingkat == null) {
            formErrorLabel.setText("Tingkat wajib dipilih.");
            return;
        }
        if (tahunDipilih == null) {
            formErrorLabel.setText("Tahun ajaran wajib dipilih.");
            return;
        }

        try {
            Integer excludeId = kelasSedangDiedit != null ? kelasSedangDiedit.getKelasId() : null;
            if (kelasDAO.existsByNamaDanTahun(namaKelas, tahunDipilih.getTahunAjaranId(), excludeId)) {
                formErrorLabel.setText("Nama kelas ini sudah dipakai pada tahun ajaran yang sama.");
                return;
            }

            Kelas kelas = kelasSedangDiedit != null ? kelasSedangDiedit : new Kelas();
            kelas.setNamaKelas(namaKelas);
            kelas.setTingkat(tingkat);
            kelas.setJurusan(jurusan == null || jurusan.isEmpty() ? null : jurusan);
            kelas.setWaliKelasId(waliDipilih != null ? waliDipilih.getPenggunaId() : null);
            kelas.setTahunAjaranId(tahunDipilih.getTahunAjaranId());

            if (kelasSedangDiedit != null) {
                kelasDAO.update(kelas);
            } else {
                kelasDAO.insert(kelas);
            }

            resetForm();
            muatDaftarKelas();
        } catch (SQLException e) {
            formErrorLabel.setText("Gagal menyimpan data kelas. Coba lagi.");
            System.err.println("[ManajemenDataKelasController] SQLException (simpan): " + e.getMessage());
        }
    }

    private void handleHapus() {
        Kelas terpilih = tabelKelas.getSelectionModel().getSelectedItem();
        if (terpilih == null) {
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Hapus Kelas");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Hapus kelas \"" + terpilih.getNamaKelas() + "\"?\n"
                + "Tindakan ini tidak dapat dibatalkan.");

        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isPresent() && jawaban.get() == ButtonType.OK) {
            try {
                kelasDAO.delete(terpilih.getKelasId());
                if (kelasSedangDiedit != null && kelasSedangDiedit.getKelasId() == terpilih.getKelasId()) {
                    resetForm();
                }
                muatDaftarKelas();
                errorLabel.setText("");
            } catch (KelasMasihDipakaiException e) {
                errorLabel.setText(e.getMessage());
            } catch (SQLException e) {
                errorLabel.setText("Gagal menghapus kelas. Coba lagi.");
                System.err.println("[ManajemenDataKelasController] SQLException (hapus): " + e.getMessage());
            }
        }
    }

    // ================= Navigasi =================

    private void bukaLaporan() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/LaporanKehadiran.fxml",
                    Main.APP_TITLE + " - Laporan");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Laporan.");
            System.err.println("[ManajemenDataKelasController] IOException: " + e.getMessage());
        }
    }

    private void bukaCetakKartu() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenCetakKartu.fxml",
                    Main.APP_TITLE + " - Cetak Kartu Pelajar");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Cetak Kartu Pelajar.");
            System.err.println("[ManajemenDataKelasController] IOException: " + e.getMessage());
        }
    }

    private void bukaDataMurid() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenDataMurid.fxml",
                    Main.APP_TITLE + " - Data Murid");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Data Murid.");
            System.err.println("[ManajemenDataKelasController] IOException: " + e.getMessage());
        }
    }

    private void bukaMataPelajaran() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenMataPelajaran.fxml",
                    Main.APP_TITLE + " - Mata Pelajaran");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Mata Pelajaran.");
            System.err.println("[ManajemenDataKelasController] IOException: " + e.getMessage());
        }
    }

    private void bukaDashboard() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Dashboard.fxml", Main.APP_TITLE + " - Dashboard");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Dashboard.");
            System.err.println("[ManajemenDataKelasController] IOException: " + e.getMessage());
        }
    }

    private void bukaVerifikasiAkun() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/VerifikasiAkun.fxml",
                    Main.APP_TITLE + " - Verifikasi Akun");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Verifikasi Akun.");
            System.err.println("[ManajemenDataKelasController] IOException: " + e.getMessage());
        }
    }

    private void bukaScanQr() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ScanQr.fxml", Main.APP_TITLE + " - Scan QR");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Scan QR.");
            System.err.println("[ManajemenDataKelasController] IOException: " + e.getMessage());
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
            System.err.println("[ManajemenDataKelasController] Gagal membuka Profil Saya: " + e.getMessage());
        }
    }

    private void handleLogout() {
        SessionManager.logout();
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", Main.APP_TITLE + " - Masuk");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Login.");
            System.err.println("[ManajemenDataKelasController] IOException: " + e.getMessage());
        }
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