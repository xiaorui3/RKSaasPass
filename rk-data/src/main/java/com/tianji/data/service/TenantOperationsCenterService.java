package com.tianji.data.service;

import com.tianji.data.model.vo.TenantOperationsCenterVO;

import java.util.List;

public interface TenantOperationsCenterService {
    TenantOperationsCenterVO getOverview(Long tenantId);

    List<TenantOperationsCenterVO> warmupAllTenants();
}
