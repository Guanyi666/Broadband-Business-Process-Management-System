package com.bbpms.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.bbpms.common.security.SecurityContextHolder;
import com.bbpms.common.security.SecurityUser;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Fills auditing columns based on the current SecurityUser. */
@Component
public class AutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long uid = currentUserId();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        if (uid != null) {
            strictInsertFill(metaObject, "createBy", Long.class, uid);
            strictInsertFill(metaObject, "updateBy", Long.class, uid);
        }
        fillStrategy(metaObject, "deleted", 0);
        fillStrategy(metaObject, "version", 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long uid = currentUserId();
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        if (uid != null) strictUpdateFill(metaObject, "updateBy", Long.class, uid);
    }

    private Long currentUserId() {
        SecurityUser u = SecurityContextHolder.get();
        return u == null ? null : u.getUserId();
    }
}
