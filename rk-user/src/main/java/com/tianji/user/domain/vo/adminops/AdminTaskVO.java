package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminTaskVO {
    private Long id;
    private String name;
    private String groupName;
    private String scheduleType;
    private String cron;
    private String handler;
    private String status;
    private String lastExecuteTime;
    private String nextExecuteTime;
    private String author;
}
