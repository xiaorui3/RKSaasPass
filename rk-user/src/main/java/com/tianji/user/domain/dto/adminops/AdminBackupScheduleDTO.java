package com.tianji.user.domain.dto.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminBackupScheduleDTO {
    private Boolean enabled;
    private String frequency;
    private String time;
    private Integer retentionDays;
    private List<String> databases;
}
