package com.nurulislam.siap.model;

/**
 * Representasi baris tb_tahun_ajaran.
 * Dipakai antara lain oleh Manajemen Data Kelas untuk menampilkan/menetapkan
 * tahun ajaran suatu kelas (mengikuti tahun ajaran yang sedang aktif).
 */
public class TahunAjaran {

    private int tahunAjaranId;
    private String tahun;       // contoh: "2025/2026"
    private String semester;    // "GANJIL" atau "GENAP"
    private boolean statusAktif;

    public TahunAjaran() {
    }

    public TahunAjaran(int tahunAjaranId, String tahun, String semester, boolean statusAktif) {
        this.tahunAjaranId = tahunAjaranId;
        this.tahun = tahun;
        this.semester = semester;
        this.statusAktif = statusAktif;
    }

    public int getTahunAjaranId() {
        return tahunAjaranId;
    }

    public void setTahunAjaranId(int tahunAjaranId) {
        this.tahunAjaranId = tahunAjaranId;
    }

    public String getTahun() {
        return tahun;
    }

    public void setTahun(String tahun) {
        this.tahun = tahun;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public boolean isStatusAktif() {
        return statusAktif;
    }

    public void setStatusAktif(boolean statusAktif) {
        this.statusAktif = statusAktif;
    }

    /** Label ringkas untuk ditampilkan di tabel/label, contoh: "2025/2026 - Ganjil". */
    public String getLabel() {
        String semesterLabel = "GENAP".equalsIgnoreCase(semester) ? "Genap" : "Ganjil";
        return tahun + " - " + semesterLabel;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}