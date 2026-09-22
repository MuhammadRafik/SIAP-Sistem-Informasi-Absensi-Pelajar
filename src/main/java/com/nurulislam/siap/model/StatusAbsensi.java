package com.nurulislam.siap.model;

/**
 * Status kehadiran, dipakai bersama oleh absensi harian (tb_absensi)
 * maupun absensi per mata pelajaran (tb_absensi_mapel).
 */
public enum StatusAbsensi {
    HADIR,
    TERLAMBAT,
    IZIN,
    SAKIT,
    ALFA;

    public String getLabel() {
        return switch (this) {
            case HADIR -> "Hadir";
            case TERLAMBAT -> "Terlambat";
            case IZIN -> "Izin";
            case SAKIT -> "Sakit";
            case ALFA -> "Alfa";
        };
    }
}
