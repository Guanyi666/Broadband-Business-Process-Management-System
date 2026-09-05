package com.bbpms.customerportal.job;

import com.bbpms.customerportal.service.PackageNameDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 套餐中文名字典定时刷新。
 *
 * <p>每 5 分钟全量刷新一次数据库字典缓存，使运营在套餐管理页
 * 修改套餐名后最多 5 分钟即可在订单列表/客户 H5 生效，无需发版。
 * 启动时通过 {@link ApplicationRunner} 预热一次，避免首次查询空缓存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PackageNameDictRefreshJob implements ApplicationRunner {

    private final PackageNameDictService dictService;

    /** 首次启动延迟 30s 预热（等待依赖就绪），此后每 5 分钟刷新。 */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 30_000L)
    public void refresh() {
        dictService.refresh();
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            dictService.refresh();
            log.info("[PackageNameDict] 启动预热完成");
        } catch (Exception e) {
            log.warn("[PackageNameDict] 启动预热失败，将由定时任务兜底: {}", e.getMessage());
        }
    }
}
