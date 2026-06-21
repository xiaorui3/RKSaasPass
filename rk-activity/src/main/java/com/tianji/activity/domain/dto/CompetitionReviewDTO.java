package com.tianji.activity.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("比赛审核请求")
public class CompetitionReviewDTO {

    @ApiModelProperty("是否通过")
    private Boolean approved;

    @ApiModelProperty("审核意见")
    private String reviewComment;
}
