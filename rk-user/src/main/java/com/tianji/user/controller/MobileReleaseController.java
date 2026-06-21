package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.AdminMobileReleaseConfigDTO;
import com.tianji.user.domain.dto.AdminMobileReleaseRollbackDTO;
import com.tianji.user.domain.vo.MobileReleaseBuildLogVO;
import com.tianji.user.domain.vo.MobileReleaseBuildRecordVO;
import com.tianji.user.domain.vo.MobileReleaseConfigVO;
import com.tianji.user.domain.vo.MobileReleaseHistoryVO;
import com.tianji.user.service.IMobileReleaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "Mobile Release API")
@RestController
@RequiredArgsConstructor
public class MobileReleaseController {

    private final IMobileReleaseService mobileReleaseService;

    @ApiOperation("Admin mobile release config")
    @GetMapping("/admin/mobile/releases/config")
    public R<MobileReleaseConfigVO> getAdminConfig() {
        return R.ok(mobileReleaseService.getAdminConfig());
    }

    @ApiOperation("Save admin mobile release config")
    @PostMapping("/admin/mobile/releases/config")
    public R<MobileReleaseConfigVO> saveAdminConfig(@RequestBody AdminMobileReleaseConfigDTO dto) {
        return R.ok(mobileReleaseService.saveAdminConfig(dto));
    }

    @ApiOperation("Admin mobile release history")
    @GetMapping("/admin/mobile/releases/history")
    public R<List<MobileReleaseHistoryVO>> getAdminHistory() {
        return R.ok(mobileReleaseService.getAdminHistory());
    }

    @ApiOperation("Admin mobile release build history")
    @GetMapping("/admin/mobile/releases/builds")
    public R<List<MobileReleaseBuildRecordVO>> getAdminBuildHistory() {
        return R.ok(mobileReleaseService.getAdminBuildHistory());
    }

    @ApiOperation("Latest admin mobile release build")
    @GetMapping("/admin/mobile/releases/builds/latest")
    public R<MobileReleaseBuildRecordVO> getLatestBuildRecord() {
        return R.ok(mobileReleaseService.getLatestBuildRecord());
    }

    @ApiOperation("Admin mobile release build logs")
    @GetMapping("/admin/mobile/releases/builds/{buildId}/logs")
    public R<List<MobileReleaseBuildLogVO>> getBuildLogs(
            @org.springframework.web.bind.annotation.PathVariable Long buildId,
            @RequestParam(required = false, defaultValue = "0") Integer afterLineNo) {
        return R.ok(mobileReleaseService.getBuildLogs(buildId, afterLineNo));
    }

    @ApiOperation("Withdraw admin mobile release")
    @PostMapping("/admin/mobile/releases/withdraw")
    public R<MobileReleaseConfigVO> withdrawAdminConfig() {
        return R.ok(mobileReleaseService.withdrawAdminConfig());
    }

    @ApiOperation("Rollback admin mobile release")
    @PostMapping("/admin/mobile/releases/rollback")
    public R<MobileReleaseConfigVO> rollbackAdminConfig(@RequestBody AdminMobileReleaseRollbackDTO dto) {
        return R.ok(mobileReleaseService.rollbackAdminConfig(dto));
    }

    @ApiOperation("Auto build admin mobile release")
    @PostMapping("/admin/mobile/releases/auto-build")
    public R<MobileReleaseConfigVO> autoBuildAdminConfig() {
        return R.ok(mobileReleaseService.autoBuildAdminConfig());
    }

    @ApiOperation("Latest mobile release")
    @GetMapping("/mobile/releases/latest")
    public R<MobileReleaseConfigVO> getLatestRelease(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false, defaultValue = "0") Integer versionCode) {
        return R.ok(mobileReleaseService.getLatestRelease(tenantId, roleId, versionCode));
    }
}
