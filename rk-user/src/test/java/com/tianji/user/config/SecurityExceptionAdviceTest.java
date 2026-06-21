package com.tianji.user.config;

import com.tianji.common.constants.Constant;
import com.tianji.common.domain.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityExceptionAdviceTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void handleAccessDeniedException_shouldReturnForbiddenBusinessCodeForGatewayRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Constant.REQUEST_FROM_HEADER, Constant.GATEWAY_ORIGIN_NAME);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));

        Object response = new SecurityExceptionAdvice().handleAccessDeniedException(new AccessDeniedException("Access is denied"));

        R<?> body = (R<?>) response;
        assertEquals(403, body.getCode());
        assertEquals("拒绝访问，权限不足", body.getMsg());
    }
}
