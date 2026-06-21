package com.tianji.content.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CommentCreateDTO {

    @NotBlank(message = "评论目标类型不能为空")
    private String targetType;

    @NotNull(message = "评论目标ID不能为空")
    private Long targetId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    private String content;

    private Long userId;

    @Size(max = 80, message = "评论人名称不能超过80字")
    private String userName;

    @Size(max = 500, message = "评论人头像不能超过500字")
    private String userAvatar;
}
