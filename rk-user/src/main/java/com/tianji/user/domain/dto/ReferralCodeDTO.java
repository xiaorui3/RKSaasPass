package com.tianji.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 内推码DTO
 */
@Data
@ApiModel("内推码表单数据")
public class ReferralCodeDTO {

    @ApiModelProperty("内推码ID")
    private Long id;

    @ApiModelProperty(value = "内推码", required = true)
    @NotBlank(message = "内推码不能为空")
    private String code;

    @ApiModelProperty(value = "最大使用次数", required = true)
    @NotNull(message = "最大使用次数不能为空")
    private Integer maxUses;

    @ApiModelProperty("过期时间")
    private LocalDateTime expiresAt;

    @ApiModelProperty("状态: 1-有效 0-失效")
    private Integer status;

    @ApiModelProperty("生成者用户ID")
    private Long generatorId;

    @ApiModelProperty("生成者用户名")
    private String generatorName;
}
