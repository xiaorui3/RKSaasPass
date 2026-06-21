package com.tianji.message.api.client;

import com.tianji.message.api.dto.EmailTemplateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(contextId = "emailTemplateClient", value = "rk-message")
public interface EmailTemplateClient {

    @GetMapping("/api/email-templates/by-code/{templateCode}")
    EmailTemplateDTO queryByCode(@PathVariable("templateCode") String templateCode);
}
