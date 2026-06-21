package com.tianji.message.service.impl;

import com.tianji.api.client.auth.AuthClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.auth.RoleAccountDTO;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;
import com.tianji.api.dto.user.TenantUserRecipientQueryDTO;
import com.tianji.message.domain.dto.ContactPublicShieldDTO;
import com.tianji.message.domain.po.ContactUsMessage;
import com.tianji.message.service.IEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactUsServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private AuthClient authClient;
    @Mock
    private UserClient userClient;
    @Mock
    private IEmailService emailService;

    @Spy
    @InjectMocks
    private ContactUsServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void submitMessage_shouldNotifyTenantAdminsUsingTenantScopedUserEmails() throws Exception {
        ContactUsMessage message = new ContactUsMessage();
        message.setTenantId(3L);
        message.setName("admin_a");
        message.setEmail("");
        message.setSubject("合作咨询");
        message.setMessage("这里是一条足够长的联系留言内容，用来验证通知发给当前租户管理员。");
        message.setIpAddress("127.0.0.1");

        RoleDTO adminRole = new RoleDTO();
        adminRole.setId(5L);
        adminRole.setCode("ADMIN");

        RoleAccountDTO roleAccount = new RoleAccountDTO();
        roleAccount.setAccountId(66L);
        roleAccount.setTenantId(3L);
        roleAccount.setUsername("tenant-admin");

        doReturn(true).when(service).save(any(ContactUsMessage.class));
        when(valueOperations.increment(any(String.class))).thenReturn(1L);
        when(valueOperations.setIfAbsent(any(String.class), eq(1), any(Long.class), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(authClient.listAllRoles(3L)).thenReturn(List.of(adminRole));
        when(authClient.queryAccountsByRoles(any())).thenReturn(List.of(roleAccount));
        when(userClient.queryRecipientEmails(any())).thenReturn(List.of("tenant-admin@example.com"));
        when(emailService.sendHtmlEmail(eq("tenant-admin@example.com"), contains("合作咨询"), contains("admin_a")))
                .thenReturn(true);

        boolean result = service.submitMessage(message);

        assertTrue(result);
        verify(userClient).queryRecipientEmails(argThat((TenantUserRecipientQueryDTO dto) ->
                dto != null
                        && Long.valueOf(3L).equals(dto.getTenantId())
                        && dto.getAuthUserIds() != null
                        && dto.getAuthUserIds().contains(66L)
                        && dto.getUsernames() != null
                        && dto.getUsernames().contains("tenant-admin")));
        verify(emailService).sendHtmlEmail(eq("tenant-admin@example.com"), contains("合作咨询"), contains("admin_a"));
        verify(authClient, never()).queryAdminUserById(any());
    }

    @Test
    void issuePublicShield_shouldPersistShieldPayload() {
        ContactPublicShieldDTO shield = service.issuePublicShield(8L, "10.0.0.8", "Mozilla/5.0");

        assertNotNull(shield.getToken());
        assertTrue(shield.getIssuedAt() > 0);
        assertEquals(1200L, shield.getMinSubmitDelayMs());
        assertEquals(900L, shield.getExpiresInSeconds());
        verify(valueOperations).set(
                eq("contact:public:shield:" + shield.getToken()),
                contains("8|10.0.0.8|"),
                eq(900L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void submitPublicMessage_shouldConsumeShieldAndSaveMessage() throws Exception {
        ContactUsMessage message = new ContactUsMessage();
        message.setTenantId(8L);
        message.setName("visitor");
        message.setEmail("visitor@example.com");
        message.setSubject("test");
        message.setMessage("这里是一条足够长的公开联系留言内容。");
        message.setIpAddress("10.0.0.8");
        message.setUserAgent("Mozilla/5.0");

        long issuedAt = System.currentTimeMillis() - 5000;
        String shieldPayload = issuedAt + "|8|10.0.0.8|" + Integer.toHexString("Mozilla/5.0".hashCode());

        doReturn(true).when(service).save(any(ContactUsMessage.class));
        when(valueOperations.get("contact:public:shield:shield-token")).thenReturn(shieldPayload);
        when(valueOperations.increment(any(String.class))).thenReturn(1L);
        when(valueOperations.setIfAbsent(any(String.class), eq(1), any(Long.class), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(userClient.verifyEmailVerificationCode(any(EmailVerificationVerifyDTO.class))).thenReturn(true);

        assertTrue(service.submitPublicMessage(message, "shield-token", issuedAt, "", "123456"));
        verify(redisTemplate).delete("contact:public:shield:shield-token");
        verify(userClient).verifyEmailVerificationCode(any(EmailVerificationVerifyDTO.class));
        verify(service, times(1)).save(any(ContactUsMessage.class));
    }

    @Test
    void submitPublicMessage_shouldVerifyEmailCodeBeforeSavingWhenCodeProvided() throws Exception {
        ContactUsMessage message = new ContactUsMessage();
        message.setTenantId(8L);
        message.setName("visitor");
        message.setEmail("visitor@example.com");
        message.setSubject("test");
        message.setMessage("this is a long enough contact message");
        message.setIpAddress("10.0.0.8");
        message.setUserAgent("Mozilla/5.0");

        long issuedAt = System.currentTimeMillis() - 5000;
        String shieldPayload = issuedAt + "|8|10.0.0.8|" + Integer.toHexString("Mozilla/5.0".hashCode());

        doReturn(true).when(service).save(any(ContactUsMessage.class));
        when(valueOperations.get("contact:public:shield:shield-token")).thenReturn(shieldPayload);
        when(valueOperations.increment(any(String.class))).thenReturn(1L);
        when(valueOperations.setIfAbsent(any(String.class), eq(1), any(Long.class), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(userClient.verifyEmailVerificationCode(any(EmailVerificationVerifyDTO.class))).thenReturn(true);

        assertTrue(service.submitPublicMessage(message, "shield-token", issuedAt, "", "123456"));

        verify(userClient).verifyEmailVerificationCode(argThat((EmailVerificationVerifyDTO dto) ->
                dto != null
                        && Long.valueOf(8L).equals(dto.getTenantId())
                        && "visitor@example.com".equals(dto.getEmail())
                        && "CONTACT".equals(dto.getScene())
                        && "123456".equals(dto.getCode())));
        verify(service, times(1)).save(any(ContactUsMessage.class));
    }

    @Test
    void submitMessage_shouldRejectWhenSameTenantAndIpExceedsRateLimit() throws Exception {
        ContactUsMessage message = new ContactUsMessage();
        message.setTenantId(9L);
        message.setName("spammer");
        message.setEmail("");
        message.setSubject("test");
        message.setMessage("test message");
        message.setIpAddress("10.0.0.10");

        doReturn(true).when(service).save(any(ContactUsMessage.class));
        when(valueOperations.increment(any(String.class))).thenReturn(1L, 2L, 3L, 4L);
        when(valueOperations.setIfAbsent(any(String.class), eq(1), any(Long.class), eq(TimeUnit.MINUTES)))
                .thenReturn(true, true, true, true);

        assertTrue(service.submitMessage(message));
        assertTrue(service.submitMessage(message));
        assertTrue(service.submitMessage(message));
        assertFalse(service.submitMessage(message));
        verify(service, times(3)).save(any(ContactUsMessage.class));
    }

    @Test
    void submitMessage_shouldRejectDuplicatePayloadWithinWindow() throws Exception {
        ContactUsMessage message = new ContactUsMessage();
        message.setTenantId(9L);
        message.setName("spammer");
        message.setEmail("");
        message.setSubject("test");
        message.setMessage("test message");
        message.setIpAddress("10.0.0.10");

        doReturn(true).when(service).save(any(ContactUsMessage.class));
        when(valueOperations.increment(any(String.class))).thenReturn(1L, 2L);
        when(valueOperations.setIfAbsent(any(String.class), eq(1), any(Long.class), eq(TimeUnit.MINUTES)))
                .thenReturn(true, false);

        assertTrue(service.submitMessage(message));
        assertFalse(service.submitMessage(message));
        verify(service, times(1)).save(any(ContactUsMessage.class));
    }

    @Test
    void getMessageById_shouldReturnDatabaseResultWhenCacheWriteFails() {
        ContactUsMessage message = new ContactUsMessage();
        message.setId(7L);
        message.setName("visitor");
        message.setSubject("cache");
        message.setMessage("cache fallback");

        when(valueOperations.get("contact:message:7")).thenReturn(null);
        doReturn(message).when(service).getById(7L);
        doThrow(new RuntimeException("redis serializer failed"))
                .when(valueOperations)
                .set("contact:message:7", message, 24L, TimeUnit.HOURS);

        ContactUsMessage result = service.getMessageById(7L);

        assertEquals(message, result);
    }
}
