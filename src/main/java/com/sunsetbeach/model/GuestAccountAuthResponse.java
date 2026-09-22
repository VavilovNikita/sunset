package com.sunsetbeach.model;

/**
 * Body of POST /guest-auth/verify and POST /guest-auth/login (and PATCH /guest/password): the
 * guest JWT to send as {@code Authorization: Bearer <token>} on subsequent GuestAccount-tagged
 * requests, plus the safe account projection (no passwordHash, no verification token).
 */
public class GuestAccountAuthResponse {

    private String token;
    private GuestAccount account;

    public GuestAccountAuthResponse(String token, GuestAccount account) {
        this.token = token;
        this.account = account;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public GuestAccount getAccount() {
        return account;
    }

    public void setAccount(GuestAccount account) {
        this.account = account;
    }
}
