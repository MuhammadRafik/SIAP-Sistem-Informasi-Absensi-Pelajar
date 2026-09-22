package com.nurulislam.siap.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility koneksi MySQL yang lebih aman untuk aplikasi desktop.
 * Fix 11:
 * - Tidak melempar RuntimeException saat konfigurasi/driver bermasalah pada startup.
 * - Memberikan pesan error yang lebih mudah dipahami pengguna.
 * - Menyediakan testConnection(), getLastError(), dan isDatabaseAvailable().
 * - Menggunakan timeout agar aplikasi tidak terasa hang ketika MySQL tidak tersedia.
 * - Tetap kompatibel dengan DAO lama yang memanggil getConnection().
 */
public final class DatabaseConnection {

    private static final String CONFIG_PATH = "/config/db.properties";
    private static final int CONNECT_TIMEOUT_SECONDS = 3;

    private static final Properties PROPERTIES = new Properties();
    private static volatile String lastError = "";
    private static volatile boolean driverLoaded = false;

    static {
        loadProperties();
        loadDriver();
    }

    /**
     * Mengubah error JDBC menjadi pesan yang mudah dipahami pengguna.
     * Sebelumnya method ini masih berisi kode template dan dapat
     * menghentikan aplikasi ketika dipanggil.
     */
    public static String getUserFriendlyMessage(SQLException e) {
        if (e == null) {
            return "Koneksi database gagal. Silakan periksa MySQL/XAMPP.";
        }

        return getFriendlyMessage(e);
    }

    private DatabaseConnection() {
        // Utility class.
    }

    private static void loadProperties() {
        try (InputStream input = DatabaseConnection.class.getResourceAsStream(CONFIG_PATH)) {
            if (input == null) {
                lastError = "File konfigurasi database tidak ditemukan: " + CONFIG_PATH;
                return;
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            lastError = "Gagal membaca konfigurasi database: " + e.getMessage();
        }
    }

    private static void loadDriver() {
        String driver = PROPERTIES.getProperty("db.driver", "com.mysql.cj.jdbc.Driver").trim();
        try {
            Class.forName(driver);
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            driverLoaded = false;
            lastError = "Driver MySQL tidak ditemukan. Pastikan dependency mysql-connector-j tersedia.";
        }
    }

    /**
     * Membuka koneksi baru.
     * Pemanggil tetap wajib menggunakan try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            SQLException ex = new SQLException("Driver MySQL belum tersedia.");
            lastError = getFriendlyMessage(ex);
            throw ex;
        }

        String url = PROPERTIES.getProperty("db.url", "").trim();
        String user = PROPERTIES.getProperty("db.user", "").trim();
        String password = PROPERTIES.getProperty("db.password", "");

        if (url.isEmpty()) {
            SQLException ex = new SQLException("URL database kosong.");
            lastError = "Konfigurasi database belum lengkap. Periksa file db.properties.";
            throw ex;
        }

        String finalUrl = addConnectionTimeout(url);

        try {
            Connection conn = DriverManager.getConnection(finalUrl, user, password);
            if (conn == null || conn.isClosed()) {
                SQLException ex = new SQLException("Koneksi database tidak terbentuk.");
                lastError = getFriendlyMessage(ex);
                throw ex;
            }

            lastError = "";
            return conn;
        } catch (SQLException e) {
            lastError = getFriendlyMessage(e);
            throw e;
        }
    }

    /**
     * Mengecek koneksi database tanpa membuat aplikasi crash.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean ok = conn != null && !conn.isClosed() && conn.isValid(CONNECT_TIMEOUT_SECONDS);
            if (!ok) {
                lastError = "Database tidak memberikan koneksi yang valid.";
            } else {
                lastError = "";
            }
            return ok;
        } catch (SQLException e) {
            // getConnection() sudah menyimpan pesan ramah pengguna.
            System.err.println("[DATABASE] " + lastError);
            return false;
        }
    }

    /**
     * Alias yang mudah dipakai controller/dashboard.
     */
    public static boolean isDatabaseAvailable() {
        return testConnection();
    }

    /**
     * Pesan error terakhir untuk ditampilkan pada UI.
     */
    public static String getLastError() {
        return lastError == null || lastError.isBlank()
                ? "Tidak diketahui. Silakan periksa layanan MySQL/XAMPP."
                : lastError;
    }

    /**
     * Pesan yang lebih mudah dimengerti operator TU/Guru daripada pesan JDBC mentah.
     */
    private static String getFriendlyMessage(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        String lower = message.toLowerCase();

        if (lower.contains("communications link failure")
                || lower.contains("connection refused")
                || lower.contains("connect timed out")
                || lower.contains("could not create connection")) {
            return "Database tidak dapat dihubungi. Pastikan MySQL/XAMPP sudah menyala.";
        }

        if (lower.contains("unknown database")) {
            return "Database sekolah tidak ditemukan. Pastikan database 'db_siap_nurul_islam' sudah dibuat.";
        }

        if (lower.contains("access denied")) {
            return "Akses database ditolak. Periksa username/password pada db.properties.";
        }

        if (lower.contains("no suitable driver")) {
            return "Driver MySQL tidak cocok atau belum tersedia pada project.";
        }

        return "Koneksi database gagal. " + message;
    }

    /**
     * Menambahkan timeout bila URL belum memilikinya.
     * Timeout hanya untuk percobaan koneksi; tidak mengubah URL di properties.
     */
    private static String addConnectionTimeout(String url) {
        if (url.contains("connectTimeout=")) {
            return url;
        }

        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "connectTimeout=" + (CONNECT_TIMEOUT_SECONDS * 1000)
                + "&socketTimeout=" + (CONNECT_TIMEOUT_SECONDS * 1000);
    }
}
