package com.bbpms.notify.controller;
import jakarta.validation.Valid;
import com.bbpms.common.result.R;
import com.bbpms.notify.dto.MessageTemplateCreateReq;
import com.bbpms.notify.dto.MessageTemplatePageReq;
import com.bbpms.notify.entity.MessageTemplate;
import com.bbpms.notify.service.MessageTemplateService;
import com.bbpms.common.result.PageResp;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/notify/templates")
@RequiredArgsConstructor
public class MessageTemplateController {
    private final MessageTemplateService templateService;
    @PostMapping
    @PreAuthorize("hasAuthority('notify:template:edit')")
    public R<Long> create(@Valid @RequestBody MessageTemplateCreateReq req) {
        return R.ok(templateService.create(req));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('notify:template:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody MessageTemplateCreateReq req) {
        req.setId(id);
        templateService.update(req);
        return R.ok();
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('notify:template:edit')")
    public R<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return R.ok();
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('notify:template:view')")
    public R<MessageTemplate> getById(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }
    @GetMapping
    @PreAuthorize("hasAuthority('notify:template:view')")
    public R<List<MessageTemplate>> list() {
        return R.ok(templateService.listEnabled());
    }
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('notify:view')")
    public R<PageResp<MessageTemplate>> page(MessageTemplatePageReq req) {
        return R.ok(templateService.page(req));
    }
}
