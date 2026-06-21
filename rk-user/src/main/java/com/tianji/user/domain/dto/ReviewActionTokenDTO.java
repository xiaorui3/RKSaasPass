package com.tianji.user.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewActionTokenDTO {
    private String token;
    private Long tenantId;
    private String targetType;
    private Long targetId;
    private String action;
    private LocalDateTime expiresAt;
    private Boolean used;
    private Long usedByAuthUserId;
    private LocalDateTime usedTime;
}
