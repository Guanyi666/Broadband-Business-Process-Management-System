package com.bbpms.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CaptchaRespVO {

    @JsonProperty("captchaId")
    private String captchaKey;

    @JsonProperty("image")
    private String captchaImage;

    private Integer expireSeconds;
}