package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminBackupStorageStatsVO {
    private Integer totalFiles;
    private String totalSize;
    private String lastBackup;
    private Boolean autoBackupEnabled;
    private Integer databaseCount;
}
