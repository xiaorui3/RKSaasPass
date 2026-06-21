package com.tianji.user.service;

import com.tianji.api.dto.user.TenantWorkflowConfigDTO;

public interface ITenantWorkflowConfigService {
    TenantWorkflowConfigDTO loadCurrentConfig(Long tenantId);

    TenantWorkflowConfigDTO saveCurrentConfig(Long tenantId, TenantWorkflowConfigDTO dto);
}
