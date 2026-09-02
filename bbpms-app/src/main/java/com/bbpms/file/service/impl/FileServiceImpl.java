package com.bbpms.file.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.result.PageResp;
import com.bbpms.file.dto.FilePageReq;
import com.bbpms.file.dto.FileUploadResp;
import com.bbpms.file.dto.PresignedUrlResp;
import com.bbpms.file.entity.Attachment;
import com.bbpms.file.mapper.AttachmentMapper;
import com.bbpms.file.service.FileService;
import com.bbpms.file.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<AttachmentMapper, Attachment> implements FileService {
    private static final long DEFAULT_MAX_SIZE = 20L * 1024 * 1024;
    private final FileStorageService storageService;
    @Override
    @Transactional
    public FileUploadResp upload(MultipartFile file, String bizType, Long bizId, Long uploaderId) {
        if (file == null || file.isEmpty()) throw new BizException(ResultCode.FILE_UPLOAD_FAILED, "文件为空");
        if (file.getSize() > DEFAULT_MAX_SIZE) throw new BizException(ResultCode.FILE_UPLOAD_FAILED, "文件超过20MB");
        String ct = file.getContentType();
        boolean okType = ct != null && (ct.startsWith("image/") || ct.equals("application/pdf"));
        if (!okType) throw new BizException(ResultCode.FILE_UPLOAD_FAILED, "不支持的文件类型");
        String ext = "";
        String orig = file.getOriginalFilename();
        if (orig != null && orig.contains(".")) ext = orig.substring(orig.lastIndexOf('.') + 1);
        String type = bizType == null ? "other" : bizType.toLowerCase();
        String objectKey = type + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
                + "/" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String url;
        try {
            url = storageService.upload(objectKey, file);
        } catch (IOException ex) {
            log.error("upload failed", ex);
            throw new BizException(ResultCode.FILE_UPLOAD_FAILED, ex.getMessage());
        }
        Attachment att = new Attachment();
        att.setObjectKey(objectKey);
        att.setBucket(storageService.getBucket());
        att.setOriginalName(orig);
        att.setContentType(ct);
        att.setSize(file.getSize());
        att.setBizType(bizType);
        att.setBizId(bizId);
        att.setUploaderId(uploaderId);
        save(att);
        FileUploadResp resp = new FileUploadResp();
        resp.setId(att.getId());
        resp.setObjectKey(objectKey);
        resp.setUrl(url);
        resp.setSize(file.getSize());
        resp.setContentType(ct);
        return resp;
    }
    @Override
    public PresignedUrlResp getPresignedUrl(Long id) {
        Attachment a = getById(id);
        if (a == null) throw new BizException(ResultCode.NOT_FOUND);
        PresignedUrlResp resp = new PresignedUrlResp();
        resp.setUrl(storageService.getPresignedUrl(a.getObjectKey(), 300));
        resp.setExpiresAt(LocalDateTime.now().plusSeconds(300));
        return resp;
    }
    @Override
    public void delete(Long id) {
        Attachment a = getById(id);
        if (a == null) return;
        storageService.delete(a.getObjectKey());
        a.setDeleted(1);
        updateById(a);
    }
    @Override
    public PageResp<FileUploadResp> page(FilePageReq req) {
        Page<Attachment> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<Attachment> qw = new LambdaQueryWrapper<>();
        if (req.getBizType() != null) qw.eq(Attachment::getBizType, req.getBizType());
        if (req.getBizId() != null) qw.eq(Attachment::getBizId, req.getBizId());
        if (req.getUploaderId() != null) qw.eq(Attachment::getUploaderId, req.getUploaderId());
        qw.orderByDesc(Attachment::getCreateTime);
        Page<Attachment> result = baseMapper.selectPage(page, qw);
        PageResp<FileUploadResp> resp = new PageResp<>();
        resp.setTotal(result.getTotal());
        resp.setPageNum(result.getCurrent());
        resp.setPageSize(result.getSize());
        resp.setPages(result.getPages());
        resp.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return resp;
    }
    @Override
    public List<FileUploadResp> getByBiz(String bizType, Long bizId) {
        List<Attachment> list = baseMapper.listByBiz(bizType, bizId);
        return list.stream().map(this::toVO).toList();
    }
    private FileUploadResp toVO(Attachment a) {
        FileUploadResp v = new FileUploadResp();
        BeanUtils.copyProperties(a, v);
        v.setUrl(storageService.getPresignedUrl(a.getObjectKey(), 300));
        return v;
    }
}