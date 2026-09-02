package com.bbpms.auth.service;

import com.bbpms.auth.dto.LoginRespVO;
import com.bbpms.user.dto.UserAuthInfoDTO;
import io.jsonwebtoken.Claims;

public interface TokenService {

    LoginRespVO issue(UserAuthInfoDTO info);

    Claims parseRefresh(String token);

    Claims parseAccess(String token);

    void revoke(String jti, long ttlSeconds);

    boolean isRevoked(String jti);

    String stripBearer(String header);
}