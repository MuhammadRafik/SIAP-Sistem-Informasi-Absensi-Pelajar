package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.AbsensiMapelDAO;
import com.nurulislam.siap.dao.JadwalMengajarDAO;
import com.nurulislam.siap.model.AbsensiMapel;
import com.nurulislam.siap.model.JadwalMengajar;
import com.nurulislam.siap.model.MataPelajaran;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.StatusAbsensi;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller untuk halaman "Laporan" versi akun Guru: rekap absensi mata
 * pelajaran (tb_absensi_mapel) khusus untuk jadwal mengajar milik guru yang
 * sedang login. Berbeda dengan {@link LaporanKehadiranController} milik
 * Admin/Staf TU yang bisa melihat lintas guru, di sini semua query DAO
 * (lewat {@link AbsensiMapelDAO}) selalu disaring dengan guru_id pengguna
 * aktif, sehingga satu guru tidak pernah melihat data guru lain.
 * <p>
 * Filter Kelas dan Mata Pelajaran hanya diisi dari jadwal mengajar guru yang
 * bersangkutan (lewat {@link JadwalMengajarDAO#findAllByGuru(int)}), bukan
 * seluruh kelas/mapel di sekolah.
 * <p>
 * Tombol "Cetak / Ekspor PDF" memakai {@link PrinterJob} bawaan JavaFX, sama
 * seperti pola pada {@link LaporanKehadiranController} dan
 * {@link ManajemenCetakKartuController}.
 */
public class LaporanMapelController {

    private static final DateTimeFormatter FORMAT_TANGGAL = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter FORMAT_JAM = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** Jumlah baris data per halaman cetak (selain baris header tabel). */
    private static final int BARIS_PER_HALAMAN_CETAK = 20;

    // --- Sidebar ---
    @FXML private Button btnNavBeranda;
    @FXML private Button btnNavJadwal;
    @FXML private Button btnNavAbsensiMapel;
    @FXML private Button btnNavLaporan;
    @FXML private Hyperlink linkLogout;

    // --- Top bar ---
    @FXML private Label labelNamaUser;
    @FXML private Label labelRoleUser;
    @FXML private Label labelInisialUser;
    @FXML private Label errorLabel;

    // --- Filter ---
    @FXML private DatePicker datePickerMulai;
    @FXML private DatePicker datePickerAkhir;
    @FXML private ComboBox<KelasFilterItem> comboKelasFilter;
    @FXML private ComboBox<MataPelajaran> comboMapelFilter;
    @FXML private ComboBox<StatusAbsensi> comboStatusFilter;
    @FXML private Button btnTerapkanFilter;
    @FXML private Button btnEksporPdf;

    // --- Kartu ringkasan ---
    @FXML private Label labelTotalHadir;
    @FXML private Label labelIzinSakit;
    @FXML private Label labelTerlambat;
    @FXML private Label labelAlfa;

    // --- Tabel ---
    @FXML private Label labelJumlahData;
    @FXML private TextField fieldPencarian;
    @FXML private TableView<AbsensiMapel> tabelLaporan;
    @FXML private TableColumn<AbsensiMapel, String> kolomNamaSiswa;
    @FXML private TableColumn<AbsensiMapel, String> kolomNis;
    @FXML private TableColumn<AbsensiMapel, String> kolomKelas;
    @FXML private TableColumn<AbsensiMapel, String> kolomMapel;
    @FXML private TableColumn<AbsensiMapel, String> kolomTanggal;
    @FXML private TableColumn<AbsensiMapel, String> kolomStatus;
    @FXML private TableColumn<AbsensiMapel, String> kolomWaktuScan;

    private final AbsensiMapelDAO absensiMapelDAO = new AbsensiMapelDAO();
    private final JadwalMengajarDAO jadwalMengajarDAO = new JadwalMengajarDAO();

    /** Hasil query sesuai filter tanggal/kelas/mapel/status (sebelum pencarian nama/NIS diterapkan). */
    private final ObservableList<AbsensiMapel> hasilFilter = FXCollections.observableArrayList();
    /** Yang benar-benar ditampilkan di tabel, setelah pencarian nama/NIS diterapkan. */
    private final ObservableList<AbsensiMapel> dataTampil = FXCollections.observableArrayList();

    private int guruId;

    @FXML
    public void initialize() {
        errorLabel.setText("");

        Pengguna pengguna = SessionManager.getPenggunaAktif();
        guruId = (pengguna != null && pengguna.getPenggunaId() != null) ? pengguna.getPenggunaId() : 0;

        isiInfoPengguna();
        siapkanSidebar();
        siapkanFilter();
        siapkanTabel();

        fieldPencarian.textProperty().addListener((obs, lama, baru) -> terapkanPencarian());
        btnTerapkanFilter.setOnAction(e -> muatData());
        btnEksporPdf.setOnAction(e -> handleEksporPdf());
        linkLogout.setOnAction(e -> handleLogout());

        muatDaftarFilterJadwal();
        muatData();
    }

    // ================= Info pengguna & navigasi =================

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
        btnNavBeranda.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/DashboardGuru.fxml", "Dashboard"));
        btnNavJadwal.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/JadwalGuru.fxml", "Jadwal Mengajar"));
        btnNavAbsensiMapel.setOnAction(e -> navigasiKe("/com/nurulislam/siap/fxml/AbsensiMapel.fxml", "Absensi Mata Pelajaran"));
        btnNavLaporan.setOnAction(e -> { /* sudah di halaman Laporan */ });
    }

    private void navigasiKe(String fxml, String judul) {
        try {
            SceneManager.switchTo(fxml, Main.APP_TITLE + " - " + judul);
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman " + judul + ".");
            System.err.println("[LaporanMapelController] IOException: " + e.getMessage());
        }
    }

    private void handleLogout() {
        SessionManager.logout();
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", Main.APP_TITLE + " - Masuk");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Login.");
            System.err.println("[LaporanMapelController] IOException: " + e.getMessage());
        }
    }

    // ================= Filter =================

    private void siapkanFilter() {
        // Default: dari awal bulan berjalan sampai hari ini.
        LocalDate hariIni = LocalDate.now();
        datePickerMulai.setValue(hariIni.withDayOfMonth(1));
        datePickerAkhir.setValue(hariIni);

        comboKelasFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(KelasFilterItem k) {
                return k == null ? "Semua Kelas" : k.namaKelas();
            }

            @Override
            public KelasFilterItem fromString(String s) {
                return null;
            }
        });

        comboMapelFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(MataPelajaran m) {
                return m == null ? "Semua Mata Pelajaran" : m.getNamaMapel();
            }

            @Override
            public MataPelajaran fromString(String s) {
                return null;
            }
        });

        // Item null merepresentasikan "Semua Status" - StatusAbsensi tidak punya nilai sentinel sendiri.
        comboStatusFilter.setItems(FXCollections.observableArrayList(
                null, StatusAbsensi.HADIR, StatusAbsensi.TERLAMBAT,
                StatusAbsensi.IZIN, StatusAbsensi.SAKIT, StatusAbsensi.ALFA));
        comboStatusFilter.getSelectionModel().selectFirst(); // null = Semua Status
        comboStatusFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(StatusAbsensi s) {
                return s == null ? "Semua Status" : capitalisasi(s.name());
            }

            @Override
            public StatusAbsensi fromString(String s) {
                return null;
            }
        });
    }

    /**
     * Mengisi filter Kelas & Mata Pelajaran HANYA dari kelas/mapel yang benar-benar
     * diajar guru ini (lewat jadwal mengajarnya), bukan seluruh kelas/mapel sekolah -
     * supaya guru tidak bisa "mengintip" filter untuk kelas/mapel yang bukan miliknya.
     */
    private void muatDaftarFilterJadwal() {
        try {
            List<JadwalMengajar> jadwalGuru = jadwalMengajarDAO.findAllByGuru(guruId);

            // LinkedHashMap supaya urutan tampil konsisten & otomatis unik per kelas_id/mapel_id.
            Map<Integer, KelasFilterItem> kelasUnik = new LinkedHashMap<>();
            Map<Integer, MataPelajaran> mapelUnik = new LinkedHashMap<>();
            for (JadwalMengajar j : jadwalGuru) {
                kelasUnik.putIfAbsent(j.getKelasId(), new KelasFilterItem(j.getKelasId(), j.getNamaKelas()));
                MataPelajaran mp = new MataPelajaran();
                mp.setMapelId(j.getMapelId());
                mp.setNamaMapel(j.getNamaMapel());
                mp.setKodeMapel(j.getKodeMapel());
                mapelUnik.putIfAbsent(j.getMapelId(), mp);
            }

            ObservableList<KelasFilterItem> daftarKelas = FXCollections.observableArrayList();
            daftarKelas.add(null); // null = "Semua Kelas"
            daftarKelas.addAll(kelasUnik.values());
            comboKelasFilter.setItems(daftarKelas);
            comboKelasFilter.getSelectionModel().selectFirst();

            ObservableList<MataPelajaran> daftarMapel = FXCollections.observableArrayList();
            daftarMapel.add(null); // null = "Semua Mata Pelajaran"
            daftarMapel.addAll(mapelUnik.values());
            comboMapelFilter.setItems(daftarMapel);
            comboMapelFilter.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            errorLabel.setText("Gagal memuat daftar kelas/mata pelajaran untuk filter.");
            System.err.println("[LaporanMapelController] SQLException (filter jadwal): " + e.getMessage());
        }
    }

    /** Item ringan untuk ComboBox filter Kelas, dibangun dari JadwalMengajar (bukan query Kelas terpisah). */
    private record KelasFilterItem(int kelasId, String namaKelas) {
    }

    // ================= Tabel & data =================

    private void siapkanTabel() {
        kolomNamaSiswa.setCellValueFactory(new PropertyValueFactory<>("namaMurid"));
        kolomNis.setCellValueFactory(new PropertyValueFactory<>("nis"));
        kolomKelas.setCellValueFactory(new PropertyValueFactory<>("namaKelas"));
        kolomMapel.setCellValueFactory(new PropertyValueFactory<>("namaMapel"));
        kolomTanggal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTanggal() != null ? data.getValue().getTanggal().format(FORMAT_TANGGAL) : "-"));
        kolomWaktuScan.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getWaktuScan() != null ? data.getValue().getWaktuScan().format(FORMAT_JAM) : "-"));
        kolomStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                capitalisasi(data.getValue().getStatus().name())));

        kolomStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean kosong) {
                super.updateItem(status, kosong);
                if (kosong || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle(warnaStatus(status));
                }
            }
        });

        tabelLaporan.setItems(dataTampil);
        tabelLaporan.setPlaceholder(new Label("Tidak ada data absensi mata pelajaran untuk filter yang dipilih."));
    }

    private String warnaStatus(String status) {
        return switch (status) {
            case "Hadir" -> "-fx-text-fill: #006a62; -fx-font-weight: bold;";
            case "Terlambat" -> "-fx-text-fill: #a5580a; -fx-font-weight: bold;";
            case "Izin", "Sakit" -> "-fx-text-fill: #3d5afe; -fx-font-weight: bold;";
            case "Alfa" -> "-fx-text-fill: #ba1a1a; -fx-font-weight: bold;";
            default -> "";
        };
    }

    private void muatData() {
        LocalDate tglMulai = datePickerMulai.getValue();
        LocalDate tglAkhir = datePickerAkhir.getValue();

        if (tglMulai == null || tglAkhir == null) {
            errorLabel.setText("Tanggal mulai dan tanggal akhir wajib diisi.");
            return;
        }
        if (tglMulai.isAfter(tglAkhir)) {
            errorLabel.setText("Tanggal mulai tidak boleh setelah tanggal akhir.");
            return;
        }

        KelasFilterItem kelasDipilih = comboKelasFilter.getSelectionModel().getSelectedItem();
        MataPelajaran mapelDipilih = comboMapelFilter.getSelectionModel().getSelectedItem();
        StatusAbsensi statusDipilih = comboStatusFilter.getSelectionModel().getSelectedItem();
        Integer kelasId = kelasDipilih != null ? kelasDipilih.kelasId() : null;
        Integer mapelId = mapelDipilih != null ? mapelDipilih.getMapelId() : null;

        try {
            hasilFilter.setAll(absensiMapelDAO.findFiltered(
                    tglMulai, tglAkhir, kelasId, mapelId, guruId, statusDipilih, null));
            terapkanPencarian();

            Map<StatusAbsensi, Integer> rekap = absensiMapelDAO.hitungRekap(tglMulai, tglAkhir, kelasId, mapelId, guruId);
            labelTotalHadir.setText(String.valueOf(rekap.getOrDefault(StatusAbsensi.HADIR, 0)));
            labelIzinSakit.setText(String.valueOf(
                    rekap.getOrDefault(StatusAbsensi.IZIN, 0) + rekap.getOrDefault(StatusAbsensi.SAKIT, 0)));
            labelTerlambat.setText(String.valueOf(rekap.getOrDefault(StatusAbsensi.TERLAMBAT, 0)));
            labelAlfa.setText(String.valueOf(rekap.getOrDefault(StatusAbsensi.ALFA, 0)));

            errorLabel.setText("");
        } catch (SQLException e) {
            errorLabel.setText("Gagal memuat data laporan. Coba lagi.");
            System.err.println("[LaporanMapelController] SQLException (muat data): " + e.getMessage());
        }
    }

    private void terapkanPencarian() {
        String kataKunci = fieldPencarian.getText() == null ? "" : fieldPencarian.getText().trim().toLowerCase();
        if (kataKunci.isEmpty()) {
            dataTampil.setAll(hasilFilter);
        } else {
            dataTampil.setAll(hasilFilter.stream()
                    .filter(a -> (a.getNamaMurid() != null && a.getNamaMurid().toLowerCase().contains(kataKunci))
                            || (a.getNis() != null && a.getNis().toLowerCase().contains(kataKunci)))
                    .toList());
        }
        labelJumlahData.setText("Menampilkan " + dataTampil.size() + " dari " + hasilFilter.size() + " data");
    }

    // ================= Cetak / Ekspor PDF =================

    private void handleEksporPdf() {
        if (dataTampil.isEmpty()) {
            errorLabel.setText("Tidak ada data untuk dicetak. Ubah filter terlebih dahulu.");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            errorLabel.setText("Tidak ada printer yang terdeteksi di komputer ini.");
            return;
        }

        boolean lanjut = job.showPrintDialog(btnEksporPdf.getScene().getWindow());
        if (!lanjut) {
            return;
        }

        Printer printer = job.getPrinter();
        PageLayout layout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);

        List<AbsensiMapel> semuaData = dataTampil;
        int totalHalaman = (int) Math.ceil(semuaData.size() / (double) BARIS_PER_HALAMAN_CETAK);
        String ringkasanFilter = buatRingkasanFilter();

        try {
            for (int halaman = 0; halaman < totalHalaman; halaman++) {
                int awal = halaman * BARIS_PER_HALAMAN_CETAK;
                int akhir = Math.min(awal + BARIS_PER_HALAMAN_CETAK, semuaData.size());
                List<AbsensiMapel> potongan = semuaData.subList(awal, akhir);

                VBox halamanCetak = buatHalamanCetak(potongan, ringkasanFilter, halaman + 1, totalHalaman);
                // applyCss() + layout() WAJIB dipanggil manual: node ini dibangun baru di
                // memori (tidak terpasang ke Scene manapun), jadi ukurannya belum terhitung
                // sebelum frame render pertama. Tanpa baris ini, printPage bisa mencetak
                // halaman kosong/terpotong.
                halamanCetak.applyCss();
                halamanCetak.layout();
                job.printPage(layout, halamanCetak);
            }
            job.endJob();
            errorLabel.setText("");
        } catch (Exception e) {
            errorLabel.setText("Gagal mencetak laporan. Coba lagi.");
            System.err.println("[LaporanMapelController] Gagal mencetak: " + e.getMessage());
        }
    }

    private String buatRingkasanFilter() {
        StringBuilder sb = new StringBuilder();
        sb.append(datePickerMulai.getValue().format(FORMAT_TANGGAL))
                .append(" - ")
                .append(datePickerAkhir.getValue().format(FORMAT_TANGGAL));
        KelasFilterItem kelas = comboKelasFilter.getSelectionModel().getSelectedItem();
        sb.append(" | Kelas: ").append(kelas != null ? kelas.namaKelas() : "Semua Kelas");
        MataPelajaran mapel = comboMapelFilter.getSelectionModel().getSelectedItem();
        sb.append(" | Mata Pelajaran: ").append(mapel != null ? mapel.getNamaMapel() : "Semua Mata Pelajaran");
        StatusAbsensi status = comboStatusFilter.getSelectionModel().getSelectedItem();
        sb.append(" | Status: ").append(status != null ? capitalisasi(status.name()) : "Semua Status");
        return sb.toString();
    }

    /**
     * Membangun satu halaman cetak sebagai node VBox mandiri (bukan bagian dari
     * Scene aplikasi), sehingga memakai gaya inline (bukan style.css) agar tetap
     * tampil benar walau tidak terpasang ke stylesheet manapun.
     */
    private VBox buatHalamanCetak(List<AbsensiMapel> baris, String ringkasanFilter, int halamanKe, int totalHalaman) {
        VBox halaman = new VBox(10);
        halaman.setPadding(new Insets(24));
        halaman.setPrefWidth(555); // kira-kira lebar area cetak A4 potrait dikurangi margin, dalam poin

        Pengguna pengguna = SessionManager.getPenggunaAktif();
        String namaGuru = pengguna != null && pengguna.getNama() != null ? pengguna.getNama() : "Guru";

        Label judul = new Label("Laporan Kehadiran Mata Pelajaran - " + namaGuru);
        judul.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #191c1e;");

        Label subjudul = new Label(ringkasanFilter);
        subjudul.setStyle("-fx-font-size: 10px; -fx-text-fill: #4b5563;");
        subjudul.setWrapText(true);

        GridPane tabel = new GridPane();
        tabel.setHgap(6);
        tabel.setVgap(4);
        tabel.setPadding(new Insets(8, 0, 0, 0));

        String[] header = {"Nama Siswa", "NIS", "Kelas", "Mata Pelajaran", "Tanggal", "Status", "Waktu"};
        double[] lebarKolom = {140, 65, 55, 110, 75, 60, 55};
        for (int i = 0; i < header.length; i++) {
            Label labelHeader = new Label(header[i]);
            labelHeader.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #191c1e;");
            labelHeader.setPrefWidth(lebarKolom[i]);
            tabel.add(labelHeader, i, 0);
        }

        int barisKe = 1;
        for (AbsensiMapel a : baris) {
            String[] nilai = {
                    a.getNamaMurid() != null ? a.getNamaMurid() : "-",
                    a.getNis() != null ? a.getNis() : "-",
                    a.getNamaKelas() != null ? a.getNamaKelas() : "-",
                    a.getNamaMapel() != null ? a.getNamaMapel() : "-",
                    a.getTanggal() != null ? a.getTanggal().format(FORMAT_TANGGAL) : "-",
                    capitalisasi(a.getStatus().name()),
                    a.getWaktuScan() != null ? a.getWaktuScan().format(FORMAT_JAM) : "-"
            };
            for (int i = 0; i < nilai.length; i++) {
                Label sel = new Label(nilai[i]);
                sel.setStyle("-fx-font-size: 9px; -fx-text-fill: #191c1e;");
                sel.setPrefWidth(lebarKolom[i]);
                sel.setWrapText(true);
                tabel.add(sel, i, barisKe);
            }
            barisKe++;
        }

        Label footer = new Label("Halaman " + halamanKe + " dari " + totalHalaman
                + "  -  Dicetak melalui SIAP MA Nurul Islam");
        footer.setStyle("-fx-font-size: 8px; -fx-text-fill: #9ca3af;");
        VBox.setMargin(footer, new Insets(12, 0, 0, 0));

        VBox.setVgrow(tabel, Priority.ALWAYS);
        halaman.getChildren().addAll(judul, subjudul, tabel, footer);
        halaman.setAlignment(Pos.TOP_LEFT);
        return halaman;
    }

    private String capitalisasi(String teksEnum) {
        if (teksEnum == null || teksEnum.isEmpty()) {
            return "";
        }
        return teksEnum.charAt(0) + teksEnum.substring(1).toLowerCase();
    }
}