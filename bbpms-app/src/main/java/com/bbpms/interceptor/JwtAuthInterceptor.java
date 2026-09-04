package com.bbpms.interceptor;

import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.security.SecurityContextHolder;
import com.bbpms.common.security.SecurityUser;
import com.bbpms.common.util.JwtUtils;
import com.bbpms.common.util.RedisUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String REVOKED_KEY = "auth:revoked:";
    private static final String ACTIVE_KEY = "auth:active:";

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        String token = header.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        Claims claims = jwtUtils.parse(token);
        if (claims == null) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        String jti = claims.getId();
        if (jti != null && redisUtils.get(REVOKED_KEY + jti) != null) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        if (jti != null && redisUtils.get(ACTIVE_KEY + jti) == null) {
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        }

        // Claim keys match JwtUtils: uid / usr / roles / perms / scope.
        SecurityUser user = new SecurityUser();
        user.setUserId(claims.get("uid", Long.class));
        user.setUsername(claims.get("usr", String.class));
        user.setRoles(toList(claims.get("roles")));
        user.setPermissions(toList(claims.get("perms")));
        user.setDataScope(claims.get("scope", Integer.class));
        user.setDeptId(claims.get("dept", Long.class));
        user.setJti(jti);
        SecurityContextHolder.set(user);

        // Bridge the authenticated user into Spring Security so @PreAuthorize
        // (hasAuthority / hasRole) sees the real principal instead of an
        // anonymous one. Without this every protected method returned 403.
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getPermissions() != null) {
            for (String perm : user.getPermissions()) {
                if (perm != null && !perm.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(perm.trim()));
                }
            }
        }
        if (user.getRoles() != null) {
            for (String role : user.getRoles()) {
                if (role == null || role.isBlank()) continue;
                String r = role.trim();
                authorities.add(new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r));
            }
        }
        org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
        return true;
    }

    private List<String> toList(Object o) {
        if (o instanceof List<?> l) {
            return l.stream().map(String::valueOf).collect(java.util.stream.Collectors.toList());
        }
        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContextHolder.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}