package com.nurulislam.siap.model;

/**
 * Representasi baris tb_mata_pelajaran.
 * Dipakai oleh layar Manajemen Mata Pelajaran &amp; Jadwal Mengajar (Staf TU)
 * dan sebagai referensi (JOIN) oleh JadwalMengajar/AbsensiMapel.
 */
public class MataPelajaran {

    private int mapelId;
    private String kodeMapel;
    private String namaMapel;

    public MataPelajaran() {
    }

    public MataPelajaran(int mapelId, String kodeMapel, String namaMapel) {
        this.mapelId = mapelId;
        this.kodeMapel = kodeMapel;
        this.namaMapel = namaMapel;
    }

    public int getMapelId() {
        return mapelId;
    }

    public void setMapelId(int mapelId) {
        this.mapelId = mapelId;
    }

    public String getKodeMapel() {
        return kodeMapel;
    }

    public void setKodeMapel(String kodeMapel) {
        this.kodeMapel = kodeMapel;
    }

    public String getNamaMapel() {
        return namaMapel;
    }

    public void setNamaMapel(String namaMapel) {
        this.namaMapel = namaMapel;
    }

    @Override
    public String toString() {
        return kodeMapel + " - " + namaMapel;
    }
}