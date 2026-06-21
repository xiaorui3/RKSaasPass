package com.tianji.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.message.domain.dto.EmailTemplateDTO;
import com.tianji.message.domain.dto.EmailTemplateFormDTO;
import com.tianji.message.domain.po.MessageTemplate;
import com.tianji.message.domain.query.EmailTemplatePageQuery;
import com.tianji.message.mapper.MessageTemplateMapper;
import com.tianji.message.service.IEmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl implements IEmailTemplateService {

    private static final int EMAIL_TEMPLATE_TYPE = 2;

    private final MessageTemplateMapper messageTemplateMapper;

    @Override
    public Long saveEmailTemplate(EmailTemplateFormDTO dto) {
        MessageTemplate entity = new MessageTemplate();
        fillEntity(entity, dto);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setCreator(0L);
        entity.setUpdater(0L);
        messageTemplateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateEmailTemplate(Long id, EmailTemplateFormDTO dto) {
        MessageTemplate existing = getRequired(id);
        fillEntity(existing, dto);
        existing.setId(id);
        existing.setUpdateTime(LocalDateTime.now());
        messageTemplateMapper.updateById(existing);
    }

    @Override
    public void updateEmailTemplateStatus(Long id, Integer status) {
        MessageTemplate existing = getRequired(id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        messageTemplateMapper.updateById(existing);
    }

    @Override
    public void deleteEmailTemplate(Long id) {
        MessageTemplate existing = getRequired(id);
        messageTemplateMapper.deleteById(existing.getId());
    }

    @Override
    public EmailTemplateDTO queryEmailTemplate(Long id) {
        return toDTO(getRequired(id));
    }

    @Override
    public EmailTemplateDTO queryEmailTemplateByCode(String templateCode) {
        if (StringUtils.isBlank(templateCode)) {
            return null;
        }
        MessageTemplate template = messageTemplateMapper.selectOne(
                new LambdaQueryWrapper<MessageTemplate>()
                        .eq(MessageTemplate::getTemplateType, EMAIL_TEMPLATE_TYPE)
                        .eq(MessageTemplate::getTemplateCode, templateCode)
                        .eq(MessageTemplate::getStatus, 1)
                        .last("LIMIT 1")
        );
        return template == null ? null : toDTO(template);
    }

    @Override
    public PageDTO<EmailTemplateDTO> queryEmailTemplates(EmailTemplatePageQuery query) {
        Page<MessageTemplate> page = query.toMpPage();
        Page<MessageTemplate> result = new Page<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());

        Page<MessageTemplate> queried = messageTemplateMapper.selectPage(
                page,
                new LambdaQueryWrapper<MessageTemplate>()
                        .eq(MessageTemplate::getTemplateType, EMAIL_TEMPLATE_TYPE)
                        .eq(query.getStatus() != null, MessageTemplate::getStatus, query.getStatus())
                        .and(StringUtils.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(MessageTemplate::getTemplateName, query.getKeyword())
                                .or()
                                .like(MessageTemplate::getTemplateCode, query.getKeyword()))
                        .orderByDesc(MessageTemplate::getUpdateTime)
        );
        return PageDTO.of(queried, this::toDTO);
    }

    private MessageTemplate getRequired(Long id) {
        MessageTemplate template = messageTemplateMapper.selectById(id);
        if (template == null || !Objects.equals(template.getTemplateType(), EMAIL_TEMPLATE_TYPE)) {
            throw new BadRequestException("邮件模板不存在");
        }
        return template;
    }

    private void fillEntity(MessageTemplate entity, EmailTemplateFormDTO dto) {
        entity.setTenantId(resolveTenantId());
        entity.setTemplateType(EMAIL_TEMPLATE_TYPE);
        entity.setTemplateCode(dto.getTemplateCode());
        entity.setTemplateName(dto.getTemplateName());
        entity.setEmailSubject(dto.getEmailSubject());
        entity.setTemplateContent(dto.getTemplateContent());
        entity.setHtmlMode(Boolean.TRUE.equals(dto.getHtmlMode()) ? 1 : 0);
        entity.setMediaPayload(dto.getMediaPayload());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setIsDeleted(0);
    }

    private EmailTemplateDTO toDTO(MessageTemplate entity) {
        EmailTemplateDTO dto = new EmailTemplateDTO();
        dto.setId(entity.getId());
        dto.setTemplateCode(entity.getTemplateCode());
        dto.setTemplateName(entity.getTemplateName());
        dto.setEmailSubject(entity.getEmailSubject());
        dto.setTemplateContent(entity.getTemplateContent());
        dto.setHtmlMode(entity.getHtmlMode() != null && entity.getHtmlMode() == 1);
        dto.setMediaPayload(entity.getMediaPayload());
        dto.setStatus(entity.getStatus());
        dto.setCreateTime(entity.getCreateTime() == null ? null : entity.getCreateTime().toString());
        return dto;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
