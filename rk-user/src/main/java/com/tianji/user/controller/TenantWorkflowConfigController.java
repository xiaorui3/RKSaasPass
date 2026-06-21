package com.tianji.user.controller;

import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.service.ITenantWorkflowConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "租户工作流配置")
@RestController
@RequestMapping("/api/workflow/config")
@RequiredArgsConstructor
public class TenantWorkflowConfigController {

    private final ITenantWorkflowConfigService tenantWorkflowConfigService;

    @ApiOperation("获取当前租户工作流配置")
    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('system:tenant:view','system:user:view','content:news:edit')")
    public R<TenantWorkflowConfigDTO> current() {
        return R.ok(tenantWorkflowConfigService.loadCurrentConfig(resolveTenantId()));
    }

    @ApiOperation("保存当前租户工作流配置")
    @PutMapping("/current")
    @PreAuthorize("hasAnyAuthority('system:tenant:edit','content:news:edit')")
    public R<TenantWorkflowConfigDTO> saveCurrent(@RequestBody TenantWorkflowConfigDTO dto) {
        return R.ok(tenantWorkflowConfigService.saveCurrentConfig(resolveTenantId(), dto));
    }

    @GetMapping("/current/internal")
    public TenantWorkflowConfigDTO currentInternal(@RequestParam("tenantId") Long tenantId) {
        return tenantWorkflowConfigService.loadCurrentConfig(tenantId);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
