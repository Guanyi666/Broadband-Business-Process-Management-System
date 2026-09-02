package com.bbpms.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * Response for {@code GET /api/auth/me}. Matches the admin frontend's
 * {@code UserInfo} shape: user profile fields + roles + permission codes.
 */
@Data
public class MeVO {
    private Long id;
    private String username;
    /** Display name (nickname || realName || username); H5 uses {@code name}. */
    private String name;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Long deptId;
    private String deptName;
    private List<String> roles;
    private List<String> permissions;
}
