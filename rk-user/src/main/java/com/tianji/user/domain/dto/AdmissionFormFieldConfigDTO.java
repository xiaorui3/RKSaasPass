package com.tianji.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("入社表单字段配置")
public class AdmissionFormFieldConfigDTO {

    @ApiModelProperty("字段标识")
    private String key;

    @ApiModelProperty("字段标签")
    private String label;

    @ApiModelProperty("占位提示")
    private String placeholder;

    @ApiModelProperty("是否显示")
    private Boolean enabled;

    @ApiModelProperty("是否必填")
    private Boolean required;

    @ApiModelProperty("字段来源：builtin/custom")
    private String source;

    @ApiModelProperty("字段类型：text/textarea/select/radio/checkbox/number/date/url")
    private String type;

    @ApiModelProperty("字段排序")
    private Integer sort;

    @ApiModelProperty("可选项")
    private List<AdmissionFormFieldOptionDTO> options;
}
