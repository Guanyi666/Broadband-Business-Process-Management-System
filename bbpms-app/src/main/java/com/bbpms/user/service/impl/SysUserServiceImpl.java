package com.bbpms.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.util.CryptoUtils;
import com.bbpms.user.dto.*;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.entity.SysUserRole;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.user.mapper.SysUserRoleMapper;
import com.bbpms.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateReq req) {
        SysUser exist = baseMapper.selectByUsername(req.getUsername());
        if (exist != null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "Username already exists");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        if (StringUtils.hasText(req.getPhone())) {
            user.setPhoneEnc(CryptoUtils.sm4Encrypt(req.getPhone(), "bbpms-user-sm4-key-2026"));
        }
        user.setEmail(req.getEmail());
        user.setDeptId(req.getDeptId());
        user.setUserType(req.getUserType());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        save(user);
        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), req.getRoleIds());
        }
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:auth", key = "#req.id")
    public void update(UserUpdateReq req) {
        SysUser user = getById(req.getId());
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        if (StringUtils.hasText(req.getPhone())) {
            user.setPhoneEnc(CryptoUtils.sm4Encrypt(req.getPhone(), "bbpms-user-sm4-key-2026"));
        }
        user.setEmail(req.getEmail());
        user.setDeptId(req.getDeptId());
        user.setStatus(req.getStatus());
        updateById(user);
        if (req.getRoleIds() != null) {
            assignRoles(user.getId(), req.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:auth", key = "#id")
    public void delete(Long id) {
        removeById(id);
        userRoleMapper.deleteByUserId(id);
    }

    @Override
    public SysUser getById(Long id) {
        return super.getById(id);
    }

    @Override
    public SysUser getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public PageResp<SysUser> page(UserPageReq req) {
        Page<SysUser> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (req.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, req.getDeptId());
        }
        if (req.getUserType() != null) {
            wrapper.eq(SysUser::getUserType, req.getUserType());
        }
        if (req.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, req.getStatus());
        }
        if (StringUtils.hasText(req.getRealName())) {
            wrapper.like(SysUser::getRealName, req.getRealName());
        }
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w.like(SysUser::getUsername, req.getKeyword())
                    .or().like(SysUser::getRealName, req.getKeyword()));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = page(page, wrapper);
        return PageResp.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysUserRole> list = roleIds.stream().map(rid -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(rid);
            return ur;
        }).collect(Collectors.toList());
        for (SysUserRole ur : list) {
            userRoleMapper.insert(ur);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, PasswordChangeReq req) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        updateById(user);
    }

    @Override
    @Cacheable(value = "user:auth", key = "#userId")
    public UserAuthInfoDTO getAuthInfo(Long userId) {
        UserAuthInfoDTO info = new UserAuthInfoDTO();
        SysUser user = getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        info.setUserId(user.getId());
        info.setUsername(user.getUsername());
        info.setStatus(user.getStatus());
        info.setUserType(user.getUserType());
        info.setRoles(baseMapper.selectUserRoles(userId));
        info.setPermissions(baseMapper.selectUserPermissions(userId));
        info.setDataScope(baseMapper.selectUserDataScope(userId));
        info.setDeptId(user.getDeptId());
        return info;
    }

    @Override
    public void recordLogin(LoginRecordDTO record) {
        SysUser user = getById(record.getUserId());
        if (user == null) {
            return;
        }
        if (Boolean.TRUE.equals(record.getSuccess())) {
            user.setLastLoginAt(record.getLoginAt());
            user.setLastLoginIp(record.getIp());
            updateById(user);
        }
    }
}