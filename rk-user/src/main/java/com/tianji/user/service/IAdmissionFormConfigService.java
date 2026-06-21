package com.tianji.user.service;

import com.tianji.user.domain.dto.AdmissionFormConfigDTO;

public interface IAdmissionFormConfigService {

    AdmissionFormConfigDTO getCurrentTenantConfig();

    AdmissionFormConfigDTO getTenantConfig(Long tenantId);

    AdmissionFormConfigDTO saveCurrentTenantConfig(AdmissionFormConfigDTO dto);
}
