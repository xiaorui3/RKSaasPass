package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.user.domain.dto.ReferralCodeDTO;
import com.tianji.user.domain.dto.ReferralCodeQueryDTO;
import com.tianji.user.domain.po.ReferralCode;
import com.tianji.user.domain.vo.ReferralConversionOverviewVO;

public interface IReferralCodeService extends IService<ReferralCode> {

    Page<ReferralCodeDTO> queryReferralCodes(ReferralCodeQueryDTO queryDTO);

    String generateReferralCode(ReferralCodeDTO dto);

    void updateReferralCode(Long id, ReferralCodeDTO dto);

    void deleteReferralCode(Long id);

    ReferralCodeDTO getReferralCodeDetail(Long id);

    String generateUniqueCode();

    boolean validateReferralCode(String code);

    boolean validateReferralCode(String code, Long tenantId);

    boolean markGenericRegisterSuccess(Long tenantId, String referralCode, String targetEmail, Long authUserId);

    boolean markGenericJoinSubmitted(Long tenantId, String referralCode, String targetEmail, Long joinRequestId);

    boolean markGenericJoinApproved(Long tenantId, String referralCode, String targetEmail, Long authUserId, Long joinRequestId);

    ReferralCodeDTO getCurrentTenantReferralCode();

    ReferralCodeDTO refreshCurrentTenantReferralCode();

    ReferralConversionOverviewVO getReferralConversionOverview(Long id);
}
