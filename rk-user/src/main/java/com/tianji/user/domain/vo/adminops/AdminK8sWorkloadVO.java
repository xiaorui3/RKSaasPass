package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminK8sWorkloadVO {
    private String namespace;
    private String kind;
    private String name;
    private Integer replicas;
    private Integer readyReplicas;
    private Integer availableReplicas;
    private String containers;
    private String images;
    private String status;
}
