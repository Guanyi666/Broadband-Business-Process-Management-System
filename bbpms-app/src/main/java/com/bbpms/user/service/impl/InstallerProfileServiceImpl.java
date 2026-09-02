package com.bbpms.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.user.dto.InstallerLocationDTO;
import com.bbpms.user.entity.InstallerProfile;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.mapper.InstallerProfileMapper;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.user.service.InstallerProfileService;
import com.bbpms.user.vo.InstallerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstallerProfileServiceImpl extends ServiceImpl<InstallerProfileMapper, InstallerProfile> implements InstallerProfileService {

    /** Dispatch/online-list source of truth (SA-P1-005) — also written by clock-in/out. */
    public static final String ONLINE_KEY = "installers:active";

    private final RedisUtils redisUtils;
    private final SysUserMapper userMapper;

    @Override
    public InstallerProfile getByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public List<InstallerVO> getOnline() {
        Set<String> userIds = redisUtils.zRange(ONLINE_KEY, 0, -1);
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = userIds.stream().map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
        List<InstallerVO> result = new ArrayList<>();
        for (Long uid : ids) {
            InstallerProfile p = baseMapper.selectByUserId(uid);
            if (p == null) {
                continue;
            }
            result.add(toInstallerVO(p, userMapper.selectById(uid)));
        }
        return result;
    }

    @Override
    public InstallerVO getProfile(Long userId) {
        InstallerProfile p = baseMapper.selectByUserId(userId);
        if (p == null) {
            return null;
        }
        return toInstallerVO(p, userMapper.selectById(userId));
    }

    @Override
    public List<InstallerVO> listLocations() {
        List<InstallerProfile> profiles = lambdaQuery()
                .isNotNull(InstallerProfile::getCurrentLat)
                .isNotNull(InstallerProfile::getCurrentLng)
                .list();
        return toInstallerVOs(profiles);
    }

    @Override
    public Page<InstallerVO> pageInstallers(String keyword, long pageNum, long pageSize) {
        Page<InstallerProfile> page = new Page<>(pageNum, pageSize);
        if (keyword != null && !keyword.isBlank()) {
            List<SysUser> matched = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword));
            if (matched.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            List<Long> ids = matched.stream().map(SysUser::getId).toList();
            page = lambdaQuery().in(InstallerProfile::getUserId, ids).page(page);
        } else {
            page = page(page);
        }
        Page<InstallerVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toInstallerVOs(page.getRecords()));
        return voPage;
    }

    private List<InstallerVO> toInstallerVOs(List<InstallerProfile> profiles) {
        if (profiles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = profiles.stream().map(InstallerProfile::getUserId).toList();
        Map<Long, SysUser> users = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        return profiles.stream()
                .map(p -> toInstallerVO(p, users.get(p.getUserId())))
                .collect(Collectors.toList());
    }

    private InstallerVO toInstallerVO(InstallerProfile p, SysUser u) {
        InstallerVO vo = new InstallerVO();
        vo.setUserId(p.getUserId());
        vo.setUsername(u == null ? null : u.getUsername());
        vo.setRealName(u == null ? null : u.getRealName());
        vo.setPhone(u == null ? null : u.getPhone());
        vo.setOnDuty(p.getOnDuty());
        vo.setLat(p.getCurrentLat());
        vo.setLng(p.getCurrentLng());
        vo.setWorkload(p.getWorkload());
        vo.setRating(p.getScore());
        vo.setLastActiveAt(p.getLastLocationTime());
        return vo;
    }

    @Override
    public void updateLocation(InstallerLocationDTO dto) {
        InstallerProfile p = baseMapper.selectByUserId(dto.getUserId());
        if (p == null) {
            p = new InstallerProfile();
            p.setUserId(dto.getUserId());
            p.setOnDuty(dto.getOnDuty() == null ? 1 : dto.getOnDuty());
            p.setWorkload(0);
            save(p);
        } else {
            p.setCurrentLat(dto.getLat());
            p.setCurrentLng(dto.getLng());
            if (dto.getOnDuty() != null) {
                p.setOnDuty(dto.getOnDuty());
            }
        }
        p.setLastLocationTime(LocalDateTime.now());
        updateById(p);
        if (p.getOnDuty() != null && p.getOnDuty() == 1) {
            redisUtils.zAdd(ONLINE_KEY, String.valueOf(dto.getUserId()), System.currentTimeMillis());
        } else {
            redisUtils.zRem(ONLINE_KEY, dto.getUserId());
        }
    }

    @Override
    public void incrementWorkload(Long userId) {
        baseMapper.incrementWorkload(userId);
    }

    @Override
    public void decrementWorkload(Long userId) {
        baseMapper.decrementWorkload(userId);
    }
}