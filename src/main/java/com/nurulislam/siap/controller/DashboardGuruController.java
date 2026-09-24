package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.AbsensiMapelDAO;
import com.nurulislam.siap.dao.JadwalMengajarDAO;
import com.nurulislam.siap.dao.MuridDAO;
import com.nurulislam.siap.model.AbsensiMapel;
import com.nurulislam.siap.model.HariMengajar;
import com.nurulislam.siap.model.JadwalMengajar;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.StatusAbsensi;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.DatabaseConnection;
import com.nurulislam.siap.util.SessionManager;
import com.nurulislam.siap.util.ProfileMenu;
import com.nurulislam.siap.util.AvatarUtil;
import com.nurulislam.siap.util.BrandLogo;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/** Dashboard Guru berbasis jadwal dan absensi mata pelajaran dari database. */
public class DashboardGuruController {

    // FIX BUG 39: cegah statistik lama tetap tampil saat database terputus.

    // FIX BUG 38: pemantauan status database secara berkala.
    private Timeline timelineStatusDatabase;
    private volatile boolean databaseSebelumnyaTersedia = false;
    private volatile boolean sedangMemuatUlangData = false;
    @FXML private Button btnNavBeranda, btnNavJadwal, btnNavAbsensiMapel, btnNavLaporan;
    @FXML private StackPane avatarBox;
    @FXML private ImageView imgAvatar;
    @FXML private ImageView imgLogo;
    @FXML private Label labelTanggal, labelNamaUser, labelRoleUser, labelInisialUser, labelHalo;
    @FXML private Label labelJumlahKelas, labelJumlahSiswa, labelSesiBerjalan, labelSesiSelesai;
    @FXML private Label labelStatusDatabase;
    @FXML private VBox jadwalBox, riwayatBox;
    @FXML private Button btnMulaiScan, btnLihatLaporan, btnPengaturanAkun;

    private final JadwalMengajarDAO jadwalDAO = new JadwalMengajarDAO();
    private final AbsensiMapelDAO absensiMapelDAO = new AbsensiMapelDAO();
    private final MuridDAO muridDAO = new MuridDAO();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("id", "ID"));

    @FXML public void initialize() {
        isiInfoPengguna();
        isiTanggalHariIni();

        boolean databaseAwal = DatabaseConnection.isDatabaseAvailable();
        databaseSebelumnyaTersedia = databaseAwal;
        perbaruiStatusDatabase();

        // Dashboard tetap dapat dibuka saat database terputus.
        // Data yang bergantung pada database hanya dimuat jika koneksi tersedia.
        if (DatabaseConnection.isDatabaseAvailable()) {
            try {
                isiJadwalMengajar();
            } catch (Exception e) {
                System.err.println("[DashboardGuruController] Jadwal gagal dimuat: " + e.getMessage());
                tampilkanDataTidakTersedia();
            }

            try {
                isiRiwayatAbsensi();
            } catch (Exception e) {
                System.err.println("[DashboardGuruController] Riwayat gagal dimuat: " + e.getMessage());
                if (riwayatBox != null) {
                    riwayatBox.getChildren().clear();
                    Label l = new Label("Riwayat tidak dapat dimuat karena koneksi database bermasalah.");
                    l.getStyleClass().add("log-waktu");
                    riwayatBox.getChildren().add(l);
                }
            }
        } else {
            tampilkanDataTidakTersedia();
        }

        btnNavBeranda.setOnAction(e -> buka("/com/nurulislam/siap/fxml/DashboardGuru.fxml", "Beranda"));
        btnNavJadwal.setOnAction(e -> bukaJadwalMengajar());
        btnNavAbsensiMapel.setOnAction(e -> bukaAbsensiMapel());
        btnNavLaporan.setOnAction(e -> bukaLaporan());
        btnMulaiScan.setOnAction(e -> bukaAbsensiMapel());
        btnLihatLaporan.setOnAction(e -> bukaLaporan());
        btnPengaturanAkun.setOnAction(e -> buka("/com/nurulislam/siap/fxml/PengaturanAkun.fxml", "Profil Saya"));
        ProfileMenu.pasang(avatarBox, this::bukaProfil, this::handleLogout);
        AvatarUtil.tampilkan(avatarBox, imgAvatar, labelInisialUser);
        BrandLogo.pasang(imgLogo);
    
        mulaiPemantauanDatabase();}

    private void isiInfoPengguna() {
        Pengguna u = SessionManager.getPenggunaAktif();
        String nama = u != null && u.getNama() != null ? u.getNama() : "Guru";
        String role = u != null && u.getRole() != null ? u.getRole().getLabel() : "";
        labelNamaUser.setText(nama); labelRoleUser.setText(role); labelHalo.setText("Halo, " + namaDepan(nama) + "!"); labelInisialUser.setText(inisial(nama));
    }
    private String namaDepan(String n) { return n == null || n.isBlank() ? "Guru" : n.trim().split("\\s+")[0]; }
    private String inisial(String n) { if (n == null || n.isBlank()) return "?"; String[] a=n.trim().split("\\s+"); StringBuilder s=new StringBuilder(); for(int i=0;i<Math.min(2,a.length);i++) s.append(Character.toUpperCase(a[i].charAt(0))); return s.toString(); }
    private void isiTanggalHariIni() { LocalDate d=LocalDate.now(); String h=d.getDayOfWeek().getDisplayName(TextStyle.FULL,new Locale("id","ID")); labelTanggal.setText(Character.toUpperCase(h.charAt(0))+h.substring(1)+", "+d.format(dateFormatter)); }

    private record JadwalTampil(JadwalMengajar data, String status) {}
    private void isiJadwalMengajar() throws Exception {
        Pengguna u = SessionManager.getPenggunaAktif();
        if (u == null || u.getPenggunaId() == null) { throw new IllegalStateException("Sesi guru tidak valid."); }
        HariMengajar hari = HariMengajar.dariDayOfWeek(LocalDate.now().getDayOfWeek());
        List<JadwalMengajar> jadwal = hari == null ? List.of() : jadwalDAO.findHariIniByGuru(u.getPenggunaId(), hari);
        Set<Integer> kelas = new HashSet<>(); int totalSiswa=0; int berjalan=0; int selesai=0; LocalTime now=LocalTime.now();
        jadwalBox.getChildren().clear();
        Set<Integer> kelasSudahDihitung = new HashSet<>();
        for (JadwalMengajar j: jadwal) {
            kelas.add(j.getKelasId());
            if (kelasSudahDihitung.add(j.getKelasId())) totalSiswa += (int) muridDAO.findByKelas(j.getKelasId()).stream().filter(m -> m.getStatus() != null && m.getStatus().name().equals("AKTIF")).count();
            String status=switch(j.statusPada(now)) { case BELUM_MULAI -> "BELUM MULAI"; case AKTIF -> "BERJALAN"; case SELESAI -> "SELESAI"; };
            if ("BERJALAN".equals(status)) berjalan++; if ("SELESAI".equals(status)) selesai++;
            jadwalBox.getChildren().add(buatBarisJadwal(new JadwalTampil(j,status)));
        }
        labelJumlahKelas.setText(String.valueOf(kelas.size())); labelJumlahSiswa.setText(String.valueOf(totalSiswa)); labelSesiBerjalan.setText(String.valueOf(berjalan)); labelSesiSelesai.setText(String.valueOf(selesai));
        if (jadwal.isEmpty()) { Label l=new Label("Tidak ada jadwal mengajar hari ini."); l.getStyleClass().add("log-waktu"); jadwalBox.getChildren().add(l); }
    }
    private HBox buatBarisJadwal(JadwalTampil jt) {
        JadwalMengajar j=jt.data(); Label mapel=new Label(j.getNamaMapel()==null?"-":j.getNamaMapel()); mapel.getStyleClass().add("log-nama");
        Label kelasJam=new Label((j.getNamaKelas()==null?"-":j.getNamaKelas())+" • "+j.getJamMulai().format(timeFormatter)+" - "+j.getJamSelesai().format(timeFormatter)); kelasJam.getStyleClass().add("log-kelas");
        VBox info=new VBox(mapel,kelasJam); info.setSpacing(1); HBox.setHgrow(info,Priority.ALWAYS);
        Label status=new Label(jt.status()); status.getStyleClass().addAll("status-badge",statusJadwalStyleClass(jt.status())); HBox row=new HBox(10,info,status); row.setAlignment(Pos.CENTER_LEFT); return row;
    }
    private String statusJadwalStyleClass(String s){ return switch(s){case "SELESAI"->"status-badge-hadir";case "BERJALAN"->"status-badge-terlambat";default->"status-badge-izin";}; }

    private record RiwayatAbsensi(JadwalMengajar jadwal, String ringkasan) {}
    private void isiRiwayatAbsensi() throws Exception {
        Pengguna u=SessionManager.getPenggunaAktif(); if(u==null||u.getPenggunaId()==null) return;
        HariMengajar hari=HariMengajar.dariDayOfWeek(LocalDate.now().getDayOfWeek());
        List<JadwalMengajar> jadwal=hari==null?List.of():jadwalDAO.findHariIniByGuru(u.getPenggunaId(),hari);
        riwayatBox.getChildren().clear(); int count=0;
        for(int i=jadwal.size()-1;i>=0 && count<5;i--){
            JadwalMengajar j=jadwal.get(i); Map<StatusAbsensi,Integer> r=absensiMapelDAO.hitungRekap(j.getJadwalId(),LocalDate.now());
            int h=r.getOrDefault(StatusAbsensi.HADIR,0), t=r.getOrDefault(StatusAbsensi.TERLAMBAT,0), iz=r.getOrDefault(StatusAbsensi.IZIN,0), s=r.getOrDefault(StatusAbsensi.SAKIT,0), a=r.getOrDefault(StatusAbsensi.ALFA,0);
            int total=h+t+iz+s+a; if(total==0) continue;
            String ringkasan=String.format("%d Hadir, %d Terlambat, %d Izin, %d Sakit, %d Alfa",h,t,iz,s,a);
            riwayatBox.getChildren().add(buatBarisRiwayat(new RiwayatAbsensi(j,ringkasan))); count++;
        }
        if(count==0){Label l=new Label("Belum ada riwayat absensi hari ini.");l.getStyleClass().add("log-waktu");riwayatBox.getChildren().add(l);}
    }
    private VBox buatBarisRiwayat(RiwayatAbsensi r){ JadwalMengajar j=r.jadwal(); Label judul=new Label((j.getNamaMapel()==null?"-":j.getNamaMapel())+" - "+(j.getNamaKelas()==null?"-":j.getNamaKelas())); judul.getStyleClass().add("log-nama"); Label tanggal=new Label("Hari ini, "+j.getJamMulai().format(timeFormatter)); tanggal.getStyleClass().add("log-kelas"); Label ring=new Label(r.ringkasan()); ring.getStyleClass().add("log-waktu"); return new VBox(2,judul,tanggal,ring); }
    /**
     * Mengosongkan data dinamis yang gagal dimuat agar dashboard tetap usable.
     */
    private void tampilkanDataTidakTersedia() {
        if (labelJumlahKelas != null) labelJumlahKelas.setText("-");
        if (labelJumlahSiswa != null) labelJumlahSiswa.setText("-");
        if (labelSesiBerjalan != null) labelSesiBerjalan.setText("-");
        if (labelSesiSelesai != null) labelSesiSelesai.setText("-");

        if (jadwalBox != null) {
            jadwalBox.getChildren().clear();
            Label l = new Label("Data jadwal tidak tersedia. Periksa koneksi database.");
            l.getStyleClass().add("log-waktu");
            jadwalBox.getChildren().add(l);
        }

        if (riwayatBox != null) {
            riwayatBox.getChildren().clear();
            Label l = new Label("Data riwayat tidak tersedia. Periksa koneksi database.");
            l.getStyleClass().add("log-waktu");
            riwayatBox.getChildren().add(l);
        }

        if (labelStatusDatabase != null) {
            labelStatusDatabase.setText("Database: Terputus");
        }
    }

    private void tampilkanBelumTersedia(String n){Alert a=new Alert(Alert.AlertType.INFORMATION);a.setTitle(n);a.setHeaderText(null);a.setContentText("Fitur \""+n+"\" belum tersedia.");a.showAndWait();}
    private void tampilkanError(String t,String m,Exception e){System.err.println("[DashboardGuruController] "+e.getMessage());Alert a=new Alert(Alert.AlertType.WARNING);a.setTitle(t);a.setHeaderText(null);a.setContentText(m);a.showAndWait();}
    private void bukaJadwalMengajar() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();

        if (pengguna == null || pengguna.getRole() != com.nurulislam.siap.model.Role.GURU) {
            tampilkanBelumTersedia("Jadwal Mengajar");
            return;
        }

        buka("/com/nurulislam/siap/fxml/JadwalGuru.fxml", "Jadwal Mengajar");
    }

    private void bukaAbsensiMapel(){buka("/com/nurulislam/siap/fxml/AbsensiMapel.fxml","Absensi Mata Pelajaran");}
    private void bukaLaporan(){buka("/com/nurulislam/siap/fxml/LaporanMapel.fxml","Laporan");}
    private void buka(String fxml, String title) {
        try {
            SceneManager.switchTo(fxml, Main.APP_TITLE + " - " + title);
        } catch (IOException e) {
            // Sebelumnya error hanya ditulis ke console sehingga dari sisi pengguna
            // tombol terlihat seperti tidak berfungsi dan dashboard tetap tampil.
            // Sekarang tampilkan penyebabnya agar sumber masalah halaman target
            // dapat langsung diketahui.
            String detail = e.getMessage() == null ? e.toString() : e.getMessage();
            System.err.println("[DashboardGuruController] Gagal membuka " + title + ": " + detail);
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Gagal Membuka Halaman");
            alert.setHeaderText("Halaman " + title + " tidak dapat dibuka");
            alert.setContentText(detail);
            alert.showAndWait();
        }
    }
    /**
     * Menampilkan kondisi koneksi database yang sebenarnya pada dashboard.
     * Tidak menghentikan dashboard jika database sedang tidak tersedia.
     */
    private void perbaruiStatusDatabase() {
        tampilkanStatusDatabase(DatabaseConnection.isDatabaseAvailable());
    }

    /**
     * Memantau koneksi database setiap 10 detik.
     * Pengecekan dijalankan di background thread agar UI JavaFX tidak macet.
     */
    private void mulaiPemantauanDatabase() {
        if (timelineStatusDatabase != null) {
            timelineStatusDatabase.stop();
        }

        timelineStatusDatabase = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> cekDanPerbaruiStatusDatabase())
        );
        timelineStatusDatabase.setCycleCount(Animation.INDEFINITE);
        timelineStatusDatabase.play();
    }

    private void cekDanPerbaruiStatusDatabase() {
        Thread checker = new Thread(() -> {
            boolean tersedia = DatabaseConnection.isDatabaseAvailable();
            boolean baruTersedia = tersedia && !databaseSebelumnyaTersedia;
            boolean baruTerputus = !tersedia && databaseSebelumnyaTersedia;

            databaseSebelumnyaTersedia = tersedia;

            Platform.runLater(() -> {
                tampilkanStatusDatabase(tersedia);

                if (baruTerputus) {
                    // Jangan biarkan angka lama terlihat seolah-olah masih berasal
                    // dari database yang sedang terputus.
                    tampilkanDataTidakTersedia();
                } else if (baruTersedia) {
                    // Saat MySQL kembali, muat ulang statistik dan daftar.
                    muatUlangDataDashboard();
                }
            });
        }, "dashboard-guru-db-check");

        checker.setDaemon(true);
        checker.start();
    }

    private void tampilkanStatusDatabase(boolean tersedia) {
        if (labelStatusDatabase == null) {
            return;
        }

        if (tersedia) {
            labelStatusDatabase.setText("Database: Terhubung");
            labelStatusDatabase.getStyleClass().removeAll(
                    "system-status-error", "system-status-warning"
            );
            if (!labelStatusDatabase.getStyleClass().contains("system-status-ok")) {
                labelStatusDatabase.getStyleClass().add("system-status-ok");
            }
        } else {
            labelStatusDatabase.setText("Database: Terputus");
            labelStatusDatabase.getStyleClass().removeAll(
                    "system-status-ok", "system-status-warning"
            );
            if (!labelStatusDatabase.getStyleClass().contains("system-status-error")) {
                labelStatusDatabase.getStyleClass().add("system-status-error");
            }
        }
    }

    private void muatUlangDataDashboard() {
        if (sedangMemuatUlangData || !DatabaseConnection.isDatabaseAvailable()) {
            return;
        }

        sedangMemuatUlangData = true;

        Thread reload = new Thread(() -> {
            try {
                isiJadwalMengajar();
                isiRiwayatAbsensi();
            } catch (Exception e) {
                System.err.println(
                        "[DashboardGuruController] Gagal memuat ulang data setelah database pulih: "
                                + e.getMessage()
                );
                Platform.runLater(this::tampilkanDataTidakTersedia);
            } finally {
                sedangMemuatUlangData = false;
            }
        }, "dashboard-guru-data-reload");

        reload.setDaemon(true);
        reload.start();
    }

    /**
     * Dipanggil bila controller/dashboard akan dibuang.
     */
    public void dispose() {
        if (timelineStatusDatabase != null) {
            timelineStatusDatabase.stop();
            timelineStatusDatabase = null;
        }
    }

    /** Membuka halaman Profil Saya (diakses dari menu avatar kanan atas). */
    private void bukaProfil() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/PengaturanAkun.fxml",
                    Main.APP_TITLE + " - Profil Saya");
        } catch (IOException e) {
            System.err.println("[DashboardGuruController] Gagal membuka Profil Saya: " + e.getMessage());
        }
    }

    private void handleLogout(){SessionManager.logout();buka("/com/nurulislam/siap/fxml/Login.fxml","Masuk");}
}
