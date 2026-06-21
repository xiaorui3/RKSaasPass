package com.tianji.user.domain.vo;

import lombok.Data;

@Data
public class MobileReleaseBuildLogVO {
    private Long id;
    private Long buildId;
    private Integer lineNo;
    private String logType;
    private String content;
    private String createdAt;
}
