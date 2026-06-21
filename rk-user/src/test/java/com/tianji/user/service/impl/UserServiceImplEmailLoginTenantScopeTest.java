package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.dto.user.EmailLoginCandidateDTO;
import com.tianji.api.dto.user.EmailLoginCandidateQueryDTO;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.User;
import com.tianji.user.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplEmailLoginTenantScopeTest {

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        TenantContext.clear();
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void queryEmailLoginCandidates_shouldTemporarilyIgnoreAmbientTenantScope() {
        User candidate = new User();
        candidate.setId(5557L);
        candidate.setAuthUserId(5464L);
        candidate.setTenantId(30L);
        candidate.setUsername("xiaorui");
        candidate.setEmail("3505469466@qq.com");
        candidate.setRealName("赵锐");
        candidate.setStatus(com.tianji.user.enums.UserStatus.NORMAL);
        candidate.setIsDeleted(0);

        AtomicReference<Long> tenantSeenByMapper = new AtomicReference<>();
        AtomicReference<Boolean> superAdminSeenByMapper = new AtomicReference<>();
        when(userMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            tenantSeenByMapper.set(TenantContext.getTenantId());
            superAdminSeenByMapper.set(TenantContext.isSuperAdmin());
            return List.of(candidate);
        });

        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(false);

        EmailLoginCandidateQueryDTO dto = new EmailLoginCandidateQueryDTO();
        dto.setEmail("3505469466@qq.com");

        List<EmailLoginCandidateDTO> result = service.queryEmailLoginCandidates(dto);

        assertEquals(1, result.size());
        assertEquals("xiaorui", result.get(0).getUsername());
        assertNull(tenantSeenByMapper.get());
        assertTrue(Boolean.TRUE.equals(superAdminSeenByMapper.get()));
        assertEquals(1L, TenantContext.getTenantId());
        assertFalse(Boolean.TRUE.equals(TenantContext.isSuperAdmin()));
    }
}
