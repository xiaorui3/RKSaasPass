package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.AlumniProfileSubmitDTO;
import com.tianji.user.domain.vo.AlumniGraduationResultVO;
import com.tianji.user.domain.vo.AlumniProfileFormVO;
import com.tianji.user.service.IAlumniGraduationWorkflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Api(tags = "校友年度毕业工作流")
@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
public class AlumniGraduationController {

    private final IAlumniGraduationWorkflowService alumniGraduationWorkflowService;

    @ApiOperation("手动执行年度毕业转校友任务")
    @PostMapping("/graduation/run")
    @PreAuthorize("hasAuthority('alumni:edit')")
    public R<AlumniGraduationResultVO> runAnnualGraduation(
            @RequestParam(required = false) Integer processYear,
            @RequestParam(required = false) Long tenantId) {
        return R.ok(alumniGraduationWorkflowService.runAnnualGraduation(processYear, tenantId));
    }

    @ApiOperation("读取一次性校友资料表单")
    @GetMapping("/profile-form/{token}")
    public R<AlumniProfileFormVO> getProfileForm(@PathVariable String token) {
        return R.ok(alumniGraduationWorkflowService.getProfileForm(token));
    }

    @ApiOperation("提交一次性校友资料表单")
    @PostMapping("/profile-form/{token}")
    public R<Boolean> submitProfileForm(@PathVariable String token, @RequestBody AlumniProfileSubmitDTO dto) {
        return R.ok(alumniGraduationWorkflowService.submitProfileForm(token, dto));
    }
}
