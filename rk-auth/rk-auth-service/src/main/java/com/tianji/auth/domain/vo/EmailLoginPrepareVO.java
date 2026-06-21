package com.tianji.auth.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("邮箱登录预处理返回")
public class EmailLoginPrepareVO {

    @ApiModelProperty("登录票据")
    private String loginTicket;

    @ApiModelProperty("候选账号")
    private List<EmailLoginCandidateVO> candidates;
}
