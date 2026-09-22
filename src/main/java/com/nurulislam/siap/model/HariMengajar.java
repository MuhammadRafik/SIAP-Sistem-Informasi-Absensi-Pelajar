package com.nurulislam.siap.model;

import java.time.DayOfWeek;

/**
 * Hari mengajar, sesuai kolom ENUM tb_jadwal_mengajar.hari.
 * Dipakai untuk mencocokkan jadwal mengajar guru dengan hari ini
 * (lihat JadwalMengajarDAO#findHariIniByGuru).
 */
public enum HariMengajar {
    SENIN, SELASA, RABU, KAMIS, JUMAT, SABTU;

    /**
     * Mengonversi {@link DayOfWeek} (Java, MONDAY..SUNDAY) ke HariMengajar.
     * MINGGU (Sunday) tidak punya jadwal mengajar, dikembalikan null.
     */
    public static HariMengajar dariDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> SENIN;
            case TUESDAY -> SELASA;
            case WEDNESDAY -> RABU;
            case THURSDAY -> KAMIS;
            case FRIDAY -> JUMAT;
            case SATURDAY -> SABTU;
            case SUNDAY -> null;
        };
    }
}