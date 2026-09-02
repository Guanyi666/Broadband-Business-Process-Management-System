package com.bbpms.common.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public abstract class BaseDO implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    @TableField(value = "create_time", fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableField(value = "create_by", fill = FieldFill.INSERT) private Long createBy;
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE) private Long updateBy;
    @TableLogic @TableField("deleted") private Integer deleted;
    @Version @TableField("version") private Integer version;
}
