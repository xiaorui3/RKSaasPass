package com.tianji.api.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class EmailCenterSendDTO {
    private Long tenantId;
    private List<Long> roleIds;
    private List<Long> authUserIds;
    private List<String> usernames;
    private Long activityId;
    private Long competitionId;
    private List<String> manualEmails;
    private String subject;
    private String content;
    private Boolean html;
}
