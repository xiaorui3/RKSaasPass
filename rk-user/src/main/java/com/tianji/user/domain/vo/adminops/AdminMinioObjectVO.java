package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminMinioObjectVO {
    private String bucket;
    private String objectName;
    private String fileName;
    private Long sizeBytes;
    private String size;
    private String lastModified;
    private String etag;
    private String downloadUrl;
}
