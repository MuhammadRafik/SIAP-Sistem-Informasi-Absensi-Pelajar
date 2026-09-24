package com.nurulislam.siap.util;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Format tanggal Indonesia (tanggal-bulan-tahun) untuk DatePicker.
 * Bawaan JavaFX mengikuti locale US (bulan/tanggal/tahun) sehingga
 * membingungkan; helper ini menyeragamkan tampilan + ketikan manual.
 */
public final class TanggalUtil {

    /** Contoh: 24-09-2026. */
    public static final DateTimeFormatter FORMAT_ID = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TanggalUtil() {
    }

    /**
     * @param picker DatePicker yang diformat (boleh null, aman diabaikan)
     */
    public static void pasangFormatIndonesia(DatePicker picker) {
        if (picker == null) {
            return;
        }
        picker.setPromptText("tt-bb-tttt");
        LocalDate nilaiAwal = picker.getValue();
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate tanggal) {
                return tanggal == null ? "" : FORMAT_ID.format(tanggal);
            }

            @Override
            public LocalDate fromString(String teks) {
                if (teks == null || teks.isBlank()) {
                    return null;
                }
                String t = teks.trim();
                try {
                    return LocalDate.parse(t, FORMAT_ID);
                } catch (DateTimeParseException e) {
                    // Coba lentur: "1-9-2026", "1/9/2026", "1.9.2026".
                }
                String[] p = t.split("[-/. ]+");
                if (p.length == 3) {
                    try {
                        int hari = Integer.parseInt(p[0]);
                        int bulan = Integer.parseInt(p[1]);
                        int tahun = Integer.parseInt(p[2]);
                        if (tahun < 100) {
                            tahun += 2000;
                        }
                        return LocalDate.of(tahun, bulan, hari);
                    } catch (Exception ex) {
                        // Jatuh ke bawah: pertahankan nilai lama.
                    }
                }
                // Ketikan tidak dikenali: jangan kosongkan filter,
                // pertahankan tanggal terakhir yang valid.
                return picker.getValue();
            }
        });
        // Pasang converter setelah value terisi tidak me-refresh editor,
        // jadi picu sekali agar teks langsung tampil terformat.
        picker.setValue(null);
        picker.setValue(nilaiAwal);
    }
}
