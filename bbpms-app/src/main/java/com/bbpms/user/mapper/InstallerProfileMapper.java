package com.bbpms.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.user.entity.InstallerProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InstallerProfileMapper extends BaseMapper<InstallerProfile> {

    @Select("SELECT * FROM installer_profile WHERE user_id = #{userId} AND deleted = 0 LIMIT 1")
    InstallerProfile selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE installer_profile SET workload = workload + 1 WHERE user_id = #{userId}")
    int incrementWorkload(@Param("userId") Long userId);

    @Update("UPDATE installer_profile SET workload = workload - 1 WHERE user_id = #{userId} AND workload > 0")
    int decrementWorkload(@Param("userId") Long userId);
}