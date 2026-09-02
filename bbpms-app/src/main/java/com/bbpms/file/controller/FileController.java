package com.bbpms.file.controller;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.result.R;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.file.dto.FilePageReq;
import com.bbpms.file.dto.FileUploadResp;
import com.bbpms.file.dto.PresignedUrlResp;
import com.bbpms.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('file:upload')")
    public R<FileUploadResp> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("bizType") String bizType,
                                   @RequestParam(value = "bizId", required = false) Long bizId) {
        Long uploaderId = SecurityUtils.getCurrentUserId();
        return R.ok(fileService.upload(file, bizType, bizId, uploaderId));
    }
    @GetMapping("/{id}/presign")
    @PreAuthorize("hasAuthority('file:download')")
    public R<PresignedUrlResp> presign(@PathVariable Long id) {
        return R.ok(fileService.getPresignedUrl(id));
    }
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('file:view')")
    public R<PageResp<FileUploadResp>> page(FilePageReq req) {
        return R.ok(fileService.page(req));
    }
    @GetMapping("/by-biz")
    @PreAuthorize("hasAuthority('file:view')")
    public R<List<FileUploadResp>> byBiz(@RequestParam String bizType, @RequestParam Long bizId) {
        return R.ok(fileService.getByBiz(bizType, bizId));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('file:delete')")
    public R<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return R.ok();
    }
}