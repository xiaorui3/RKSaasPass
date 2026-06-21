package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminNacosConfigVO {
    private String namespaceId;
    private String groupName;
    private String dataId;
    private String type;
    private String md5;
    private String lastModified;
    private String content;
    private String message;
}
