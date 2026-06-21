package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminK8sNodeVO {
    private String name;
    private String status;
    private String roles;
    private String version;
    private String internalIp;
}
