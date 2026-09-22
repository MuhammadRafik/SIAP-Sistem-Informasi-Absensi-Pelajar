# SIAP - Sistem Informasi Absensi Pelajar MA Nurul Islam

Aplikasi absensi siswa berbasis desktop (Java + JavaFX + MySQL) untuk Tugas Akhir
Muhammad Rafik (24302037) - D3 Teknik Informatika, Politeknik Hasnur.

## Struktur Project

```
SIAP-NurulIslam/
├── pom.xml                          # dependency: JavaFX, MySQL, ZXing, BCrypt
├── database/
│   └── siap_schema.sql              # skema database (11 tabel sesuai ERD)
└── src/main/
    ├── java/com/nurulislam/siap/
    │   ├── app/        -> Main.java, Launcher.java (entry point)
    │   ├── model/      -> kelas entitas (Pengguna, Murid, Kelas, dst)
    │   ├── dao/        -> akses database (JDBC) per entitas
    │   ├── controller/ -> controller untuk tiap layar FXML
    │   └── util/       -> DatabaseConnection, helper lain
    └── resources/
        ├── config/db.properties     # kredensial database
        └── com/nurulislam/siap/
            ├── fxml/                # file tampilan (.fxml)
            ├── css/style.css        # styling (warna sesuai desain)
            └── images/              # aset gambar/logo
```

## Langkah Setup

### 1. Siapkan Database
1. Nyalakan MySQL (XAMPP/Laragon/MySQL Server).
2. Jalankan file `database/siap_schema.sql`, contoh lewat terminal:
   ```
   mysql -u root -p < database/siap_schema.sql
   ```
   atau import lewat phpMyAdmin.
3. Ini akan membuat database `db_siap_nurul_islam` beserta 11 tabel & data awal.

### 2. Sesuaikan Konfigurasi Koneksi
Edit `src/main/resources/config/db.properties` sesuai environment Anda:
```
db.url=jdbc:mysql://localhost:3306/db_siap_nurul_islam?useSSL=false&serverTimezone=Asia/Makassar
db.user=root
db.password=      <-- isi jika MySQL Anda pakai password
```

### 3. Buka Project di NetBeans
1. Buka NetBeans -> **File > Open Project** -> pilih folder `SIAP-NurulIslam`
   (NetBeans akan otomatis mengenali `pom.xml` sebagai project Maven).
2. Klik kanan project -> **Clean and Build** (proses download dependency
   JavaFX, MySQL Connector, ZXing, BCrypt dari repository Maven).
3. Jalankan project: klik kanan project -> **Run**, atau tekan **F6**.
   - Jika NetBeans menjalankan `Main` class secara langsung dan muncul
     error terkait modul JavaFX, jalankan lewat terminal:
     ```
     mvn clean javafx:run
     ```

### 4. Verifikasi Fondasi
Jika berhasil, akan muncul jendela **"SIAP - Absensi Siswa MA Nurul Islam"**
dengan tulisan **"Status database: TERHUBUNG"**. Ini menandakan:
- JavaFX + FXML berjalan normal
- Koneksi JDBC ke MySQL berhasil
- Project siap dilanjutkan ke tahap berikutnya (halaman Login)

## Tahapan Pengembangan (Roadmap)

- [x] 1. Setup project & skema database (tahap ini)
- [ ] 2. Model & DAO (kelas Java + JDBC untuk semua tabel)
- [ ] 3. Modul Autentikasi (Login, Daftar Akun, Verifikasi, Reset Password)
- [ ] 4. Dashboard (menu berbeda untuk role TU vs GURU)
- [ ] 5. Modul Data Master (Murid, Kelas, Mata Pelajaran & Jadwal) - khusus TU
- [ ] 6. Modul Absensi (Scan QR harian, Absensi Mapel oleh Guru, input manual)
- [ ] 7. Cetak Kartu Pelajar (generate QR Code + token via ZXing)
- [ ] 8. Laporan & Rekap Absensi (filter & sort)

## Catatan Teknis

- Password disimpan dalam bentuk **hash BCrypt**, bukan plain text
  (lihat kolom `password_hash` di `tb_pengguna`).
- QR Code kartu pelajar memuat **NIS + qr_token acak** (bukan NIS saja),
  sesuai batasan masalah di proposal, untuk mencegah penggandaan kartu.
- Pola arsitektur: **MVC + DAO**, koneksi database lewat JDBC murni
  (bukan ORM/Hibernate) agar sesuai cakupan Tugas Akhir D3.
