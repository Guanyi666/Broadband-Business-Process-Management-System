package com.bbpms.common.util;

import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT helper. Access token = RS256 (private/public RSA pair, base64-encoded).
 * Refresh token = HS256 (shared secret, base64-encoded). jjwt 0.12.5 API.
 */
public class JwtUtils {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final SecretKey refreshKey;

    public JwtUtils(String privateKeyBase64, String publicKeyBase64, String refreshSecretBase64) {
        try {
            byte[] priv = Decoders.BASE64.decode(privateKeyBase64);
            byte[] pub = Decoders.BASE64.decode(publicKeyBase64);
            this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(priv));
            this.publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pub));
            this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecretBase64));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init JWT keys", e);
        }
    }

    /** access token TTL: 30 min */
    private static final long ACCESS_TTL_MS = 30 * 60 * 1000L;
    /** refresh token TTL: 7 days */
    private static final long REFRESH_TTL_MS = 7L * 24 * 60 * 60 * 1000L;

    public String generateAccessToken(Long userId, String username, List<String> roles,
                                      Integer dataScope, List<String> permissions) {
        String jti = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("usr", username);
        claims.put("roles", roles);
        claims.put("perms", permissions);
        claims.put("scope", dataScope);
        claims.put("jti", jti);
        claims.put("typ", "access");
        JwtBuilder b = Jwts.builder().claims(claims).subject(String.valueOf(userId))
                .issuedAt(new Date(now)).expiration(new Date(now + ACCESS_TTL_MS)).id(jti);
        return b.signWith(privateKey, Jwts.SIG.RS256).compact();
    }

    public String generateRefreshToken(Long userId, String deviceId) {
        long now = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("dev", deviceId);
        claims.put("typ", "refresh");
        JwtBuilder b = Jwts.builder().claims(claims).subject(String.valueOf(userId))
                .issuedAt(new Date(now)).expiration(new Date(now + REFRESH_TTL_MS))
                .id(UUID.randomUUID().toString());
        return b.signWith(refreshKey, Jwts.SIG.HS256).compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith((PublicKey) publicKey).build().parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            // Business exception so GlobalExceptionHandler maps it to a clean
            // 401-style R response instead of an unhandled 500.
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
    }

    public Claims parseRefresh(String token) {
        try {
            return Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
    }

    public String getJti(Claims c) {
        Object j = c.get("jti");
        return j == null ? null : j.toString();
    }

    public String getJti(String token) { return getJti(parse(token)); }

    public Date getExpiresAt(Claims c) { return c.getExpiration(); }

    public Date getExpiresAt(String token) { return getExpiresAt(parse(token)); }

    /** helper for refresh parsing with raw token */
    private Key publicKeyAsKey() { return publicKey; }
}
