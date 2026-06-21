package com.tianji.api.dto.activity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("比赛报名收件人DTO")
public class CompetitionRegistrationRecipientDTO {

    @ApiModelProperty("比赛ID")
    private Long competitionId;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("队伍名称")
    private String teamName;
}
