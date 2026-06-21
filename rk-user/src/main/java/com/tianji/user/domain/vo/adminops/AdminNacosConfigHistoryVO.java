package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminNacosConfigHistoryVO {
    private String namespaceId;
    private String groupName;
    private String dataId;
    private String nid;
    private String lastModifiedTime;
    private String operator;
    private String content;
    private String message;
}
