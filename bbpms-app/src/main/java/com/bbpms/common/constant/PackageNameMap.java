package com.bbpms.common.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 套餐名称中文化映射。
 *
 * <p>订单表 {@code broadband_order.package_name} 历史数据为英文（如 100M Broadband），
 * 管理端/客户端展示时统一映射为中文。展示层兜底，不改写数据库。
 *
 * <p>优先按 {@code package_code} 精确映射；无 code 时按 {@code package_name} 关键词映射。
 */
public final class PackageNameMap {

    private PackageNameMap() {
    }

    /** 套餐编码 → 中文名（code 精确映射优先） */
    private static final Map<String, String> BY_CODE = new LinkedHashMap<>();

    /** 英文关键词 → 中文名（name 关键词映射兜底） */
    private static final Map<String, String> BY_KEYWORD = new LinkedHashMap<>();

    static {
        BY_CODE.put("PKG_100M", "100M 宽带");
        BY_CODE.put("PKG-100M", "100M 宽带");
        BY_CODE.put("PKG_300M", "300M 宽带");
        BY_CODE.put("PKG-300M", "300M 宽带");
        BY_CODE.put("PKG_500M", "500M 宽带");
        BY_CODE.put("PKG-500M", "500M 宽带");
        BY_CODE.put("PKG_1G", "1000M 宽带");
        BY_CODE.put("PKG-1G", "1000M 宽带");
        BY_CODE.put("PKG_1000M", "1000M 宽带");
        BY_CODE.put("PKG-1000M", "1000M 宽带");
        BY_CODE.put("FIBER_500M", "光纤 500M 宽带");
        BY_CODE.put("FIBER-500M", "光纤 500M 宽带");

        BY_KEYWORD.put("1g", "1000M 宽带");
        BY_KEYWORD.put("1000m", "1000M 宽带");
        BY_KEYWORD.put("100m", "100M 宽带");
        BY_KEYWORD.put("300m", "300M 宽带");
        BY_KEYWORD.put("500m", "500M 宽带");
    }

    /**
     * 将套餐名映射为中文。
     *
     * @param packageCode 套餐编码（可空）
     * @param packageName 原始套餐名（可空）
     * @return 中文名；无法识别时返回原值（可空）
     */
    public static String toChinese(String packageCode, String packageName) {
        if (packageCode != null && !packageCode.isBlank()) {
            String byCode = BY_CODE.get(packageCode.trim());
            if (byCode != null) return byCode;
        }
        if (packageName == null || packageName.isBlank()) return packageName;
        String lower = packageName.trim().toLowerCase();
        for (Map.Entry<String, String> e : BY_KEYWORD.entrySet()) {
            if (lower.contains(e.getKey())) return e.getValue();
        }
        // 已是中文（含中文或"宽带"字样）则原样返回
        if (lower.contains("宽带") || lower.matches(".*[\\u4e00-\\u9fa5].*")) return packageName;
        return packageName;
    }
}
