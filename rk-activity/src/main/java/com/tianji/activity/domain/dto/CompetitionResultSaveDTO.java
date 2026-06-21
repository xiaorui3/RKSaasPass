package com.tianji.activity.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "比赛成绩保存DTO")
public class CompetitionResultSaveDTO {

    @ApiModelProperty("报名记录ID")
    private Long participantId;

    @ApiModelProperty("得分")
    private Integer score;

    @ApiModelProperty("奖项（1一等奖 2二等奖 3三等奖 4优秀奖）")
    private Integer award;

    @ApiModelProperty("排名")
    private Integer ranking;
}
