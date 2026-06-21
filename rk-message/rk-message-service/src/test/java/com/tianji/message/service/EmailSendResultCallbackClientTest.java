package com.tianji.message.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailSendResultCallbackClientTest {

    @Test
    void buildSuccessUrl_shouldUseGatewayDefaultBasePath() {
        EmailSendResultCallbackClient client = new EmailSendResultCallbackClient(new RestTemplateBuilder());
        ReflectionTestUtils.setField(client, "callbackBaseUrl", EmailSendResultCallbackClient.DEFAULT_CALLBACK_BASE_URL);

        String url = client.buildSuccessUrl(10L, 100L);

        assertEquals("http://rk-gateway:10010/api/email-center/internal/tasks/10/recipients/100/success", url);
    }

    @Test
    void buildFailureUrl_shouldEncodeErrorMessage() {
        EmailSendResultCallbackClient client = new EmailSendResultCallbackClient(new RestTemplateBuilder());
        ReflectionTestUtils.setField(client, "callbackBaseUrl", EmailSendResultCallbackClient.DEFAULT_CALLBACK_BASE_URL);

        String url = client.buildFailureUrl(11L, 200L, "smtp boom");

        assertEquals("http://rk-gateway:10010/api/email-center/internal/tasks/11/recipients/200/failure?errorMessage=smtp boom", url);
    }
}
