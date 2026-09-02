package com.bbpms.auth.service.impl;

import com.bbpms.auth.config.AuthProperties;
import com.bbpms.auth.dto.CaptchaRespVO;
import com.bbpms.auth.dto.LoginReq;
import com.bbpms.auth.dto.LoginRespVO;
import com.bbpms.user.dto.LoginRecordDTO;
import com.bbpms.user.dto.UserAuthInfoDTO;
import com.bbpms.auth.service.AuthService;
import com.bbpms.auth.service.CaptchaService;
import com.bbpms.auth.service.RsaKeyService;
import com.bbpms.auth.service.TokenService;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.util.CryptoUtils;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.service.SysUserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ACTIVE_KEY = "auth:active:";
    private static final String REFRESH_KEY = "auth:refresh:";
    private static final String USER_JTI_KEY = "auth:user-jti:";
    private static final String REFRESH_SET_PREFIX = "auth:refresh-set:";

    private final SysUserService userService;
    private final CaptchaService captchaService;
    private final RsaKeyService rsaKeyService;
    private final TokenService tokenService;
    private final AuthProperties authProperties;
    private final RedisUtils redisUtils;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher publisher;

    @Override
    public LoginRespVO login(LoginReq req, String ip, String userAgent) {
        if (StringUtils.hasText(req.getCaptchaKey()) && !captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "Invalid captcha");
        }
        SysUser user = userService.getByUsername(req.getUsername());
        LoginRecordDTO record = new LoginRecordDTO();
        record.setIp(ip);
        record.setLoginAt(LocalDateTime.now());
        record.setUserAgent(userAgent);
        if (user == null) {
            record.setSuccess(false);
            try {
                userService.recordLogin(record);
            } catch (Exception ignored) {
            }
            publishLoginLog(null, req.getUsername(), ip, userAgent, 0, "用户不存在");
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        record.setUserId(user.getId());

        String rawPwd;
        try {
            rawPwd = CryptoUtils.rsaDecrypt(req.getPassword(), rsaKeyService.getPrivateKey());
        } catch (Exception ex) {
            record.setSuccess(false);
            userService.recordLogin(record);
            publishLoginLog(user.getId(), user.getUsername(), ip, userAgent, 0, "密码解密失败");
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "Password decryption failed");
        }

        if (!passwordEncoder.matches(rawPwd, user.getPassword())) {
            record.setSuccess(false);
            userService.recordLogin(record);
            publishLoginLog(user.getId(), user.getUsername(), ip, userAgent, 0, "密码错误");
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            record.setSuccess(false);
            userService.recordLogin(record);
            publishLoginLog(user.getId(), user.getUsername(), ip, userAgent, 0, "账号禁用");
            throw new BizException(ResultCode.USER_DISABLED);
        }

        UserAuthInfoDTO info = userService.getAuthInfo(user.getId());
        LoginRespVO resp = tokenService.issue(info);
        record.setSuccess(true);
        userService.recordLogin(record);
        publishLoginLog(user.getId(), user.getUsername(), ip, userAgent, 1, "登录成功");
        return resp;
    }

    /**
     * SA-P2-007: LoginLogListener is the only writer of login_log, so every
     * login outcome (success or failure) must publish LoginLogEvent.
     */
    private void publishLoginLog(Long userId, String username, String ip, String userAgent,
                                 Integer status, String message) {
        try {
            publisher.publishEvent(new BbpmsEvents.LoginLogEvent(userId, username, ip, userAgent, status, message));
        } catch (Exception ex) {
            log.warn("publishLoginLog failed for {}: {}", username, ex.getMessage());
        }
    }

    @Override
    public LoginRespVO refresh(String refreshToken) {
        Claims claims = tokenService.parseRefresh(refreshToken);
        if (claims == null) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        String jti = claims.getId();
        if (jti != null && tokenService.isRevoked(jti)) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        if (jti != null && redisUtils.get(REFRESH_KEY + jti) == null) {
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        }
        // Refresh tokens store the user id under "uid" (see JwtUtils.generateRefreshToken).
        Long userId = claims.get("uid", Long.class);
        if (userId == null) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        // Single-use rotation: invalidate the presented refresh token so it can't
        // be replayed. The next token pair is issued by TokenServiceImpl below.
        long refreshTtl = authProperties.getRefreshTtlDays() * 86400L;
        if (jti != null) {
            tokenService.revoke(jti, refreshTtl);
            redisUtils.del(REFRESH_KEY + jti);
        }
        UserAuthInfoDTO info = userService.getAuthInfo(userId);
        return tokenService.issue(info);
    }

    @Override
    public void logout(Long userId, String jti) {
        long accessTtl = authProperties.getAccessTtlMinutes() * 60L;
        long refreshTtl = authProperties.getRefreshTtlDays() * 86400L;
        tokenService.revoke(jti, accessTtl);
        redisUtils.del(USER_JTI_KEY + userId);
        // Revoke every outstanding refresh token for this user, not just the
        // access token — otherwise a stolen refresh token survives logout.
        String setKey = REFRESH_SET_PREFIX + userId;
        Set<String> refreshJtis = redisUtils.sMembers(setKey);
        if (refreshJtis != null) {
            for (String rj : refreshJtis) {
                tokenService.revoke(rj, refreshTtl);
                redisUtils.del(REFRESH_KEY + rj);
            }
        }
        redisUtils.del(setKey);
    }

    @Override
    public CaptchaRespVO generateCaptcha() {
        return captchaService.generate();
    }

    @Override
    public String getPublicKey() {
        return rsaKeyService.getPublicKey();
    }
}