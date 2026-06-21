package com.tianji.message.domain.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知DTO
 * 基于master分支的通知功能
 */
@Data
public class NoticeDTO {

    private Long id;

    private String title;

    private String content;

    private String coverImage;

    private String attachmentUrl;

    private Integer noticeType;

    private String priority;

    private Boolean isPublished;

    private Integer isTop;

    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    private Integer viewCount;

    private String targetType;

    /**
     * 租户ID（企业级多租户标准）
     */
    private Long tenantId;

    private String createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long managerReviewerId;

    private Long teacherReviewerId;

    @JsonSetter("isTop")
    public void setIsTop(Object value) {
        if (value == null) {
            this.isTop = null;
            return;
        }
        if (value instanceof Boolean) {
            this.isTop = Boolean.TRUE.equals(value) ? 1 : 0;
            return;
        }
        if (value instanceof Number) {
            this.isTop = ((Number) value).intValue();
            return;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            this.isTop = null;
            return;
        }
        if ("true".equalsIgnoreCase(text)) {
            this.isTop = 1;
            return;
        }
        if ("false".equalsIgnoreCase(text)) {
            this.isTop = 0;
            return;
        }
        this.isTop = Integer.parseInt(text);
    }
}
