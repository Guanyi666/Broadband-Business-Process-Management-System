package com.bbpms.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.resource.entity.NetBuilding;
import com.bbpms.resource.entity.NetCommunity;
import com.bbpms.resource.entity.NetOlt;
import com.bbpms.resource.entity.NetOnu;
import com.bbpms.resource.entity.NetPon;
import com.bbpms.resource.entity.NetRegion;
import com.bbpms.resource.entity.NetRoom;
import com.bbpms.resource.entity.NetUnit;
import com.bbpms.resource.mapper.NetBuildingMapper;
import com.bbpms.resource.mapper.NetCommunityMapper;
import com.bbpms.resource.mapper.NetOltMapper;
import com.bbpms.resource.mapper.NetOnuMapper;
import com.bbpms.resource.mapper.NetPonMapper;
import com.bbpms.resource.mapper.NetRegionMapper;
import com.bbpms.resource.mapper.NetRoomMapper;
import com.bbpms.resource.mapper.NetUnitMapper;
import com.bbpms.resource.service.ResourceAdminService;
import com.bbpms.resource.vo.BuildingVO;
import com.bbpms.resource.vo.CommunityVO;
import com.bbpms.resource.vo.OltVO;
import com.bbpms.resource.vo.OnuVO;
import com.bbpms.resource.vo.PonVO;
import com.bbpms.resource.vo.RoomVO;
import com.bbpms.resource.vo.UnitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 地址与网络资源台账管理实现。
 */
@Service
@RequiredArgsConstructor
public class ResourceAdminServiceImpl implements ResourceAdminService {

    private final NetRegionMapper regionMapper;
    private final NetCommunityMapper communityMapper;
    private final NetBuildingMapper buildingMapper;
    private final NetUnitMapper unitMapper;
    private final NetRoomMapper roomMapper;
    private final NetOltMapper oltMapper;
    private final NetPonMapper ponMapper;
    private final NetOnuMapper onuMapper;

    @Override
    public List<NetRegion> listRegions() {
        return regionMapper.selectList(new LambdaQueryWrapper<NetRegion>()
                .eq(NetRegion::getStatus, 1).orderByAsc(NetRegion::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NetRegion createRegion(String name, String code) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(code)) {
            throw new BizException(ResultCode.BAD_REQUEST, "名称与编码不能为空");
        }
        NetRegion r = new NetRegion();
        r.setName(name);
        r.setCode(code.toUpperCase());
        r.setSort(0);
        r.setStatus(1);
        regionMapper.insert(r);
        return r;
    }

    @Override
    public List<CommunityVO> listCommunities(Long regionId, String name) {
        LambdaQueryWrapper<NetCommunity> qw = new LambdaQueryWrapper<NetCommunity>()
                .eq(regionId != null, NetCommunity::getRegionId, regionId)
                .eq(NetCommunity::getStatus, 1)
                .like(StringUtils.hasText(name), NetCommunity::getName, name)
                .orderByAsc(NetCommunity::getSort);
        List<NetCommunity> list = communityMapper.selectList(qw);
        Map<Long, String> regionNames = regionId == null
                ? regionMapper.selectList(null).stream()
                    .collect(Collectors.toMap(NetRegion::getId, NetRegion::getName, (a, b) -> a))
                : Map.of();
        return list.stream().map(c -> {
            CommunityVO vo = new CommunityVO();
            vo.setId(c.getId());
            vo.setRegionId(c.getRegionId());
            vo.setRegionName(regionId == null ? regionNames.getOrDefault(c.getRegionId(), "") : null);
            vo.setName(c.getName());
            vo.setAddress(c.getAddress());
            vo.setLat(c.getLat());
            vo.setLng(c.getLng());
            vo.setGridCode(c.getGridCode());
            vo.setSort(c.getSort());
            vo.setStatus(c.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityVO createCommunity(Long regionId, String name, String address) {
        if (regionId == null || !StringUtils.hasText(name)) {
            throw new BizException(ResultCode.BAD_REQUEST, "区域与名称不能为空");
        }
        NetCommunity c = new NetCommunity();
        c.setRegionId(regionId);
        c.setName(name);
        c.setAddress(address);
        c.setSort(0);
        c.setStatus(1);
        communityMapper.insert(c);
        CommunityVO vo = new CommunityVO();
        vo.setId(c.getId());
        vo.setRegionId(regionId);
        vo.setName(name);
        vo.setAddress(address);
        vo.setSort(0);
        vo.setStatus(1);
        return vo;
    }

    @Override
    public List<BuildingVO> listBuildings(Long communityId) {
        List<NetBuilding> list = buildingMapper.selectList(new LambdaQueryWrapper<NetBuilding>()
                .eq(NetBuilding::getCommunityId, communityId)
                .eq(NetBuilding::getStatus, 1)
                .orderByAsc(NetBuilding::getSort));
        String communityName = communityId == null ? null
                : communityMapper.selectById(communityId) == null ? null
                : communityMapper.selectById(communityId).getName();
        return list.stream().map(b -> {
            BuildingVO vo = new BuildingVO();
            vo.setId(b.getId());
            vo.setCommunityId(b.getCommunityId());
            vo.setCommunityName(communityName);
            vo.setName(b.getName());
            vo.setTotalFloors(b.getTotalFloors());
            vo.setSort(b.getSort());
            vo.setStatus(b.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuildingVO createBuilding(Long communityId, String name, Integer totalFloors) {
        if (communityId == null || !StringUtils.hasText(name)) {
            throw new BizException(ResultCode.BAD_REQUEST, "小区与楼栋名不能为空");
        }
        NetBuilding b = new NetBuilding();
        b.setCommunityId(communityId);
        b.setName(name);
        b.setTotalFloors(totalFloors);
        b.setSort(0);
        b.setStatus(1);
        buildingMapper.insert(b);
        BuildingVO vo = new BuildingVO();
        vo.setId(b.getId());
        vo.setCommunityId(communityId);
        vo.setName(name);
        vo.setTotalFloors(totalFloors);
        vo.setSort(0);
        vo.setStatus(1);
        return vo;
    }

    @Override
    public List<UnitVO> listUnits(Long buildingId) {
        List<NetUnit> list = unitMapper.selectList(new LambdaQueryWrapper<NetUnit>()
                .eq(NetUnit::getBuildingId, buildingId)
                .eq(NetUnit::getStatus, 1)
                .orderByAsc(NetUnit::getSort));
        return list.stream().map(u -> {
            UnitVO vo = new UnitVO();
            vo.setId(u.getId());
            vo.setBuildingId(u.getBuildingId());
            vo.setName(u.getName());
            vo.setSort(u.getSort());
            vo.setStatus(u.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnitVO createUnit(Long buildingId, String name) {
        if (buildingId == null || !StringUtils.hasText(name)) {
            throw new BizException(ResultCode.BAD_REQUEST, "楼栋与单元名不能为空");
        }
        NetUnit u = new NetUnit();
        u.setBuildingId(buildingId);
        u.setName(name);
        u.setSort(0);
        u.setStatus(1);
        unitMapper.insert(u);
        UnitVO vo = new UnitVO();
        vo.setId(u.getId());
        vo.setBuildingId(buildingId);
        vo.setName(name);
        vo.setSort(0);
        vo.setStatus(1);
        return vo;
    }

    @Override
    public List<RoomVO> listRooms(Long unitId, Integer isInstalled) {
        List<NetRoom> list = roomMapper.selectList(new LambdaQueryWrapper<NetRoom>()
                .eq(NetRoom::getUnitId, unitId)
                .eq(isInstalled != null, NetRoom::getIsInstalled, isInstalled)
                .orderByAsc(NetRoom::getRoomNo));
        return list.stream().map(r -> {
            RoomVO vo = new RoomVO();
            vo.setId(r.getId());
            vo.setUnitId(r.getUnitId());
            vo.setRoomNo(r.getRoomNo());
            vo.setIsInstalled(r.getIsInstalled());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoomVO createRoom(Long unitId, String roomNo) {
        if (unitId == null || !StringUtils.hasText(roomNo)) {
            throw new BizException(ResultCode.BAD_REQUEST, "单元与房号不能为空");
        }
        NetRoom r = new NetRoom();
        r.setUnitId(unitId);
        r.setRoomNo(roomNo);
        r.setIsInstalled(0);
        roomMapper.insert(r);
        RoomVO vo = new RoomVO();
        vo.setId(r.getId());
        vo.setUnitId(unitId);
        vo.setRoomNo(roomNo);
        vo.setIsInstalled(0);
        return vo;
    }

    @Override
    public List<OltVO> listOlts(Long regionId) {
        List<NetOlt> list = oltMapper.selectList(new LambdaQueryWrapper<NetOlt>()
                .eq(regionId != null, NetOlt::getRegionId, regionId)
                .orderByAsc(NetOlt::getName));
        return list.stream().map(o -> {
            OltVO vo = new OltVO();
            vo.setId(o.getId());
            vo.setName(o.getName());
            vo.setRegionId(o.getRegionId());
            vo.setIp(o.getIp());
            vo.setVendor(o.getVendor());
            vo.setModel(o.getModel());
            vo.setStatus(o.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OltVO createOlt(String name, Long regionId, String ip, String vendor, String model) {
        if (!StringUtils.hasText(name) || regionId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "OLT 名称与区域不能为空");
        }
        NetOlt o = new NetOlt();
        o.setName(name);
        o.setRegionId(regionId);
        o.setIp(ip);
        o.setVendor(vendor);
        o.setModel(model);
        o.setStatus(1);
        oltMapper.insert(o);
        OltVO vo = new OltVO();
        vo.setId(o.getId());
        vo.setName(name);
        vo.setRegionId(regionId);
        vo.setIp(ip);
        vo.setVendor(vendor);
        vo.setModel(model);
        vo.setStatus(1);
        return vo;
    }

    @Override
    public List<PonVO> listPons(Long oltId) {
        List<NetPon> list = ponMapper.selectList(new LambdaQueryWrapper<NetPon>()
                .eq(NetPon::getOltId, oltId)
                .orderByAsc(NetPon::getName));
        return list.stream().map(p -> {
            PonVO vo = new PonVO();
            vo.setId(p.getId());
            vo.setOltId(p.getOltId());
            vo.setName(p.getName());
            vo.setTotalPorts(p.getTotalPorts());
            vo.setUsedPorts(p.getUsedPorts());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PonVO createPon(Long oltId, String name, Integer totalPorts) {
        if (oltId == null || !StringUtils.hasText(name)) {
            throw new BizException(ResultCode.BAD_REQUEST, "OLT 与 PON 名不能为空");
        }
        NetPon p = new NetPon();
        p.setOltId(oltId);
        p.setName(name);
        p.setTotalPorts(totalPorts == null ? 32 : totalPorts);
        p.setUsedPorts(0);
        ponMapper.insert(p);
        PonVO vo = new PonVO();
        vo.setId(p.getId());
        vo.setOltId(oltId);
        vo.setName(name);
        vo.setTotalPorts(totalPorts == null ? 32 : totalPorts);
        vo.setUsedPorts(0);
        return vo;
    }

    @Override
    public List<OnuVO> listOnus(Long roomId, String status) {
        List<NetOnu> list = onuMapper.selectList(new LambdaQueryWrapper<NetOnu>()
                .eq(roomId != null, NetOnu::getRoomId, roomId)
                .eq(StringUtils.hasText(status), NetOnu::getStatus, status)
                .orderByDesc(NetOnu::getId));
        return list.stream().map(o -> {
            OnuVO vo = new OnuVO();
            vo.setId(o.getId());
            vo.setRoomId(o.getRoomId());
            vo.setPonId(o.getPonId());
            vo.setSn(o.getSn());
            vo.setModel(o.getModel());
            vo.setStatus(o.getStatus());
            if (o.getRoomId() != null) {
                NetRoom room = roomMapper.selectById(o.getRoomId());
                vo.setRoomNo(room == null ? null : room.getRoomNo());
            }
            if (o.getPonId() != null) {
                NetPon pon = ponMapper.selectById(o.getPonId());
                vo.setPonName(pon == null ? null : pon.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnuVO createOnu(String sn, String model, Long roomId) {
        if (!StringUtils.hasText(sn)) {
            throw new BizException(ResultCode.BAD_REQUEST, "SN 不能为空");
        }
        NetOnu o = new NetOnu();
        o.setSn(sn.trim());
        o.setModel(model);
        o.setRoomId(roomId);
        o.setStatus(roomId == null ? "IN_STOCK" : "INSTALLED");
        onuMapper.insert(o);
        OnuVO vo = new OnuVO();
        vo.setId(o.getId());
        vo.setSn(o.getSn());
        vo.setModel(model);
        vo.setRoomId(roomId);
        vo.setStatus(o.getStatus());
        return vo;
    }
}