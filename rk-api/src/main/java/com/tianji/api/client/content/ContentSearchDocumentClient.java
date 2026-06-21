package com.tianji.api.client.content;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(contextId = "contentSearchDocument", value = "rk-content")
public interface ContentSearchDocumentClient {

    @GetMapping("/api/search-documents/internal/export")
    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
