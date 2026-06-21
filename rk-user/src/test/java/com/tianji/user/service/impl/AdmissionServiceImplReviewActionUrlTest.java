package com.tianji.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.api.client.auth.AuthClient;
import com.tianji.message.api.client.EmailTemplateClient;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.JoinRequestMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.ReferralCodeMapper;
import com.tianji.user.mapper.RegisterReviewRequestMapper;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.IEmailCenterService;
import com.tianji.user.service.IEmailVerificationService;
import com.tianji.user.service.IReferralCodeService;
import com.tianji.user.service.ITenantWorkflowConfigService;
import com.tianji.user.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceImplReviewActionUrlTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthClient authClient;
    @Mock
    private IEmailCenterService emailCenterService;
    @Mock
    private IEmailVerificationService emailVerificationService;
    @Mock
    private IReferralCodeService referralCodeService;
    @Mock
    private IUserService userService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RKTenantMapper tenantMapper;
    @Mock
    private ReferralCodeMapper referralCodeMapper;
    @Mock
    private ClubMemberMapper clubMemberMapper;
    @Mock
    private EmailTemplateClient emailTemplateClient;
    @Mock
    private RegisterReviewRequestMapper registerReviewRequestMapper;
    @Mock
    private ReviewActionTokenServiceImpl reviewActionTokenService;
    @Mock
    private ITenantWorkflowConfigService tenantWorkflowConfigService;
    @Mock
    private SystemConfigMapper systemConfigMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private AdmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdmissionServiceImpl(
                redisTemplate,
                passwordEncoder,
                authClient,
                emailCenterService,
                emailVerificationService,
                referralCodeService,
                userService,
                userMapper,
                tenantMapper,
                referralCodeMapper,
                clubMemberMapper,
                emailTemplateClient,
                registerReviewRequestMapper,
                reviewActionTokenService,
                tenantWorkflowConfigService,
                systemConfigMapper,
                new ObjectMapper().findAndRegisterModules(),
                stringRedisTemplate
        );
    }

    @Test
    void buildReviewActionUrls_shouldFallbackToFrontendDomainWhenConfiguredBaseUrlUsesGatewayPort() {
        ReflectionTestUtils.setField(service, "publicWebBaseUrl", "https://203.0.113.10:10010");
        when(reviewActionTokenService.createToken(eq(2L), eq("REGISTER"), eq(88L), eq("APPROVE"))).thenReturn("approve-token");
        when(reviewActionTokenService.createToken(eq(2L), eq("REGISTER"), eq(88L), eq("REJECT"))).thenReturn("reject-token");

        @SuppressWarnings("unchecked")
        Map<String, String> urls = (Map<String, String>) ReflectionTestUtils.invokeMethod(
                service,
                "buildReviewActionUrls",
                2L,
                "REGISTER",
                88L
        );

        assertEquals("https://example.com/review/admission?token=approve-token", urls.get("approveUrl"));
        assertEquals("https://example.com/review/admission?token=reject-token", urls.get("rejectUrl"));
    }

    @Test
    void buildReviewActionUrls_shouldKeepConfiguredPublicDomainWhenConfigIsClean() {
        ReflectionTestUtils.setField(service, "publicWebBaseUrl", "https://example.com");
        when(reviewActionTokenService.createToken(eq(2L), eq("JOIN"), eq(66L), eq("APPROVE"))).thenReturn("join-approve-token");
        when(reviewActionTokenService.createToken(eq(2L), eq("JOIN"), eq(66L), eq("REJECT"))).thenReturn("join-reject-token");

        @SuppressWarnings("unchecked")
        Map<String, String> urls = (Map<String, String>) ReflectionTestUtils.invokeMethod(
                service,
                "buildReviewActionUrls",
                2L,
                "JOIN",
                66L
        );

        assertEquals("https://example.com/review/admission?token=join-approve-token", urls.get("approveUrl"));
        assertEquals("https://example.com/review/admission?token=join-reject-token", urls.get("rejectUrl"));
    }
}
