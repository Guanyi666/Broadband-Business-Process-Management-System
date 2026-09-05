package com.bbpms.user.service.impl;

import com.bbpms.user.entity.SysMenu;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.user.service.RbacService;
import com.bbpms.user.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private final SysUserMapper userMapper;

    @Override
    public List<SysMenuVO> buildMenuTree(List<SysMenu> flat) {
        if (flat == null || flat.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, SysMenuVO> voMap = flat.stream().collect(Collectors.toMap(SysMenu::getId, m -> {
            SysMenuVO vo = new SysMenuVO();
            BeanUtils.copyProperties(m, vo);
            // Map the entity's column-backed fields onto the VO's frontend-facing names.
            vo.setName(m.getMenuName());
            if (m.getMenuType() != null) {
                try {
                    vo.setType(Integer.parseInt(m.getMenuType()));
                } catch (NumberFormatException ignored) {
                    vo.setType(null);
                }
            }
            vo.setPerm(m.getPerms());
            vo.setSort(m.getSortOrder());
            vo.setChildren(new ArrayList<>());
            return vo;
        }));
        List<SysMenuVO> roots = new ArrayList<>();
        for (SysMenu m : flat) {
            SysMenuVO vo = voMap.get(m.getId());
            Long parentId = m.getParentId() == null ? 0L : m.getParentId();
            if (parentId == 0L || !voMap.containsKey(parentId)) {
                roots.add(vo);
            } else {
                voMap.get(parentId).getChildren().add(vo);
            }
        }
        sortRecursively(roots);
        return roots;
    }

    private void sortRecursively(List<SysMenuVO> nodes) {
        nodes.sort(Comparator
                .comparing(SysMenuVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysMenuVO::getId, Comparator.nullsLast(Long::compareTo)));
        for (SysMenuVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortRecursively(node.getChildren());
            }
        }
    }

    @Override
    public Integer getCurrentDataScope(Long userId) {
        return userMapper.selectUserDataScope(userId);
    }
}
