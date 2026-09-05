package com.bbpms.auth.controller;

import jakarta.validation.Valid;
import com.bbpms.auth.dto.CaptchaRespVO;
import com.bbpms.auth.dto.LoginReq;
import com.bbpms.auth.dto.LoginRespVO;
import com.bbpms.auth.dto.MeVO;
import com.bbpms.auth.service.AuthService;
import com.bbpms.auth.service.RsaKeyService;
import com.bbpms.common.result.R;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.user.dto.UserAuthInfoDTO;
import com.bbpms.user.entity.SysDept;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.service.SysDeptService;
import com.bbpms.user.service.SysMenuService;
import com.bbpms.user.service.SysUserService;
import com.bbpms.user.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Auth endpoints. Mapping is {@code /api/auth} to keep the whole API under one
 * {@code /api} prefix (matches every business controller and both frontends).
 * The {@code /me} and {@code /menus} endpoints require a valid token — unlike
 * login / refresh / captcha / public-key which are public.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RsaKeyService rsaKeyService;
    private final SysUserService userService;
    private final SysMenuService menuService;
    private final SysDeptService deptService;

    @PostMapping("/login")
    public R<LoginRespVO> login(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        return R.ok(authService.login(req, ip, ua));
    }

    @PostMapping("/refresh")
    public R<LoginRespVO> refresh(@RequestParam String refreshToken) {
        return R.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        Long userId = SecurityUtils.getCurrentUserId();
        String jti = SecurityUtils.getCurrentUser() == null ? null : SecurityUtils.getCurrentUser().getJti();
        authService.logout(userId, jti);
        return R.ok();
    }

    @GetMapping("/captcha")
    public R<CaptchaRespVO> captcha() {
        return R.ok(authService.generateCaptcha());
    }

    @GetMapping("/public-key")
    public R<String> publicKey() {
        return R.ok(authService.getPublicKey());
    }

    /** Current user identity + roles + permissions for the admin UI. */
    @GetMapping("/me")
    public R<MeVO> me() {
        Long userId = SecurityUtils.requireUserId();
        UserAuthInfoDTO info = userService.getAuthInfo(userId);
        SysUser user = userService.getById(userId);

        MeVO vo = new MeVO();
        vo.setId(user.getId());
        vo.setUsername(info.getUsername());
        String displayName = firstNonBlank(user.getNickname(), user.getRealName(), info.getUsername());
        vo.setName(displayName);
        vo.setNickname(displayName);
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setDeptId(user.getDeptId());
        vo.setRoles(info.getRoles());
        vo.setPermissions(info.getPermissions());
        if (user.getDeptId() != null) {
            SysDept dept = deptService.getById(user.getDeptId());
            if (dept != null) vo.setDeptName(dept.getName());
        }
        return R.ok(vo);
    }

    /** Current user's menu tree for the sidebar. */
    @GetMapping("/menus")
    public R<List<SysMenuVO>> menus() {
        Long userId = SecurityUtils.requireUserId();
        return R.ok(menuService.getMenuTreeByUserId(userId));
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
