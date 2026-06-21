package com.tianji.api.dto.activity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("活动报名收件人DTO")
public class ActivityRegistrationRecipientDTO {

    @ApiModelProperty("活动ID")
    private Long activityId;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("报名状态")
    private Integer registrationStatus;

    @ApiModelProperty("报名时间")
    private LocalDateTime registrationTime;
}
