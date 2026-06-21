package com.tianji.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSendResultCallbackClient {

    static final String DEFAULT_CALLBACK_BASE_URL = "http://rk-gateway:10010";

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${rk.message.callback-base-url:" + DEFAULT_CALLBACK_BASE_URL + "}")
    private String callbackBaseUrl;

    public void markRecipientSuccess(Long taskId, Long recipientId) {
        String url = buildSuccessUrl(taskId, recipientId);
        post(url);
    }

    public void markRecipientFailure(Long taskId, Long recipientId, String errorMessage) {
        String url = buildFailureUrl(taskId, recipientId, errorMessage);
        post(url);
    }

    String buildSuccessUrl(Long taskId, Long recipientId) {
        return UriComponentsBuilder.fromHttpUrl(callbackBaseUrl)
                .path("/api/email-center/internal/tasks/{taskId}/recipients/{recipientId}/success")
                .buildAndExpand(taskId, recipientId)
                .toUriString();
    }

    String buildFailureUrl(Long taskId, Long recipientId, String errorMessage) {
        return UriComponentsBuilder.fromHttpUrl(callbackBaseUrl)
                .path("/api/email-center/internal/tasks/{taskId}/recipients/{recipientId}/failure")
                .queryParam("errorMessage", errorMessage)
                .buildAndExpand(taskId, recipientId)
                .toUriString();
    }

    private void post(String url) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
        log.debug("email result callback posted, url={}, status={}", url, response.getStatusCode());
    }
}
