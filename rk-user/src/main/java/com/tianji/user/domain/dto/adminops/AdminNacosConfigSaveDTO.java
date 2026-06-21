package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminNacosConfigSaveDTO {
    private String namespaceId;
    private String groupName;
    private String dataId;
    private String content;
    private String type;
}
