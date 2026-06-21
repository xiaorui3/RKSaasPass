package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminTrafficOverviewVO {
    private Long defaultTenantId;
    private Long selectedTenantId;
    private Long totalVisits = 0L;
    private Long totalLogins = 0L;
    private Long totalOperations = 0L;
    private Long countryCount = 0L;
    private Long abnormalIpCount = 0L;
    private Long onlineUserCount = 0L;
    private Long onlinePcCount = 0L;
    private Long onlineMobileCount = 0L;
    private AdminTrafficLocationVO currentOrigin;
    private List<AdminTrafficTenantOptionVO> tenantOptions = new ArrayList<>();
    private List<AdminTrafficLocationVO> countryRankings = new ArrayList<>();
    private List<AdminTrafficLocationVO> provinceRankings = new ArrayList<>();
    private List<AdminTrafficLocationVO> recentIps = new ArrayList<>();
    private List<AdminTrafficOnlineUserVO> onlineUsers = new ArrayList<>();
}
