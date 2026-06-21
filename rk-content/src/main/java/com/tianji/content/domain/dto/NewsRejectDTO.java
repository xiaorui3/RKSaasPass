package com.tianji.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 新闻审核拒绝DTO
 * 用于审核拒绝新闻的数据传输
 *
 * @author RK-Web
 * @since 2026/03/25
 */
@Data
@ApiModel(description = "新闻审核拒绝DTO")
public class NewsRejectDTO {

    @ApiModelProperty(value = "新闻ID", required = true)
    @NotNull(message = "新闻ID不能为空")
    private Long id;

    @ApiModelProperty(value = "拒绝原因", required = true)
    @NotBlank(message = "拒绝原因不能为空")
    @Size(max = 500, message = "拒绝原因长度不能超过500个字符")
    private String rejectReason;
}
