package com.bbpms.notify.dto;

import com.bbpms.common.entity.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageTemplatePageReq extends BaseDTO {
    private String channel;
    private Integer enabled;
}
