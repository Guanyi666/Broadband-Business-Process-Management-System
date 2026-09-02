package com.bbpms.file.dto;
import com.bbpms.common.entity.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true)
public class FilePageReq extends BaseDTO {
    private String bizType;
    private Long bizId;
    private Long uploaderId;
}