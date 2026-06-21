package com.tianji.user.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReferralConversionOverviewVO {
    private Long referralCodeId;
    private String referralCode;
    private Integer usedCount;
    private Integer openedCount;
    private Integer registerSuccessCount;
    private Integer joinApprovedCount;
    private List<ReferralConversionRecordVO> records;
}
