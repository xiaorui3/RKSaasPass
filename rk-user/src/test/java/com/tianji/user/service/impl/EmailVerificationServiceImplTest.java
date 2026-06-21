package com.tianji.user.service.impl;

import com.tianji.api.dto.user.EmailVerificationSendDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.message.api.client.AsyncEmailClient;
import com.tianji.message.api.client.EmailTemplateClient;
import com.tianji.message.api.dto.EmailTemplateDTO;
import com.tianji.message.domain.dto.EmailInfoDTO;
import com.tianji.user.mapper.RKTenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private AsyncEmailClient asyncEmailClient;
    @Mock
    private EmailTemplateClient emailTemplateClient;
    @Mock
    private RKTenantMapper tenantMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private EmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void sendCode_shouldEnqueueEmailWhenNotRateLimited() {
        EmailVerificationSendDTO dto = new EmailVerificationSendDTO();
        dto.setEmail("test@example.com");
        dto.setTenantId(1L);
        dto.setScene("REGISTER");
        when(valueOperations.get(contains("email:verify:limit:REGISTER:1:test@example.com"))).thenReturn(null);

        boolean result = service.sendCode(dto);

        assertTrue(result);
        verify(valueOperations).set(contains("email:verify:code:REGISTER:1:test@example.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(valueOperations).set(contains("email:verify:limit:REGISTER:1:test@example.com"), eq("1"), eq(Duration.ofSeconds(60)));

        ArgumentCaptor<EmailInfoDTO> captor = ArgumentCaptor.forClass(EmailInfoDTO.class);
        verify(asyncEmailClient).sendMessage(captor.capture());
        assertEquals("注册邮箱验证码", captor.getValue().getSubject());
        assertTrue(Boolean.TRUE.equals(captor.getValue().getHtml()));
    }

    @Test
    void sendCode_shouldUseLoginTemplateFallbackSubject() {
        EmailVerificationSendDTO dto = new EmailVerificationSendDTO();
        dto.setEmail("test@example.com");
        dto.setTenantId(1L);
        dto.setScene("LOGIN");
        when(valueOperations.get(contains("email:verify:limit:LOGIN:1:test@example.com"))).thenReturn(null);
        when(emailTemplateClient.queryByCode("LOGIN_VERIFY")).thenThrow(new RuntimeException("missing login template"));
        when(emailTemplateClient.queryByCode("REGISTER_VERIFY")).thenReturn(new EmailTemplateDTO());

        boolean result = service.sendCode(dto);

        assertTrue(result);
        ArgumentCaptor<EmailInfoDTO> captor = ArgumentCaptor.forClass(EmailInfoDTO.class);
        verify(asyncEmailClient).sendMessage(captor.capture());
        assertEquals("登录邮箱验证码", captor.getValue().getSubject());
    }

    @Test
    void verifyCode_shouldDeleteKeyWhenCodeMatches() {
        EmailVerificationVerifyDTO dto = new EmailVerificationVerifyDTO();
        dto.setEmail("test@example.com");
        dto.setTenantId(1L);
        dto.setScene("REGISTER");
        dto.setCode("123456");
        when(valueOperations.get(contains("email:verify:code:REGISTER:1:test@example.com"))).thenReturn("123456");

        boolean result = service.verifyCode(dto);

        assertTrue(result);
        verify(stringRedisTemplate).delete(contains("email:verify:code:REGISTER:1:test@example.com"));
    }

    @Test
    void verifyCode_shouldThrowWhenCodeDoesNotMatch() {
        EmailVerificationVerifyDTO dto = new EmailVerificationVerifyDTO();
        dto.setEmail("test@example.com");
        dto.setTenantId(1L);
        dto.setScene("REGISTER");
        dto.setCode("654321");
        when(valueOperations.get(anyString())).thenReturn("123456");

        assertThrows(BadRequestException.class, () -> service.verifyCode(dto));
        verify(stringRedisTemplate, never()).delete(anyString());
    }
}
