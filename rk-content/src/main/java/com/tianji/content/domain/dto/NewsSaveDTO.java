package com.tianji.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 新闻保存DTO
 * 用于新增新闻的数据传输
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Data
@ApiModel(description = "新闻保存DTO")
public class NewsSaveDTO {

    @ApiModelProperty(value = "标题", required = true)
    @NotBlank(message = "新闻标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @ApiModelProperty("摘要")
    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;

    @ApiModelProperty(value = "内容", required = true)
    @NotBlank(message = "新闻内容不能为空")
    private String content;

    @ApiModelProperty("封面图片URL")
    @Size(max = 500, message = "封面图片URL长度不能超过500个字符")
    private String coverImage;

    @ApiModelProperty("视频URL（支持视频上传）")
    @Size(max = 500, message = "视频URL长度不能超过500个字符")
    private String videoUrl;

    @ApiModelProperty("附件URL")
    @Size(max = 500, message = "附件URL长度不能超过500个字符")
    private String attachmentUrl;

    @ApiModelProperty("作者")
    @Size(max = 50, message = "作者名称长度不能超过50个字符")
    private String author;

    @ApiModelProperty("分类")
    @Size(max = 50, message = "分类长度不能超过50个字符")
    private String category;

    @ApiModelProperty("标签，多个用逗号分隔")
    @Size(max = 200, message = "标签长度不能超过200个字符")
    private String tags;

    @ApiModelProperty("是否发布：0-草稿，1-发布，默认0")
    private Integer isPublished = 0;

    @ApiModelProperty("是否置顶：0-否，1-是，默认0")
    private Integer isFeatured = 0;

    @ApiModelProperty("是否允许跨租户公开展示：0-否，1-是")
    private Integer isCrossTenant = 0;

    @ApiModelProperty(value = "负责人审批人ID", required = true)
    @NotNull(message = "负责人审批人不能为空")
    private Long managerReviewerId;

    @ApiModelProperty(value = "指导老师审批人ID", required = true)
    @NotNull(message = "指导老师审批人不能为空")
    private Long teacherReviewerId;
}
