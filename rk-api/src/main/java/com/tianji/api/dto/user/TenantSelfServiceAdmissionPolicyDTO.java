package com.tianji.api.dto.user;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantSelfServiceAdmissionPolicyDTO {
    private Long tenantId;
    private Boolean allowPublicRegister;
    private Boolean allowJoinApplication;
    private Boolean requireTeacherReview;
    private Boolean requireClubManagerReview;
    private Boolean emailNoticeEnabled;
    private Boolean siteNoticeEnabled;
    private Boolean approvalNoticeEnabled;
    private Integer maxClubMembers;
    private Integer maxActiveActivities;
    private Integer maxMonthlyNews;
    private Integer maxStorageMb;
}
