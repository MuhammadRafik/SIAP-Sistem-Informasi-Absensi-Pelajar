package com.nurulislam.siap.model;

import java.time.LocalDate;

/**
 * Representasi baris tb_murid.
 * <p>
 * Field {@code namaKelas} bersifat transient (bukan kolom asli tb_murid) -
 * diisi oleh MuridDAO lewat JOIN ke tb_kelas agar controller tidak perlu
 * query kedua hanya untuk menampilkan nama kelas di layar Scan QR / Data Murid.
 */
public class Murid {

    private int muridId;
    private String nis;
    private String nama;
    private String jenisKelamin; // "L" atau "P"
    private LocalDate tanggalLahir;
    private String foto;
    private int kelasId;
    private String qrToken;
    private Statusmurid status;

    private String namaKelas; // hasil JOIN, boleh null jika tidak di-set

    public Murid() {
    }

    public Murid(int muridId, String nis, String nama, String jenisKelamin, LocalDate tanggalLahir,
                 String foto, int kelasId, String qrToken, Statusmurid status) {
        this.muridId = muridId;
        this.nis = nis;
        this.nama = nama;
        this.jenisKelamin = jenisKelamin;
        this.tanggalLahir = tanggalLahir;
        this.foto = foto;
        this.kelasId = kelasId;
        this.qrToken = qrToken;
        this.status = status;
    }

    public int getMuridId() {
        return muridId;
    }

    public void setMuridId(int muridId) {
        this.muridId = muridId;
    }

    public String getNis() {
        return nis;
    }

    public void setNis(String nis) {
        this.nis = nis;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getJenisKelamin() {
        return jenisKelamin;
    }

    public void setJenisKelamin(String jenisKelamin) {
        this.jenisKelamin = jenisKelamin;
    }

    public LocalDate getTanggalLahir() {
        return tanggalLahir;
    }

    public void setTanggalLahir(LocalDate tanggalLahir) {
        this.tanggalLahir = tanggalLahir;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public int getKelasId() {
        return kelasId;
    }

    public void setKelasId(int kelasId) {
        this.kelasId = kelasId;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public Statusmurid getStatus() {
        return status;
    }

    public void setStatus(Statusmurid status) {
        this.status = status;
    }

    public String getNamaKelas() {
        return namaKelas;
    }

    public void setNamaKelas(String namaKelas) {
        this.namaKelas = namaKelas;
    }
}