package com.nurulislam.siap.model;

import java.time.LocalDateTime;

/**
 * Merepresentasikan satu baris pada tabel tb_pengguna.
 * Gabungan aktor Staf TU dan Guru Mata Pelajaran, dibedakan lewat field role.
 */
public class Pengguna {

    private Integer penggunaId;
    private String email;
    private String passwordHash;
    private String nama;
    private Role role;
    private StatusAkun statusAkun;
    private LocalDateTime createdAt;

    public Pengguna() {
    }

    public Pengguna(String email, String passwordHash, String nama, Role role, StatusAkun statusAkun) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nama = nama;
        this.role = role;
        this.statusAkun = statusAkun;
    }

    public Pengguna(Integer penggunaId, String email, String passwordHash, String nama,
                     Role role, StatusAkun statusAkun, LocalDateTime createdAt) {
        this.penggunaId = penggunaId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nama = nama;
        this.role = role;
        this.statusAkun = statusAkun;
        this.createdAt = createdAt;
    }

    public Integer getPenggunaId() {
        return penggunaId;
    }

    public void setPenggunaId(Integer penggunaId) {
        this.penggunaId = penggunaId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public StatusAkun getStatusAkun() {
        return statusAkun;
    }

    public void setStatusAkun(StatusAkun statusAkun) {
        this.statusAkun = statusAkun;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isTU() {
        return role == Role.TU;
    }

    public boolean isGuru() {
        return role == Role.GURU;
    }

    @Override
    public String toString() {
        return "Pengguna{id=" + penggunaId + ", nama='" + nama + "', email='" + email
                + "', role=" + role + ", status=" + statusAkun + "}";
    }
}
