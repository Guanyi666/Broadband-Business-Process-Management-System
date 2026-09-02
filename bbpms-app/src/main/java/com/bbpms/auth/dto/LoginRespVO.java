package com.bbpms.auth.dto;

import lombok.Data;

import java.util.Map;

@Data
public class LoginRespVO {

    private String accessToken;

    /** Alias of {@link #accessToken} — the admin web reads {@code token}. */
    private String token;

    /** Refresh token (single-use; rotated on each refresh). */
    private String refreshToken;

    private Long expiresIn;

    private String jti;

    private Long userId;

    private String username;

    /** Lightweight user summary for the H5 client (login response {@code user}). */
    private Map<String, Object> user;
}