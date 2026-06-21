package com.tianji.message.controller;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.utils.TenantContext;
import com.tianji.message.service.INoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search-documents/internal")
@RequiredArgsConstructor
public class SearchDocumentInternalController {

    private final INoticeService noticeService;

    @GetMapping("/export")
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        return runWithoutTenantIsolation(noticeService::exportSearchDocuments);
    }

    private List<GlobalSearchDocumentDTO> runWithoutTenantIsolation(SearchDocumentExporter exporter) {
        Long originalTenantId = TenantContext.getTenantId();
        Boolean originalSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setSuperAdmin(true);
            TenantContext.removeTenantId();
            return exporter.export();
        } finally {
            TenantContext.clear();
            if (originalTenantId != null) {
                TenantContext.setTenantId(originalTenantId);
            }
            TenantContext.setSuperAdmin(originalSuperAdmin);
        }
    }

    private interface SearchDocumentExporter {
        List<GlobalSearchDocumentDTO> export();
    }
}
