package com.tianji.user.service;

import com.tianji.user.domain.dto.adminops.AdminReleaseDeploymentContext;
import com.tianji.user.domain.vo.adminops.AdminReleaseDeploymentResult;

public interface IAdminReleaseDeploymentExecutor {

    boolean supports(String runtimeMode);

    AdminReleaseDeploymentResult deploy(AdminReleaseDeploymentContext context);
}
