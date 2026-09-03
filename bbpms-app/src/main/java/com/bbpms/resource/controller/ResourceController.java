package com.bbpms.resource.controller;

import com.bbpms.common.result.R;
import com.bbpms.resource.dto.ResourceCheckReq;
import com.bbpms.resource.entity.NetRegion;
import com.bbpms.resource.service.ResourceAdminService;
import com.bbpms.resource.service.ResourceCheckService;
import com.bbpms.resource.vo.BuildingVO;
import com.bbpms.resource.vo.CommunityVO;
import com.bbpms.resource.vo.OltVO;
import com.bbpms.resource.vo.OnuVO;
import com.bbpms.resource.vo.PonVO;
import com.bbpms.resource.vo.ResourceCheckResp;
import com.bbpms.resource.vo.RoomVO;
import com.bbpms.resource.vo.UnitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 地址与网络资源管理（ITERATION 2）。
 */
@Tag(name = "resource", description = "地址与网络资源管理")
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceAdminService adminService;
    private final ResourceCheckService checkService;

    @Operation(summary = "资源核查")
    @PostMapping("/check")
    @PreAuthorize("hasAuthority('order:create')")
    public R<ResourceCheckResp> check(@Valid @RequestBody ResourceCheckReq req) {
        return R.ok(checkService.check(req));
    }

    // ---------- 区域 ----------
    @Operation(summary = "区域列表")
    @GetMapping("/regions")
    @PreAuthorize("hasAuthority('resource:view')")
    public R<List<NetRegion>> regions() {
        return R.ok(adminService.listRegions());
    }

    @Operation(summary = "新增区域")
    @PostMapping("/regions")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<NetRegion> createRegion(@RequestParam String name, @RequestParam String code) {
        return R.ok(adminService.createRegion(name, code));
    }

    // ---------- 小区 ----------
    @Operation(summary = "小区列表")
    @GetMapping("/communities")
    @PreAuthorize("hasAuthority('resource:view')")
    public R<List<CommunityVO>> communities(@RequestParam(required = false) Long regionId,
                                            @RequestParam(required = false) String name) {
        return R.ok(adminService.listCommunities(regionId, name));
    }

    @Operation(summary = "新增小区")
    @PostMapping("/communities")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<CommunityVO> createCommunity(@RequestParam Long regionId, @RequestParam String name,
                                          @RequestParam(required = false) String address) {
        return R.ok(adminService.createCommunity(regionId, name, address));
    }

    // ---------- 楼栋 ----------
    @Operation(summary = "楼栋列表")
    @GetMapping("/buildings")
    @PreAuthorize("hasAuthority('resource:view')")
    public R<List<BuildingVO>> buildings(@RequestParam Long communityId) {
        return R.ok(adminService.listBuildings(communityId));
    }

    @Operation(summary = "新增楼栋")
    @PostMapping("/buildings")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<BuildingVO> createBuilding(@RequestParam Long communityId, @RequestParam String name,
                                        @RequestParam(required = false) Integer totalFloors) {
        return R.ok(adminService.createBuilding(communityId, name, totalFloors));
    }

    // ---------- 单元 ----------
    @Operation(summary = "单元列表")
    @GetMapping("/units")
    @PreAuthorize("hasAuthority('resource:view')")
    public R<List<UnitVO>> units(@RequestParam Long buildingId) {
        return R.ok(adminService.listUnits(buildingId));
    }

    @Operation(summary = "新增单元")
    @PostMapping("/units")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<UnitVO> createUnit(@RequestParam Long buildingId, @RequestParam String name) {
        return R.ok(adminService.createUnit(buildingId, name));
    }

    // ---------- 房间 ----------
    @Operation(summary = "房间列表")
    @GetMapping("/rooms")
    @PreAuthorize("hasAuthority('resource:view')")
    public R<List<RoomVO>> rooms(@RequestParam Long unitId,
                                 @RequestParam(required = false) Integer isInstalled) {
        return R.ok(adminService.listRooms(unitId, isInstalled));
    }

    @Operation(summary = "新增房间")
    @PostMapping("/rooms")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<RoomVO> createRoom(@RequestParam Long unitId, @RequestParam String roomNo) {
        return R.ok(adminService.createRoom(unitId, roomNo));
    }

    // ---------- OLT / PON / ONU ----------
    @Operation(summary = "OLT 列表")
    @GetMapping("/olts")
    @PreAuthorize("hasAuthority('resource:view')")
    public R<List<OltVO>> olts(@RequestParam(required = false) Long regionId) {
        return R.ok(adminService.listOlts(regionId));
    }

    @Operation(summary = "新增 OLT")
    @PostMapping("/olts")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<OltVO> createOlt(@RequestParam String name, @RequestParam Long regionId,
                              @RequestParam(required = false) String ip,
                              @RequestParam(required = false) String vendor,
                              @RequestParam(required = false) String model) {
        return R.ok(adminService.createOlt(name, regionId, ip, vendor, model));
    }

    @Operation(summary = "PON 列表")
    @GetMapping("/pons")
    @PreAuthorize("hasAuthority('resource:view')")
    public R<List<PonVO>> pons(@RequestParam Long oltId) {
        return R.ok(adminService.listPons(oltId));
    }

    @Operation(summary = "新增 PON")
    @PostMapping("/pons")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<PonVO> createPon(@RequestParam Long oltId, @RequestParam String name,
                              @RequestParam(required = false) Integer totalPorts) {
        return R.ok(adminService.createPon(oltId, name, totalPorts));
    }

    @Operation(summary = "ONU 列表")
    @GetMapping("/onus")
    @PreAuthorize("hasAuthority('resource:view')")
    public R<List<OnuVO>> onus(@RequestParam(required = false) Long roomId,
                               @RequestParam(required = false) String status) {
        return R.ok(adminService.listOnus(roomId, status));
    }

    @Operation(summary = "新增 ONU")
    @PostMapping("/onus")
    @PreAuthorize("hasAuthority('resource:edit')")
    public R<OnuVO> createOnu(@RequestParam String sn, @RequestParam(required = false) String model,
                              @RequestParam(required = false) Long roomId) {
        return R.ok(adminService.createOnu(sn, model, roomId));
    }
}