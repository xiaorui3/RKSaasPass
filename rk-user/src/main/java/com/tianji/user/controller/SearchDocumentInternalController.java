package com.tianji.user.controller;

import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.service.IClubAlumniService;
import com.tianji.user.service.IClubMemberService;
import com.tianji.user.service.IHistoryService;
import com.tianji.user.service.IRKTenantService;
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

    private final IRKTenantService tenantService;
    private final IClubMemberService clubMemberService;
    private final IClubAlumniService clubAlumniService;
    private final IHistoryService historyService;

    @GetMapping("/export")
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        return runWithoutTenantIsolation(() -> {
            List<GlobalSearchDocumentDTO> documents = new ArrayList<>();
            documents.addAll(tenantService.exportSearchDocuments());
            documents.addAll(clubMemberService.exportSearchDocuments());
            documents.addAll(clubAlumniService.exportSearchDocuments());
            documents.addAll(historyService.exportSearchDocuments());
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
