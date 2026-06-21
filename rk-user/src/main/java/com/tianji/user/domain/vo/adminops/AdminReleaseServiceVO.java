package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminReleaseServiceVO {
    private Long id;
    private String serviceCode;
    private String displayName;
    private String serviceType;
    private String namespaceName;
    private String workloadType;
    private String workloadName;
    private String containerName;
    private String currentImage;
    private String currentVersion;
    private String status;
    private Integer replicas;
    private Integer readyReplicas;
    private List<AdminReleaseVersionVO> availableVersions = new ArrayList<>();
}
