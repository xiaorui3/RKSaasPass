package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminDeployPackageRecordVO {
    private Long id;
    private String packageName;
    private String status;
    private Integer progress;
    private String packageMode;
    private String deliveryMode;
    private String imageArtifactMode;
    private String deployMode;
    private Boolean remoteDeploy;
    private String remoteClusterName;
    private String deployStartedAt;
    private Long deployStartedAtMillis;
    private String deployFinishedAt;
    private Long deployFinishedAtMillis;
    private String currentStep;
    private String currentImage;
    private Integer uploadedImages;
    private Integer totalImages;
    private Integer uploadPercent;
    private Boolean registryPullPackage;
    private Boolean streamingMigration;
    private Boolean globalMigrationLock;
    private Integer estimatedRemainingSeconds;
    private String migrationStartedAt;
    private Long migrationStartedAtMillis;
    private String migrationUpdatedAt;
    private Long migrationUpdatedAtMillis;
    private String fileName;
    private String fileSize;
    private String remotePath;
    private Boolean downloadable;
    private Boolean deleteAfterDownload;
    private Boolean downloaded;
    private String downloadedAt;
    private String note;
    private String logs;
    private String createTime;
    private String updateTime;
}
