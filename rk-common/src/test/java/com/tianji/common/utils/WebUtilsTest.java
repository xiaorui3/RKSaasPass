package com.tianji.common.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebUtilsTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getRemoteAddr_shouldPreferFirstPublicIpFromForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.8, 10.42.0.12");
        request.setRemoteAddr("10.42.0.12");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("203.0.113.8", WebUtils.getRemoteAddr());
    }

    @Test
    void getRemoteAddr_shouldUseRealIpHeaderBeforeInternalRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.1");
        request.setRemoteAddr("10.42.0.12");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("198.51.100.1", WebUtils.getRemoteAddr());
    }

    @Test
    void getRemoteAddr_shouldIgnoreNonIpFieldsInForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Forwarded", "for=10.42.0.12;proto=https, for=203.0.113.44;host=example.com");
        request.setRemoteAddr("10.42.0.12");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("203.0.113.44", WebUtils.getRemoteAddr());
    }
}
