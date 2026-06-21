package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminDeployPackagePreflightVO {
    private Boolean passed;
    private String targetClusterType;
    private String storageClassName;
    private String externalExposureType;
    private List<String> checks;
    private List<String> warnings;
    private List<String> errors;
    private List<String> storageClasses;
    private List<String> ingressClasses;
    private List<String> nodeExternalIps;
}
