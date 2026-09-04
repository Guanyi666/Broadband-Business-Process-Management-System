package com.bbpms.resource.service;

import com.bbpms.resource.vo.BuildingVO;
import com.bbpms.resource.vo.CommunityVO;
import com.bbpms.resource.vo.OltVO;
import com.bbpms.resource.vo.OnuVO;
import com.bbpms.resource.vo.PonVO;
import com.bbpms.resource.vo.RoomVO;
import com.bbpms.resource.vo.UnitVO;

import java.util.List;

/**
 * 地址与网络资源台账管理（区域/小区/楼栋/单元/房间/OLT/PON/ONU）。
 * 合并为一个管理服务，避免 8 个琐碎 service。
 */
public interface ResourceAdminService {

    // ---- 区域 ----
    List<com.bbpms.resource.entity.NetRegion> listRegions();

    com.bbpms.resource.entity.NetRegion createRegion(String name, String code);

    // ---- 小区 ----
    List<CommunityVO> listCommunities(Long regionId, String name);

    CommunityVO createCommunity(Long regionId, String name, String address);

    // ---- 楼栋 ----
    List<BuildingVO> listBuildings(Long communityId);

    com.bbpms.resource.vo.BuildingVO createBuilding(Long communityId, String name, Integer totalFloors);

    // ---- 单元 ----
    List<UnitVO> listUnits(Long buildingId);

    UnitVO createUnit(Long buildingId, String name);

    // ---- 房间 ----
    List<RoomVO> listRooms(Long unitId, Integer isInstalled);

    RoomVO createRoom(Long unitId, String roomNo);

    // ---- OLT/PON/ONU ----
    List<OltVO> listOlts(Long regionId);

    OltVO createOlt(String name, Long regionId, String ip, String vendor, String model);

    List<PonVO> listPons(Long oltId);

    PonVO createPon(Long oltId, String name, Integer totalPorts);

    List<OnuVO> listOnus(Long roomId, String status);

    OnuVO createOnu(String sn, String model, Long roomId);
}