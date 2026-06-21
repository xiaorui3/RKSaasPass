package com.tianji.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@ApiModel("入社表单配置")
public class AdmissionFormConfigDTO {

    @ApiModelProperty("页面标题")
    @NotBlank
    private String pageTitle;

    @ApiModelProperty("页面说明")
    @NotBlank
    private String pageDescription;

    @ApiModelProperty("提交成功提示")
    @NotBlank
    private String successMessage;

    @ApiModelProperty("字段配置列表")
    @Valid
    @NotEmpty
    private List<AdmissionFormFieldConfigDTO> fields;
}
