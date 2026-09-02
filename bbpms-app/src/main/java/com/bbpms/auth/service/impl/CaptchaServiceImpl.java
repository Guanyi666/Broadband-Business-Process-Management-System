package com.bbpms.auth.service.impl;

import cn.hutool.captcha.LineCaptcha;
import com.bbpms.auth.dto.CaptchaRespVO;
import com.bbpms.auth.service.CaptchaService;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.common.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    private static final long CAPTCHA_TTL_SECONDS = 300;

    private final RedisUtils redisUtils;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public CaptchaServiceImpl(RedisUtils redisUtils,
                              @Qualifier("orderSnowflakeIdGenerator") SnowflakeIdGenerator snowflakeIdGenerator) {
        this.redisUtils = redisUtils;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public CaptchaRespVO generate() {
        LineCaptcha captcha = new LineCaptcha(160, 60, 4, 50);
        String code = captcha.getCode();
        String key = String.valueOf(snowflakeIdGenerator.nextId());
        redisUtils.set(CAPTCHA_KEY_PREFIX + key, code.toLowerCase(), CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);

        // Build clean data URI: strip any whitespace/newlines from Hutool's output
        String raw = captcha.getImageBase64Data();
        String imageData = raw.replaceAll("\\s+", "");

        CaptchaRespVO vo = new CaptchaRespVO();
        vo.setCaptchaKey(key);
        vo.setCaptchaImage(imageData);
        vo.setExpireSeconds((int) CAPTCHA_TTL_SECONDS);
        return vo;
    }

    @Override
    public boolean verify(String key, String code) {
        if (key == null || code == null) {
            return false;
        }
        Object stored = redisUtils.get(CAPTCHA_KEY_PREFIX + key);
        if (stored == null) {
            return false;
        }
        boolean ok = stored.toString().equalsIgnoreCase(code.trim());
        if (ok) {
            redisUtils.del(CAPTCHA_KEY_PREFIX + key);
        }
        return ok;
    }
}