package com.nurulislam.siap.model;

import java.time.LocalTime;

/**
 * Representasi baris tb_sesi_absensi (mis. "Sesi Pagi", jam masuk 07:00,
 * batas terlambat 07:15). Dipakai halaman Scan QR untuk menentukan status
 * HADIR/TERLAMBAT berdasarkan waktu scan.
 */
public class Sesiabsensi {

    private int sesiAbsensiId;
    private String namaSesi;
    private LocalTime jamMasuk;
    private LocalTime batasTerlambat;

    public Sesiabsensi() {
    }

    public Sesiabsensi(int sesiAbsensiId, String namaSesi, LocalTime jamMasuk, LocalTime batasTerlambat) {
        this.sesiAbsensiId = sesiAbsensiId;
        this.namaSesi = namaSesi;
        this.jamMasuk = jamMasuk;
        this.batasTerlambat = batasTerlambat;
    }

    public int getSesiAbsensiId() {
        return sesiAbsensiId;
    }

    public void setSesiAbsensiId(int sesiAbsensiId) {
        this.sesiAbsensiId = sesiAbsensiId;
    }

    public String getNamaSesi() {
        return namaSesi;
    }

    public void setNamaSesi(String namaSesi) {
        this.namaSesi = namaSesi;
    }

    public LocalTime getJamMasuk() {
        return jamMasuk;
    }

    public void setJamMasuk(LocalTime jamMasuk) {
        this.jamMasuk = jamMasuk;
    }

    public LocalTime getBatasTerlambat() {
        return batasTerlambat;
    }

    public void setBatasTerlambat(LocalTime batasTerlambat) {
        this.batasTerlambat = batasTerlambat;
    }

    /** Tentukan status HADIR/TERLAMBAT berdasarkan waktu scan aktual. */
    public StatusAbsensi tentukanStatus(LocalTime waktuScan) {
        return waktuScan.isAfter(batasTerlambat) ? StatusAbsensi.TERLAMBAT : StatusAbsensi.HADIR;
    }
}