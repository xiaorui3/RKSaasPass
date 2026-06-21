package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminMinioBucketVO {
    private String name;
    private String creationDate;
    private Long objectCount;
    private Long totalBytes;
    private String totalSize;
}
