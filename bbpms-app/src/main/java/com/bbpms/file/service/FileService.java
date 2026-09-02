package com.bbpms.file.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bbpms.common.result.PageResp;
import com.bbpms.file.dto.FilePageReq;
import com.bbpms.file.dto.FileUploadResp;
import com.bbpms.file.dto.PresignedUrlResp;
import com.bbpms.file.entity.Attachment;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
public interface FileService extends IService<Attachment> {
    FileUploadResp upload(MultipartFile file, String bizType, Long bizId, Long uploaderId);
    PresignedUrlResp getPresignedUrl(Long id);
    void delete(Long id);
    PageResp<FileUploadResp> page(FilePageReq req);
    List<FileUploadResp> getByBiz(String bizType, Long bizId);
}