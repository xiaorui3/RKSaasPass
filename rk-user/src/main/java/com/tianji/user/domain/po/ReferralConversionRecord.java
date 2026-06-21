package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("referral_conversion_record")
public class ReferralConversionRecord extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long referralCodeId;

    private String referralCode;

    private Long invitationId;

    private String inviteToken;

    private String targetEmail;

    private String conversionType;

    private String conversionStatus;

    private Long authUserId;

    private Long joinRequestId;

    private LocalDateTime convertedTime;
}
