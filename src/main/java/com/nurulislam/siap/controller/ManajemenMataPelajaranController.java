package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.*;
import com.nurulislam.siap.model.*;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Controller untuk halaman "Manajemen Mata Pelajaran &amp; Jadwal Mengajar",
 * khusus diakses oleh Staf TU. Menggabungkan dua bagian sesuai desain
 * "manajemen_mata_pelajaran_jadwal_siap":
 * <ol>
 *   <li>Daftar Mata Pelajaran (CRUD kode + nama mata pelajaran)</li>
 *   <li>Jadwal Mengajar (penugasan mapel + guru + kelas + hari + jam, dengan
 *       deteksi bentrok otomatis) beserta pratinjau jadwal mingguan berbentuk grid</li>
 * </ol>
 * Kedua tabel memakai pola pilih-baris + tombol Ubah/Hapus di bawah tabel,
 * konsisten dengan {@link ManajemenDataKelasController} dan
 * {@link ManajemenDataMuridController}.
 */
public class ManajemenMataPelajaranController {

    private static final DateTimeFormatter FORMAT_JAM = DateTimeFormatter.ofPattern("HH:mm");
    private static final HariMengajar[] URUTAN_HARI = {
            HariMengajar.SENIN, HariMengajar.SELASA, HariMengajar.RABU,
            HariMengajar.KAMIS, HariMengajar.JUMAT, HariMengajar.SABTU
    };

    // --- Sidebar ---
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavVerifikasi;
    @FXML private Button btnNavScanQr;
    @FXML private Button btnNavDataMurid;
    @FXML private Button btnNavDataKelas;
    @FXML private Button btnNavMataPelajaran;
    @FXML private Button btnNavCetakKartu;
    @FXML private Button btnNavLaporan;
    @FXML private Hyperlink linkLogout;

    // --- Top bar ---
    @FXML private Label labelNamaUser;
    @FXML private Label labelRoleUser;
    @FXML private Label labelInisialUser;

    // --- Ringkasan ---
    @FXML private Label labelTotalMapel;
    @FXML private Label labelTotalJadwal;
    @FXML private Label labelJadwalBentrok;
    @FXML private Label errorLabel;

    // --- Daftar Mata Pelajaran ---
    @FXML private TextField fieldKodeMapelBaru;
    @FXML private TextField fieldNamaMapelBaru;
    @FXML private Button btnSimpanMapel;
    @FXML private Button btnBatalMapel;
    @FXML private Label mapelFormErrorLabel;
    @FXML private TableView<MataPelajaran> tabelMapel;
    @FXML private TableColumn<MataPelajaran, String> kolomKodeMapel;
    @FXML private TableColumn<MataPelajaran, String> kolomNamaMapel;
    @FXML private Button btnUbahMapel;
    @FXML private Button btnHapusMapel;

    // --- Jadwal Mengajar ---
    @FXML private TextField fieldPencarianJadwal;
    @FXML private Button btnRefreshJadwal;
    @FXML private Button btnTambahJadwal;
    @FXML private TableView<JadwalMengajar> tabelJadwal;
    @FXML private TableColumn<JadwalMengajar, String> kolomJadwalMapel;
    @FXML private TableColumn<JadwalMengajar, String> kolomJadwalGuru;
    @FXML private TableColumn<JadwalMengajar, String> kolomJadwalKelas;
    @FXML private TableColumn<JadwalMengajar, String> kolomJadwalHari;
    @FXML private TableColumn<JadwalMengajar, String> kolomJadwalJam;
    @FXML private TableColumn<JadwalMengajar, String> kolomJadwalStatus;
    @FXML private Button btnUbahJadwal;
    @FXML private Button btnHapusJadwal;

    // --- Pratinjau mingguan ---
    @FXML private VBox containerJadwalMingguan;

    // --- Form Jadwal (panel kanan) ---
    @FXML private Label labelJudulFormJadwal;
    @FXML private ComboBox<MataPelajaran> comboMapel;
    @FXML private ComboBox<Pengguna> comboGuru;
    @FXML private ComboBox<Kelas> comboKelasJadwal;
    @FXML private ComboBox<HariMengajar> comboHari;
    @FXML private TextField fieldJamMulai;
    @FXML private TextField fieldJamSelesai;
    @FXML private ComboBox<TahunAjaran> comboTahunAjaranJadwal;
    @FXML private Label jadwalFormErrorLabel;
    @FXML private Button btnSimpanJadwal;
    @FXML private Button btnBatalJadwal;

    private final MataPelajaranDAO mataPelajaranDAO = new MataPelajaranDAO();
    private final JadwalMengajarDAO jadwalMengajarDAO = new JadwalMengajarDAO();
    private final KelasDAO kelasDAO = new KelasDAO();
    private final PenggunaDAO penggunaDAO = new PenggunaDAO();
    private final TahunAjaranDAO tahunAjaranDAO = new TahunAjaranDAO();

    private final ObservableList<MataPelajaran> semuaMapel = FXCollections.observableArrayList();

    private final ObservableList<JadwalMengajar> semuaJadwal = FXCollections.observableArrayList();
    private final ObservableList<JadwalMengajar> jadwalTampil = FXCollections.observableArrayList();
    /** ID jadwal yang bentrok dengan jadwal lain (guru/kelas sama, waktu beririsan, hari sama). */
    private final Set<Integer> jadwalIdBentrok = new HashSet<>();

    /** Mata pelajaran yang sedang diedit; null berarti form Mata Pelajaran dalam mode "Tambah". */
    private MataPelajaran mapelSedangDiedit;
    /** Jadwal yang sedang diedit; null berarti form Jadwal dalam mode "Tambah". */
    private JadwalMengajar jadwalSedangDiedit;

    @FXML
    public void initialize() {
        if (!pastikanAksesTU()) return;
        errorLabel.setText("");
        mapelFormErrorLabel.setText("");
        jadwalFormErrorLabel.setText("");

        isiInfoPengguna();
        siapkanSidebar();
        siapkanTabelMapel();
        siapkanTabelJadwal();
        siapkanFormJadwal();

        fieldPencarianJadwal.textProperty().addListener((obs, lama, baru) -> terapkanFilterJadwal());

        btnUbahMapel.setDisable(true);
        btnHapusMapel.setDisable(true);
        tabelMapel.getSelectionModel().selectedItemProperty().addListener((obs, lama, baru) -> {
            btnUbahMapel.setDisable(baru == null);
            btnHapusMapel.setDisable(baru == null);
        });

        btnUbahJadwal.setDisable(true);
        btnHapusJadwal.setDisable(true);
        tabelJadwal.getSelectionModel().selectedItemProperty().addListener((obs, lama, baru) -> {
            btnUbahJadwal.setDisable(baru == null);
            btnHapusJadwal.setDisable(baru == null);
        });

        btnSimpanMapel.setOnAction(e -> handleSimpanMapel());
        btnBatalMapel.setOnAction(e -> resetFormMapel());
        btnUbahMapel.setOnAction(e -> mulaiModeUbahMapel());
        btnHapusMapel.setOnAction(e -> handleHapusMapel());

        btnRefreshJadwal.setOnAction(e -> muatSemuaData());
        btnTambahJadwal.setOnAction(e -> resetFormJadwal());
        btnUbahJadwal.setOnAction(e -> mulaiModeUbahJadwal());
        btnHapusJadwal.setOnAction(e -> handleHapusJadwal());
        btnSimpanJadwal.setOnAction(e -> handleSimpanJadwal());
        btnBatalJadwal.setOnAction(e -> resetFormJadwal());

        linkLogout.setOnAction(e -> handleLogout());

        muatDaftarPendukungForm();
        muatSemuaData();
        resetFormMapel();
        resetFormJadwal();
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
        btnNavDashboard.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/Dashboard.fxml", "Dashboard"));
        btnNavVerifikasi.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/VerifikasiAkun.fxml", "Verifikasi Akun"));
        btnNavScanQr.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/ScanQr.fxml", "Scan QR"));
        btnNavDataMurid.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/ManajemenDataMurid.fxml", "Data Murid"));
        btnNavDataKelas.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/ManajemenDataKelas.fxml", "Data Kelas"));
        btnNavMataPelajaran.setOnAction(e -> { /* sudah di halaman Mata Pelajaran */ });
        btnNavCetakKartu.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/ManajemenCetakKartu.fxml", "Cetak Kartu Pelajar"));
        btnNavLaporan.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/LaporanKehadiran.fxml", "Laporan"));
    }

    private void navigasiKe(String fxml, String judul) {
        try {
            SceneManager.switchTo(fxml, Main.APP_TITLE + " - " + judul);
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman " + judul + ".");
            System.err.println("[ManajemenMataPelajaranController] IOException: " + e.getMessage());
        }
    }

    // ================= Muat semua data =================

    private void muatSemuaData() {
        try {
            semuaMapel.setAll(mataPelajaranDAO.findAll());
            semuaJadwal.setAll(jadwalMengajarDAO.findAllDetail());
            hitungBentrok();
            terapkanFilterJadwal();
            perbaruiRingkasan();
            bangunGridJadwalMingguan();
            errorLabel.setText("");
        } catch (SQLException e) {
            errorLabel.setText("Gagal memuat data mata pelajaran/jadwal. Coba lagi.");
            System.err.println("[ManajemenMataPelajaranController] SQLException: " + e.getMessage());
        }
    }

    /** Menghitung ulang jadwalIdBentrok dari data yang sudah dimuat (tanpa query tambahan ke DB). */
    private void hitungBentrok() {
        jadwalIdBentrok.clear();
        List<JadwalMengajar> daftar = semuaJadwal;
        for (int i = 0; i < daftar.size(); i++) {
            JadwalMengajar a = daftar.get(i);
            for (int j = i + 1; j < daftar.size(); j++) {
                JadwalMengajar b = daftar.get(j);
                if (a.getHari() != b.getHari()) {
                    continue;
                }
                boolean entitasSama = a.getGuruId() == b.getGuruId() || a.getKelasId() == b.getKelasId();
                boolean waktuBeririsan = a.getJamMulai().isBefore(b.getJamSelesai())
                        && b.getJamMulai().isBefore(a.getJamSelesai());
                if (entitasSama && waktuBeririsan) {
                    jadwalIdBentrok.add(a.getJadwalId());
                    jadwalIdBentrok.add(b.getJadwalId());
                }
            }
        }
    }

    private void perbaruiRingkasan() {
        labelTotalMapel.setText(String.valueOf(semuaMapel.size()));
        labelTotalJadwal.setText(String.valueOf(semuaJadwal.size()));
        labelJadwalBentrok.setText(String.valueOf(jadwalIdBentrok.size()));
    }

    // ================= Tabel Mata Pelajaran =================

    private void siapkanTabelMapel() {
        kolomKodeMapel.setCellValueFactory(new PropertyValueFactory<>("kodeMapel"));
        kolomNamaMapel.setCellValueFactory(new PropertyValueFactory<>("namaMapel"));
        tabelMapel.setItems(semuaMapel);
        tabelMapel.setPlaceholder(new Label("Belum ada mata pelajaran."));
    }

    private void mulaiModeUbahMapel() {
        MataPelajaran terpilih = tabelMapel.getSelectionModel().getSelectedItem();
        if (terpilih == null) {
            return;
        }
        mapelSedangDiedit = terpilih;
        fieldKodeMapelBaru.setText(terpilih.getKodeMapel());
        fieldNamaMapelBaru.setText(terpilih.getNamaMapel());
        btnSimpanMapel.setText("Ubah");
        mapelFormErrorLabel.setText("");
    }

    private void resetFormMapel() {
        mapelSedangDiedit = null;
        fieldKodeMapelBaru.clear();
        fieldNamaMapelBaru.clear();
        btnSimpanMapel.setText("Simpan");
        mapelFormErrorLabel.setText("");
        tabelMapel.getSelectionModel().clearSelection();
    }

    private void handleSimpanMapel() {
        mapelFormErrorLabel.setText("");
        String kode = fieldKodeMapelBaru.getText() == null ? "" : fieldKodeMapelBaru.getText().trim().toUpperCase();
        String nama = fieldNamaMapelBaru.getText() == null ? "" : fieldNamaMapelBaru.getText().trim();

        if (kode.isEmpty()) {
            mapelFormErrorLabel.setText("Kode mata pelajaran wajib diisi.");
            return;
        }
        if (nama.isEmpty()) {
            mapelFormErrorLabel.setText("Nama mata pelajaran wajib diisi.");
            return;
        }

        try {
            Integer excludeId = mapelSedangDiedit != null ? mapelSedangDiedit.getMapelId() : null;
            if (mataPelajaranDAO.existsByKode(kode, excludeId)) {
                mapelFormErrorLabel.setText("Kode mata pelajaran ini sudah dipakai.");
                return;
            }

            MataPelajaran mapel = mapelSedangDiedit != null ? mapelSedangDiedit : new MataPelajaran();
            mapel.setKodeMapel(kode);
            mapel.setNamaMapel(nama);

            if (mapelSedangDiedit != null) {
                mataPelajaranDAO.update(mapel);
            } else {
                mataPelajaranDAO.insert(mapel);
            }

            resetFormMapel();
            muatSemuaData();
            muatDaftarPendukungForm(); // refresh combo Mata Pelajaran di form Jadwal
        } catch (SQLException e) {
            mapelFormErrorLabel.setText("Gagal menyimpan mata pelajaran. Coba lagi.");
            System.err.println("[ManajemenMataPelajaranController] SQLException (simpan mapel): " + e.getMessage());
        }
    }

    private void handleHapusMapel() {
        MataPelajaran terpilih = tabelMapel.getSelectionModel().getSelectedItem();
        if (terpilih == null) {
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Hapus Mata Pelajaran");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Hapus mata pelajaran \"" + terpilih.getNamaMapel() + "\"?\n"
                + "Tindakan ini tidak dapat dibatalkan.");

        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isPresent() && jawaban.get() == ButtonType.OK) {
            try {
                mataPelajaranDAO.delete(terpilih.getMapelId());
                if (mapelSedangDiedit != null && mapelSedangDiedit.getMapelId() == terpilih.getMapelId()) {
                    resetFormMapel();
                }
                muatSemuaData();
                muatDaftarPendukungForm();
                errorLabel.setText("");
            } catch (MataPelajaranMasihDipakaiException e) {
                errorLabel.setText(e.getMessage());
            } catch (SQLException e) {
                errorLabel.setText("Gagal menghapus mata pelajaran. Coba lagi.");
                System.err.println("[ManajemenMataPelajaranController] SQLException (hapus mapel): " + e.getMessage());
            }
        }
    }

    // ================= Tabel Jadwal Mengajar =================

    private void siapkanTabelJadwal() {
        kolomJadwalMapel.setCellValueFactory(new PropertyValueFactory<>("namaMapel"));
        kolomJadwalGuru.setCellValueFactory(new PropertyValueFactory<>("namaGuru"));
        kolomJadwalKelas.setCellValueFactory(new PropertyValueFactory<>("namaKelas"));
        kolomJadwalHari.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                capitalisasi(data.getValue().getHari().name())));
        kolomJadwalJam.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getJamMulai().format(FORMAT_JAM) + " - "
                        + data.getValue().getJamSelesai().format(FORMAT_JAM)));
        kolomJadwalStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                jadwalIdBentrok.contains(data.getValue().getJadwalId()) ? "Bentrok" : "Normal"));

        kolomJadwalStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean kosong) {
                super.updateItem(status, kosong);
                if (kosong || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle("Bentrok".equals(status)
                            ? "-fx-text-fill: #ba1a1a; -fx-font-weight: bold;"
                            : "-fx-text-fill: #006a62; -fx-font-weight: bold;");
                }
            }
        });

        tabelJadwal.setItems(jadwalTampil);
        tabelJadwal.setPlaceholder(new Label("Belum ada jadwal mengajar."));
    }

    private void terapkanFilterJadwal() {
        String kataKunci = fieldPencarianJadwal.getText() == null
                ? "" : fieldPencarianJadwal.getText().trim().toLowerCase();
        if (kataKunci.isEmpty()) {
            jadwalTampil.setAll(semuaJadwal);
        } else {
            jadwalTampil.setAll(semuaJadwal.stream()
                    .filter(j -> j.getNamaMapel().toLowerCase().contains(kataKunci)
                            || j.getNamaGuru().toLowerCase().contains(kataKunci)
                            || j.getNamaKelas().toLowerCase().contains(kataKunci))
                    .toList());
        }
        tabelJadwal.refresh();
    }

    // ================= Form Jadwal =================

    private void siapkanFormJadwal() {
        comboMapel.setConverter(new StringConverter<>() {
            @Override
            public String toString(MataPelajaran m) {
                return m == null ? "" : m.toString();
            }

            @Override
            public MataPelajaran fromString(String s) {
                return null;
            }
        });

        comboGuru.setConverter(new StringConverter<>() {
            @Override
            public String toString(Pengguna p) {
                return p == null ? "" : p.getNama();
            }

            @Override
            public Pengguna fromString(String s) {
                return null;
            }
        });

        comboKelasJadwal.setConverter(new StringConverter<>() {
            @Override
            public String toString(Kelas k) {
                return k == null ? "" : k.getNamaKelas();
            }

            @Override
            public Kelas fromString(String s) {
                return null;
            }
        });

        comboHari.setItems(FXCollections.observableArrayList(URUTAN_HARI));
        comboHari.setConverter(new StringConverter<>() {
            @Override
            public String toString(HariMengajar h) {
                return h == null ? "" : capitalisasi(h.name());
            }

            @Override
            public HariMengajar fromString(String s) {
                return null;
            }
        });

        comboTahunAjaranJadwal.setConverter(new StringConverter<>() {
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

    private void muatDaftarPendukungForm() {
        try {
            comboMapel.setItems(FXCollections.observableArrayList(mataPelajaranDAO.findAll()));
        } catch (SQLException e) {
            jadwalFormErrorLabel.setText("Gagal memuat daftar mata pelajaran.");
            System.err.println("[ManajemenMataPelajaranController] SQLException (mapel combo): " + e.getMessage());
        }
        try {
            comboGuru.setItems(FXCollections.observableArrayList(penggunaDAO.findByRole(Role.GURU)));
        } catch (SQLException e) {
            jadwalFormErrorLabel.setText("Gagal memuat daftar guru.");
            System.err.println("[ManajemenMataPelajaranController] SQLException (guru combo): " + e.getMessage());
        }
        try {
            comboKelasJadwal.setItems(FXCollections.observableArrayList(kelasDAO.findAll()));
        } catch (SQLException e) {
            jadwalFormErrorLabel.setText("Gagal memuat daftar kelas.");
            System.err.println("[ManajemenMataPelajaranController] SQLException (kelas combo): " + e.getMessage());
        }
        try {
            ObservableList<TahunAjaran> daftarTahun = FXCollections.observableArrayList(tahunAjaranDAO.findAll());
            comboTahunAjaranJadwal.setItems(daftarTahun);
            tahunAjaranDAO.findAktif().ifPresent(comboTahunAjaranJadwal.getSelectionModel()::select);
        } catch (SQLException e) {
            jadwalFormErrorLabel.setText("Gagal memuat daftar tahun ajaran.");
            System.err.println("[ManajemenMataPelajaranController] SQLException (tahun ajaran combo): " + e.getMessage());
        }
    }

    private void mulaiModeUbahJadwal() {
        JadwalMengajar terpilih = tabelJadwal.getSelectionModel().getSelectedItem();
        if (terpilih == null) {
            return;
        }
        jadwalSedangDiedit = terpilih;
        labelJudulFormJadwal.setText("Ubah Jadwal");

        comboMapel.getItems().stream().filter(m -> m.getMapelId() == terpilih.getMapelId()).findFirst()
                .ifPresent(comboMapel.getSelectionModel()::select);
        comboGuru.getItems().stream().filter(g -> g.getPenggunaId() == terpilih.getGuruId()).findFirst()
                .ifPresent(comboGuru.getSelectionModel()::select);
        comboKelasJadwal.getItems().stream().filter(k -> k.getKelasId() == terpilih.getKelasId()).findFirst()
                .ifPresent(comboKelasJadwal.getSelectionModel()::select);
        comboHari.getSelectionModel().select(terpilih.getHari());
        fieldJamMulai.setText(terpilih.getJamMulai().format(FORMAT_JAM));
        fieldJamSelesai.setText(terpilih.getJamSelesai().format(FORMAT_JAM));
        comboTahunAjaranJadwal.getItems().stream()
                .filter(ta -> ta.getTahunAjaranId() == terpilih.getTahunAjaranId()).findFirst()
                .ifPresent(comboTahunAjaranJadwal.getSelectionModel()::select);

        jadwalFormErrorLabel.setText("");
    }

    private void resetFormJadwal() {
        jadwalSedangDiedit = null;
        labelJudulFormJadwal.setText("Tambah Jadwal");
        comboMapel.getSelectionModel().clearSelection();
        comboGuru.getSelectionModel().clearSelection();
        comboKelasJadwal.getSelectionModel().clearSelection();
        comboHari.getSelectionModel().clearSelection();
        fieldJamMulai.clear();
        fieldJamSelesai.clear();
        pilihTahunAjaranAktifAman();
        jadwalFormErrorLabel.setText("");
        tabelJadwal.getSelectionModel().clearSelection();
    }

    private void pilihTahunAjaranAktifAman() {
        try {
            Optional<TahunAjaran> aktif = tahunAjaranDAO.findAktif();
            if (aktif.isPresent()) {
                comboTahunAjaranJadwal.getItems().stream()
                        .filter(ta -> ta.getTahunAjaranId() == aktif.get().getTahunAjaranId())
                        .findFirst()
                        .ifPresent(comboTahunAjaranJadwal.getSelectionModel()::select);
            } else {
                comboTahunAjaranJadwal.getSelectionModel().clearSelection();
            }
        } catch (SQLException e) {
            comboTahunAjaranJadwal.getSelectionModel().clearSelection();
        }
    }

    private void handleSimpanJadwal() {
        jadwalFormErrorLabel.setText("");

        MataPelajaran mapelDipilih = comboMapel.getSelectionModel().getSelectedItem();
        Pengguna guruDipilih = comboGuru.getSelectionModel().getSelectedItem();
        Kelas kelasDipilih = comboKelasJadwal.getSelectionModel().getSelectedItem();
        HariMengajar hariDipilih = comboHari.getSelectionModel().getSelectedItem();
        TahunAjaran tahunDipilih = comboTahunAjaranJadwal.getSelectionModel().getSelectedItem();
        String teksJamMulai = fieldJamMulai.getText() == null ? "" : fieldJamMulai.getText().trim();
        String teksJamSelesai = fieldJamSelesai.getText() == null ? "" : fieldJamSelesai.getText().trim();

        if (mapelDipilih == null) {
            jadwalFormErrorLabel.setText("Mata pelajaran wajib dipilih.");
            return;
        }
        if (guruDipilih == null) {
            jadwalFormErrorLabel.setText("Guru pengampu wajib dipilih.");
            return;
        }
        if (kelasDipilih == null) {
            jadwalFormErrorLabel.setText("Kelas wajib dipilih.");
            return;
        }
        if (hariDipilih == null) {
            jadwalFormErrorLabel.setText("Hari wajib dipilih.");
            return;
        }
        if (tahunDipilih == null) {
            jadwalFormErrorLabel.setText("Tahun ajaran wajib dipilih.");
            return;
        }

        LocalTime jamMulai;
        LocalTime jamSelesai;
        try {
            jamMulai = parseJam(teksJamMulai);
            jamSelesai = parseJam(teksJamSelesai);
        } catch (DateTimeParseException e) {
            jadwalFormErrorLabel.setText("Format jam tidak valid: \"" + e.getParsedString() + "\". "
                    + "Gunakan format jam:menit, contoh 07:30 (titik dua, bukan titik).");
            return;
        }
        if (!jamMulai.isBefore(jamSelesai)) {
            jadwalFormErrorLabel.setText("Jam mulai harus lebih awal dari jam selesai.");
            return;
        }

        try {
            Integer excludeId = jadwalSedangDiedit != null ? jadwalSedangDiedit.getJadwalId() : null;
            boolean bentrok = jadwalMengajarDAO.adaBentrok(guruDipilih.getPenggunaId(), kelasDipilih.getKelasId(),
                    hariDipilih, jamMulai, jamSelesai, excludeId);
            if (bentrok) {
                jadwalFormErrorLabel.setText("Jadwal bentrok: guru atau kelas ini sudah punya jadwal lain "
                        + "yang beririsan waktu pada hari " + capitalisasi(hariDipilih.name()) + ".");
                return;
            }

            JadwalMengajar jadwal = jadwalSedangDiedit != null ? jadwalSedangDiedit : new JadwalMengajar();
            jadwal.setMapelId(mapelDipilih.getMapelId());
            jadwal.setGuruId(guruDipilih.getPenggunaId());
            jadwal.setKelasId(kelasDipilih.getKelasId());
            jadwal.setHari(hariDipilih);
            jadwal.setJamMulai(jamMulai);
            jadwal.setJamSelesai(jamSelesai);
            jadwal.setTahunAjaranId(tahunDipilih.getTahunAjaranId());

            if (jadwalSedangDiedit != null) {
                jadwalMengajarDAO.update(jadwal);
            } else {
                jadwalMengajarDAO.insert(jadwal);
            }

            resetFormJadwal();
            muatSemuaData();
        } catch (SQLException e) {
            jadwalFormErrorLabel.setText("Gagal menyimpan jadwal. Coba lagi.");
            System.err.println("[ManajemenMataPelajaranController] SQLException (simpan jadwal): " + e.getMessage());
        }
    }

    private void handleHapusJadwal() {
        JadwalMengajar terpilih = tabelJadwal.getSelectionModel().getSelectedItem();
        if (terpilih == null) {
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Hapus Jadwal");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Hapus jadwal \"" + terpilih.getNamaMapel() + " - " + terpilih.getNamaKelas() + "\"?\n"
                + "Tindakan ini tidak dapat dibatalkan.");

        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isPresent() && jawaban.get() == ButtonType.OK) {
            try {
                jadwalMengajarDAO.delete(terpilih.getJadwalId());
                if (jadwalSedangDiedit != null && jadwalSedangDiedit.getJadwalId() == terpilih.getJadwalId()) {
                    resetFormJadwal();
                }
                muatSemuaData();
                errorLabel.setText("");
            } catch (JadwalMasihDipakaiException e) {
                errorLabel.setText(e.getMessage());
            } catch (SQLException e) {
                errorLabel.setText("Gagal menghapus jadwal. Coba lagi.");
                System.err.println("[ManajemenMataPelajaranController] SQLException (hapus jadwal): " + e.getMessage());
            }
        }
    }

    // ================= Pratinjau Jadwal Mingguan (grid) =================

    /**
     * Membangun ulang grid pratinjau jadwal mingguan (Waktu x Hari) dari
     * {@code semuaJadwal}, dengan sel berwarna merah muda untuk jadwal yang
     * bentrok (lihat {@code jadwalIdBentrok}). Grid dibangun dari kode karena
     * jumlah baris (slot waktu) bergantung sepenuhnya pada data jadwal yang ada.
     */
    private void bangunGridJadwalMingguan() {
        containerJadwalMingguan.getChildren().clear();

        if (semuaJadwal.isEmpty()) {
            Label kosong = new Label("Belum ada jadwal untuk ditampilkan.");
            kosong.setStyle("-fx-padding: 20; -fx-text-fill: #6b7280;");
            containerJadwalMingguan.getChildren().add(kosong);
            return;
        }

        // Kumpulkan slot waktu unik (jamMulai-jamSelesai) dari seluruh jadwal, urut ascending.
        TreeSet<LocalTime> jamMulaiUnik = new TreeSet<>();
        for (JadwalMengajar j : semuaJadwal) {
            jamMulaiUnik.add(j.getJamMulai());
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(4));

        // Header baris pertama: "Waktu" + nama hari.
        grid.add(buatHeaderSel("Waktu"), 0, 0);
        for (int kolom = 0; kolom < URUTAN_HARI.length; kolom++) {
            grid.add(buatHeaderSel(capitalisasi(URUTAN_HARI[kolom].name())), kolom + 1, 0);
        }

        int baris = 1;
        for (LocalTime jamMulaiSlot : jamMulaiUnik) {
            // Cari jamSelesai yang representatif untuk slot ini (dari jadwal manapun yang punya jamMulai ini).
            LocalTime jamSelesaiSlot = semuaJadwal.stream()
                    .filter(j -> j.getJamMulai().equals(jamMulaiSlot))
                    .map(JadwalMengajar::getJamSelesai)
                    .findFirst().orElse(jamMulaiSlot.plusHours(1));

            VBox labelWaktu = new VBox(2);
            labelWaktu.setAlignment(Pos.CENTER_LEFT);
            Label labelSlot = new Label(jamMulaiSlot.format(FORMAT_JAM) + " - " + jamSelesaiSlot.format(FORMAT_JAM));
            labelSlot.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            labelWaktu.getChildren().add(labelSlot);
            grid.add(labelWaktu, 0, baris);

            for (int kolom = 0; kolom < URUTAN_HARI.length; kolom++) {
                HariMengajar hariKolom = URUTAN_HARI[kolom];
                List<JadwalMengajar> jadwalDiSel = semuaJadwal.stream()
                        .filter(j -> j.getHari() == hariKolom && j.getJamMulai().equals(jamMulaiSlot))
                        .toList();

                VBox sel = new VBox(4);
                sel.setPadding(new Insets(8));
                sel.setPrefWidth(150);
                if (jadwalDiSel.isEmpty()) {
                    sel.setStyle("-fx-background-color: transparent;");
                } else {
                    boolean adaBentrokDiSel = jadwalDiSel.stream()
                            .anyMatch(j -> jadwalIdBentrok.contains(j.getJadwalId()));
                    sel.getStyleClass().add(adaBentrokDiSel ? "jadwal-cell-bentrok" : "jadwal-cell-normal");
                    for (JadwalMengajar j : jadwalDiSel) {
                        Label labelMapel = new Label(j.getNamaMapel());
                        labelMapel.setWrapText(true);
                        labelMapel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                        Label labelKelas = new Label(j.getNamaKelas());
                        labelKelas.setStyle("-fx-font-size: 11px; -fx-text-fill: #4b5563;");
                        sel.getChildren().addAll(labelMapel, labelKelas);
                    }
                }
                grid.add(sel, kolom + 1, baris);
            }
            baris++;
        }

        for (int kolom = 0; kolom <= URUTAN_HARI.length; kolom++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            if (kolom == 0) {
                cc.setMinWidth(110);
            } else {
                cc.setPrefWidth(150);
                cc.setHgrow(Priority.SOMETIMES);
            }
            grid.getColumnConstraints().add(cc);
        }

        containerJadwalMingguan.getChildren().add(grid);
    }

    private Label buatHeaderSel(String teks) {
        Label label = new Label(teks);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #191c1e; "
                + "-fx-padding: 4 0 8 0;");
        return label;
    }

    /**
     * Mem-parse teks jam ke LocalTime dengan format "HH:mm", tapi juga menerima
     * "HH.mm" (kebiasaan umum orang Indonesia menulis jam pakai titik, seperti
     * pada jam digital) dengan menormalisasi titik menjadi titik dua dulu.
     */
    private LocalTime parseJam(String teks) throws DateTimeParseException {
        String dinormalisasi = teks == null ? "" : teks.trim().replace('.', ':');
        return LocalTime.parse(dinormalisasi, FORMAT_JAM);
    }

    // ================= Util =================

    private String capitalisasi(String teksEnum) {
        if (teksEnum == null || teksEnum.isEmpty()) {
            return "";
        }
        return teksEnum.charAt(0) + teksEnum.substring(1).toLowerCase();
    }

    private void tampilkanBelumTersedia(String namaFitur) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(namaFitur);
        alert.setHeaderText(null);
        alert.setContentText("Fitur \"" + namaFitur + "\" akan dibuat pada tahap pengembangan berikutnya.");
        alert.showAndWait();
    }

    private void handleLogout() {
        SessionManager.logout();
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", Main.APP_TITLE + " - Masuk");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Login.");
            System.err.println("[ManajemenMataPelajaranController] IOException: " + e.getMessage());
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