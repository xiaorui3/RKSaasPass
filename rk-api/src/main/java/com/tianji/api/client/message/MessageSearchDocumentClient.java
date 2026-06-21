package com.tianji.api.client.message;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(contextId = "messageSearchDocument", value = "rk-message")
public interface MessageSearchDocumentClient {

    @GetMapping("/api/search-documents/internal/export")
    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
