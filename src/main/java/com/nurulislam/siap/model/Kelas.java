package com.nurulislam.siap.model;

/**
 * Representasi baris tb_kelas.
 * <p>
 * Field {@code waliKelasNama}, {@code jumlahMurid}, dan {@code tahunAjaranLabel}
 * bersifat transient (bukan kolom asli tb_kelas) - diisi oleh KelasDAO#findAllDetail
 * lewat JOIN ke tb_pengguna, tb_murid, dan tb_tahun_ajaran agar layar Manajemen
 * Data Kelas tidak perlu query tambahan untuk setiap baris tabel.
 */
public class Kelas {

    private int kelasId;
    private String namaKelas;
    private String tingkat;
    private String jurusan;
    private Integer waliKelasId;
    private int tahunAjaranId;

    private String waliKelasNama;   // hasil JOIN, "-" jika belum ada wali kelas
    private int jumlahMurid;        // hasil agregasi JOIN, jumlah murid berstatus AKTIF
    private String tahunAjaranLabel; // hasil JOIN, contoh: "2025/2026 - Ganjil"

    public Kelas() {
    }

    public Kelas(int kelasId, String namaKelas, String tingkat, String jurusan,
                 Integer waliKelasId, int tahunAjaranId) {
        this.kelasId = kelasId;
        this.namaKelas = namaKelas;
        this.tingkat = tingkat;
        this.jurusan = jurusan;
        this.waliKelasId = waliKelasId;
        this.tahunAjaranId = tahunAjaranId;
    }

    public int getKelasId() {
        return kelasId;
    }

    public void setKelasId(int kelasId) {
        this.kelasId = kelasId;
    }

    public String getNamaKelas() {
        return namaKelas;
    }

    public void setNamaKelas(String namaKelas) {
        this.namaKelas = namaKelas;
    }

    public String getTingkat() {
        return tingkat;
    }

    public void setTingkat(String tingkat) {
        this.tingkat = tingkat;
    }

    public String getJurusan() {
        return jurusan;
    }

    public void setJurusan(String jurusan) {
        this.jurusan = jurusan;
    }

    public Integer getWaliKelasId() {
        return waliKelasId;
    }

    public void setWaliKelasId(Integer waliKelasId) {
        this.waliKelasId = waliKelasId;
    }

    public int getTahunAjaranId() {
        return tahunAjaranId;
    }

    public void setTahunAjaranId(int tahunAjaranId) {
        this.tahunAjaranId = tahunAjaranId;
    }

    public String getWaliKelasNama() {
        return waliKelasNama;
    }

    public void setWaliKelasNama(String waliKelasNama) {
        this.waliKelasNama = waliKelasNama;
    }

    public int getJumlahMurid() {
        return jumlahMurid;
    }

    public void setJumlahMurid(int jumlahMurid) {
        this.jumlahMurid = jumlahMurid;
    }

    public String getTahunAjaranLabel() {
        return tahunAjaranLabel;
    }

    public void setTahunAjaranLabel(String tahunAjaranLabel) {
        this.tahunAjaranLabel = tahunAjaranLabel;
    }

    /** Gabungan Tingkat + Jurusan untuk tampilan ringkas, contoh: "XII - MIPA". */
    public String getTingkatJurusan() {
        if (jurusan == null || jurusan.isBlank()) {
            return tingkat;
        }
        return tingkat + " - " + jurusan;
    }
}