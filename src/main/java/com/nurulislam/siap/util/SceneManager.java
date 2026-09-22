package com.nurulislam.siap.util;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Utilitas untuk berpindah antar layar (FXML) di dalam satu Stage yang sama.
 * Dipakai misalnya: Login -> Dashboard, Dashboard -> Data Murid, dst.
 * Stage utama didaftarkan sekali dari Main.start().
 */
public class SceneManager {

    private static Stage primaryStage;

    private SceneManager() {
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Mengganti isi jendela utama dengan FXML baru.
     *
     * @param fxmlResourcePath path resource, contoh: "/com/nurulislam/siap/fxml/Login.fxml"
     * @param title            judul jendela yang ditampilkan
     */
    public static void switchTo(String fxmlResourcePath, String title) throws IOException {
        switchToWithController(fxmlResourcePath, title);
    }

    /**
     * Sama seperti {@link #switchTo}, tapi mengembalikan instance controller dari
     * FXML yang baru dimuat. Berguna saat layar berikutnya perlu diberi data awal,
     * misalnya mengoper email dari halaman "Lupa Password" ke "Reset Password"
     * tanpa perlu menyimpan state itu secara global (lihat SessionManager).
     *
     * @param <T> tipe controller sesuai fx:controller pada file FXML
     */
    public static <T> T switchToWithController(String fxmlResourcePath, String title) throws IOException {
        URL fxmlUrl = SceneManager.class.getResource(fxmlResourcePath);
        if (fxmlUrl == null) {
            throw new IOException("FXML tidak ditemukan: " + fxmlResourcePath);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root;
        try {
            root = loader.load();
        } catch (Throwable t) {
            // FXMLLoader.load() sering hanya melempar pesan singkat (mis. lokasi baris FXML)
            // sementara PENYEBAB SEBENARNYA (NoClassDefFoundError, UnsatisfiedLinkError,
            // dsb. dari controller yang gagal dibuat) ada di getCause()/stack trace.
            // Dicetak lengkap di sini supaya kelihatan jelas di console, tidak cuma
            // "gagal membuka halaman X" tanpa detail di layar aplikasi.
            System.err.println("[SceneManager] Gagal memuat FXML: " + fxmlResourcePath);
            t.printStackTrace();
            throw new IOException("Gagal memuat " + fxmlResourcePath + ": " + rootPenyebab(t), t);
        }

        Scene scene = new Scene(root);

        URL cssUrl = SceneManager.class.getResource("/com/nurulislam/siap/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setScene(scene);
        primaryStage.setTitle(title);

        // Beberapa halaman (Dashboard, Scan QR, dll) didesain dengan prefWidth/prefHeight
        // besar (mis. 1280x800). Tanpa pembatasan ini, window bisa membesar melebihi area
        // layar yang terlihat (di luar taskbar) pada layar laptop yang lebih kecil - membuat
        // title bar (tombol minimize/close) terdorong ke luar layar dan konten paling bawah
        // ikut terpotong. sizeToScene() dulu supaya ukuran window mengikuti konten FXML,
        // baru dibatasi (clamp) agar selalu muat di layar mana pun.
        primaryStage.sizeToScene();
        Rectangle2D areaLayar = Screen.getPrimary().getVisualBounds();
        double marginAman = 40;
        if (primaryStage.getWidth() > areaLayar.getWidth() - marginAman) {
            primaryStage.setWidth(areaLayar.getWidth() - marginAman);
        }
        if (primaryStage.getHeight() > areaLayar.getHeight() - marginAman) {
            primaryStage.setHeight(areaLayar.getHeight() - marginAman);
        }

        primaryStage.centerOnScreen();

        return loader.getController();
    }

    /** Menelusuri ke exception paling dalam (root cause) supaya pesannya lebih informatif. */
    private static String rootPenyebab(Throwable t) {
        Throwable akar = t;
        while (akar.getCause() != null && akar.getCause() != akar) {
            akar = akar.getCause();
        }
        String pesan = akar.getMessage();
        return akar.getClass().getSimpleName() + (pesan != null ? ": " + pesan : "");
    }
}