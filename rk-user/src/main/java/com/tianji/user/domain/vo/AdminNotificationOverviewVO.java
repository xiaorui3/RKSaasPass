package com.tianji.user.domain.vo;

import lombok.Data;

@Data
public class AdminNotificationOverviewVO {
    private Integer totalCount;
    private Integer sentCount;
    private Integer upgradeCount;
    private Integer highPriorityCount;
}
