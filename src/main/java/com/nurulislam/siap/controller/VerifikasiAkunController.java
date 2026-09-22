package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.PenggunaDAO;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.Role;
import com.nurulislam.siap.model.StatusAkun;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import com.nurulislam.siap.util.ProfileMenu;
import com.nurulislam.siap.util.AvatarUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.image.ImageView;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller untuk halaman "Verifikasi Akun", khusus diakses oleh Staf TU.
 * Tiga tab status mengatur daftar yang ditampilkan:
 *   - Menunggu Verifikasi -> Verifikasi (jadi AKTIF) atau Tolak (hapus pendaftaran)
 *   - Terverifikasi       -> Nonaktifkan akun AKTIF yang sudah tidak dipakai
 *   - Nonaktif            -> Aktifkan Kembali akun NONAKTIF
 * <p>
 * Tersedia filter peran (Semua / Guru Mata Pelajaran / Staf TU) supaya TU bisa
 * fokus memverifikasi satu jenis akun saja, misalnya khusus akun Guru yang
 * baru mendaftar.
 * <p>
 * Halaman ini sekarang memakai shell sidebar yang sama dengan Dashboard
 * (lihat Dashboard.fxml / DashboardController) supaya navigasi konsisten.
 */
public class VerifikasiAkunController {

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

    @FXML private Label labelNamaUser;
    @FXML private Label labelRoleUser;
    @FXML private Label labelInisialUser;

    @FXML private ToggleButton tabStatusMenunggu;
    @FXML private ToggleButton tabStatusTerverifikasi;
    @FXML private ToggleButton tabStatusNonaktif;

    @FXML private ToggleButton tabSemua;
    @FXML private ToggleButton tabGuru;
    @FXML private ToggleButton tabTU;
    @FXML private Label labelJumlah;

    @FXML private TableView<Pengguna> tabelAkun;
    @FXML private TableColumn<Pengguna, String> kolomNama;
    @FXML private TableColumn<Pengguna, String> kolomEmail;
    @FXML private TableColumn<Pengguna, String> kolomRole;
    @FXML private TableColumn<Pengguna, String> kolomStatus;
    @FXML private TableColumn<Pengguna, String> kolomTanggalDaftar;
    @FXML private TableColumn<Pengguna, Void> kolomAksi;
    @FXML private Label errorLabel;
    @FXML private Button btnRefresh;

    private static final DateTimeFormatter FORMAT_TANGGAL = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final PenggunaDAO penggunaDAO = new PenggunaDAO();

    /** Menyimpan seluruh akun pada tab status aktif (belum difilter peran). */
    private final ObservableList<Pengguna> semuaAkun = FXCollections.observableArrayList();
    /** Daftar yang benar-benar ditampilkan di tabel, hasil filter peran aktif. */
    private final ObservableList<Pengguna> daftarTampil = FXCollections.observableArrayList();

    private ToggleGroup grupFilter;
    private Role filterRoleAktif = null; // null = tampilkan semua peran

    private ToggleGroup grupStatus;
    private StatusAkun filterStatusAktif = StatusAkun.MENUNGGU_VERIFIKASI;

    @FXML
    public void initialize() {
        if (!pastikanAksesTU()) return;
        errorLabel.setText("");
        isiInfoPengguna();
        siapkanSidebar();
        siapkanFilterStatus();
        siapkanFilterPeran();
        siapkanTabel();

        btnRefresh.setOnAction(e -> muatDaftarAkun());
        ProfileMenu.pasang(avatarBox, this::bukaProfil, this::handleLogout);
        AvatarUtil.tampilkan(avatarBox, imgAvatar, labelInisialUser);

        muatDaftarAkun();
    }

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
        btnNavVerifikasi.setOnAction(e -> { /* sudah di halaman Verifikasi Akun */ });
        btnNavScanQr.setOnAction(e -> bukaScanQr());
        btnNavDataMurid.setOnAction(e -> bukaDataMurid());
        btnNavDataKelas.setOnAction(e -> bukaDataKelas());
        btnNavMataPelajaran.setOnAction(e -> bukaMataPelajaran());
        btnNavCetakKartu.setOnAction(e -> bukaCetakKartu());
        btnNavLaporan.setOnAction(e -> bukaLaporan());
    }

    private void siapkanFilterStatus() {
        grupStatus = new ToggleGroup();
        tabStatusMenunggu.setToggleGroup(grupStatus);
        tabStatusTerverifikasi.setToggleGroup(grupStatus);
        tabStatusNonaktif.setToggleGroup(grupStatus);
        tabStatusMenunggu.setSelected(true);

        grupStatus.selectedToggleProperty().addListener((obs, lama, baru) -> {
            // Minimal satu tab harus tetap terpilih (tidak boleh semua ter-unselect).
            if (baru == null) {
                grupStatus.selectToggle(lama != null ? lama : tabStatusMenunggu);
                return;
            }

            perbaruiGayaTabStatus();

            if (baru == tabStatusTerverifikasi) {
                filterStatusAktif = StatusAkun.AKTIF;
            } else if (baru == tabStatusNonaktif) {
                filterStatusAktif = StatusAkun.NONAKTIF;
            } else {
                filterStatusAktif = StatusAkun.MENUNGGU_VERIFIKASI;
            }
            tabelAkun.getSelectionModel().clearSelection();
            muatDaftarAkun();
        });

        perbaruiGayaTabStatus();
    }

    private void perbaruiGayaTabStatus() {
        for (ToggleButton tab : List.of(tabStatusMenunggu, tabStatusTerverifikasi, tabStatusNonaktif)) {
            tab.getStyleClass().remove("filter-tab-active");
            if (tab.isSelected()) {
                tab.getStyleClass().add("filter-tab-active");
            }
        }
    }

    private void siapkanFilterPeran() {
        grupFilter = new ToggleGroup();
        tabSemua.setToggleGroup(grupFilter);
        tabGuru.setToggleGroup(grupFilter);
        tabTU.setToggleGroup(grupFilter);
        tabSemua.setSelected(true);

        grupFilter.selectedToggleProperty().addListener((obs, lama, baru) -> {
            // Minimal satu tab harus tetap terpilih (tidak boleh semua ter-unselect).
            if (baru == null) {
                grupFilter.selectToggle(lama != null ? lama : tabSemua);
                return;
            }

            perbaruiGayaTab();

            if (baru == tabGuru) {
                filterRoleAktif = Role.GURU;
            } else if (baru == tabTU) {
                filterRoleAktif = Role.TU;
            } else {
                filterRoleAktif = null;
            }
            terapkanFilter();
        });

        perbaruiGayaTab();
    }

    private void perbaruiGayaTab() {
        for (ToggleButton tab : List.of(tabSemua, tabGuru, tabTU)) {
            tab.getStyleClass().remove("filter-tab-active");
            if (tab.isSelected()) {
                tab.getStyleClass().add("filter-tab-active");
            }
        }
    }

    private void siapkanTabel() {
        kolomNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        kolomEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        kolomRole.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole().getLabel()));
        kolomStatus.setCellValueFactory(data ->
                new SimpleStringProperty(labelStatus(data.getValue().getStatusAkun())));
        kolomTanggalDaftar.setCellValueFactory(data -> {
            var createdAt = data.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt == null ? "-" : createdAt.format(FORMAT_TANGGAL));
        });

        kolomStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("status-badge");
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String teks, boolean empty) {
                super.updateItem(teks, empty);
                if (empty || teks == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(teks);
                badge.getStyleClass().removeAll(
                        "status-badge-hadir", "status-badge-alfa", "status-badge-izin");
                if ("Aktif".equals(teks)) {
                    badge.getStyleClass().add("status-badge-hadir");
                } else if ("Nonaktif".equals(teks)) {
                    badge.getStyleClass().add("status-badge-alfa");
                } else {
                    badge.getStyleClass().add("status-badge-izin");
                }
                setGraphic(badge);
            }
        });

        kolomAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnA = new Button();
            private final Button btnB = new Button();
            private final HBox box = new HBox(6, btnA, btnB);

            {
                box.setAlignment(Pos.CENTER);
                btnA.getStyleClass().add("tabel-btn");
                btnB.getStyleClass().add("tabel-btn");
            }

            private Pengguna barisIni() {
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) {
                    return null;
                }
                return getTableView().getItems().get(idx);
            }

            private void gaya(Button tombol, String teks, boolean primer, Runnable aksi) {
                tombol.setText(teks);
                tombol.getStyleClass().removeAll("tabel-btn-primer", "tabel-btn-danger");
                tombol.getStyleClass().add(primer ? "tabel-btn-primer" : "tabel-btn-danger");
                tombol.setOnAction(e -> aksi.run());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Pengguna row = barisIni();
                if (row == null) {
                    setGraphic(null);
                    return;
                }
                box.getChildren().clear();
                if (filterStatusAktif == StatusAkun.MENUNGGU_VERIFIKASI) {
                    final Pengguna target = row;
                    gaya(btnA, "Verifikasi", true, () -> verifikasiAkun(target));
                    gaya(btnB, "Tolak", false, () -> tolakAkun(target));
                    box.getChildren().addAll(btnA, btnB);
                } else if (filterStatusAktif == StatusAkun.AKTIF) {
                    final Pengguna target = row;
                    gaya(btnA, "Nonaktifkan", false, () -> nonaktifkanAkun(target));
                    box.getChildren().add(btnA);
                } else {
                    final Pengguna target = row;
                    gaya(btnA, "Aktifkan", true, () -> aktifkanAkun(target));
                    box.getChildren().add(btnA);
                }
                setGraphic(box);
            }
        });

        // Kolom mengisi penuh lebar tabel sehingga tidak ada kolom hantu di kanan.
        tabelAkun.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelAkun.setItems(daftarTampil);
        tabelAkun.setPlaceholder(new Label("Tidak ada akun yang menunggu verifikasi."));
    }

    private String labelStatus(StatusAkun status) {
        if (status == StatusAkun.AKTIF) {
            return "Aktif";
        }
        if (status == StatusAkun.NONAKTIF) {
            return "Nonaktif";
        }
        return "Menunggu";
    }

    private void muatDaftarAkun() {
        try {
            List<Pengguna> hasil = penggunaDAO.findByStatus(filterStatusAktif);
            semuaAkun.setAll(hasil);
            terapkanFilter();
            errorLabel.setText("");
        } catch (SQLException e) {
            errorLabel.setText("Gagal memuat daftar akun. Coba lagi.");
            System.err.println("[VerifikasiAkunController] SQLException: " + e.getMessage());
        }
    }

    private void terapkanFilter() {
        if (filterRoleAktif == null) {
            daftarTampil.setAll(semuaAkun);
        } else {
            daftarTampil.setAll(semuaAkun.stream()
                    .filter(p -> p.getRole() == filterRoleAktif)
                    .toList());
        }
        String satuan;
        String statusLabel;
        if (filterStatusAktif == StatusAkun.AKTIF) {
            satuan = "akun terverifikasi";
            statusLabel = "Tidak ada akun terverifikasi pada filter ini.";
        } else if (filterStatusAktif == StatusAkun.NONAKTIF) {
            satuan = "akun nonaktif";
            statusLabel = "Tidak ada akun nonaktif pada filter ini.";
        } else {
            satuan = "akun menunggu";
            statusLabel = "Tidak ada akun yang menunggu verifikasi.";
        }
        labelJumlah.setText(daftarTampil.size() + " " + satuan);
        tabelAkun.setPlaceholder(new Label(statusLabel));
    }

    private void verifikasiAkun(Pengguna terpilih) {
        if (terpilih == null) {
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Verifikasi Akun");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Verifikasi akun \"" + terpilih.getNama() + "\" (" + terpilih.getEmail()
                + ") sebagai " + terpilih.getRole().getLabel() + "?\n"
                + "Setelah diverifikasi, akun ini bisa dipakai untuk masuk ke aplikasi.");

        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isPresent() && jawaban.get() == ButtonType.OK) {
            ubahStatus(terpilih, StatusAkun.AKTIF);
        }
    }

    private void tolakAkun(Pengguna terpilih) {
        if (terpilih == null) {
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Tolak Pendaftaran");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Tolak dan hapus pendaftaran akun \"" + terpilih.getNama()
                + "\" (" + terpilih.getEmail() + ")?\n"
                + "Tindakan ini tidak dapat dibatalkan.");

        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isPresent() && jawaban.get() == ButtonType.OK) {
            try {
                penggunaDAO.delete(terpilih.getPenggunaId());
                semuaAkun.remove(terpilih);
                terapkanFilter();
                errorLabel.setText("");
            } catch (SQLException e) {
                errorLabel.setText("Gagal menolak pendaftaran. Coba lagi.");
                System.err.println("[VerifikasiAkunController] SQLException: " + e.getMessage());
            }
        }
    }

    private void nonaktifkanAkun(Pengguna terpilih) {
        if (terpilih == null) {
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Nonaktifkan Akun");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Nonaktifkan akun \"" + terpilih.getNama()
                + "\" (" + terpilih.getEmail() + ")?\n"
                + "Akun yang dinonaktifkan tidak bisa dipakai untuk masuk ke aplikasi.");

        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isPresent() && jawaban.get() == ButtonType.OK) {
            ubahStatus(terpilih, StatusAkun.NONAKTIF);
        }
    }

    private void aktifkanAkun(Pengguna terpilih) {
        if (terpilih == null) {
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Aktifkan Kembali Akun");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Aktifkan kembali akun \"" + terpilih.getNama()
                + "\" (" + terpilih.getEmail() + ")?\n"
                + "Setelah diaktifkan, akun ini bisa dipakai untuk masuk ke aplikasi.");

        Optional<ButtonType> jawaban = konfirmasi.showAndWait();
        if (jawaban.isPresent() && jawaban.get() == ButtonType.OK) {
            ubahStatus(terpilih, StatusAkun.AKTIF);
        }
    }

    private void ubahStatus(Pengguna pengguna, StatusAkun statusBaru) {
        try {
            penggunaDAO.updateStatusAkun(pengguna.getPenggunaId(), statusBaru);
            semuaAkun.remove(pengguna);
            terapkanFilter();
            errorLabel.setText("");
        } catch (SQLException e) {
            errorLabel.setText("Gagal memperbarui status akun. Coba lagi.");
            System.err.println("[VerifikasiAkunController] SQLException: " + e.getMessage());
        }
    }

    private void bukaDashboard() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Dashboard.fxml", Main.APP_TITLE + " - Dashboard");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Dashboard.");
            System.err.println("[VerifikasiAkunController] IOException: " + e.getMessage());
        }
    }

    private void bukaScanQr() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ScanQr.fxml", Main.APP_TITLE + " - Scan QR");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Scan QR.");
            System.err.println("[VerifikasiAkunController] IOException: " + e.getMessage());
        }
    }

    private void bukaLaporan() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/LaporanKehadiran.fxml",
                    Main.APP_TITLE + " - Laporan");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Laporan.");
            System.err.println("[VerifikasiAkunController] IOException: " + e.getMessage());
        }
    }

    private void bukaCetakKartu() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenCetakKartu.fxml",
                    Main.APP_TITLE + " - Cetak Kartu Pelajar");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Cetak Kartu Pelajar.");
            System.err.println("[VerifikasiAkunController] IOException: " + e.getMessage());
        }
    }

    private void bukaDataMurid() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenDataMurid.fxml",
                    Main.APP_TITLE + " - Data Murid");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Data Murid.");
            System.err.println("[VerifikasiAkunController] IOException: " + e.getMessage());
        }
    }

    private void bukaDataKelas() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenDataKelas.fxml",
                    Main.APP_TITLE + " - Data Kelas");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Data Kelas.");
            System.err.println("[VerifikasiAkunController] IOException: " + e.getMessage());
        }
    }

    private void bukaMataPelajaran() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/ManajemenMataPelajaran.fxml",
                    Main.APP_TITLE + " - Mata Pelajaran");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Mata Pelajaran.");
            System.err.println("[VerifikasiAkunController] IOException: " + e.getMessage());
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
            System.err.println("[VerifikasiAkunController] Gagal membuka Profil Saya: " + e.getMessage());
        }
    }

    private void handleLogout() {
        SessionManager.logout();
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/Login.fxml", Main.APP_TITLE + " - Masuk");
        } catch (IOException e) {
            errorLabel.setText("Gagal membuka halaman Login.");
            System.err.println("[VerifikasiAkunController] IOException: " + e.getMessage());
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