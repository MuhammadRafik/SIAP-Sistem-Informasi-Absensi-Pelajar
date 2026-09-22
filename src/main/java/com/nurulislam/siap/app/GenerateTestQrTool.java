package com.nurulislam.siap.app;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.nurulislam.siap.dao.MuridDAO;
import com.nurulislam.siap.model.Murid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Tool baris perintah (bukan bagian aplikasi utama) untuk mencetak QR Code
 * dari qr_token setiap murid di database, dalam bentuk file PNG.
 * <p>
 * Dipakai sementara untuk menguji halaman Scan QR sebelum modul
 * "Cetak Kartu Pelajar" (yang akan menampilkan QR ini di kartu pelajar
 * lengkap dengan foto & identitas) selesai dibuat.
 * <p>
 * Cara pakai: buka class ini di IDE (NetBeans/IntelliJ) lalu jalankan
 * method main()-nya langsung (klik kanan -> Run File), sama seperti
 * SeedAdminTool. Pastikan database & data pada seed_data_uji_scanqr.sql
 * sudah dimuat lebih dulu.
 * Hasil PNG akan tersimpan di folder "qr-uji-coba/" pada root project,
 * satu file per murid, nama file = NIS murid.
 */
public class GenerateTestQrTool {

    private static final String FOLDER_OUTPUT = "qr-uji-coba";
    private static final int UKURAN_PX = 300;

    public static void main(String[] args) {
        try {
            List<Murid> semuaMurid = new MuridDAO().findAll();
            if (semuaMurid.isEmpty()) {
                System.out.println("[GenerateTestQrTool] Tidak ada data murid. "
                        + "Jalankan database/seed_data_uji_scanqr.sql terlebih dahulu.");
                return;
            }

            Path folder = Path.of(FOLDER_OUTPUT);
            Files.createDirectories(folder);

            for (Murid murid : semuaMurid) {
                Path file = folder.resolve(murid.getNis() + "_" + slug(murid.getNama()) + ".png");
                buatPngQr(murid.getQrToken(), file);
                System.out.println("[GenerateTestQrTool] Dibuat: " + file
                        + "  (murid: " + murid.getNama() + ", token: " + murid.getQrToken() + ")");
            }

            System.out.println("[GenerateTestQrTool] Selesai. Buka folder \"" + FOLDER_OUTPUT
                    + "\" lalu arahkan salah satu file PNG ke kamera di halaman Scan QR untuk menguji.");
        } catch (SQLException e) {
            System.err.println("[GenerateTestQrTool] Gagal mengambil data murid: " + e.getMessage());
        } catch (IOException | WriterException e) {
            System.err.println("[GenerateTestQrTool] Gagal membuat file QR: " + e.getMessage());
        }
    }

    private static void buatPngQr(String isiToken, Path tujuanFile) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(isiToken, BarcodeFormat.QR_CODE, UKURAN_PX, UKURAN_PX);
        MatrixToImageWriter.writeToPath(matrix, "PNG", tujuanFile);
    }

    private static String slug(String nama) {
        return nama.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }
}