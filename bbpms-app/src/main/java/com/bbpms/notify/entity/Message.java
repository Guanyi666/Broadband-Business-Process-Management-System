package com.bbpms.notify.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data @EqualsAndHashCode(callSuper = true) @TableName("message")
public class Message extends BaseDO {
    @TableField("user_id") private Long userId;
    @TableField("channel") private String channel;
    @TableField("template_code") private String templateCode;
    @TableField("params") private String params; // JSON
    @TableField("content") private String content;
    @TableField("status") private String status;
    @TableField("error_msg") private String errorMsg;
    @TableField("send_time") private LocalDateTime sendTime;
}