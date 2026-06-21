package com.tianji.api.client.activity;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(contextId = "activitySearchDocument", value = "rk-activity")
public interface ActivitySearchDocumentClient {

    @GetMapping("/api/search-documents/internal/export")
    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
