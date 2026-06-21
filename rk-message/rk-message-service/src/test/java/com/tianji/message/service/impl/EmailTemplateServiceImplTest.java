package com.tianji.message.service.impl;

import com.tianji.common.exceptions.BadRequestException;
import com.tianji.message.mapper.MessageTemplateMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailTemplateServiceImplTest {

    @Test
    void queryEmailTemplate_shouldReturnBadRequestWhenTemplateMissing() {
        MessageTemplateMapper mapper = mock(MessageTemplateMapper.class);
        when(mapper.selectById(1L)).thenReturn(null);

        EmailTemplateServiceImpl service = new EmailTemplateServiceImpl(mapper);

        assertThrows(BadRequestException.class, () -> service.queryEmailTemplate(1L));
    }
}
