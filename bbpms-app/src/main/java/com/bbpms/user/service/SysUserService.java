package com.bbpms.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bbpms.common.result.PageResp;
import com.bbpms.user.dto.*;
import com.bbpms.user.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    Long create(UserCreateReq req);

    void update(UserUpdateReq req);

    void delete(Long id);

    SysUser getById(Long id);

    SysUser getByUsername(String username);

    PageResp<SysUser> page(UserPageReq req);

    void assignRoles(Long userId, java.util.List<Long> roleIds);

    void changePassword(Long userId, PasswordChangeReq req);

    UserAuthInfoDTO getAuthInfo(Long userId);

    void recordLogin(LoginRecordDTO record);
}