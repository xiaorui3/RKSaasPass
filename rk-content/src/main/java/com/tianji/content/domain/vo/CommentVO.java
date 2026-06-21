package com.tianji.content.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long tenantId;
    private String targetType;
    private Long targetId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private Boolean featured;
    private Integer status;
    private Long dr;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
