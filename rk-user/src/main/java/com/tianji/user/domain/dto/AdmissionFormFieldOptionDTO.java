package com.tianji.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("入社表单字段选项")
public class AdmissionFormFieldOptionDTO {

    @ApiModelProperty("选项标签")
    private String label;

    @ApiModelProperty("选项值")
    private String value;
}
