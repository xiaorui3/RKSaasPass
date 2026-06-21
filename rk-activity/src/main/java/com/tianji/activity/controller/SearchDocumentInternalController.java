package com.tianji.activity.controller;

import com.tianji.activity.service.IActivityService;
import com.tianji.activity.service.ICompetitionService;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.utils.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/search-documents/internal")
@RequiredArgsConstructor
public class SearchDocumentInternalController {

    private final IActivityService activityService;
    private final ICompetitionService competitionService;

    @GetMapping("/export")
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        return runWithoutTenantIsolation(() -> {
            List<GlobalSearchDocumentDTO> documents = new ArrayList<>();
            documents.addAll(activityService.exportSearchDocuments());
            documents.addAll(competitionService.exportSearchDocuments());
            return documents;
        });
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
