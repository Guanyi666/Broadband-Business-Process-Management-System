package com.bbpms.auth.service.impl;

import com.bbpms.auth.config.AuthProperties;
import com.bbpms.auth.dto.LoginRespVO;
import com.bbpms.user.dto.UserAuthInfoDTO;
import com.bbpms.auth.service.TokenService;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.util.JwtUtils;
import com.bbpms.common.util.RedisUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String ACTIVE_KEY = "auth:active:";
    private static final String REFRESH_KEY = "auth:refresh:";
    private static final String USER_JTI_KEY = "auth:user-jti:";
    private static final String REVOKED_KEY = "auth:revoked:";
    /** Per-user Redis SET of outstanding refresh-token jtis, for logout revocation. */
    private static final String REFRESH_SET_PREFIX = "auth:refresh-set:";

    private final AuthProperties authProperties;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;

    @Override
    public LoginRespVO issue(UserAuthInfoDTO info) {
        long accessTtl = authProperties.getAccessTtlMinutes() * 60L;
        long refreshTtl = authProperties.getRefreshTtlDays() * 86400L;
        String accessToken = jwtUtils.generateAccessToken(info.getUserId(), info.getUsername(),
                info.getRoles(), info.getDataScope(), info.getPermissions());
        String refreshToken = jwtUtils.generateRefreshToken(info.getUserId(), null);
        String jti = jwtUtils.getJti(accessToken);
        Date expiresAt = jwtUtils.getExpiresAt(accessToken);
        long ttl = expiresAt == null ? accessTtl : Math.max(1, (expiresAt.getTime() - System.currentTimeMillis()) / 1000);
        redisUtils.set(ACTIVE_KEY + jti, info.getUserId().toString(), ttl, TimeUnit.SECONDS);
        // refreshToken is HS256-signed (refresh key); parse() is RSA-only for the
        // access token, so it must go through parseRefresh() to extract the jti.
        String refreshJti = jwtUtils.getJti(jwtUtils.parseRefresh(refreshToken));
        redisUtils.set(REFRESH_KEY + refreshJti, info.getUserId().toString(), refreshTtl, TimeUnit.SECONDS);
        redisUtils.set(USER_JTI_KEY + info.getUserId(), jti, refreshTtl, TimeUnit.SECONDS);
        // Index the refresh jti so logout can revoke every outstanding refresh token.
        redisUtils.sAdd(REFRESH_SET_PREFIX + info.getUserId(), refreshJti);
        redisUtils.expire(REFRESH_SET_PREFIX + info.getUserId(), refreshTtl, TimeUnit.SECONDS);

        LoginRespVO vo = new LoginRespVO();
        vo.setAccessToken(accessToken);
        vo.setToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(accessTtl);
        vo.setJti(jti);
        vo.setUserId(info.getUserId());
        vo.setUsername(info.getUsername());
        // Lightweight user summary for the H5 client; the full profile is at /api/auth/me.
        vo.setUser(java.util.Map.of(
                "id", info.getUserId(),
                "username", info.getUsername() == null ? "" : info.getUsername(),
                "name", info.getUsername(),
                "roles", info.getRoles() == null ? java.util.List.of() : info.getRoles(),
                "permissions", info.getPermissions() == null ? java.util.List.of() : info.getPermissions()));
        return vo;
    }

    @Override
    public Claims parseRefresh(String token) {
        return jwtUtils.parseRefresh(token);
    }

    @Override
    public Claims parseAccess(String token) {
        return jwtUtils.parse(token);
    }

    @Override
    public void revoke(String jti, long ttlSeconds) {
        if (jti == null) {
            return;
        }
        redisUtils.set(REVOKED_KEY + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        redisUtils.del(ACTIVE_KEY + jti);
    }

    @Override
    public boolean isRevoked(String jti) {
        return redisUtils.get(REVOKED_KEY + jti) != null;
    }

    @Override
    public String stripBearer(String header) {
        if (header == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        String h = header.trim();
        if (h.toLowerCase().startsWith("bearer ")) {
            return h.substring(7).trim();
        }
        return h;
    }
}