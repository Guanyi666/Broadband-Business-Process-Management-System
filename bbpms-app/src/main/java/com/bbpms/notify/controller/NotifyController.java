package com.bbpms.notify.controller;
import jakarta.validation.Valid;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.notify.dto.MessagePageReq;
import com.bbpms.notify.dto.SmsSendReq;
import com.bbpms.notify.dto.WechatTemplateSendReq;
import com.bbpms.notify.service.NotifyService;
import com.bbpms.notify.vo.MessageVO;
import com.bbpms.notify.vo.SmsSendResp;
import com.bbpms.notify.vo.WechatSendResp;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {
    private final NotifyService notifyService;
    @PostMapping("/sms")
    @PreAuthorize("hasAuthority('notify:sms:send')")
    public R<SmsSendResp> sendSms(@Valid @RequestBody SmsSendReq req) {
        return R.ok(notifyService.sendSms(req));
    }
    @PostMapping("/wechat/template")
    @PreAuthorize("hasAuthority('notify:wechat:send')")
    public R<WechatSendResp> sendWechat(@Valid @RequestBody WechatTemplateSendReq req) {
        return R.ok(notifyService.sendWechat(req));
    }
    @GetMapping("/messages/page")
    @PreAuthorize("hasAuthority('notify:view')")
    public R<PageResp<MessageVO>> pageMessages(MessagePageReq req) {
        return R.ok(notifyService.pageMessages(req));
    }
}