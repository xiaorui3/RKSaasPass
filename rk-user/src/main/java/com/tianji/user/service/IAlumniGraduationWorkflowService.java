package com.tianji.user.service;

import com.tianji.user.domain.dto.AlumniProfileSubmitDTO;
import com.tianji.user.domain.vo.AlumniGraduationResultVO;
import com.tianji.user.domain.vo.AlumniProfileFormVO;

public interface IAlumniGraduationWorkflowService {

    AlumniGraduationResultVO runAnnualGraduation(Integer processYear, Long tenantId);

    AlumniProfileFormVO getProfileForm(String token);

    Boolean submitProfileForm(String token, AlumniProfileSubmitDTO dto);
}
