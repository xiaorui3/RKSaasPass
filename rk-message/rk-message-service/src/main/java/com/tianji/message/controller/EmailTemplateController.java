package com.tianji.message.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.message.domain.dto.EmailTemplateDTO;
import com.tianji.message.domain.dto.EmailTemplateFormDTO;
import com.tianji.message.domain.query.EmailTemplatePageQuery;
import com.tianji.message.service.IEmailTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "邮件模板管理接口")
@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final IEmailTemplateService emailTemplateService;

    @PostMapping
    @ApiOperation("新增邮件模板")
    public Long saveEmailTemplate(@RequestBody EmailTemplateFormDTO dto) {
        return emailTemplateService.saveEmailTemplate(dto);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新邮件模板")
    public void updateEmailTemplate(@PathVariable Long id, @RequestBody EmailTemplateFormDTO dto) {
        emailTemplateService.updateEmailTemplate(id, dto);
    }

    @PutMapping("/{id}/status/{status}")
    @ApiOperation("更新邮件模板状态")
    public void updateEmailTemplateStatus(@PathVariable Long id, @PathVariable Integer status) {
        emailTemplateService.updateEmailTemplateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除邮件模板")
    public void deleteEmailTemplate(@PathVariable Long id) {
        emailTemplateService.deleteEmailTemplate(id);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询邮件模板详情")
    public EmailTemplateDTO queryEmailTemplate(@PathVariable Long id) {
        return emailTemplateService.queryEmailTemplate(id);
    }

    @GetMapping("/by-code/{templateCode}")
    @ApiOperation("按模板编码查询邮件模板")
    public EmailTemplateDTO queryEmailTemplateByCode(@PathVariable String templateCode) {
        return emailTemplateService.queryEmailTemplateByCode(templateCode);
    }

    @GetMapping
    @ApiOperation("分页查询邮件模板")
    public PageDTO<EmailTemplateDTO> queryEmailTemplates(EmailTemplatePageQuery query) {
        return emailTemplateService.queryEmailTemplates(query);
    }
}
