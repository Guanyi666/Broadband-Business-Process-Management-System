package com.bbpms.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bbpms.user.dto.InstallerLocationDTO;
import com.bbpms.user.entity.InstallerProfile;
import com.bbpms.user.vo.InstallerVO;

import java.util.List;

public interface InstallerProfileService extends IService<InstallerProfile> {

    InstallerProfile getByUserId(Long userId);

    List<InstallerVO> getOnline();

    /** Enabled installer accounts that are on duty and below the workload limit. */
    List<InstallerVO> listAvailable(int maxWorkload);

    /** All installers that have reported a location (for the admin map view). */
    List<InstallerVO> listLocations();

    /** Single installer profile (online or offline). */
    InstallerVO getProfile(Long userId);

    /** Keyword filters sys_user.username / real_name / phone; joins profile data. */
    Page<InstallerVO> pageInstallers(String keyword, long pageNum, long pageSize);

    void updateLocation(InstallerLocationDTO dto);

    void incrementWorkload(Long userId);

    void decrementWorkload(Long userId);
}
