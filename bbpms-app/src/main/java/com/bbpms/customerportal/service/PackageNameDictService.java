package com.bbpms.customerportal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbpms.common.constant.PackageNameMap;
import com.bbpms.customerportal.entity.BroadbandPackage;
import com.bbpms.customerportal.mapper.BroadbandPackageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 套餐名称中文映射的数据库字典源。
 *
 * <p>订单表 {@code broadband_order.package_name} 历史数据为英文（如 100M Broadband），
 * 展示层需统一映射为中文。优先使用 {@code broadband_package} 表中运营维护的
 * 权威中文名（code → name），避免硬编码、支持运营改文案免发版；
 * 数据库未命中时降级到 {@link PackageNameMap} 代码兜底。
 *
 * <p><b>已知数据差异</b>：订单表套餐编码为下划线（如 PKG_100M），套餐表为连字符
 * （如 PKG-100M），且套餐表中 3/5 的 name 为 latin1 双重编码乱码。因此当前
 * 字典实际命中率可能为 0，展示主要由代码兜底保证；本服务为「运营可维护字典」
 * 的未来能力预留，待数据治理（编码统一 + 乱码修复）后自动生效。
 *
 * <p>字典为低频静态数据，使用本地 {@link ConcurrentHashMap} 缓存，
 * 由 {@code PackageNameDictRefreshJob} 每 5 分钟全量刷新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageNameDictService {

    /** 单次刷新上限：套餐表数据量极小（个位数~几十条），全量读取无压力 */
    private static final int MAX_REFRESH = 500;

    /** 乱码特征检测：UTF-8 字节被 latin1 二次编码后出现的高位拉丁字符（如 Ã¢Â¦），命中即视为脏数据跳过 */
    private static final Pattern MOJIBAKE = Pattern.compile("[ÃÂ¥¡¦€¢£§°±]");

    private final BroadbandPackageMapper packageMapper;

    /** 套餐编码（小写）→ 中文名；仅收录 status=1 且名称健康的启用套餐 */
    private final Map<String, String> codeToName = new ConcurrentHashMap<>();

    /**
     * 将套餐名映射为中文。
     * <p>优先：数据库字典 code → name（仅健康中文名，乱码/脏数据自动跳过）；
     * 未命中时：{@link PackageNameMap} 代码兜底。
     *
     * @param packageCode 套餐编码（可空）
     * @param packageName 原始套餐名（可空）
     * @return 中文名；两者均无法识别时返回原始名
     */
    public String toChinese(String packageCode, String packageName) {
        if (packageCode != null && !packageCode.isBlank()) {
            String byDict = codeToName.get(packageCode.trim().toLowerCase());
            if (byDict != null) return byDict;
        }
        return PackageNameMap.toChinese(packageCode, packageName);
    }

    /** 全量刷新字典（供定时任务与启动预热调用）。 */
    public void refresh() {
        List<BroadbandPackage> list = packageMapper.selectList(
                new LambdaQueryWrapper<BroadbandPackage>()
                        .eq(BroadbandPackage::getStatus, 1)
                        .isNotNull(BroadbandPackage::getName)
                        .ne(BroadbandPackage::getName, "")
                        .last("LIMIT " + MAX_REFRESH));
        Map<String, String> fresh = new HashMap<>(list.size() * 2);
        for (BroadbandPackage p : list) {
            String name = p.getName();
            if (name == null || name.isBlank()) continue;
            // 健康检查：必须是含中文域字符的干净文本；乱码（mojibake）或纯英文跳过，避免脏数据污染展示
            if (MOJIBAKE.matcher(name).find() || !name.matches(".*[\\u4e00-\\u9fa5].*")) {
                log.warn("[PackageNameDict] 跳过脏数据 code={} name={}", p.getCode(), name);
                continue;
            }
            fresh.putIfAbsent(p.getCode().trim().toLowerCase(), name.trim());
        }
        codeToName.clear();
        codeToName.putAll(fresh);
        log.info("[PackageNameDict] 刷新完成，共 {} 条健康套餐中文名", fresh.size());
    }
}
