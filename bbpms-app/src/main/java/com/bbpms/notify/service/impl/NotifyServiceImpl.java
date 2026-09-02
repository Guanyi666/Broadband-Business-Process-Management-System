package com.bbpms.notify.service.impl;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.util.JsonUtils;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.notify.dto.MessagePageReq;
import com.bbpms.notify.dto.SmsSendReq;
import com.bbpms.notify.dto.WechatTemplateSendReq;
import com.bbpms.notify.entity.Message;
import com.bbpms.notify.entity.MessageTemplate;
import com.bbpms.notify.mapper.MessageMapper;
import com.bbpms.notify.mapper.MessageTemplateMapper;
import com.bbpms.notify.service.NotifyService;
import com.bbpms.notify.service.sender.SendResult;
import com.bbpms.notify.service.sender.SmsSender;
import com.bbpms.notify.service.sender.WechatSender;
import com.bbpms.notify.vo.MessageVO;
import com.bbpms.notify.vo.SmsSendResp;
import com.bbpms.notify.vo.WechatSendResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {
    private static final String CACHE_KEY = "notify:template:";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)\\}");
    private final MessageMapper messageMapper;
    private final MessageTemplateMapper templateMapper;
    private final SmsSender smsSender;
    private final WechatSender wechatSender;
    private final RedisUtils redisUtils;

    @Override
    @Transactional
    public SmsSendResp sendSms(SmsSendReq req) {
        return doSendSms(req);
    }

    private SmsSendResp doSendSms(SmsSendReq req) {
        MessageTemplate tmpl = getCachedTemplate(req.getTemplateCode());
        Message msg = new Message();
        msg.setChannel("SMS");
        msg.setUserId(null);
        msg.setTemplateCode(req.getTemplateCode());
        msg.setParams(JsonUtils.toJson(req.getParams()));
        msg.setContent(renderTemplate(tmpl.getContent(), req.getParams()));
        msg.setStatus("PENDING");
        messageMapper.insert(msg);
        SendResult r;
        try {
            r = smsSender.send(req.getPhone(), tmpl.getAliyunTemplateId(), req.getParams());
            msg.setStatus(r.isSuccess() ? "SUCCESS" : "FAILED");
            msg.setErrorMsg(r.getErrorMsg());
            msg.setSendTime(LocalDateTime.now());
        } catch (Exception ex) {
            log.warn("SMS send failed: {}", ex.getMessage());
            msg.setStatus("FAILED");
            msg.setErrorMsg(ex.getMessage());
            msg.setSendTime(LocalDateTime.now());
        }
        messageMapper.updateById(msg);
        return new SmsSendResp(msg.getId(), msg.getStatus());
    }

    @Override
    @Transactional
    public WechatSendResp sendWechat(WechatTemplateSendReq req) {
        Message msg = new Message();
        msg.setChannel("WECHAT");
        msg.setTemplateCode(req.getTemplateId());
        msg.setParams(JsonUtils.toJson(req.getParams()));
        msg.setContent(JsonUtils.toJson(req.getParams()));
        msg.setStatus("PENDING");
        messageMapper.insert(msg);
        SendResult r = wechatSender.send(req.getOpenId(), req.getTemplateId(), req.getParams(), req.getUrl());
        msg.setStatus(r.isSuccess() ? "SUCCESS" : "FAILED");
        msg.setErrorMsg(r.getErrorMsg());
        msg.setSendTime(LocalDateTime.now());
        messageMapper.updateById(msg);
        return new WechatSendResp(msg.getId(), msg.getStatus());
    }

    @Override
    public void sendInApp(Long userId, String content) {
        Message msg = new Message();
        msg.setChannel("INAPP");
        msg.setUserId(userId);
        msg.setContent(content);
        msg.setStatus("SUCCESS");
        msg.setSendTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    @Override
    public PageResp<MessageVO> pageMessages(MessagePageReq req) {
        Page<Message> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<Message> qw = new LambdaQueryWrapper<>();
        if (req.getUserId() != null) qw.eq(Message::getUserId, req.getUserId());
        if (StrUtil.isNotBlank(req.getChannel())) qw.eq(Message::getChannel, req.getChannel());
        if (StrUtil.isNotBlank(req.getStatus())) qw.eq(Message::getStatus, req.getStatus());
        qw.orderByDesc(Message::getCreateTime);
        Page<Message> result = messageMapper.selectPage(page, qw);
        List<MessageVO> records = result.getRecords().stream().map(m -> {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).toList();
        PageResp<MessageVO> resp = new PageResp<>();
        resp.setRecords(records);
        resp.setTotal(result.getTotal());
        resp.setPageNum(result.getCurrent());
        resp.setPageSize(result.getSize());
        resp.setPages(result.getPages());
        return resp;
    }

    @Override
    public void handleNotifyEvent(BbpmsEvents.NotifyEvent event) {
        if ("SMS".equalsIgnoreCase(event.getChannel())) {
            SmsSendReq req = new SmsSendReq();
            req.setPhone(event.getPhone());
            req.setTemplateCode(event.getTemplateCode());
            req.setParams(event.getParams());
            sendSms(req);
        } else if ("WECHAT".equalsIgnoreCase(event.getChannel())) {
            WechatTemplateSendReq req = new WechatTemplateSendReq();
            req.setOpenId(event.getOpenId());
            req.setTemplateId(event.getTemplateCode());
            req.setParams(event.getParams());
            sendWechat(req);
        } else if ("INAPP".equalsIgnoreCase(event.getChannel())) {
            sendInApp(event.getUserId(), event.getParams() == null ? "" : event.getParams().toString());
        }
    }

    private MessageTemplate getCachedTemplate(String code) {
        String cached = redisUtils.get(CACHE_KEY + code);
        if (cached != null) return JsonUtils.parse(cached, MessageTemplate.class);
        MessageTemplate t = templateMapper.getByCode(code);
        if (t == null) throw new BizException(ResultCode.NOT_FOUND, "模板不存在: " + code);
        redisUtils.set(CACHE_KEY + code, JsonUtils.toJson(t), 1, TimeUnit.HOURS);
        return t;
    }

    private String renderTemplate(String content, Map<String, Object> params) {
        if (content == null || params == null) return content;
        Matcher m = PLACEHOLDER.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object val = params.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(val)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}