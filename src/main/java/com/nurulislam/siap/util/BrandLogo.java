package com.nurulislam.siap.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;

/**
 * Logo SIAP pada sidebar ({@code logo-siap.png}).
 * <p>
 * Sengaja dimuat lewat kode (bukan atribut {@code image="@..."} di FXML)
 * karena koersi URL-gambar FXML tidak stabil di project ini dan membuat
 * halaman gagal dibuka.
 */
public final class BrandLogo {

    private static final String LOGO_PATH = "/com/nurulislam/siap/images/logo-siap.png";

    private static Image cache;

    private BrandLogo() {
    }

    public static synchronized Image logo() {
        if (cache == null || cache.isError()) {
            try (InputStream in = BrandLogo.class.getResourceAsStream(LOGO_PATH)) {
                if (in != null) {
                    cache = new Image(in);
                }
            } catch (Exception e) {
                System.err.println("[BrandLogo] Gagal memuat logo: " + e.getMessage());
            }
        }
        return cache;
    }

    /** Memasang logo ke ImageView sidebar (fx:id imgLogo). Aman null. */
    public static void pasang(ImageView view) {
        if (view == null) {
            return;
        }
        Image img = logo();
        if (img != null && !img.isError()) {
            view.setImage(img);
        }
    }
}
