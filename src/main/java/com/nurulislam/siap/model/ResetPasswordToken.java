package com.nurulislam.siap.model;

import java.time.LocalDateTime;

/**
 * Merepresentasikan satu baris pada tabel tb_reset_password_token.
 * Dipakai pada mekanisme "Lupa Password" di halaman Login.
 */
public class ResetPasswordToken {

    private Integer tokenId;
    private Integer penggunaId;
    private String token;
    private LocalDateTime expiredAt;

    public ResetPasswordToken() {
    }

    public ResetPasswordToken(Integer penggunaId, String token, LocalDateTime expiredAt) {
        this.penggunaId = penggunaId;
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public ResetPasswordToken(Integer tokenId, Integer penggunaId, String token, LocalDateTime expiredAt) {
        this.tokenId = tokenId;
        this.penggunaId = penggunaId;
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public Integer getTokenId() {
        return tokenId;
    }

    public void setTokenId(Integer tokenId) {
        this.tokenId = tokenId;
    }

    public Integer getPenggunaId() {
        return penggunaId;
    }

    public void setPenggunaId(Integer penggunaId) {
        this.penggunaId = penggunaId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public boolean isExpired() {
        return expiredAt != null && expiredAt.isBefore(LocalDateTime.now());
    }
}
