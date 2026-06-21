package com.tianji.data.service;

import com.tianji.data.model.vo.ParticipationAnalysisCenterVO;

import java.util.List;

public interface ParticipationAnalysisCenterService {
    ParticipationAnalysisCenterVO getParticipationAnalysisCenter(Long tenantId, String dimension, String timeRange);

    List<ParticipationAnalysisCenterVO> warmupAllTenants();
}
