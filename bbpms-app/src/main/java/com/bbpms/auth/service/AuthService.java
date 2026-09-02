package com.bbpms.auth.service;

import com.bbpms.auth.dto.CaptchaRespVO;
import com.bbpms.auth.dto.LoginReq;
import com.bbpms.auth.dto.LoginRespVO;

public interface AuthService {

    LoginRespVO login(LoginReq req, String ip, String userAgent);

    LoginRespVO refresh(String refreshToken);

    void logout(Long userId, String jti);

    CaptchaRespVO generateCaptcha();

    String getPublicKey();
}