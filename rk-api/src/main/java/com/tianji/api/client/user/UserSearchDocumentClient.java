package com.tianji.api.client.user;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(contextId = "userSearchDocument", value = "rk-user")
public interface UserSearchDocumentClient {

    @GetMapping("/api/search-documents/internal/export")
    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
