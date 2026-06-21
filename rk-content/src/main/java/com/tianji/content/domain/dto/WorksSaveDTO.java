package com.tianji.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 作品保存DTO
 * 用于新增作品的数据传输
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Data
@ApiModel(description = "作品保存DTO")
public class WorksSaveDTO {

    @ApiModelProperty(value = "标题", required = true)
    @NotBlank(message = "作品标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @ApiModelProperty("描述")
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @ApiModelProperty(value = "内容", required = true)
    @NotBlank(message = "作品内容不能为空")
    private String content;

    @ApiModelProperty("分类：web/mobile/ai/desktop/other")
    @Size(max = 50, message = "分类长度不能超过50个字符")
    private String category;

    @ApiModelProperty("封面图片URL")
    @Size(max = 500, message = "封面图片URL长度不能超过500个字符")
    private String coverImage;

    @ApiModelProperty("演示视频URL")
    @Size(max = 500, message = "演示视频URL长度不能超过500个字符")
    private String demoVideo;

    @ApiModelProperty("项目链接（JSON格式）")
    private String projectLinks;

    @ApiModelProperty("技术栈（JSON格式）")
    private String technologies;

    @ApiModelProperty("作者（JSON格式）")
    private String authors;

    @ApiModelProperty("是否精选：true-是，false-否，默认false")
    private Boolean isFeatured = false;

    @ApiModelProperty("展示顺序，默认0")
    private Integer displayOrder = 0;

    @ApiModelProperty(value = "负责人审批人ID", required = true)
    @NotNull(message = "负责人审批人不能为空")
    private Long managerReviewerId;

    @ApiModelProperty(value = "指导老师审批人ID", required = true)
    @NotNull(message = "指导老师审批人不能为空")
    private Long teacherReviewerId;
}
