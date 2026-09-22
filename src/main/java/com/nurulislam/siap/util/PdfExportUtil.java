package com.nurulislam.siap.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Mengekspor data laporan ke file PDF berbentuk tabel (header berwarna,
 * garis pembatas sel, dan nomor halaman otomatis).
 */
public final class PdfExportUtil {

    private static final DateTimeFormatter FORMAT_CETAK =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private PdfExportUtil() {
    }

    /**
     * @param file          file tujuan (biasanya dipilih lewat FileChooser, berekstensi .pdf)
     * @param judul         judul laporan, mis. "Laporan Kehadiran Harian - MA Nurul Islam"
     * @param subjudul      ringkasan filter, mis. rentang tanggal + kelas + status
     * @param header        nama kolom tabel
     * @param baris         isi baris (satu array String per baris, panjang sama dengan header)
     * @param lebarRelatif  bobot lebar tiap kolom (boleh null = rata)
     * @param landscape     true untuk A4 landscape, false untuk portrait
     */
    public static void eksporTabel(File file, String judul, String subjudul,
                                   String[] header, List<String[]> baris,
                                   float[] lebarRelatif, boolean landscape) throws Exception {
        Rectangle ukuran = landscape ? PageSize.A4.rotate() : PageSize.A4;
        Document dokumen = new Document(ukuran, 36, 36, 40, 40);
        PdfWriter penulis = PdfWriter.getInstance(dokumen, new FileOutputStream(file));
        penulis.setPageEvent(new NomorHalaman());
        dokumen.addTitle(judul);
        dokumen.addAuthor("SIAP - MA Nurul Islam");
        dokumen.open();

        try {
            Font fontJudul = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(0x19, 0x1c, 0x1e));
            Paragraph pJudul = new Paragraph(judul, fontJudul);
            pJudul.setAlignment(Element.ALIGN_CENTER);
            pJudul.setSpacingAfter(4);
            dokumen.add(pJudul);

            if (subjudul != null && !subjudul.isBlank()) {
                Font fontSub = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(0x4b, 0x55, 0x63));
                Paragraph pSub = new Paragraph(subjudul, fontSub);
                pSub.setAlignment(Element.ALIGN_CENTER);
                pSub.setSpacingAfter(4);
                dokumen.add(pSub);
            }

            Font fontMeta = new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(0x6b, 0x70, 0x76));
            Paragraph pMeta = new Paragraph(
                    "Diekspor melalui SIAP MA Nurul Islam pada " + LocalDateTime.now().format(FORMAT_CETAK),
                    fontMeta);
            pMeta.setAlignment(Element.ALIGN_CENTER);
            pMeta.setSpacingAfter(12);
            dokumen.add(pMeta);

            PdfPTable tabel = new PdfPTable(header.length);
            tabel.setWidthPercentage(100);
            if (lebarRelatif != null && lebarRelatif.length == header.length) {
                tabel.setWidths(lebarRelatif);
            }
            tabel.setHeaderRows(1);

            Font fontHeader = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Color latarHeader = new Color(0x00, 0x6a, 0x65);
            for (String h : header) {
                PdfPCell sel = new PdfPCell(new Paragraph(h == null ? "" : h, fontHeader));
                sel.setBackgroundColor(latarHeader);
                sel.setHorizontalAlignment(Element.ALIGN_CENTER);
                sel.setVerticalAlignment(Element.ALIGN_MIDDLE);
                sel.setPadding(6);
                tabel.addCell(sel);
            }

            Font fontIsi = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(0x19, 0x1c, 0x1e));
            Color latarGanjil = Color.WHITE;
            Color latarGenap = new Color(0xf3, 0xf4, 0xf4);
            for (int r = 0; r < baris.size(); r++) {
                String[] nilai = baris.get(r);
                for (int c = 0; c < header.length; c++) {
                    String teks = (nilai != null && c < nilai.length && nilai[c] != null) ? nilai[c] : "-";
                    PdfPCell sel = new PdfPCell(new Paragraph(teks, fontIsi));
                    sel.setBackgroundColor(r % 2 == 0 ? latarGanjil : latarGenap);
                    sel.setPadding(5);
                    tabel.addCell(sel);
                }
            }

            dokumen.add(tabel);

            Font fontJumlah = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(0x19, 0x1c, 0x1e));
            Paragraph pJumlah = new Paragraph("Total data: " + baris.size(), fontJumlah);
            pJumlah.setAlignment(Element.ALIGN_RIGHT);
            pJumlah.setSpacingBefore(10);
            dokumen.add(pJumlah);
        } finally {
            dokumen.close();
        }
    }

    /** Menampilkan "Halaman X" di kanan bawah setiap halaman. */
    private static class NomorHalaman extends com.lowagie.text.pdf.PdfPageEventHelper {
        @Override
        public void onEndPage(com.lowagie.text.pdf.PdfWriter writer,
                              Document document) {
            try {
                com.lowagie.text.pdf.PdfContentByte kanvas = writer.getDirectContent();
                Font font = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(0x9c, 0xa3, 0xaf));
                Paragraph nomor = new Paragraph("Halaman " + writer.getPageNumber(), font);
                com.lowagie.text.pdf.ColumnText.showTextAligned(
                        kanvas, Element.ALIGN_RIGHT, nomor,
                        document.right(), document.bottom() - 18, 0);
            } catch (RuntimeException ignored) {
            }
        }
    }
}
