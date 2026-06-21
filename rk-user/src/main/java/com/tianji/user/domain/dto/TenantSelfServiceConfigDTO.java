package com.tianji.user.domain.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantSelfServiceConfigDTO {
    private Long tenantId;
    private BrandSettings brandSettings = new BrandSettings();
    private PortalSettings portalSettings = new PortalSettings();
    private AdmissionSettings admissionSettings = new AdmissionSettings();
    private ReviewSettings reviewSettings = new ReviewSettings();
    private NotificationSettings notificationSettings = new NotificationSettings();
    private QuotaSettings quotaSettings = new QuotaSettings();

    @Data
    @Accessors(chain = true)
    public static class BrandSettings {
        private String tenantDisplayName;
        private String slogan;
        private String contactEmail;
        private String contactAddress;
    }

    @Data
    @Accessors(chain = true)
    public static class PortalSettings {
        private Boolean showNews;
        private Boolean showActivities;
        private Boolean showCompetitions;
        private Boolean showAlumni;
        private Boolean showWorks;
        private Boolean allowPublicSearch;
    }

    @Data
    @Accessors(chain = true)
    public static class AdmissionSettings {
        private Boolean allowPublicRegister;
        private Boolean allowJoinApplication;
        private Boolean requireEmailVerification;
        private Boolean allowReferralCode;
        private Integer maxPendingApplications;
    }

    @Data
    @Accessors(chain = true)
    public static class ReviewSettings {
        private Boolean requireTeacherReview;
        private Boolean requireClubManagerReview;
        private Boolean autoRejectExpired;
        private Integer reviewSlaHours;
    }

    @Data
    @Accessors(chain = true)
    public static class NotificationSettings {
        private Boolean emailNoticeEnabled;
        private Boolean siteNoticeEnabled;
        private Boolean approvalNoticeEnabled;
        private Boolean weeklyDigestEnabled;
    }

    @Data
    @Accessors(chain = true)
    public static class QuotaSettings {
        private Integer maxClubMembers;
        private Integer maxActiveActivities;
        private Integer maxMonthlyNews;
        private Integer maxStorageMb;
    }
}
