package com.bbpms.file.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class PresignedUrlResp {
    private String url;
    private LocalDateTime expiresAt;
}