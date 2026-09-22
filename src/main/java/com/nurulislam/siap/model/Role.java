package com.nurulislam.siap.model;

/**
 * Peran pengguna sistem, sesuai Batasan Masalah pada proposal:
 * "Pengguna sistem terdiri atas Staf TU dan Guru Mata Pelajaran".
 */
public enum Role {
    TU("Staf TU"),
    GURU("Guru Mata Pelajaran");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
