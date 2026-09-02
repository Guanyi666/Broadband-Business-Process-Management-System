package com.bbpms.common.security;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data @NoArgsConstructor
public class SecurityUser {
    private Long userId;
    private String username;
    private List<String> roles;
    private List<String> permissions;
    private Integer dataScope;
    private Long tenantId;
    private String jti;
    public SecurityUser(Long userId, String username, List<String> roles, Integer dataScope, Long tenantId) {
        this.userId = userId; this.username = username; this.roles = roles; this.dataScope = dataScope; this.tenantId = tenantId;
    }
}
