package com.bbpms.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "休息开始/结束请求")
public class BreakReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "备注")
    @Size(max = 200, message = "备注不能超过 200 个字符")
    private String remark;
}
