package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.service.IClubMemberService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 社团成员申请控制器
 * 基于master分支的核心业务逻辑
 */
@Slf4j
@Api(tags = "社团成员申请接口")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class ClubMemberController {

    private final IClubMemberService clubMemberService;

    /**
     * 提交社团加入申请
     */
    @ApiOperation("提交社团加入申请")
    @PostMapping("/apply")
    public R<String> handleJoinRequest(@RequestBody ClubMember member) {
        try {
            clubMemberService.submitApplication(member);
            log.info("✅ 提交社团申请成功，学号: {}", member.getStudentId());
            return R.ok("申请提交成功");
        } catch (Exception e) {
            log.error("提交社团申请失败", e);
            return R.error("申请提交失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有申请列表
     */
    @ApiOperation("获取所有申请列表")
    @GetMapping("/list")
    public R<List<ClubMember>> getAllApplications(@RequestParam(required = false) Long tenantId) {
        try {
            List<ClubMember> members = tenantId == null
                    ? clubMemberService.getAllApplications()
                    : clubMemberService.getAllApplications(tenantId);
            return R.ok(members);
        } catch (Exception e) {
            log.error("获取申请列表失败", e);
            return R.error("获取申请列表失败");
        }
    }

    @ApiOperation("获取申请列表别名")
    @GetMapping("/applications")
    public R<List<ClubMember>> getApplicationsAlias(@RequestParam(required = false) Long tenantId) {
        return getAllApplications(tenantId);
    }

    @ApiOperation("获取已删除成员列表")
    @GetMapping("/deleted")
    public R<List<ClubMember>> getDeletedApplications() {
        try {
            return R.ok(clubMemberService.getDeletedApplications());
        } catch (Exception e) {
            log.error("获取已删除成员列表失败", e);
            return R.error("获取已删除成员列表失败");
        }
    }

    @ApiOperation("恢复已删除成员")
    @PostMapping("/{id}/restore")
    public R<String> restoreApplication(@PathVariable Long id) {
        try {
            clubMemberService.restoreApplication(id);
            log.info("恢复已删除成员成功，ID: {}", id);
            return R.ok("恢复成功");
        } catch (Exception e) {
            log.error("恢复已删除成员失败，ID: {}", id, e);
            return R.error("恢复失败：" + e.getMessage());
        }
    }

    /**
     * 根据学号查询申请状态
     */
    @ApiOperation("检查用户申请状态")
    @GetMapping("/check-user-application")
    public R<Map<String, Object>> checkApplication(@RequestParam String studentId) {
        try {
            Map<String, Object> result = clubMemberService.checkApplicationStatus(studentId);
            return R.ok(result);
        } catch (Exception e) {
            log.error("检查申请状态失败", e);
            return R.error("检查申请状态失败");
        }
    }

    /**
     * 审核申请
     */
    @ApiOperation("审核申请")
    @PostMapping("/review/{id}")
    public R<String> reviewApplication(
            @PathVariable Long id,
            @RequestParam Integer agreeStatus,
            @RequestParam(required = false) String reviewComment) {
        try {
            clubMemberService.reviewApplication(id, agreeStatus, reviewComment);
            log.info("✅ 审核申请成功，ID: {}, 状态: {}", id, agreeStatus);
            return R.ok("审核完成");
        } catch (Exception e) {
            log.error("审核申请失败", e);
            return R.error("审核失败：" + e.getMessage());
        }
    }

    /**
     * 获取申请详情
     */
    @ApiOperation("获取申请详情")
    @GetMapping("/{id}")
    public R<ClubMember> getApplicationDetail(@PathVariable Long id) {
        try {
            ClubMember member = clubMemberService.getApplicationById(id);
            if (member != null) {
                return R.ok(member);
            } else {
                return R.error("申请不存在");
            }
        } catch (Exception e) {
            log.error("获取申请详情失败", e);
            return R.error("获取申请详情失败");
        }
    }

    /**
     * 删除申请
     */
    @ApiOperation("删除申请")
    @DeleteMapping("/{id}")
    public R<String> deleteApplication(@PathVariable Long id) {
        try {
            clubMemberService.deleteApplication(id);
            log.info("✅ 删除申请成功，ID: {}", id);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("删除申请失败", e);
            return R.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取申请统计数据
     */
    @ApiOperation("获取申请统计数据")
    @GetMapping("/statistics")
    public R<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = clubMemberService.getStatistics();
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return R.error("获取统计数据失败");
        }
    }

    /**
     * 更新成员信息
     */
    @ApiOperation("更新成员信息")
    @PutMapping("/{id}")
    public R<String> updateMember(@PathVariable Long id, @RequestBody ClubMember member) {
        try {
            clubMemberService.updateMember(id, member);
            log.info("✅ 更新成员信息成功，ID: {}", id);
            return R.ok("更新成功");
        } catch (Exception e) {
            log.error("更新成员信息失败", e);
            return R.error("更新失败：" + e.getMessage());
        }
    }
}
