package com.bbpms.notify.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.bbpms.common.util.JsonUtils;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.notify.dto.MessageTemplateCreateReq;
import com.bbpms.notify.dto.MessageTemplatePageReq;
import com.bbpms.common.result.PageResp;
import com.bbpms.notify.entity.MessageTemplate;
import com.bbpms.notify.mapper.MessageTemplateMapper;
import com.bbpms.notify.service.MessageTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Service
@RequiredArgsConstructor
public class MessageTemplateServiceImpl extends ServiceImpl<MessageTemplateMapper, MessageTemplate> implements MessageTemplateService {
    private static final String CACHE_PREFIX = "notify:template:";
    private final RedisUtils redisUtils;
    @Override
    @Transactional
    public Long create(MessageTemplateCreateReq req) {
        MessageTemplate t = new MessageTemplate();
        BeanUtils.copyProperties(req, t);
        t.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        save(t);
        redisUtils.del(CACHE_PREFIX + req.getCode());
        return t.getId();
    }
    @Override
    @Transactional
    public void update(MessageTemplateCreateReq req) {
        if (req.getId() == null) return;
        MessageTemplate t = getById(req.getId());
        if (t == null) return;
        BeanUtils.copyProperties(req, t, "id");
        updateById(t);
        redisUtils.del(CACHE_PREFIX + req.getCode());
    }
    @Override
    public void delete(Long id) {
        MessageTemplate t = getById(id);
        if (t == null) return;
        t.setDeleted(1);
        updateById(t);
        if (t.getCode() != null) redisUtils.del(CACHE_PREFIX + t.getCode());
    }
    @Override
    public MessageTemplate getByCode(String code) {
        String cached = redisUtils.get(CACHE_PREFIX + code);
        if (cached != null) return JsonUtils.parse(cached, MessageTemplate.class);
        MessageTemplate t = baseMapper.getByCode(code);
        if (t != null) redisUtils.set(CACHE_PREFIX + code, JsonUtils.toJson(t), 1, TimeUnit.HOURS);
        return t;
    }
    @Override
    public List<MessageTemplate> listEnabled() {
        return baseMapper.listEnabled();
    }

    @Override
    public PageResp<MessageTemplate> page(MessageTemplatePageReq req) {
        Page<MessageTemplate> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<MessageTemplate> query = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(req.getKeyword())) {
            query.and(q -> q.like(MessageTemplate::getCode, req.getKeyword())
                    .or().like(MessageTemplate::getSubject, req.getKeyword())
                    .or().like(MessageTemplate::getContent, req.getKeyword()));
        }
        if (StrUtil.isNotBlank(req.getChannel())) query.eq(MessageTemplate::getChannel, req.getChannel());
        if (req.getEnabled() != null) query.eq(MessageTemplate::getEnabled, req.getEnabled());
        query.orderByDesc(MessageTemplate::getUpdateTime);
        Page<MessageTemplate> result = baseMapper.selectPage(page, query);
        PageResp<MessageTemplate> response = new PageResp<>();
        response.setRecords(result.getRecords());
        response.setTotal(result.getTotal());
        response.setPageNum(result.getCurrent());
        response.setPageSize(result.getSize());
        response.setPages(result.getPages());
        return response;
    }
}
