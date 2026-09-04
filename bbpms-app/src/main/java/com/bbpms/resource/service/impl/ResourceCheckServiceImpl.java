package com.bbpms.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.resource.dto.ResourceCheckReq;
import com.bbpms.resource.entity.NetBuilding;
import com.bbpms.resource.entity.NetCommunity;
import com.bbpms.resource.entity.NetRoom;
import com.bbpms.resource.entity.NetUnit;
import com.bbpms.resource.mapper.NetBuildingMapper;
import com.bbpms.resource.mapper.NetCommunityMapper;
import com.bbpms.resource.mapper.NetRoomMapper;
import com.bbpms.resource.mapper.NetUnitMapper;
import com.bbpms.resource.service.ResourceCheckService;
import com.bbpms.resource.vo.ResourceCheckResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 资源核查实现。
 *
 * <p>地址解析规则（演示数据对应的中文地址形态）：
 * {@code ...小区名 + 数字/字母楼栋(号楼/A栋) + 数字单元(n单元) + 房号(纯数字)}。
 * 逆向逐级匹配：小区名 LIKE 模糊 → 楼栋名模糊 → 单元名模糊 → 房号精确。
 *
 * <p>判定：
 * <ul>
 *   <li>房号存在且未安装 → RESOURCE_OK（可安装）</li>
 *   <li>房号存在但已安装 / 楼栋在册但无此房 → RESOURCE_INSUFFICIENT（资源不足）</li>
 *   <li>小区/楼栋不在册 → NO_COVERAGE（暂无覆盖）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceCheckServiceImpl implements ResourceCheckService {

    private final NetCommunityMapper communityMapper;
    private final NetBuildingMapper buildingMapper;
    private final NetUnitMapper unitMapper;
    private final NetRoomMapper roomMapper;

    private static final Pattern BUILDING_PATTERN = Pattern.compile("([0-9A-Za-z]+)(?:号楼|栋|座|幢)");
    private static final Pattern UNIT_PATTERN = Pattern.compile("([0-9A-Za-z]+)(?:单元|门)");
    private static final Pattern ROOM_PATTERN = Pattern.compile("\\d{2,4}");

    @Override
    public ResourceCheckResp check(ResourceCheckReq req) {
        ResourceCheckResp resp = new ResourceCheckResp();
        if (req == null || req.getAddress() == null || req.getAddress().isBlank()) {
            resp.setStatus("NO_COVERAGE");
            resp.setMessage("地址为空，无法核查");
            return resp;
        }
        String address = req.getAddress().trim();

        // 1) 定位小区
        NetCommunity community = matchCommunity(address);
        if (community == null) {
            resp.setStatus("NO_COVERAGE");
            resp.setMessage("当前地址暂未覆盖，请选择其他地址或联系客服");
            return resp;
        }
        resp.setCommunityId(community.getId());
        resp.setCommunityName(community.getName());

        // 2) 定位楼栋
        String buildingKey = extractBuilding(address);
        NetBuilding building = null;
        if (buildingKey != null) {
            building = buildingMapper.selectOne(new LambdaQueryWrapper<NetBuilding>()
                    .eq(NetBuilding::getCommunityId, community.getId())
                    .likeRight(NetBuilding::getName, buildingKey)
                    .last("LIMIT 1"));
        }
        if (building == null) {
            // 小区覆盖但在册楼栋未匹配上 — 视为未覆盖楼栋
            resp.setStatus("NO_COVERAGE");
            resp.setMessage("该小区尚未覆盖此楼栋，请联系客服登记");
            return resp;
        }
        resp.setBuildingId(building.getId());
        resp.setBuildingName(building.getName());

        // 3) 定位单元（可缺省，按楼栋首个单元兜底）
        String unitKey = extractUnit(address);
        NetUnit unit = null;
        if (unitKey != null) {
            unit = unitMapper.selectOne(new LambdaQueryWrapper<NetUnit>()
                    .eq(NetUnit::getBuildingId, building.getId())
                    .likeRight(NetUnit::getName, unitKey)
                    .last("LIMIT 1"));
        }
        if (unit == null) {
            unit = unitMapper.selectOne(new LambdaQueryWrapper<NetUnit>()
                    .eq(NetUnit::getBuildingId, building.getId())
                    .eq(NetUnit::getStatus, 1)
                    .orderByAsc(NetUnit::getSort)
                    .last("LIMIT 1"));
        }
        if (unit == null) {
            resp.setStatus("NO_COVERAGE");
            resp.setMessage("该楼栋尚未录入单元信息");
            return resp;
        }
        resp.setUnitId(unit.getId());
        resp.setUnitName(unit.getName());

        // 4) 房号判定
        String roomNo = req.getRoomNo();
        if (roomNo == null || roomNo.isBlank()) {
            Matcher rm = ROOM_PATTERN.matcher(address);
            if (rm.find()) roomNo = rm.group();
        }
        if (roomNo == null || roomNo.isBlank()) {
            resp.setStatus("RESOURCE_INSUFFICIENT");
            resp.setMessage("未能从地址识别房号，请补充房号");
            return resp;
        }
        resp.setRoomNo(roomNo);

        List<NetRoom> rooms = roomMapper.selectList(new LambdaQueryWrapper<NetRoom>()
                .eq(NetRoom::getUnitId, unit.getId())
                .eq(NetRoom::getRoomNo, roomNo));
        if (rooms.isEmpty()) {
            resp.setStatus("RESOURCE_INSUFFICIENT");
            resp.setMessage("该房间不在可售资源中（无此房号）");
            return resp;
        }
        NetRoom room = rooms.get(0);
        if (Integer.valueOf(1).equals(room.getIsInstalled())) {
            resp.setStatus("RESOURCE_INSUFFICIENT");
            resp.setMessage("该房间已安装，当前端口资源不足");
            resp.setRoomId(room.getId());
            return resp;
        }
        resp.setStatus("RESOURCE_OK");
        resp.setMessage("资源核查通过，可下单安装");
        resp.setRoomId(room.getId());
        return resp;
    }

    /**
     * 逆向解析小区：优先小区名（名称含于地址），其次小区注册地址（address 列，
     * 如"北京市朝阳区建国路1号"），取最长命中，防止歧义。
     */
    private NetCommunity matchCommunity(String address) {
        List<NetCommunity> all = communityMapper.selectList(new LambdaQueryWrapper<NetCommunity>()
                .eq(NetCommunity::getStatus, 1));
        NetCommunity best = null;
        int bestLen = -1;
        for (NetCommunity c : all) {
            int len = 0;
            String name = c.getName();
            String regAddr = c.getAddress();
            if (name != null && !name.isBlank() && address.contains(name)) {
                len = Math.max(len, name.length());
            }
            if (regAddr != null && !regAddr.isBlank() && address.contains(regAddr)) {
                len = Math.max(len, regAddr.length());
            }
            if (len > 0 && len > bestLen) {
                best = c;
                bestLen = len;
            }
        }
        return best;
    }

    private String extractBuilding(String address) {
        Matcher m = BUILDING_PATTERN.matcher(address);
        if (!m.find()) return null;
        String raw = m.group(1);
        if (raw.matches("\\d+")) {
            return raw; // "5号楼" → "5" → likeRight '5%' 匹配 "5号楼"? 见上
        }
        return raw;
    }

    private String extractUnit(String address) {
        Matcher m = UNIT_PATTERN.matcher(address);
        return m.find() ? m.group(1) : null;
    }
}