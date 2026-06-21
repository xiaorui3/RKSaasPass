package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.client.auth.AuthClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.message.api.client.EmailTemplateClient;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.user.domain.po.JoinRequest;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.JoinRequestMapper;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.IEmailCenterService;
import com.tianji.user.service.IEmailVerificationService;
import com.tianji.user.service.IReferralCodeService;
import com.tianji.user.service.IUserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceImplEmailVerificationTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SystemConfig.class);
    }

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthClient authClient;
    @Mock
    private IEmailCenterService emailCenterService;
    @Mock
    private IReferralCodeService referralCodeService;
    @Mock
    private IUserService userService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ClubMemberMapper clubMemberMapper;
    @Mock
    private JoinRequestMapper joinRequestMapper;
    @Mock
    private IEmailVerificationService emailVerificationService;
    @Mock
    private EmailTemplateClient emailTemplateClient;
    @Mock
    private SystemConfigMapper systemConfigMapper;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    @InjectMocks
    private AdmissionServiceImpl service;

    @Test
    void submitApplication_shouldRejectWhenJoinEmailCodeIsInvalid() {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setTenantId(2L);
        joinRequest.setName("跨租户申请人");
        joinRequest.setStudentId("S20260414001");
        joinRequest.setUsername("member_cross_join");
        joinRequest.setPassword("Passw0rd!");
        joinRequest.setEmail("cross@example.com");
        joinRequest.setEmailCode("000000");

        when(emailVerificationService.verifyCode(any())).thenThrow(new BadRequestException("邮箱验证码错误或已过期"));

        assertThrows(BadRequestException.class, () -> service.submitApplication(joinRequest));
        verify(emailVerificationService).verifyCode(any());
    }

    @Test
    void submitApplication_shouldRejectWhenTenantSelfServiceClosesJoinApplication() {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setTenantId(2L);
        joinRequest.setName("跨租户申请人");
        joinRequest.setStudentId("S20260414002");
        joinRequest.setUsername("member_cross_join_closed");
        joinRequest.setPassword("Passw0rd!");
        joinRequest.setEmail("closed@example.com");
        joinRequest.setEmailCode("123456");

        SystemConfig config = new SystemConfig()
                .setTenantId(2L)
                .setConfigKey("tenant.self-service.config")
                .setConfigValue("{\"admissionSettings\":{\"allowPublicRegister\":true,\"allowJoinApplication\":false}}");
        when(systemConfigMapper.selectOne(any())).thenReturn(config);

        assertThrows(BadRequestException.class, () -> service.submitApplication(joinRequest));

        verify(emailVerificationService, org.mockito.Mockito.never()).verifyCode(any());
    }
}
