package com.tianji.data.service;

import com.tianji.data.model.dto.DataQualityRepairRequestDTO;
import com.tianji.data.model.vo.DataQualityCenterVO;

public interface DataQualityCenterService {
    DataQualityCenterVO getDataQualityCenter(Long tenantId, String issueType, String riskLevel);

    DataQualityCenterVO.RepairLog repair(DataQualityRepairRequestDTO request);
}
