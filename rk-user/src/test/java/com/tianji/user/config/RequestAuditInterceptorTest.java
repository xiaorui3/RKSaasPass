package com.tianji.user.config;

import com.tianji.user.domain.po.User;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.ISysOperLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestAuditInterceptorTest {

    @Mock
    private ISysOperLogService sysOperLogService;

    @Mock
    private UserMapper userMapper;

    @Test
    void afterCompletion_shouldReuseOperatorNameResolvedDuringPreHandle() {
        RequestAuditInterceptor interceptor = new RequestAuditInterceptor(sysOperLogService, userMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/statistics");
        request.addHeader("user-info", "6");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User();
        user.setUsername("tenant-admin");
        when(userMapper.selectOne(any())).thenReturn(user);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(userMapper, times(1)).selectOne(any());
        verify(sysOperLogService).recordOperation(
                eq("/api/members/statistics"),
                anyString(),
                eq("GET"),
                eq(0),
                eq("tenant-admin"),
                eq("/api/members/statistics"),
                anyString(),
                eq(null),
                eq(0),
                eq(null),
                anyLong()
        );
    }

    @Test
    void afterCompletion_shouldRecordForwardedPublicClientIpInsteadOfProxyIp() {
        RequestAuditInterceptor interceptor = new RequestAuditInterceptor(sysOperLogService, userMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ops/traffic/overview");
        request.addHeader("user-info", "6");
        request.addHeader("X-Forwarded-For", "203.0.113.8, 10.42.0.12");
        request.setRemoteAddr("10.42.0.12");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User();
        user.setUsername("admin_a");
        when(userMapper.selectOne(any())).thenReturn(user);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(sysOperLogService).recordOperation(
                eq("/admin/ops/traffic/overview"),
                anyString(),
                eq("GET"),
                eq(0),
                eq("admin_a"),
                eq("/admin/ops/traffic/overview"),
                eq("203.0.113.8"),
                eq(null),
                eq(0),
                eq(null),
                anyLong()
        );
    }

    @Test
    void afterCompletion_shouldSkipPublicTenantListRequests() {
        RequestAuditInterceptor interceptor = new RequestAuditInterceptor(sysOperLogService, userMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tenants/list");
        request.addHeader("user-info", "6");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(userMapper, never()).selectOne(any());
        verify(sysOperLogService, never()).recordOperation(
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString(),
                anyString(),
                anyString(),
                any(),
                anyInt(),
                any(),
                anyLong()
        );
    }
}
