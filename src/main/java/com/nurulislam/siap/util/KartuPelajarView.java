package com.nurulislam.siap.util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.Locale;

/**
 * Menyusun Kartu Pelajar MA Nurul Islam di atas template resmi
 * (tidak menggambar ulang desain).
 * <p>
 * Template: {@code /com/nurulislam/siap/images/kartu-depan.png} (815x980)
 * dan {@code kartu-belakang.png} (730x980). Kode ini hanya menempelkan
 * foto + data murid pada posisi yang pas di template depan, dan QR Code
 * pada template belakang. Render memakai resolusi asli template agar
 * tajam saat dicetak/disimpan PNG.
 */
public final class KartuPelajarView {

    /** Ukuran template depan (px). */
    public static final double LEBAR_DEPAN = 815;
    public static final double TINGGI_DEPAN = 980;
    /** Ukuran template belakang (px). */
    public static final double LEBAR_BELAKANG = 730;
    public static final double TINGGI_BELAKANG = 980;

    private static final String TEMPLATE_DEPAN = "/com/nurulislam/siap/images/kartu-depan.png";
    private static final String TEMPLATE_BELAKANG = "/com/nurulislam/siap/images/kartu-belakang.png";

    private static final String HIJAU_TUA = "#0c5a33";
    private static final String TEKS_GELAP = "#1c2b24";
    private static final String ABU_BAR = "#edf1f0";

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
     * @param logo       tidak dipakai lagi (logo sudah ada di template),
     *                   dipertahankan agar pemanggil lama tetap kompilasi
     */
    public static Pane buatDepan(String nama, String nis, String ttl,
                                 String jurusan, String tahunMasuk,
                                 Image foto, Image logo) {
        Pane kartu = dasar(TEMPLATE_DEPAN, LEBAR_DEPAN, TINGGI_DEPAN);
        if (kartu == null) {
            return null;
        }

        // Foto: x 70, y 360, 190x255.
        kartu.getChildren().add(bingkaiFoto(foto, nama, 70, 360, 190, 255, 28));

        // Lima baris field: caption + bar nilai (x 355, lebar 360).
        double[] captionY = {353, 434, 515, 597, 678};
        String[] nilai = {kapital(nama), nis, ttl, jurusan, tahunMasuk};
        for (int i = 0; i < 5; i++) {
            kartu.getChildren().addAll(barisField(caption(i), nilai[i], 355, captionY[i], 360));
        }
        return kartu;
    }

    /** Sisi belakang: QR Code di atas template belakang. */
    public static Pane buatBelakang(Image qr, Image logo) {
        Pane kartu = dasar(TEMPLATE_BELAKANG, LEBAR_BELAKANG, TINGGI_BELAKANG);
        if (kartu == null || qr == null || qr.isError()) {
            return kartu;
        }

        // Bingkai putih + garis hijau, QR di tengahnya.
        double kotak = 400;
        double x = (LEBAR_BELAKANG - kotak) / 2;
        double y = 395;
        StackPane bingkai = new StackPane();
        bingkai.setLayoutX(x);
        bingkai.setLayoutY(y);
        bingkai.setMinSize(kotak, kotak);
        bingkai.setPrefSize(kotak, kotak);
        bingkai.setMaxSize(kotak, kotak);
        bingkai.setStyle("-fx-background-color: white; -fx-background-radius: 24;"
                + "-fx-border-color: " + HIJAU_TUA + "; -fx-border-width: 3;"
                + "-fx-border-radius: 24;");
        bingkai.setPadding(new javafx.geometry.Insets(18));
        ImageView gambarQr = new ImageView(qr);
        gambarQr.setFitWidth(kotak - 36);
        gambarQr.setFitHeight(kotak - 36);
        gambarQr.setPreserveRatio(true);
        gambarQr.setSmooth(false);
        bingkai.getChildren().add(gambarQr);
        kartu.getChildren().add(bingkai);
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

    /** Pane dasar: gambar template full-bleed. Null bila template hilang. */
    private static Pane dasar(String resource, double lebar, double tinggi) {
        Image template = muatTemplate(resource);
        if (template == null || template.isError()) {
            System.err.println("[KartuPelajarView] Template tidak ditemukan: " + resource);
            return null;
        }
        Pane kartu = new Pane();
        kartu.setMinSize(lebar, tinggi);
        kartu.setPrefSize(lebar, tinggi);
        kartu.setMaxSize(lebar, tinggi);
        ImageView latar = new ImageView(template);
        latar.setFitWidth(lebar);
        latar.setFitHeight(tinggi);
        latar.setPreserveRatio(false);
        latar.setSmooth(true);
        kartu.getChildren().add(latar);
        return kartu;
    }

    private static Image muatTemplate(String resource) {
        try (InputStream in = KartuPelajarView.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return new Image(in);
        } catch (Exception e) {
            System.err.println("[KartuPelajarView] Gagal memuat template: " + e.getMessage());
            return null;
        }
    }

    private static String caption(int i) {
        return switch (i) {
            case 0 -> "NAMA";
            case 1 -> "NISN";
            case 2 -> "TEMPAT/TANGGAL LAHIR";
            case 3 -> "JURUSAN";
            default -> "TAHUN MASUK";
        };
    }

    private static Node[] barisField(String caption, String nilai, double x, double y, double lebar) {
        Label cap = new Label(caption);
        cap.setLayoutX(x);
        cap.setLayoutY(y);
        cap.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + HIJAU_TUA + ";");

        Pane bar = new Pane();
        bar.setLayoutX(x);
        bar.setLayoutY(y + 27);
        bar.setMinSize(lebar, 30);
        bar.setPrefSize(lebar, 30);
        bar.setMaxSize(lebar, 30);
        bar.setStyle("-fx-background-color: " + ABU_BAR + "; -fx-background-radius: 8;");
        Label val = new Label((nilai == null || nilai.isBlank()) ? "-" : nilai);
        val.setLayoutX(12);
        val.setLayoutY(4);
        val.setMaxWidth(lebar - 24);
        val.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: " + TEKS_GELAP + ";");
        val.setTextOverrun(OverrunStyle.ELLIPSIS);
        bar.getChildren().add(val);
        return new Node[]{cap, bar};
    }

    private static StackPane bingkaiFoto(Image foto, String nama,
                                         double x, double y, double lebar, double tinggi, double arc) {
        StackPane bingkai = new StackPane();
        bingkai.setLayoutX(x);
        bingkai.setLayoutY(y);
        bingkai.setMinSize(lebar, tinggi);
        bingkai.setPrefSize(lebar, tinggi);
        bingkai.setMaxSize(lebar, tinggi);
        bingkai.setStyle("-fx-background-color: white; -fx-background-radius: " + (int) arc + ";");

        if (foto != null && !foto.isError()) {
            ImageView gambar = new ImageView(foto);
            gambar.setFitWidth(lebar);
            gambar.setFitHeight(tinggi);
            gambar.setPreserveRatio(false);
            gambar.setSmooth(true);
            AvatarUtil.pasangViewportPersegi(gambar, foto);
            Rectangle klip = new Rectangle(lebar, tinggi);
            klip.setArcWidth(arc);
            klip.setArcHeight(arc);
            gambar.setClip(klip);
            bingkai.getChildren().add(gambar);
        } else {
            StackPane inisialBox = new StackPane();
            inisialBox.setMinSize(lebar, tinggi);
            inisialBox.setPrefSize(lebar, tinggi);
            inisialBox.setMaxSize(lebar, tinggi);
            inisialBox.setStyle("-fx-background-color: " + HIJAU_TUA + ";");
            Label inisial = new Label(inisial(nama));
            inisial.setStyle("-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: white;");
            inisialBox.getChildren().add(inisial);
            // Ikuti lengkung bingkai luar.
            Rectangle klip = new Rectangle(lebar, tinggi);
            klip.setArcWidth(arc);
            klip.setArcHeight(arc);
            inisialBox.setClip(klip);
            bingkai.getChildren().add(inisialBox);
        }

        // Potong apapun yang keluar dari bingkai.
        Rectangle klipLuar = new Rectangle(lebar, tinggi);
        klipLuar.setArcWidth(arc);
        klipLuar.setArcHeight(arc);
        bingkai.setClip(klipLuar);
        return bingkai;
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
