package com.bbpms.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginRecordDTO {

    private Long userId;

    private String ip;

    private LocalDateTime loginAt;

    private String userAgent;

    private Boolean success;
}