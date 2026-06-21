package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.AdmissionFormConfigDTO;
import com.tianji.user.service.IAdmissionFormConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api(tags = "入社表单配置")
@RestController
@RequestMapping("/api/admission/form-config")
@RequiredArgsConstructor
public class AdmissionFormConfigController {

    private final IAdmissionFormConfigService admissionFormConfigService;

    @ApiOperation("获取公开入社表单配置")
    @GetMapping("/public")
    public R<AdmissionFormConfigDTO> getPublicConfig(@RequestParam(value = "tenantId", required = false) Long tenantId) {
        if (tenantId != null) {
            return R.ok(admissionFormConfigService.getTenantConfig(tenantId));
        }
        return R.ok(admissionFormConfigService.getCurrentTenantConfig());
    }

    @ApiOperation("获取当前租户入社表单配置")
    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:review')")
    public R<AdmissionFormConfigDTO> getCurrentConfig() {
        return R.ok(admissionFormConfigService.getCurrentTenantConfig());
    }

    @ApiOperation("保存当前租户入社表单配置")
    @PutMapping("/current")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:review')")
    public R<AdmissionFormConfigDTO> saveCurrentConfig(@Validated @RequestBody AdmissionFormConfigDTO dto) {
        return R.ok(admissionFormConfigService.saveCurrentTenantConfig(dto));
    }
}
