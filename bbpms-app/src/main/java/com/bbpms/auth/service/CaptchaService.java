package com.bbpms.auth.service;

import com.bbpms.auth.dto.CaptchaRespVO;

public interface CaptchaService {

    CaptchaRespVO generate();

    boolean verify(String key, String code);
}