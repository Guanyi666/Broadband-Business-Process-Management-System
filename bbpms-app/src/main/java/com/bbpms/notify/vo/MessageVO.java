package com.bbpms.notify.vo;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class MessageVO {
    private Long id;
    private Long userId;
    private String channel;
    private String templateCode;
    private String params;
    private String content;
    private String status;
    private String errorMsg;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
}