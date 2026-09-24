package com.nurulislam.siap.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

import java.io.InputStream;
import java.util.Locale;

/**
 * Membangun desain Kartu Pelajar MA Nurul Islam (depan & belakang)
 * sebagai node JavaFX siap pratinjau, snapshot PNG, maupun cetak.
 * <p>
 * Ukuran baku mengikuti kartu ID CR80 (85,6 x 54 mm) pada 167 dpi:
 * 560 x 353 px — cukup tajam untuk cetak, tetap ringan di pratinjau
 * (pratinjau memakai skala 0,53 lewat controller).
 * <p>
 * Logo resmi: jika berkas {@code logo-ma.png} tersedia di
 * {@code /com/nurulislam/siap/images/}, otomatis dipakai sebagai emblem.
 * Jika belum ada, dipakai emblem vektor (segilima + kitab + bintang).
 */
public final class KartuPelajarView {

    /** Lebar kartu dalam px. */
    public static final double LEBAR_KARTU = 560;
    /** Tinggi kartu dalam px. */
    public static final double TINGGI_KARTU = 353;
    /** Skala pratinjau di kolom kanan (560 -> ~297 px). */
    public static final double SKALA_PRATINJAU = 0.53;

    private static final String HIJAU_TUA = "#0c5a33";
    private static final String HIJAU = "#177244";
    private static final String EMAS = "#d9a821";
    private static final String EMAS_TERANG = "#f0c541";
    private static final String TEKS_GELAP = "#1c2b24";
    private static final String ABU_BAR = "#edf1f0";
    private static final String GARIS = "#dfe5e4";

    private static final Locale ID = new Locale("id", "ID");

    private KartuPelajarView() {
    }

    // ================= API utama =================

    /**
     * @param nama       nama lengkap murid (ditampilkan kapital)
     * @param nis        NIS murid (ditampilkan pada baris NISN)
     * @param ttl        tempat/tanggal lahir (boleh hanya tanggal)
     * @param jurusan    jurusan kelas (boleh null)
     * @param tahunMasuk tahun masuk (boleh null)
     * @param foto       foto murid (boleh null -> blok inisial)
     * @param logo       logo resmi (boleh null -> emblem vektor)
     */
    public static Pane buatDepan(String nama, String nis, String ttl,
                                 String jurusan, String tahunMasuk,
                                 Image foto, Image logo) {
        VBox kartu = kartuKosong();

        // ---- Kepala hijau + ombak ----
        StackPane kepala = new StackPane();
        kepala.setPrefSize(LEBAR_KARTU, 128);
        kepala.setMinSize(LEBAR_KARTU, 128);
        kepala.setMaxSize(LEBAR_KARTU, 128);

        Rectangle dasar = new Rectangle(LEBAR_KARTU, 128, Color.web(HIJAU_TUA));

        SVGPath ombakHijauMuda = new SVGPath();
        ombakHijauMuda.setContent("M0,0 H560 V95 C470,137 380,139 300,109 C220,79 110,83 0,115 Z");
        ombakHijauMuda.setFill(Color.web("#2f9e5f"));

        SVGPath ombakEmas = new SVGPath();
        ombakEmas.setContent("M0,0 H560 V78 C470,120 380,122 300,92 C220,62 110,66 0,98 Z");
        ombakEmas.setFill(Color.web(EMAS));

        SVGPath ombakHijau = new SVGPath();
        ombakHijau.setContent("M0,0 H560 V73 C470,115 380,117 300,87 C220,57 110,61 0,93 Z");
        ombakHijau.setFill(Color.web(HIJAU));

        HBox isiKepala = new HBox(14);
        isiKepala.setAlignment(Pos.CENTER_LEFT);
        isiKepala.setPadding(new Insets(0, 0, 0, 26));
        isiKepala.setMaxWidth(LEBAR_KARTU);

        javafx.scene.Node emblem = buatLogo(80, logo);

        Rectangle pembatas = new Rectangle(2, 58, Color.web("#ffffff", 0.45));

        VBox judulBox = new VBox(2);
        judulBox.setAlignment(Pos.CENTER_LEFT);
        Label judul = new Label("MA NURUL ISLAM");
        judul.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subjudul = new Label("KARTU PELAJAR");
        subjudul.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + EMAS_TERANG + ";");
        judulBox.getChildren().addAll(judul, subjudul);

        isiKepala.getChildren().addAll(emblem, pembatas, judulBox);
        kepala.getChildren().addAll(dasar, ombakHijauMuda, ombakEmas, ombakHijau, isiKepala);

        // ---- Badan: foto + field ----
        HBox badan = new HBox(20);
        badan.setAlignment(Pos.TOP_LEFT);
        badan.setPadding(new Insets(14, 26, 12, 26));
        VBox.setVgrow(badan, javafx.scene.layout.Priority.ALWAYS);

        StackPane bingkaiFoto = buatBingkaiFoto(foto, nama, 100, 128);

        VBox fieldBox = new VBox(4);
        fieldBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(fieldBox, javafx.scene.layout.Priority.ALWAYS);
        fieldBox.getChildren().addAll(
                barisField("NAMA", kapital(nama)),
                barisField("NISN", nis),
                barisField("TEMPAT/TANGGAL LAHIR", ttl),
                barisField("JURUSAN", jurusan),
                barisField("TAHUN MASUK", tahunMasuk)
        );

        badan.getChildren().addAll(bingkaiFoto, fieldBox);
        kartu.getChildren().addAll(kepala, badan);
        return kartu;
    }

    /** Sisi belakang: emblem, motto, dan bingkai QR. */
    public static Pane buatBelakang(Image qr, Image logo) {
        VBox kartu = kartuKosong();
        kartu.setAlignment(Pos.CENTER);

        VBox isi = new VBox(6);
        isi.setAlignment(Pos.CENTER);
        isi.setPadding(new Insets(14, 20, 14, 20));

        javafx.scene.Node emblem = buatLogo(64, logo);

        Label judul = new Label("MA NURUL ISLAM");
        judul.setStyle("-fx-font-size: 23px; -fx-font-weight: bold; -fx-text-fill: " + HIJAU_TUA + ";");

        Label motto = new Label("BERILMU | BERAKHLAK | BERPRESTASI");
        motto.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #4a6b5d;");

        StackPane bingkaiQr = new StackPane();
        bingkaiQr.setMaxWidth(172);
        bingkaiQr.setStyle("-fx-background-color: white; -fx-background-radius: 14;"
                + "-fx-border-color: " + HIJAU_TUA + "; -fx-border-width: 2.5; -fx-border-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(12,90,51,0.18), 8, 0, 0, 2);");
        bingkaiQr.setPadding(new Insets(10));
        ImageView gambarQr = new ImageView(qr);
        gambarQr.setFitWidth(148);
        gambarQr.setFitHeight(148);
        gambarQr.setPreserveRatio(true);
        gambarQr.setSmooth(false);
        bingkaiQr.getChildren().add(gambarQr);

        isi.getChildren().addAll(emblem, judul, motto, bingkaiQr);
        kartu.getChildren().add(isi);
        return kartu;
    }

    /** Memuat logo resmi bila tersedia, null bila belum ada. */
    public static Image muatLogoResmi() {
        try (InputStream in = KartuPelajarView.class.getResourceAsStream(
                "/com/nurulislam/siap/images/logo-ma.png")) {
            if (in != null) {
                Image logo = new Image(in);
                if (!logo.isError()) {
                    return logo;
                }
            }
        } catch (Exception e) {
            System.err.println("[KartuPelajarView] Gagal memuat logo: " + e.getMessage());
        }
        return null;
    }

    // ================= Komponen =================

    private static VBox kartuKosong() {
        VBox kartu = new VBox();
        kartu.setPrefSize(LEBAR_KARTU, TINGGI_KARTU);
        kartu.setMinSize(LEBAR_KARTU, TINGGI_KARTU);
        kartu.setMaxSize(LEBAR_KARTU, TINGGI_KARTU);
        kartu.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff 0%, #eef7f4 100%);"
                + "-fx-background-radius: 24; -fx-border-color: " + GARIS + ";"
                + "-fx-border-radius: 24; -fx-border-width: 1.5;"
                + "-fx-effect: dropshadow(gaussian, rgba(12,90,51,0.20), 14, 0, 0, 4);");
        Rectangle klip = new Rectangle(LEBAR_KARTU, TINGGI_KARTU);
        klip.setArcWidth(48);
        klip.setArcHeight(48);
        kartu.setClip(klip);
        return kartu;
    }

    /** Emblem: logo resmi bila ada, vektor segilima+kitab+bintang bila tidak. */
    static javafx.scene.Node buatLogo(double ukuran, Image logo) {
        if (logo != null && !logo.isError()) {
            ImageView gambar = new ImageView(logo);
            gambar.setFitWidth(ukuran);
            gambar.setFitHeight(ukuran);
            gambar.setPreserveRatio(true);
            gambar.setSmooth(true);
            return gambar;
        }
        double r = ukuran / 2;
        Polygon segiLima = poligonBeraturan(5, r, -90);
        segiLima.setFill(Color.web(HIJAU_TUA));
        segiLima.setStroke(Color.web(EMAS));
        segiLima.setStrokeWidth(Math.max(2, ukuran * 0.04));

        double s = ukuran;
        Polygon kitabKiri = new Polygon(
                -0.19 * s, -0.05 * s,
                0.0, -0.015 * s,
                0.0, 0.095 * s,
                -0.19 * s, 0.06 * s);
        Polygon kitabKanan = new Polygon(
                0.19 * s, -0.05 * s,
                0.0, -0.015 * s,
                0.0, 0.095 * s,
                0.19 * s, 0.06 * s);
        kitabKiri.setFill(Color.web(EMAS));
        kitabKanan.setFill(Color.web(EMAS_TERANG));
        Line punggung = new Line(0, -0.015 * s, 0, 0.095 * s);
        punggung.setStroke(Color.web(HIJAU_TUA));
        punggung.setStrokeWidth(Math.max(1.5, s * 0.018));

        Polygon bintang = bintangLima(0, -0.30 * s, 0.075 * s);
        bintang.setFill(Color.web(EMAS_TERANG));

        return new javafx.scene.Group(segiLima, kitabKiri, kitabKanan, punggung, bintang);
    }

    private static VBox barisField(String caption, String nilai) {
        Label cap = new Label(caption);
        cap.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + HIJAU_TUA + ";");
        Label val = new Label((nilai == null || nilai.isBlank()) ? "-" : nilai);
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TEKS_GELAP + ";");
        val.setMaxWidth(340);
        val.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        HBox bar = new HBox(val);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 0, 10));
        bar.setMinHeight(21);
        bar.setPrefHeight(21);
        bar.setMaxHeight(21);
        bar.setStyle("-fx-background-color: " + ABU_BAR + "; -fx-background-radius: 8;");
        return new VBox(1, cap, bar);
    }

    private static StackPane buatBingkaiFoto(Image foto, String nama, double lebar, double tinggi) {
        StackPane bingkai = new StackPane();
        bingkai.setMinSize(lebar + 6, tinggi + 6);
        bingkai.setPrefSize(lebar + 6, tinggi + 6);
        bingkai.setMaxSize(lebar + 6, tinggi + 6);
        bingkai.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                + "-fx-border-color: " + GARIS + "; -fx-border-radius: 12; -fx-border-width: 1;"
                + "-fx-effect: dropshadow(gaussian, rgba(12,90,51,0.15), 6, 0, 0, 2);");
        bingkai.setPadding(new Insets(3));

        if (foto != null && !foto.isError()) {
            ImageView gambar = new ImageView(foto);
            gambar.setFitWidth(lebar);
            gambar.setFitHeight(tinggi);
            gambar.setPreserveRatio(false);
            gambar.setSmooth(true);
            AvatarUtil.pasangViewportPersegi(gambar, foto);
            Rectangle klip = new Rectangle(lebar, tinggi);
            klip.setArcWidth(18);
            klip.setArcHeight(18);
            gambar.setClip(klip);
            bingkai.getChildren().add(gambar);
        } else {
            StackPane inisialBox = new StackPane();
            inisialBox.setMinSize(lebar, tinggi);
            inisialBox.setPrefSize(lebar, tinggi);
            inisialBox.setMaxSize(lebar, tinggi);
            inisialBox.setStyle("-fx-background-color: " + HIJAU_TUA + "; -fx-background-radius: 9;");
            Label inisial = new Label(inisial(nama));
            inisial.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: white;");
            inisialBox.getChildren().add(inisial);
            bingkai.getChildren().add(inisialBox);
        }
        return bingkai;
    }

    // ================= Util bentuk & teks =================

    private static Polygon poligonBeraturan(int sisi, double radius, double sudutAwalDerajat) {
        Polygon poligon = new Polygon();
        for (int i = 0; i < sisi; i++) {
            double sudut = Math.toRadians(sudutAwalDerajat + i * 360.0 / sisi);
            poligon.getPoints().addAll(
                    radius * Math.cos(sudut), radius * Math.sin(sudut));
        }
        return poligon;
    }

    private static Polygon bintangLima(double cx, double cy, double radiusLuar) {
        Polygon bintang = new Polygon();
        double radiusDalam = radiusLuar * 0.42;
        for (int i = 0; i < 10; i++) {
            double radius = (i % 2 == 0) ? radiusLuar : radiusDalam;
            double sudut = Math.toRadians(-90 + i * 36);
            bintang.getPoints().addAll(
                    cx + radius * Math.cos(sudut), cy + radius * Math.sin(sudut));
        }
        return bintang;
    }

    private static String inisial(String nama) {
        if (nama == null || nama.isBlank()) {
            return "?";
        }
        String[] bagian = nama.trim().split("\\s+");
        StringBuilder hasil = new StringBuilder();
        for (int i = 0; i < Math.min(2, bagian.length); i++) {
            if (!bagian[i].isEmpty()) {
                hasil.append(Character.toUpperCase(bagian[i].charAt(0)));
            }
        }
        return hasil.length() > 0 ? hasil.toString() : "?";
    }

    private static String kapital(String teks) {
        return teks == null ? "-" : teks.toUpperCase(ID);
    }
}
