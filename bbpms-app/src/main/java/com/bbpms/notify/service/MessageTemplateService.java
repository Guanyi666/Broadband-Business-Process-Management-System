package com.bbpms.notify.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bbpms.notify.dto.MessageTemplateCreateReq;
import com.bbpms.notify.entity.MessageTemplate;
import java.util.List;
public interface MessageTemplateService extends IService<MessageTemplate> {
    Long create(MessageTemplateCreateReq req);
    void update(MessageTemplateCreateReq req);
    void delete(Long id);
    MessageTemplate getByCode(String code);
    List<MessageTemplate> listEnabled();
}