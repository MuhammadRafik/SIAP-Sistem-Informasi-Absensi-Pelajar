package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.AbsensiDAO;
import com.nurulislam.siap.dao.MuridDAO;
import com.nurulislam.siap.model.Absensi;
import com.nurulislam.siap.model.HariMengajar;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.StatusAbsensi;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import com.nurulislam.siap.util.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/** Dashboard Staf TU berbasis data database nyata (bukan data dummy). */
public class DashboardController {
    @FXML private Button btnNavDashboard, btnNavVerifikasi, btnNavScanQr, btnNavDataMurid,
            btnNavDataKelas, btnNavMataPelajaran, btnNavCetakKartu, btnNavLaporan;
    @FXML private Hyperlink linkLogout;
    @FXML private Label labelTanggal, labelNamaUser, labelRoleUser, labelInisialUser, labelHalo;
    @FXML private Label labelTotalHadir, labelIzinSakit, labelTerlambat, labelAlfa;
    @FXML private Label labelStatusDatabase;
    @FXML private ComboBox<String> comboRentangWaktu;
    @FXML private LineChart<String, Number> chartKehadiran;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;
    @FXML private Hyperlink linkLihatSemua;
    @FXML private VBox logAktivitasBox;
    @FXML private Button btnTambahMurid, btnExportLaporan, btnKonfigurasi, btnBacaSelengkapnya;

    private final AbsensiDAO absensiDAO = new AbsensiDAO();
    private final MuridDAO muridDAO = new MuridDAO();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("id", "ID"));

    @FXML
    public void initialize() {
        if (!pastikanAksesTU()) return;
        isiInfoPengguna();
        isiTanggalHariIni();
        perbaruiStatusDatabase();
        comboRentangWaktu.getItems().setAll("Senin - Minggu", "Minggu Lalu", "Bulan Ini");
        comboRentangWaktu.getSelectionModel().selectFirst();
        comboRentangWaktu.setOnAction(e -> isiGrafikKehadiran());
        refreshDashboard();

        btnNavDashboard.setOnAction(e -> { });
        btnNavVerifikasi.setOnAction(e -> bukaVerifikasiAkun());
        btnNavScanQr.setOnAction(e -> bukaScanQr());
        btnNavDataMurid.setOnAction(e -> bukaDataMurid());
        btnNavDataKelas.setOnAction(e -> bukaDataKelas());
        btnNavMataPelajaran.setOnAction(e -> bukaMataPelajaran());
        btnNavCetakKartu.setOnAction(e -> bukaCetakKartu());
        btnNavLaporan.setOnAction(e -> bukaLaporan());

        btnTambahMurid.setOnAction(e -> bukaDataMurid());
        btnExportLaporan.setOnAction(e -> bukaLaporan());
        btnKonfigurasi.setOnAction(e -> bukaMataPelajaran());
        btnBacaSelengkapnya.setOnAction(e -> bukaLaporan());
        linkLihatSemua.setOnAction(e -> bukaLaporan());
        linkLogout.setOnAction(e -> handleLogout());
    }

    private void refreshDashboard() {
        try {
            isiRekapHariIni();
            isiGrafikKehadiran();
            isiLogAktivitas();
        } catch (Exception e) {
            tampilkanError("Dashboard", "Data dashboard tidak dapat dimuat.", e);
        }
    }

    private void isiInfoPengguna() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();
        String nama = pengguna != null && pengguna.getNama() != null ? pengguna.getNama() : "Pengguna";
        String peran = pengguna != null && pengguna.getRole() != null ? pengguna.getRole().getLabel() : "";
        labelNamaUser.setText(nama);
        labelRoleUser.setText(peran);
        labelHalo.setText("Halo, " + namaDepan(nama) + "!");
        labelInisialUser.setText(inisial(nama));
    }

    private String namaDepan(String nama) { return (nama == null || nama.isBlank()) ? "Admin" : nama.trim().split("\\s+")[0]; }
    private String inisial(String nama) {
        if (nama == null || nama.isBlank()) return "?";
        String[] bagian = nama.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, bagian.length); i++) if (!bagian[i].isEmpty()) sb.append(Character.toUpperCase(bagian[i].charAt(0)));
        return sb.toString();
    }

    private void perbaruiStatusDatabase() {
        if (labelStatusDatabase == null) return;
        labelStatusDatabase.setText("Memeriksa database...");
        try {
            boolean connected = DatabaseConnection.isDatabaseAvailable();
            labelStatusDatabase.setText(connected ? "Database: Terhubung" : "Database: Terputus");
            labelStatusDatabase.getStyleClass().removeAll("db-connected", "db-disconnected", "db-checking");
            labelStatusDatabase.getStyleClass().add(connected ? "db-connected" : "db-disconnected");
        } catch (Exception e) {
            labelStatusDatabase.setText("Database: Terputus");
            labelStatusDatabase.getStyleClass().removeAll("db-connected", "db-disconnected", "db-checking");
            labelStatusDatabase.getStyleClass().add("db-disconnected");
        }
    }

    private void isiTanggalHariIni() {
        LocalDate hariIni = LocalDate.now();
        String namaHari = hariIni.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("id", "ID"));
        labelTanggal.setText(Character.toUpperCase(namaHari.charAt(0)) + namaHari.substring(1) + ", " + hariIni.format(dateFormatter));
    }

    private void isiRekapHariIni() throws Exception {
        Map<StatusAbsensi, Integer> rekap = absensiDAO.hitungRekapHariIni(LocalDate.now());
        labelTotalHadir.setText(formatAngka(rekap.getOrDefault(StatusAbsensi.HADIR, 0)));
        int izinSakit = rekap.getOrDefault(StatusAbsensi.IZIN, 0) + rekap.getOrDefault(StatusAbsensi.SAKIT, 0);
        labelIzinSakit.setText(formatAngka(izinSakit));
        labelTerlambat.setText(formatAngka(rekap.getOrDefault(StatusAbsensi.TERLAMBAT, 0)));
        labelAlfa.setText(formatAngka(rekap.getOrDefault(StatusAbsensi.ALFA, 0)));
    }

    private void isiGrafikKehadiran() {
        if (chartKehadiran == null) return;
        chartXAxis.setLabel(null);
        chartYAxis.setLabel("Siswa Hadir/Terlambat");
        try {
            LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
            LocalDate end = start.plusDays(6);
            String range = comboRentangWaktu.getValue();
            if ("Minggu Lalu".equals(range)) { start = start.minusWeeks(1); end = end.minusWeeks(1); }
            else if ("Bulan Ini".equals(range)) { start = LocalDate.now().withDayOfMonth(1); end = start.withDayOfMonth(start.lengthOfMonth()); }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            if ("Bulan Ini".equals(range)) {
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    Map<StatusAbsensi,Integer> r = absensiDAO.hitungRekapHariIni(d);
                    int hadir = r.getOrDefault(StatusAbsensi.HADIR,0) + r.getOrDefault(StatusAbsensi.TERLAMBAT,0);
                    series.getData().add(new XYChart.Data<>(String.valueOf(d.getDayOfMonth()), hadir));
                }
            } else {
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    Map<StatusAbsensi,Integer> r = absensiDAO.hitungRekapHariIni(d);
                    int hadir = r.getOrDefault(StatusAbsensi.HADIR,0) + r.getOrDefault(StatusAbsensi.TERLAMBAT,0);
                    String label = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("id","ID"));
                    label = label.substring(0, Math.min(3, label.length()));
                    series.getData().add(new XYChart.Data<>(label, hadir));
                }
            }
            chartKehadiran.getData().setAll(series);
        } catch (Exception e) {
            chartKehadiran.getData().clear();
            tampilkanError("Grafik Kehadiran", "Grafik tidak dapat dimuat.", e);
        }
    }

    private record LogAktivitas(String inisial, String nama, String kelas, String waktu, String status) {}

    private void isiLogAktivitas() throws Exception {
        List<Absensi> latest = absensiDAO.findRiwayatHariIni(LocalDate.now());
        logAktivitasBox.getChildren().clear();
        int limit = Math.min(8, latest.size());
        for (int i = 0; i < limit; i++) {
            Absensi a = latest.get(i);
            String nama = a.getNamaMurid() == null ? "-" : a.getNamaMurid();
            String status = a.getStatus() == null ? "-" : a.getStatus().name();
            LocalTime t = a.getWaktuMasuk();
            logAktivitasBox.getChildren().add(buatBarisLog(new LogAktivitas(inisial(nama), nama,
                    a.getNamaKelas() == null ? "-" : a.getNamaKelas(), t == null ? "-" : t.format(timeFormatter) + " WIB", status)));
        }
        if (limit == 0) {
            Label kosong = new Label("Belum ada absensi hari ini.");
            kosong.getStyleClass().add("log-waktu");
            logAktivitasBox.getChildren().add(kosong);
        }
    }

    private HBox buatBarisLog(LogAktivitas log) {
        StackPane avatar = new StackPane(); avatar.getStyleClass().add("log-avatar");
        Label inisialLabel = new Label(log.inisial()); inisialLabel.getStyleClass().add("log-avatar-text"); avatar.getChildren().add(inisialLabel);
        Label namaLabel = new Label(log.nama()); namaLabel.getStyleClass().add("log-nama");
        Label kelasLabel = new Label(log.kelas()); kelasLabel.getStyleClass().add("log-kelas");
        VBox namaBox = new VBox(namaLabel, kelasLabel); namaBox.setSpacing(1); HBox.setHgrow(namaBox, Priority.ALWAYS);
        Label waktuLabel = new Label(log.waktu()); waktuLabel.getStyleClass().add("log-waktu");
        Label statusLabel = new Label(log.status()); statusLabel.getStyleClass().addAll("status-badge", statusStyleClass(log.status()));
        HBox baris = new HBox(10, avatar, namaBox, waktuLabel, statusLabel); baris.setAlignment(javafx.geometry.Pos.CENTER_LEFT); return baris;
    }
    private String statusStyleClass(String status) {
        return switch (status) { case "HADIR" -> "status-badge-hadir"; case "TERLAMBAT" -> "status-badge-terlambat"; case "ALFA" -> "status-badge-alfa"; default -> "status-badge-izin"; };
    }
    private String formatAngka(int n) { return String.format(Locale.US, "%,d", n); }

    private void tampilkanError(String title, String message, Exception e) {
        System.err.println("[DashboardController] " + title + ": " + e.getMessage());
        Alert alert = new Alert(Alert.AlertType.WARNING); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }
    private void bukaVerifikasiAkun() { buka("/com/nurulislam/siap/fxml/VerifikasiAkun.fxml", "Verifikasi Akun"); }
    private void bukaScanQr() { buka("/com/nurulislam/siap/fxml/ScanQr.fxml", "Scan QR"); }
    private void bukaLaporan() { buka("/com/nurulislam/siap/fxml/LaporanKehadiran.fxml", "Laporan"); }
    private void bukaCetakKartu() { buka("/com/nurulislam/siap/fxml/ManajemenCetakKartu.fxml", "Cetak Kartu Pelajar"); }
    private void bukaDataMurid() { buka("/com/nurulislam/siap/fxml/ManajemenDataMurid.fxml", "Data Murid"); }
    private void bukaDataKelas() { buka("/com/nurulislam/siap/fxml/ManajemenDataKelas.fxml", "Data Kelas"); }
    private void bukaMataPelajaran() { buka("/com/nurulislam/siap/fxml/ManajemenMataPelajaran.fxml", "Mata Pelajaran"); }
    private void buka(String fxml, String nama) { try { SceneManager.switchTo(fxml, Main.APP_TITLE + " - " + nama); } catch (IOException e) { System.err.println("[DashboardController] Gagal membuka " + nama + ": " + e.getMessage()); } }
    private boolean pastikanAksesTU() {
        Pengguna sesi = SessionManager.getPenggunaAktif();
        if (sesi != null && sesi.getRole() == com.nurulislam.siap.model.Role.TU) return true;
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Akses Ditolak");
        alert.setHeaderText(null);
        alert.setContentText("Akun Guru tidak memiliki hak akses ke halaman ini.");
        alert.showAndWait();
        return false;
    }

    private void handleLogout() { SessionManager.logout(); buka("/com/nurulislam/siap/fxml/Login.fxml", "Masuk"); }
}
