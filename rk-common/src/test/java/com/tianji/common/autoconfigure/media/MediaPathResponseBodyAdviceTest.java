package com.tianji.common.autoconfigure.media;

import com.tianji.common.constants.Constant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaPathResponseBodyAdviceTest {

    private final MediaPathResponseBodyAdvice advice = new MediaPathResponseBodyAdvice(
            new MediaPathHelper(new MediaPathProperties())
    );

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void supports_shouldSkipBinaryResponseEntity() throws Exception {
        bindGatewayRequest();

        Method method = SampleController.class.getMethod("download");
        MethodParameter returnType = new MethodParameter(method, -1);

        assertFalse(advice.supports(returnType, MappingJackson2HttpMessageConverter.class));
    }

    @Test
    void supports_shouldAllowNormalGatewayResponses() throws Exception {
        bindGatewayRequest();

        Method method = SampleController.class.getMethod("plain");
        MethodParameter returnType = new MethodParameter(method, -1);

        assertTrue(advice.supports(returnType, MappingJackson2HttpMessageConverter.class));
    }

    private void bindGatewayRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Constant.REQUEST_FROM_HEADER, Constant.GATEWAY_ORIGIN_NAME);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
    }

    private static class SampleController {
        public ResponseEntity<byte[]> download() {
            return null;
        }

        public String plain() {
            return null;
        }
    }
}
