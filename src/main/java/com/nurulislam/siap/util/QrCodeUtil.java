package com.nurulislam.siap.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;

/**
 * Utilitas pembuat gambar QR Code (untuk ditampilkan di JavaFX, mis. pada
 * pratinjau Kartu Pelajar), memakai library ZXing yang sama dengan yang
 * dipakai untuk MEMBACA QR Code di halaman Scan QR / Absensi Mata Pelajaran.
 * <p>
 * Lihat juga {@code app.GenerateTestQrTool} yang membuat QR dalam bentuk file
 * PNG langsung ke disk (dipakai untuk uji coba Scan QR tanpa kartu fisik).
 */
public class QrCodeUtil {

    private QrCodeUtil() {
    }

    /**
     * Membuat gambar QR Code dari sebuah teks (biasanya tb_murid.qr_token),
     * siap dipasang ke ImageView.
     *
     * @param isi     teks yang dikodekan ke dalam QR Code
     * @param ukuranPx sisi gambar persegi dalam piksel
     */
    public static Image buatGambarQr(String isi, int ukuranPx) throws WriterException {
        BitMatrix matrix = new QRCodeWriter().encode(isi, BarcodeFormat.QR_CODE, ukuranPx, ukuranPx);

        BufferedImage buffered = new BufferedImage(ukuranPx, ukuranPx, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < ukuranPx; x++) {
            for (int y = 0; y < ukuranPx; y++) {
                buffered.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        return SwingFXUtils.toFXImage(buffered, null);
    }
}