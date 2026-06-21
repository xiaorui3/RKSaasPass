package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminMonitoringOverviewVO {
    private Map<String, Object> systemStatus;
    private List<Map<String, Object>> services;
    private List<Map<String, Object>> databases;
}
