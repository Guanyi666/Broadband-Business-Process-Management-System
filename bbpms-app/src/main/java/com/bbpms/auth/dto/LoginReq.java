package com.bbpms.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginReq {

    private String username;

    private String password;

    @JsonProperty("captchaId")
    private String captchaKey;

    @JsonProperty("captcha")
    private String captchaCode;
}