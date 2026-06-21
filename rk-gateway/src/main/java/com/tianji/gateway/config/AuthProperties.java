package com.tianji.gateway.config;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "tj.auth")
public class AuthProperties implements InitializingBean {

    private Set<String> excludePath = new HashSet<>();

    @Override
    public void afterPropertiesSet() {
        if (excludePath == null) {
            excludePath = new HashSet<>();
        }

        excludePath.add("/error/**");
        excludePath.add("/jwks");
        excludePath.add("/accounts/login");
        excludePath.add("/accounts/admin/login");
        excludePath.add("/accounts/refresh");

        excludePath.add("GET:/auth/login");
        excludePath.add("POST:/auth/login");
        excludePath.add("POST:/auth/email-login/prepare");
        excludePath.add("POST:/auth/email-login/confirm");
        excludePath.add("GET:/auth/register");
        excludePath.add("POST:/auth/register");
        excludePath.add("GET:/auth/refresh");
        excludePath.add("POST:/auth/refresh");
        excludePath.add("POST:/auth/refresh-session");

        excludePath.add("GET:/tenants/list");

        excludePath.add("GET:/api/news");
        excludePath.add("GET:/api/news/*");
        excludePath.add("GET:/api/news/latest");
        excludePath.add("GET:/api/news/top");
        excludePath.add("GET:/api/news/category/*");
        excludePath.add("GET:/api/news/search");

        excludePath.add("GET:/api/works/list");
        excludePath.add("GET:/api/works/*");
        excludePath.add("GET:/api/works/featured");
        excludePath.add("GET:/api/works/popular");
        excludePath.add("GET:/api/works/latest");
        excludePath.add("GET:/api/works/category/*");
        excludePath.add("GET:/api/works/search");

        excludePath.add("GET:/api/activity");
        excludePath.add("GET:/api/activity/list");
        excludePath.add("GET:/api/activity/*");
        excludePath.add("GET:/api/activity/shared");
        excludePath.add("GET:/api/activity/hot");
        excludePath.add("GET:/api/activity/top");
        excludePath.add("GET:/api/activity/search");

        excludePath.add("GET:/api/competition/list");
        excludePath.add("GET:/api/competition/shared");
        excludePath.add("GET:/api/competition/published");
        excludePath.add("GET:/api/competition/featured");
        excludePath.add("GET:/api/competition/status/*");
        excludePath.add("GET:/api/competition/search");
        excludePath.add("GET:/api/competition/*");

        excludePath.add("GET:/api/news/shared");
        excludePath.add("GET:/api/comments");
        excludePath.add("GET:/notifications/api/notices/published");
        excludePath.add("GET:/notifications/api/notices/*");

        excludePath.add("POST:/api/admission/submit");
        excludePath.add("GET:/api/admission/form-config/public");
        excludePath.add("POST:/api/admission/internal/register-success-notify");
        excludePath.add("GET:/api/config/theme/public");
        excludePath.add("GET:/api/config/club-profile/public");
        excludePath.add("GET:/api/config/tenant-self-service/public");
        excludePath.add("GET:/api/contact/public/shield");
        excludePath.add("POST:/api/email-verification/send");
        excludePath.add("POST:/api/email-verification/verify");
        excludePath.add("GET:/api/email-center/invitations/*");
        excludePath.add("POST:/api/email-center/invitations/*/accept");
        excludePath.add("POST:/api/email-center/internal/invitations/register-success");
        excludePath.add("POST:/api/email-center/internal/send");
        excludePath.add("POST:/api/email-center/internal/tasks/**");
        excludePath.add("POST:/api/email-center/internal/tasks/*/recipients/*/success");
        excludePath.add("POST:/api/email-center/internal/tasks/*/recipients/*/failure");
        excludePath.add("POST:/api/referral-codes/internal/validate");
        excludePath.add("POST:/api/referral-codes/internal/register-success");
        excludePath.add("GET:/api/alumni/profile-form/**");
        excludePath.add("POST:/api/alumni/profile-form/**");
        excludePath.add("GET:/api/alumni/show-overview");
        excludePath.add("GET:/api/alumni/show-grouped-by-generation");
        excludePath.add("GET:/api/history/timeline");
        excludePath.add("GET:/api/history/search");
        excludePath.add("GET:/api/history/statistics");
        excludePath.add("GET:/api/workflow/config/current/internal");
        excludePath.add("POST:/api/credit/internal/grants/batch");
        excludePath.add("GET:/mobile/releases/latest");
        excludePath.add("GET:/api/mobile/releases/latest");
        excludePath.add("GET:/api/ops/migration/active");
        excludePath.add("GET:/minio-files/**");
        excludePath.add("GET:/admin/ops/public/deploy-package/**/linux-ssh-download");

        excludePath.add("/doc.html");
        excludePath.add("/v2/api-docs/**");
        excludePath.add("/swagger-resources/**");
        excludePath.add("/webjars/**");
    }
}
