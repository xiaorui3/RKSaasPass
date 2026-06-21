package com.tianji.data.service;

import com.tianji.data.model.vo.GrowthRetentionCenterVO;

import java.util.List;

public interface GrowthRetentionCenterService {
    GrowthRetentionCenterVO getGrowthRetentionCenter(Long tenantId, String timeRange, String riskLevel);

    List<GrowthRetentionCenterVO> warmupAllTenants();
}
