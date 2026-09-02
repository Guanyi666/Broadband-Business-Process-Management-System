package com.bbpms.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.common.annotation.DataScope;
import com.bbpms.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    SysUser selectByUsername(@Param("username") String username);

    List<String> selectUserRoles(@Param("userId") Long userId);

    List<String> selectUserPermissions(@Param("userId") Long userId);

    Integer selectUserDataScope(@Param("userId") Long userId);

    @DataScope
    @Override
    List<SysUser> selectList(@Param("ew") com.baomidou.mybatisplus.core.conditions.Wrapper<SysUser> queryWrapper);
}