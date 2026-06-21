package com.tianji.user.config;

import com.tianji.authsdk.resource.config.ResourceAuthProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensures rk-user public admission endpoints stay excluded from login interception
 * even if external configuration omits them.
 */
@Configuration
public class ResourceAuthCustomizationConfig {

    private final ResourceAuthProperties resourceAuthProperties;

    public ResourceAuthCustomizationConfig(ResourceAuthProperties resourceAuthProperties) {
        this.resourceAuthProperties = resourceAuthProperties;
    }

    @PostConstruct
    public void ensureAdmissionPublicPaths() {
        List<String> excludeLoginPaths = resourceAuthProperties.getExcludeLoginPaths();
        if (excludeLoginPaths == null) {
            excludeLoginPaths = new ArrayList<>();
            resourceAuthProperties.setExcludeLoginPaths(excludeLoginPaths);
        }

        addIfMissing(excludeLoginPaths, "/api/admission/submit");
        addIfMissing(excludeLoginPaths, "/api/admission/check-status");
        addIfMissing(excludeLoginPaths, "/api/admission/validate-email");
        addIfMissing(excludeLoginPaths, "/api/admission/form-config/public");
        addIfMissing(excludeLoginPaths, "/api/config/theme/public");
        addIfMissing(excludeLoginPaths, "/api/config/tenant-self-service/public");
        addIfMissing(excludeLoginPaths, "/api/config/tenant-self-service/internal/admission-policy");
        addIfMissing(excludeLoginPaths, "/api/config/club-profile/public");
        addIfMissing(excludeLoginPaths, "/api/email-verification/send");
        addIfMissing(excludeLoginPaths, "/api/email-verification/verify");
        addIfMissing(excludeLoginPaths, "/api/email-verification/internal/send");
        addIfMissing(excludeLoginPaths, "/api/email-verification/internal/verify");
        addIfMissing(excludeLoginPaths, "/api/email-center/internal/invitations/register-success");
        addIfMissing(excludeLoginPaths, "/api/email-center/internal/send");
        addIfMissing(excludeLoginPaths, "/api/email-center/internal/recipient-emails");
        addIfMissing(excludeLoginPaths, "/api/email-center/internal/tasks/**");
        addIfMissing(excludeLoginPaths, "/api/admission/internal/register-success-notify");
        addIfMissing(excludeLoginPaths, "/api/email-center/internal/tasks/*/recipients/*/success");
        addIfMissing(excludeLoginPaths, "/api/email-center/internal/tasks/*/recipients/*/failure");
        addIfMissing(excludeLoginPaths, "/api/referral-codes/internal/validate");
        addIfMissing(excludeLoginPaths, "/api/referral-codes/internal/register-success");
        addIfMissing(excludeLoginPaths, "/api/alumni/profile-form/**");
        addIfMissing(excludeLoginPaths, "/api/alumni/show-overview");
        addIfMissing(excludeLoginPaths, "/api/alumni/show-grouped-by-generation");
        addIfMissing(excludeLoginPaths, "/api/history/timeline");
        addIfMissing(excludeLoginPaths, "/api/history/search");
        addIfMissing(excludeLoginPaths, "/api/history/statistics");
        addIfMissing(excludeLoginPaths, "/api/workflow/config/current/internal");
        addIfMissing(excludeLoginPaths, "/api/credit/internal/grants/batch");
        addIfMissing(excludeLoginPaths, "/api/search-documents/internal/export");
        addIfMissing(excludeLoginPaths, "/api/email-center/invitations/*");
        addIfMissing(excludeLoginPaths, "/api/email-center/invitations/*/accept");
        addIfMissing(excludeLoginPaths, "/mobile/releases/latest");
        addIfMissing(excludeLoginPaths, "/admin/ops/public/deploy-package/active-migration");
        addIfMissing(excludeLoginPaths, "/admin/ops/public/deploy-package/*/linux-ssh-download");
    }

    private void addIfMissing(List<String> paths, String path) {
        if (!paths.contains(path)) {
            paths.add(path);
        }
    }
}
