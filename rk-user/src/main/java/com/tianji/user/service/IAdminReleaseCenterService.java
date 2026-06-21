package com.tianji.user.service;

import com.tianji.user.domain.dto.adminops.AdminReleaseBuildDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseApplyUpdateDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseDeployDTO;
import com.tianji.user.domain.dto.adminops.AdminReleasePublishCurrentDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseUpdateManifestDTO;
import com.tianji.user.domain.vo.adminops.AdminReleaseServiceVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseTaskVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseUpdateManifestVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseVersionVO;

import java.util.List;

public interface IAdminReleaseCenterService {

    List<AdminReleaseServiceVO> listReleaseServices(String namespace);

    List<AdminReleaseVersionVO> listServiceVersions(String serviceCode);

    AdminReleaseTaskVO publishCurrentImage(String serviceCode, AdminReleasePublishCurrentDTO dto);

    AdminReleaseTaskVO triggerServiceBuild(String serviceCode, AdminReleaseBuildDTO dto);

    AdminReleaseTaskVO deployServiceVersion(String serviceCode, AdminReleaseDeployDTO dto);

    AdminReleaseUpdateManifestVO buildUpdateManifest(AdminReleaseUpdateManifestDTO dto);

    AdminReleaseTaskVO applyUpdateManifest(AdminReleaseApplyUpdateDTO dto);

    AdminReleaseTaskVO getReleaseTask(Long taskId);
}
