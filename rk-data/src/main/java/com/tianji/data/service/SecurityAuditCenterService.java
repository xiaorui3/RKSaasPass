package com.tianji.data.service;

import com.tianji.data.model.dto.SecurityAuditNotifyRequestDTO;
import com.tianji.data.model.vo.SecurityAuditCenterVO;

import java.util.List;

public interface SecurityAuditCenterService {
    SecurityAuditCenterVO getSecurityAuditCenter(Long tenantId, String riskType, String riskLevel, String notifyStatus);

    SecurityAuditCenterVO.NotifyLog notifyRisk(SecurityAuditNotifyRequestDTO request);

    List<SecurityAuditCenterVO> warmupAllTenants();
}
