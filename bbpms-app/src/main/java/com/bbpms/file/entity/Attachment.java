package com.bbpms.file.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true) @TableName("attachment")
public class Attachment extends BaseDO {
    @TableField("object_key") private String objectKey;
    @TableField("bucket") private String bucket;
    @TableField("original_name") private String originalName;
    @TableField("content_type") private String contentType;
    @TableField("size") private Long size;
    @TableField("biz_type") private String bizType;
    @TableField("biz_id") private Long bizId;
    @TableField("uploader_id") private Long uploaderId;
}