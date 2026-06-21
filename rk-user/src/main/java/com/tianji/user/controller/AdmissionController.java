package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.api.dto.user.RegisterSuccessNotifyDTO;
import com.tianji.user.domain.po.JoinRequest;
import com.tianji.user.service.IAdmissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 录取管理控制器
 * 基于master分支的AdmissionController功能
 *
 * 权限控制（根据RBAC权限矩阵）：
 * - 成员审核(audit:member:*): super_admin, manager
 * - 入社申请(user:application:submit): 所有已登录用户
 */
@Slf4j
@Api(tags = "录取管理接口")
@RestController
@RequestMapping("/api/admission")
@RequiredArgsConstructor
public class AdmissionController {

    private final IAdmissionService admissionService;

    /**
     * 提交入社申请
     * 权限：user:application:submit - 所有已登录用户可提交
     */
    @ApiOperation("提交入社申请")
    @PostMapping("/submit")
    public R<String> submitApplication(@RequestBody JoinRequest joinRequest) {
        try {
            boolean success = admissionService.submitApplication(joinRequest);
            if (success) {
                log.info("✅ 入社申请提交成功：学号={}, 姓名={}", joinRequest.getStudentId(), joinRequest.getName());
                return R.ok("申请提交成功，请等待审核");
            } else {
                return R.error("申请提交失败");
            }
        } catch (Exception e) {
            log.error("提交入社申请失败", e);
            return R.error("申请提交失败: " + e.getMessage());
        }
    }

    /**
     * 检查申请状态
     */
    @ApiOperation("检查申请状态")
    @PostMapping("/check-status")
    public R<Map<String, Object>> checkApplicationStatus(@RequestBody Map<String, String> request) {
        try {
            String studentId = request.get("studentId");
            String email = request.get("email");

            if (studentId == null || studentId.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
                return R.error("学号和邮箱不能为空");
            }

            Map<String, Object> result = admissionService.checkApplicationStatus(studentId, email);
            return R.ok(result);
        } catch (Exception e) {
            log.error("检查申请状态失败", e);
            return R.error("检查申请状态失败: " + e.getMessage());
        }
    }

    /**
     * 管理员审核申请
     * 权限：audit:member:* - super_admin, manager
     */
    @ApiOperation("管理员审核申请")
    @PostMapping("/review")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:review')")
    public R<String> reviewApplication(
            @RequestBody Map<String, Object> request) {
        try {
            Long id = Long.valueOf(request.get("id").toString());
            String reviewStatus = request.get("reviewStatus").toString();
            String reviewComment = request.get("reviewComment") != null ?
                                  request.get("reviewComment").toString() : null;
            Long reviewerId = request.get("reviewerId") != null ?
                             Long.valueOf(request.get("reviewerId").toString()) : null;

            boolean success = admissionService.reviewApplication(id, reviewStatus, reviewComment, reviewerId);
            if (success) {
                log.info("✅ 申请审核成功：ID={}, 状态={}", id, reviewStatus);
                return R.ok("申请审核成功");
            } else {
                return R.error("申请审核失败");
            }
        } catch (Exception e) {
            log.error("审核申请失败", e);
            return R.error("申请审核失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有申请
     * 权限：audit:member:* - super_admin, manager
     */
    @ApiOperation("获取所有申请")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:list')")
    public R<List<JoinRequest>> getAllApplications() {
        try {
            List<JoinRequest> applications = admissionService.getAllApplications();
            return R.ok(applications);
        } catch (Exception e) {
            log.error("获取所有申请失败", e);
            return R.error("获取所有申请失败: " + e.getMessage());
        }
    }

    /**
     * 根据状态获取申请
     * 权限：audit:member:* - super_admin, manager
     */
    @ApiOperation("根据状态获取申请")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:list')")
    public R<List<JoinRequest>> getApplicationsByStatus(@PathVariable String status) {
        try {
            List<JoinRequest> applications = admissionService.getApplicationsByStatus(status);
            return R.ok(applications);
        } catch (Exception e) {
            log.error("按状态获取申请失败", e);
            return R.error("按状态获取申请失败: " + e.getMessage());
        }
    }

    /**
     * 根据学号获取申请
     * 权限：audit:member:* - super_admin, manager
     */
    @ApiOperation("根据学号获取申请")
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:list')")
    public R<JoinRequest> getApplicationByStudentId(@PathVariable String studentId) {
        try {
            JoinRequest application = admissionService.getApplicationByStudentId(studentId);
            if (application != null) {
                return R.ok(application);
            } else {
                return R.error("未找到该学号的申请记录");
            }
        } catch (Exception e) {
            log.error("根据学号获取申请失败", e);
            return R.error("根据学号获取申请失败: " + e.getMessage());
        }
    }

    /**
     * 删除申请
     * 权限：audit:member:* - super_admin
     */
    @ApiOperation("删除申请")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:delete')")
    public R<String> deleteApplication(@PathVariable Long id) {
        try {
            boolean success = admissionService.deleteApplication(id);
            if (success) {
                log.info("✅ 申请删除成功：ID={}", id);
                return R.ok("申请删除成功");
            } else {
                return R.error("申请删除失败");
            }
        } catch (Exception e) {
            log.error("删除申请失败", e);
            return R.error("申请删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取申请统计
     * 权限：audit:member:* - super_admin, manager
     */
    @ApiOperation("获取申请统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('audit:member:*', 'audit:member:list', 'content:statistics:view')")
    public R<Map<String, Object>> getApplicationStatistics() {
        try {
            Map<String, Object> statistics = admissionService.getApplicationStatistics();
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取申请统计失败", e);
            return R.error("获取申请统计失败: " + e.getMessage());
        }
    }

    /**
     * 验证邮箱与申请是否匹配
     */
    @ApiOperation("验证邮箱与申请是否匹配")
    @PostMapping("/validate-email")
    public R<Boolean> validateEmailMatch(@RequestBody Map<String, String> request) {
        try {
            String studentId = request.get("studentId");
            String email = request.get("email");

            boolean isValid = admissionService.validateEmailMatch(studentId, email);
            return R.ok(isValid);
        } catch (Exception e) {
            log.error("验证邮箱匹配失败", e);
            return R.error("验证邮箱匹配失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否是管理员
     */
    @ApiOperation("检查用户是否是管理员")
    @PostMapping("/check-admin")
    public R<Boolean> isAdminUser(@RequestBody Map<String, String> request) {
        try {
            String studentId = request.get("studentId");
            String email = request.get("email");

            boolean isAdmin = admissionService.isAdminUser(studentId, email);
            return R.ok(isAdmin);
        } catch (Exception e) {
            log.error("检查管理员权限失败", e);
            return R.error("检查管理员权限失败: " + e.getMessage());
        }
    }
    @PostMapping("/internal/register-success-notify")
    public Boolean notifyRegisterSuccess(@RequestBody RegisterSuccessNotifyDTO dto) {
        return admissionService.notifyRegisterSuccess(
                dto.getTenantId(),
                dto.getAuthUserId(),
                dto.getUsername(),
                dto.getName(),
                dto.getEmail(),
                dto.getReferralCode(),
                dto.getFormPayload()
        );
    }
}
