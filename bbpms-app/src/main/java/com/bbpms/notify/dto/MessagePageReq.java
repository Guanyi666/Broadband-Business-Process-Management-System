package com.bbpms.notify.dto;
import com.bbpms.common.entity.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true)
public class MessagePageReq extends BaseDTO {
    private Long userId;
    private String channel;
    private String status;
    private String templateCode;
}