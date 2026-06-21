package com.tianji.message.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.message.domain.dto.EmailTemplateDTO;
import com.tianji.message.domain.dto.EmailTemplateFormDTO;
import com.tianji.message.domain.query.EmailTemplatePageQuery;

public interface IEmailTemplateService {

    Long saveEmailTemplate(EmailTemplateFormDTO dto);

    void updateEmailTemplate(Long id, EmailTemplateFormDTO dto);

    void updateEmailTemplateStatus(Long id, Integer status);

    void deleteEmailTemplate(Long id);

    EmailTemplateDTO queryEmailTemplate(Long id);

    EmailTemplateDTO queryEmailTemplateByCode(String templateCode);

    PageDTO<EmailTemplateDTO> queryEmailTemplates(EmailTemplatePageQuery query);
}
