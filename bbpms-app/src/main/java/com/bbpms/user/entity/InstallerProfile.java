package com.bbpms.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Installer profile row. Column names mirror the DDL in
 * {@code middleware/mysql/init/01-bbpms-schema.sql} exactly; explicit
 * {@link TableField} annotations are kept on the renamed fields so the
 * intent is searchable.
 *
 * <p>Does NOT extend {@link com.bbpms.common.entity.BaseDO}: the table's
 * primary key is {@code user_id}, not {@code id}, so the primary-key
 * annotation must live on {@link #userId} ({@link IdType#INPUT} — the value
 * is the referencing {@code sys_user.id}). The BaseDO audit columns are
 * declared here instead so {@code AutoFillHandler}, logical delete and
 * optimistic locking keep working.
 */
@Data
@TableName("installer_profile")
public class InstallerProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /** Maps to {@code create_time DATETIME} in DDL. */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** Maps to {@code update_time DATETIME} in DDL. */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** Maps to {@code create_by BIGINT} in DDL. */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /** Maps to {@code update_by BIGINT} in DDL. */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** Maps to {@code deleted TINYINT} in DDL. */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** Maps to {@code version INT} in DDL. */
    @Version
    @TableField("version")
    private Integer version;

    private Integer onDuty;

    private BigDecimal currentLat;

    private BigDecimal currentLng;

    /** Maps to {@code last_location_time DATETIME} in DDL. */
    @TableField("last_location_time")
    private LocalDateTime lastLocationTime;

    private Integer workload;

    /** Maps to {@code score DECIMAL(3,2)} in DDL (renamed from {@code rating}). */
    @TableField("score")
    private BigDecimal score;

    private String skillTags;

    /** Maps to {@code level TINYINT} in DDL. */
    @TableField("level")
    private Integer level;
}
