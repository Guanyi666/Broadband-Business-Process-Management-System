package com.bbpms.notify.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true) @TableName("message_template")
public class MessageTemplate extends BaseDO {
    @TableField("code") private String code;
    @TableField("channel") private String channel;
    @TableField("subject") private String subject;
    @TableField("content") private String content;
    @TableField("aliyun_template_id") private String aliyunTemplateId;
    @TableField("wechat_template_id") private String wechatTemplateId;
    @TableField("enabled") private Integer enabled;
}