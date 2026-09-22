package com.nurulislam.siap.util;

import com.nurulislam.siap.model.Pengguna;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.io.File;

/**
 * Menampilkan foto profil pada lingkaran avatar kanan atas setiap halaman.
 * Jika pengguna belum punya foto, lingkaran menampilkan inisial nama
 * seperti sebelumnya. Ukuran bingkai avatar adalah 40x40.
 */
public final class AvatarUtil {

    /** Ukuran sisi bingkai avatar lingkaran di top bar (px). */
    public static final double UKURAN = 40;

    private AvatarUtil() {
    }

    /**
     * @param box     bingkai lingkaran avatar (tidak boleh null saat dipanggil
     *                dari initialize, tapi tetap aman null)
     * @param img     ImageView di dalam bingkai (fx:id imgAvatar)
     * @param inisial label inisial di dalam bingkai
     */
    public static void tampilkan(StackPane box, ImageView img, Label inisial) {
        Pengguna pengguna = SessionManager.getPenggunaAktif();
        String pathFoto = pengguna != null ? pengguna.getFoto() : null;

        if (box != null && box.getClip() == null) {
            box.setClip(new Rectangle(UKURAN, UKURAN));
        }
        if (img != null) {
            img.setFitWidth(UKURAN);
            img.setFitHeight(UKURAN);
            img.setPreserveRatio(false);
            if (img.getClip() == null) {
                img.setClip(new Circle(UKURAN / 2, UKURAN / 2, UKURAN / 2));
            }
        }

        Image foto = muatFoto(pathFoto);
        if (foto != null && img != null) {
            pasangViewportPersegi(img, foto);
            img.setImage(foto);
            img.setVisible(true);
            if (inisial != null) {
                inisial.setVisible(false);
            }
        } else {
            if (img != null) {
                img.setImage(null);
                img.setViewport(null);
                img.setVisible(false);
            }
            if (inisial != null) {
                inisial.setVisible(true);
            }
        }
    }

    /**
     * Memuat foto dari path. Mengembalikan null jika tidak ada foto,
     * berkas hilang, atau gagal dibaca.
     */
    static Image muatFoto(String pathFoto) {
        if (pathFoto == null || pathFoto.isBlank()) {
            return null;
        }
        try {
            File berkas = new File(pathFoto);
            if (!berkas.exists()) {
                return null;
            }
            Image gambar = new Image(berkas.toURI().toString());
            return gambar.isError() ? null : gambar;
        } catch (RuntimeException e) {
            System.err.println("[AvatarUtil] Gagal memuat foto: " + e.getMessage());
            return null;
        }
    }

    /**
     * Memasang viewport crop persegi-tengah pada ImageView untuk gambar
     * yang diberikan. Dipakai internal oleh {@link #tampilkan}.
     */
    public static void pasangViewportPersegi(ImageView img, Image gambar) {
        if (img == null || gambar == null) {
            return;
        }
        double lebar = gambar.getWidth();
        double tinggi = gambar.getHeight();
        if (lebar > 0 && tinggi > 0) {
            double sisi = Math.min(lebar, tinggi);
            img.setViewport(new Rectangle2D(
                    (lebar - sisi) / 2, (tinggi - sisi) / 2, sisi, sisi));
        } else {
            img.setViewport(null);
        }
    }
}
