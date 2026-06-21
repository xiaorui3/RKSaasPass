package com.tianji.content.domain.dto;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 新闻更新DTO
 * 用于修改新闻的数据传输
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Data
@ApiModel(description = "新闻更新DTO")
public class NewsUpdateDTO {

    @ApiModelProperty(value = "新闻ID", required = true)
    @NotNull(message = "新闻ID不能为空")
    private Long id;

    @ApiModelProperty("标题")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @ApiModelProperty("摘要")
    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;

    @ApiModelProperty("内容")
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

    @ApiModelProperty("是否发布：0-草稿，1-发布")
    @JsonDeserialize(using = BooleanToIntegerDeserializer.class)
    private Integer isPublished;

    @ApiModelProperty("是否置顶：0-否，1-是")
    @JsonDeserialize(using = BooleanToIntegerDeserializer.class)
    private Integer isFeatured;

    @ApiModelProperty("是否允许跨租户公开展示：0-否，1-是")
    @JsonDeserialize(using = BooleanToIntegerDeserializer.class)
    private Integer isCrossTenant;

    @ApiModelProperty("审核状态：0-草稿 1-待审核 2-已通过 3-已拒绝")
    private Integer approvalStatus;

    @ApiModelProperty("负责人审批人ID")
    private Long managerReviewerId;

    @ApiModelProperty("指导老师审批人ID")
    private Long teacherReviewerId;

    /**
     * Boolean到Integer的反序列化器
     * 前端发送Boolean，后端转换为Integer（0或1）
     */
    public static class BooleanToIntegerDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
            if (p.currentToken().isBoolean()) {
                return p.getBooleanValue() ? 1 : 0;
            }
            if (p.currentToken().isNumeric()) {
                return p.getIntValue();
            }
            return null;
        }
    }
}
