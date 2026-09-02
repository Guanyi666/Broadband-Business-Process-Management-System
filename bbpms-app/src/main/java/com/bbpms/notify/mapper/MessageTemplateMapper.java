package com.bbpms.notify.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.notify.entity.MessageTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface MessageTemplateMapper extends BaseMapper<MessageTemplate> {
    MessageTemplate getByCode(@Param("code") String code);
    List<MessageTemplate> listEnabled();
}