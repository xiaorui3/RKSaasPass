package com.tianji.user.domain.vo;

import lombok.Data;

@Data
public class ReferralConversionRecordVO {
    private Long id;
    private String targetEmail;
    private String conversionType;
    private String conversionStatus;
    private Long authUserId;
    private Long joinRequestId;
    private String convertedTime;
}
