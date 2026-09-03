package com.bbpms.log.dto;
import com.bbpms.common.entity.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true)
public class LoginLogPageReq extends BaseDTO {
    private Long userId;
    private String username;
    private Integer status;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
