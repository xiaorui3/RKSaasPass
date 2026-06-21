package com.tianji.data.service;

import com.tianji.data.model.vo.ApprovalTaskCenterVO;

import java.util.List;

public interface ApprovalTaskCenterService {
    ApprovalTaskCenterVO getApprovalTaskCenter(Long tenantId, String taskType, String slaStatus, String status);

    List<ApprovalTaskCenterVO> warmupAllTenants();
}
