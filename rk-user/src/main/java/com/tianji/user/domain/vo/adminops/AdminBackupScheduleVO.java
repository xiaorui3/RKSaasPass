package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminBackupScheduleVO {
    private Boolean enabled;
    private String frequency;
    private String time;
    private Integer retentionDays;
    private List<String> databases;
}
