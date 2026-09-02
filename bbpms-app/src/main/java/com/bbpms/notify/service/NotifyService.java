package com.bbpms.notify.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.result.PageResp;
import com.bbpms.notify.dto.MessagePageReq;
import com.bbpms.notify.dto.SmsSendReq;
import com.bbpms.notify.dto.WechatTemplateSendReq;
import com.bbpms.notify.entity.Message;
import com.bbpms.notify.vo.MessageVO;
import com.bbpms.notify.vo.SmsSendResp;
import com.bbpms.notify.vo.WechatSendResp;

public interface NotifyService {
    SmsSendResp sendSms(SmsSendReq req);
    WechatSendResp sendWechat(WechatTemplateSendReq req);
    void sendInApp(Long userId, String content);
    PageResp<MessageVO> pageMessages(MessagePageReq req);
    void handleNotifyEvent(BbpmsEvents.NotifyEvent event);
}