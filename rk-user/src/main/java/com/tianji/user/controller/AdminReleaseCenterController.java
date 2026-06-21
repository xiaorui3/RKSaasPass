package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.adminops.AdminReleaseApplyUpdateDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseBuildDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseDeployDTO;
import com.tianji.user.domain.dto.adminops.AdminReleasePublishCurrentDTO;
import com.tianji.user.domain.dto.adminops.AdminReleaseUpdateManifestDTO;
import com.tianji.user.domain.vo.adminops.AdminReleaseServiceVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseTaskVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseUpdateManifestVO;
import com.tianji.user.domain.vo.adminops.AdminReleaseVersionVO;
import com.tianji.user.service.IAdminReleaseCenterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "Admin Release Center")
@RestController
@RequestMapping("/admin/ops/release-center")
@RequiredArgsConstructor
public class AdminReleaseCenterController {

    private final IAdminReleaseCenterService releaseCenterService;

    @ApiOperation("List release services")
    @GetMapping("/services")
    public R<List<AdminReleaseServiceVO>> listReleaseServices(
            @RequestParam(required = false) String namespace
    ) {
        return R.ok(releaseCenterService.listReleaseServices(namespace));
    }

    @ApiOperation("List service release versions")
    @GetMapping("/services/{serviceCode}/versions")
    public R<List<AdminReleaseVersionVO>> listServiceVersions(@PathVariable String serviceCode) {
        return R.ok(releaseCenterService.listServiceVersions(serviceCode));
    }

    @ApiOperation("Publish current running image as a release version")
    @PostMapping("/services/{serviceCode}/publish-current")
    public R<AdminReleaseTaskVO> publishCurrentImage(
            @PathVariable String serviceCode,
            @RequestBody(required = false) AdminReleasePublishCurrentDTO dto
    ) {
        return R.ok(releaseCenterService.publishCurrentImage(
                serviceCode,
                dto == null ? new AdminReleasePublishCurrentDTO() : dto
        ));
    }

    @ApiOperation("Trigger service Jenkins build")
    @PostMapping("/services/{serviceCode}/build")
    public R<AdminReleaseTaskVO> triggerServiceBuild(
            @PathVariable String serviceCode,
            @RequestBody(required = false) AdminReleaseBuildDTO dto
    ) {
        return R.ok(releaseCenterService.triggerServiceBuild(
                serviceCode,
                dto == null ? new AdminReleaseBuildDTO() : dto
        ));
    }

    @ApiOperation("Deploy service release version")
    @PostMapping("/services/{serviceCode}/deploy")
    public R<AdminReleaseTaskVO> deployServiceVersion(
            @PathVariable String serviceCode,
            @RequestBody(required = false) AdminReleaseDeployDTO dto
    ) {
        return R.ok(releaseCenterService.deployServiceVersion(
                serviceCode,
                dto == null ? new AdminReleaseDeployDTO() : dto
        ));
    }

    @ApiOperation("Build update manifest")
    @PostMapping("/updates/manifest")
    public R<AdminReleaseUpdateManifestVO> buildUpdateManifest(
            @RequestBody(required = false) AdminReleaseUpdateManifestDTO dto
    ) {
        return R.ok(releaseCenterService.buildUpdateManifest(
                dto == null ? new AdminReleaseUpdateManifestDTO() : dto
        ));
    }

    @ApiOperation("Apply update manifest")
    @PostMapping("/updates/apply")
    public R<AdminReleaseTaskVO> applyUpdateManifest(
            @RequestBody(required = false) AdminReleaseApplyUpdateDTO dto
    ) {
        return R.ok(releaseCenterService.applyUpdateManifest(
                dto == null ? new AdminReleaseApplyUpdateDTO() : dto
        ));
    }

    @ApiOperation("Get release task")
    @GetMapping("/tasks/{taskId}")
    public R<AdminReleaseTaskVO> getReleaseTask(@PathVariable Long taskId) {
        return R.ok(releaseCenterService.getReleaseTask(taskId));
    }
}
