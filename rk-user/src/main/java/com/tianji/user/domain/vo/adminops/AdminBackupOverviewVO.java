package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminBackupOverviewVO {
    private AdminBackupStorageStatsVO storageStats;
    private List<AdminBackupRecordVO> backups;
    private AdminBackupScheduleVO schedule;
    private Boolean restoreSupported;
}
