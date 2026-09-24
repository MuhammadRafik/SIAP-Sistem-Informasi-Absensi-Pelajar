package com.nurulislam.siap.controller;

import com.nurulislam.siap.app.Main;
import com.nurulislam.siap.dao.JadwalMengajarDAO;
import com.nurulislam.siap.model.JadwalMengajar;
import com.nurulislam.siap.model.Pengguna;
import com.nurulislam.siap.model.Role;
import com.nurulislam.siap.util.SceneManager;
import com.nurulislam.siap.util.SessionManager;
import com.nurulislam.siap.util.ProfileMenu;
import com.nurulislam.siap.util.AvatarUtil;
import com.nurulislam.siap.util.BrandLogo;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Halaman Jadwal Mengajar khusus Guru.
 *
 * Sifat halaman:
 * - hanya membaca data jadwal;
 * - tidak menyediakan tambah/edit/hapus;
 * - hanya menampilkan jadwal milik Guru yang sedang login;
 * - data diambil dari tahun ajaran aktif melalui JadwalMengajarDAO yang sudah
 *   menggunakan filter ta.status_aktif = TRUE.
 */
public class JadwalGuruController {

    // FIX ERROR TABLE: gunakan CellFactory agar kompatibel dengan project JavaFX.

    @FXML private Button btnNavBeranda;
    @FXML private Button btnNavJadwal;
    @FXML private Button btnNavAbsensiMapel;
    @FXML private Button btnNavLaporan;
    @FXML private StackPane avatarBox;
    @FXML private ImageView imgAvatar;
    @FXML private ImageView imgLogo;

    @FXML private Label labelTanggal;
    @FXML private Label labelNamaUser;
    @FXML private Label labelRoleUser;
    @FXML private Label labelInisialUser;

    @FXML private Label labelStatusInfo;
    @FXML private Label labelJumlahJadwal;

    @FXML private TableView<JadwalMengajar> tabelJadwal;
    @FXML private TableColumn<JadwalMengajar, Void> kolomHari;
    @FXML private TableColumn<JadwalMengajar, Void> kolomJam;
    @FXML private TableColumn<JadwalMengajar, Void> kolomMapel;
    @FXML private TableColumn<JadwalMengajar, Void> kolomKelas;
    @FXML private TableColumn<JadwalMengajar, Void> kolomGuru;
    @FXML private TableColumn<JadwalMengajar, Void> kolomStatus;

    private final JadwalMengajarDAO jadwalDAO = new JadwalMengajarDAO();

    @FXML
    public void initialize() {
        isiInfoPengguna();
        isiTanggal();
        siapkanTabel();
        wireNavigasi();
        muatJadwalGuru();
    }

    private void isiInfoPengguna() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();

        if (pengguna == null || pengguna.getRole() != Role.GURU || pengguna.getPenggunaId() == null) {
            labelNamaUser.setText("Guru");
            labelRoleUser.setText("Akses tidak valid");
            labelInisialUser.setText("?");
            return;
        }

        String nama = pengguna.getNama() == null || pengguna.getNama().isBlank()
                ? "Guru"
                : pengguna.getNama();

        labelNamaUser.setText(nama);
        labelRoleUser.setText(pengguna.getRole().getLabel());
        labelInisialUser.setText(inisial(nama));
    }

    private String inisial(String nama) {
        if (nama == null || nama.isBlank()) {
            return "?";
        }

        String[] bagian = nama.trim().split("\\s+");
        StringBuilder hasil = new StringBuilder();

        for (int i = 0; i < Math.min(2, bagian.length); i++) {
            if (!bagian[i].isBlank()) {
                hasil.append(Character.toUpperCase(bagian[i].charAt(0)));
            }
        }

        return hasil.isEmpty() ? "?" : hasil.toString();
    }

    private void isiTanggal() {
        LocalDate tanggal = LocalDate.now();
        String hari = tanggal.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, new Locale("id", "ID"));

        hari = Character.toUpperCase(hari.charAt(0)) + hari.substring(1);

        labelTanggal.setText(
                hari + ", " + tanggal.format(
                        java.time.format.DateTimeFormatter.ofPattern(
                                "d MMMM yyyy", new Locale("id", "ID")
                        )
                )
        );
    }

    private void siapkanTabel() {
        // Render isi tabel langsung melalui CellFactory.
        // Dengan cara ini controller tidak membutuhkan SimpleStringProperty
        // atau SimpleObjectProperty.

        kolomHari.setCellFactory(column -> buatCell(jadwal -> hariLabel(jadwal)));
        kolomJam.setCellFactory(column -> buatCell(jadwal -> formatJam(jadwal)));
        kolomMapel.setCellFactory(column -> buatCell(jadwal -> aman(jadwal.getNamaMapel())));
        kolomKelas.setCellFactory(column -> buatCell(jadwal -> aman(jadwal.getNamaKelas())));
        kolomGuru.setCellFactory(column -> buatCell(jadwal -> aman(jadwal.getNamaGuru())));

        kolomStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void ignored, boolean empty) {
                super.updateItem(ignored, empty);

                getStyleClass().removeAll(
                        "jadwal-status-aktif",
                        "jadwal-status-menunggu",
                        "jadwal-status-selesai"
                );

                if (empty) {
                    setText(null);
                    return;
                }

                int rowIndex = getIndex();
                if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                    setText(null);
                    return;
                }

                JadwalMengajar jadwal = getTableView().getItems().get(rowIndex);
                JadwalMengajar.StatusSesi status = jadwal.statusPada(LocalTime.now());

                setText(status.getLabel());

                switch (status) {
                    case AKTIF -> getStyleClass().add("jadwal-status-aktif");
                    case BELUM_MULAI -> getStyleClass().add("jadwal-status-menunggu");
                    case SELESAI -> getStyleClass().add("jadwal-status-selesai");
                }

                setAlignment(Pos.CENTER);
            }
        });

        tabelJadwal.setPlaceholder(
                new Label("Belum ada jadwal mengajar pada tahun ajaran aktif.")
        );

        // Read-only.
        tabelJadwal.setEditable(false);

        // FIX 43: jangan memaksa FXMLLoader melakukan coercion
        // terhadap string "CONSTRAINED_RESIZE_POLICY" ke Callback.
        // Atur policy langsung dari Java agar kompatibel dengan JavaFX 21.
        tabelJadwal.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FunctionalInterface
    private interface TeksJadwal {
        String get(JadwalMengajar jadwal);
    }

    private TableCell<JadwalMengajar, Void> buatCell(TeksJadwal penyediaTeks) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Void ignored, boolean empty) {
                super.updateItem(ignored, empty);

                if (empty) {
                    setText(null);
                    return;
                }

                int rowIndex = getIndex();
                if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                    setText(null);
                    return;
                }

                setText(
                        penyediaTeks.get(
                                getTableView().getItems().get(rowIndex)
                        )
                );
            }
        };
    }

    private String hariLabel(JadwalMengajar jadwal) {
        if (jadwal == null || jadwal.getHari() == null) {
            return "-";
        }

        String hari = jadwal.getHari().name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(hari.charAt(0)) + hari.substring(1);
    }

    private String formatJam(JadwalMengajar jadwal) {
        if (jadwal == null || jadwal.getJamMulai() == null || jadwal.getJamSelesai() == null) {
            return "-";
        }

        return jadwal.getJamMulai().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                + " - "
                + jadwal.getJamSelesai().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String aman(String nilai) {
        return nilai == null || nilai.isBlank() ? "-" : nilai;
    }

    private void wireNavigasi() {
        btnNavBeranda.setOnAction(e -> buka(
                "/com/nurulislam/siap/fxml/DashboardGuru.fxml",
                "Beranda"
        ));

        btnNavJadwal.setOnAction(e -> {
            // Sudah di halaman Jadwal Mengajar.
        });

        btnNavAbsensiMapel.setOnAction(e -> buka(
                "/com/nurulislam/siap/fxml/AbsensiMapel.fxml",
                "Absensi Mata Pelajaran"
        ));

        btnNavLaporan.setOnAction(e -> buka(
                "/com/nurulislam/siap/fxml/LaporanMapel.fxml",
                "Laporan"
        ));

        ProfileMenu.pasang(avatarBox, this::bukaProfil, this::handleLogout);
        AvatarUtil.tampilkan(avatarBox, imgAvatar, labelInisialUser);
        BrandLogo.pasang(imgLogo);
    }

    /** Membuka halaman Profil Saya (diakses dari menu avatar kanan atas). */
    private void bukaProfil() {
        try {
            SceneManager.switchTo("/com/nurulislam/siap/fxml/PengaturanAkun.fxml",
                    Main.APP_TITLE + " - Profil Saya");
        } catch (IOException e) {
            System.err.println("[JadwalGuruController] Gagal membuka Profil Saya: " + e.getMessage());
        }
    }

    private void handleLogout() {
        SessionManager.logout();
        buka("/com/nurulislam/siap/fxml/Login.fxml", "Masuk");
    }

    private void muatJadwalGuru() {
        Pengguna pengguna = SessionManager.getPenggunaAktif();

        if (pengguna == null || pengguna.getRole() != Role.GURU || pengguna.getPenggunaId() == null) {
            tampilkanAksesDitolak();
            return;
        }

        try {
            List<JadwalMengajar> jadwal = jadwalDAO.findAllByGuru(
                    pengguna.getPenggunaId()
            );

            tabelJadwal.setItems(FXCollections.observableArrayList(jadwal));
            labelJumlahJadwal.setText(String.valueOf(jadwal.size()));

            if (jadwal.isEmpty()) {
                labelStatusInfo.setText(
                        "Belum ada jadwal mengajar Anda pada tahun ajaran aktif."
                );
            } else {
                labelStatusInfo.setText(
                        "Jadwal bersifat read-only. Perubahan jadwal hanya dapat dilakukan oleh Staf TU."
                );
            }

        } catch (Exception e) {
            tabelJadwal.getItems().clear();
            labelJumlahJadwal.setText("0");
            labelStatusInfo.setText(
                    "Jadwal tidak dapat dimuat. Periksa koneksi database."
            );

            System.err.println(
                    "[JadwalGuruController] Gagal memuat jadwal: " + e.getMessage()
            );
        }
    }

    private void tampilkanAksesDitolak() {
        labelStatusInfo.setText(
                "Akses halaman ini hanya untuk akun Guru."
        );
        labelJumlahJadwal.setText("0");
        tabelJadwal.getItems().clear();

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Akses Ditolak");
        alert.setHeaderText(null);
        alert.setContentText("Halaman Jadwal Mengajar hanya dapat digunakan oleh akun Guru.");
        alert.showAndWait();
    }

    private void buka(String fxml, String judul) {
        try {
            SceneManager.switchTo(
                    fxml,
                    Main.APP_TITLE + " - " + judul
            );
        } catch (IOException e) {
            System.err.println(
                    "[JadwalGuruController] Gagal membuka " + judul + ": " + e.getMessage()
            );
        }
    }
}
