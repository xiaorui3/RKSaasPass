package com.tianji.data.service;

import com.tianji.data.model.vo.SaasVisualScreenVO;

public interface SaasVisualScreenService {
    SaasVisualScreenVO getOverview(Long tenantId);
}
