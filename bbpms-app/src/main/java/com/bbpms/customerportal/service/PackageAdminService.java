package com.bbpms.customerportal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.result.PageResp;
import com.bbpms.customerportal.dto.PackageSaveReq;
import com.bbpms.customerportal.entity.BroadbandPackage;
import com.bbpms.customerportal.mapper.BroadbandPackageMapper;
import com.bbpms.customerportal.vo.PackageAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐资源管理（后台「资源管理 → 套餐资源」CRUD）。
 *
 * <p>套餐编码全局唯一；删除为逻辑删除（deleted=1）。禁用套餐不影响历史订单展示，
 * 仅控制客户自助报装时可选项。</p>
 */
@Service
@RequiredArgsConstructor
public class PackageAdminService {

    private final BroadbandPackageMapper packageMapper;

    /** 分页查询套餐（按名称/编码模糊搜索，可选状态过滤，按 sort 升序）。 */
    public PageResp<PackageAdminVO> page(long pageNum, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<BroadbandPackage> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(BroadbandPackage::getName, keyword)
                    .or().like(BroadbandPackage::getCode, keyword)
                    .or().like(BroadbandPackage::getNameEn, keyword));
        }
        if (status != null) wrapper.eq(BroadbandPackage::getStatus, status);
        wrapper.orderByAsc(BroadbandPackage::getSort).orderByDesc(BroadbandPackage::getId);

        Page<BroadbandPackage> page = packageMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<PackageAdminVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        Page<PackageAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResp.of(voPage);
    }

    /** 全部启用套餐（供订单创建/报装选择）。 */
    public List<PackageAdminVO> listEnabled() {
        return packageMapper.selectList(new LambdaQueryWrapper<BroadbandPackage>()
                        .eq(BroadbandPackage::getStatus, 1)
                        .orderByAsc(BroadbandPackage::getSort))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PackageSaveReq req) {
        Long existed = packageMapper.selectCount(new LambdaQueryWrapper<BroadbandPackage>()
                .eq(BroadbandPackage::getCode, req.getCode().trim()));
        if (existed != null && existed > 0) {
            throw new BizException(ResultCode.PACKAGE_INVALID, "套餐编码已存在：" + req.getCode());
        }
        BroadbandPackage pkg = new BroadbandPackage();
        copyReq(req, pkg);
        packageMapper.insert(pkg);
        return pkg.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PackageSaveReq req) {
        BroadbandPackage pkg = packageMapper.selectById(id);
        if (pkg == null) throw new BizException(ResultCode.PACKAGE_INVALID, "套餐不存在");
        Long existed = packageMapper.selectCount(new LambdaQueryWrapper<BroadbandPackage>()
                .eq(BroadbandPackage::getCode, req.getCode().trim())
                .ne(BroadbandPackage::getId, id));
        if (existed != null && existed > 0) {
            throw new BizException(ResultCode.PACKAGE_INVALID, "套餐编码已存在：" + req.getCode());
        }
        copyReq(req, pkg);
        packageMapper.updateById(pkg);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BroadbandPackage pkg = packageMapper.selectById(id);
        if (pkg == null) throw new BizException(ResultCode.PACKAGE_INVALID, "套餐不存在");
        packageMapper.deleteById(id); // @TableLogic 逻辑删除
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        BroadbandPackage pkg = packageMapper.selectById(id);
        if (pkg == null) throw new BizException(ResultCode.PACKAGE_INVALID, "套餐不存在");
        pkg.setStatus(status == null ? 0 : status);
        packageMapper.updateById(pkg);
    }

    private void copyReq(PackageSaveReq req, BroadbandPackage pkg) {
        pkg.setCode(req.getCode().trim());
        pkg.setName(req.getName().trim());
        pkg.setNameEn(StringUtils.hasText(req.getNameEn()) ? req.getNameEn().trim() : null);
        pkg.setSpeedMbps(req.getSpeedMbps());
        pkg.setMonthlyFee(req.getMonthlyFee());
        pkg.setDescription(req.getDescription());
        pkg.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        pkg.setSort(req.getSort() == null ? 0 : req.getSort());
    }

    private PackageAdminVO toVO(BroadbandPackage pkg) {
        PackageAdminVO vo = new PackageAdminVO();
        BeanUtils.copyProperties(pkg, vo);
        return vo;
    }
}
