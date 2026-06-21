package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminDeployPackageKubeconfigVO {
    private Long packageId;
    private String kubeconfig;
    private String kubeconfigFingerprint;
    private String redacted;
    private String viewedAt;
}
