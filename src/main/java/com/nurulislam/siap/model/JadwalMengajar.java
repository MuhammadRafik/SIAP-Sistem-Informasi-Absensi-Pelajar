package com.nurulislam.siap.model;

import java.time.LocalTime;

/**
 * Representasi baris tb_jadwal_mengajar.
 * <p>
 * Field {@code namaMapel}, {@code kodeMapel}, {@code namaKelas}, {@code namaGuru}
 * bersifat transient - diisi oleh JadwalMengajarDAO lewat JOIN ke tb_mata_pelajaran,
 * tb_kelas, dan tb_pengguna agar controller Absensi Mata Pelajaran tidak perlu
 * query tambahan untuk menampilkan kartu sesi.
 */
public class JadwalMengajar {

    private int jadwalId;
    private int mapelId;
    private int kelasId;
    private int guruId;
    private int tahunAjaranId;
    private HariMengajar hari;
    private LocalTime jamMulai;
    private LocalTime jamSelesai;

    private String namaMapel;
    private String kodeMapel;
    private String namaKelas;
    private String namaGuru;

    public JadwalMengajar() {
    }

    public int getJadwalId() {
        return jadwalId;
    }

    public void setJadwalId(int jadwalId) {
        this.jadwalId = jadwalId;
    }

    public int getMapelId() {
        return mapelId;
    }

    public void setMapelId(int mapelId) {
        this.mapelId = mapelId;
    }

    public int getKelasId() {
        return kelasId;
    }

    public void setKelasId(int kelasId) {
        this.kelasId = kelasId;
    }

    public int getGuruId() {
        return guruId;
    }

    public void setGuruId(int guruId) {
        this.guruId = guruId;
    }

    public int getTahunAjaranId() {
        return tahunAjaranId;
    }

    public void setTahunAjaranId(int tahunAjaranId) {
        this.tahunAjaranId = tahunAjaranId;
    }

    public HariMengajar getHari() {
        return hari;
    }

    public void setHari(HariMengajar hari) {
        this.hari = hari;
    }

    public LocalTime getJamMulai() {
        return jamMulai;
    }

    public void setJamMulai(LocalTime jamMulai) {
        this.jamMulai = jamMulai;
    }

    public LocalTime getJamSelesai() {
        return jamSelesai;
    }

    public void setJamSelesai(LocalTime jamSelesai) {
        this.jamSelesai = jamSelesai;
    }

    public String getNamaMapel() {
        return namaMapel;
    }

    public void setNamaMapel(String namaMapel) {
        this.namaMapel = namaMapel;
    }

    public String getKodeMapel() {
        return kodeMapel;
    }

    public void setKodeMapel(String kodeMapel) {
        this.kodeMapel = kodeMapel;
    }

    public String getNamaKelas() {
        return namaKelas;
    }

    public void setNamaKelas(String namaKelas) {
        this.namaKelas = namaKelas;
    }

    public String getNamaGuru() {
        return namaGuru;
    }

    public void setNamaGuru(String namaGuru) {
        this.namaGuru = namaGuru;
    }

    /**
     * Menentukan status sesi berdasarkan waktu.
     *
     * Aturan:
     * - sebelum jam mulai  -> BELUM_MULAI
     * - tepat pada jam selesai atau setelahnya -> SELESAI
     * - di antara jam mulai dan sebelum jam selesai -> AKTIF
     *
     * Contoh jadwal 08:00-10:00:
     * 07:59:59 -> BELUM_MULAI
     * 08:00:00 -> AKTIF
     * 09:59:59 -> AKTIF
     * 10:00:00 -> SELESAI
     */
    public StatusSesi statusPada(LocalTime sekarang) {
        if (sekarang.isBefore(jamMulai)) {
            return StatusSesi.BELUM_MULAI;
        }

        if (!sekarang.isBefore(jamSelesai)) {
            return StatusSesi.SELESAI;
        }

        return StatusSesi.AKTIF;
    }

    public enum StatusSesi {
        BELUM_MULAI("Belum Mulai"),
        AKTIF("Aktif"),
        SELESAI("Selesai");

        private final String label;

        StatusSesi(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Override
    public String toString() {
        return (namaMapel != null ? namaMapel : "Mapel") + " - " + (namaKelas != null ? namaKelas : "Kelas")
                + " (" + jamMulai + "-" + jamSelesai + ")";
    }
}