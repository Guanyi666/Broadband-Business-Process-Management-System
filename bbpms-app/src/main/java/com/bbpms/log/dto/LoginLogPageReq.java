package com.bbpms.log.dto;
import com.bbpms.common.entity.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true)
public class LoginLogPageReq extends BaseDTO {
    private Long userId;
    private String username;
}