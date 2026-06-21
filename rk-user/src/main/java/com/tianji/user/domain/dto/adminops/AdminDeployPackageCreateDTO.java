package com.tianji.user.domain.dto.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminDeployPackageCreateDTO {
    private String packageMode = "offline-full";
    private String deliveryMode = "direct-download";
    private Boolean selfContained = true;
    private Boolean includeSource = true;
    private Boolean includeDatabases = true;
    private Boolean includeImages = true;
    private Boolean includeK8sManifests = true;
    private Boolean includeMinio = true;
    private Boolean includeDockerCompose = true;
    private Boolean includeAllCurrentData = true;
    private Boolean exportRuntimeArtifacts = false;
    private Boolean cleanupRuntimeArtifacts = true;
    private Boolean includePlatformImages = false;
    private Boolean deleteAfterDownload = true;
    private String imageArtifactMode = "both";
    private String registryPrefix = "registry.example.com/rk-web";
    private String registryServer = "registry.example.com";
    private String targetClusterType = "k3s";
    private String targetNamespace = "shetuanguanlixitong";
    private String targetDomain;
    private String storageClassName;
    private String externalExposureType = "none";
    private Integer frontendNodePort;
    private Integer gatewayNodePort;
    private String ingressClassName;
    private String ingressHost;
    private List<String> databases;
    private String note;
    private Boolean remoteDeploy = false;
    private String kubeconfig;
    private String kubeContext;
    private Boolean waitRollout = true;
    private Integer rolloutTimeoutSeconds = 600;
    private Boolean dryRun = false;
    private String confirmText;
    private Boolean applyDatabaseSnapshot = true;
    private Boolean applyMinioSnapshot = true;
    private Boolean streamingMigration = true;
    private Boolean globalMigrationLock = true;
    private Integer estimatedRemainingSeconds;
    private String migrationStartedAt;
    private String migrationUpdatedAt;
    private String deployMode;
    private String sshHost;
    private Integer sshPort = 22;
    private String sshUsername = "root";
    private String sshPassword;
    private String linuxDeployMode = "k8s";
    private Boolean domesticMirror = true;
    private Boolean installK8s = true;
    private Boolean installDocker = true;
    private Boolean copyServices = true;
    private String targetPath = "/opt/rk-web";
}
