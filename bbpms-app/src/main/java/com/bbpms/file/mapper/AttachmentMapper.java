package com.bbpms.file.mapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bbpms.common.annotation.DataScope;
import com.bbpms.file.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {
    @DataScope
    IPage<Attachment> selectPageWithScope(IPage<Attachment> page, @Param("ew") Wrapper<Attachment> wrapper);
    List<Attachment> listByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);
}