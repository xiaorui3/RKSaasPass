package com.tianji.activity.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.activity.domain.dto.CompetitionReviewDTO;
import com.tianji.activity.domain.po.Competition;
import com.tianji.activity.service.ICompetitionService;
import com.tianji.common.domain.R;
import com.tianji.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Slf4j
@Api(tags = "比赛审批接口")
@RestController
@RequestMapping("/api/competition")
@RequiredArgsConstructor
public class CompetitionApprovalController {

    private final ICompetitionService competitionService;

    private boolean isAdminRole(Long roleId) {
        return Objects.equals(roleId, 1L)
                || Objects.equals(roleId, 3L)
                || Objects.equals(roleId, 5L);
    }

    @ApiOperation("获取待审核比赛列表")
    @GetMapping("/review/pending")
    @PreAuthorize("hasAuthority('content:competition:view')")
    public R<Page<Competition>> getPendingReviewPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!isAdminRole(roleId) && !Objects.equals(roleId, 7L) && !Objects.equals(roleId, 8L)) {
                return R.error("无比赛审核权限");
            }
            return R.ok(competitionService.getPendingReviewPage(page, size, title, roleId));
        } catch (Exception e) {
            log.error("获取待审核比赛列表失败", e);
            return R.error("获取待审核比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("负责人审核比赛")
    @PostMapping("/{id}/review/manager")
    @PreAuthorize("hasAuthority('content:competition:edit')")
    public R<String> reviewByManager(
            @PathVariable Long id,
            @RequestBody CompetitionReviewDTO dto,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!isAdminRole(roleId) && !Objects.equals(roleId, 7L)) {
                return R.error("无比赛负责人审核权限");
            }
            boolean success = competitionService.reviewCompetitionByManager(
                    id,
                    Boolean.TRUE.equals(dto.getApproved()),
                    dto.getReviewComment(),
                    UserContext.getUser(),
                    Objects.equals(roleId, 1L)
            );
            return success ? R.ok("比赛负责人审核完成") : R.error("比赛负责人审核失败");
        } catch (Exception e) {
            log.error("负责人审核比赛失败", e);
            return R.error("负责人审核比赛失败：" + e.getMessage());
        }
    }

    @ApiOperation("指导老师审核比赛")
    @PostMapping("/{id}/review/teacher")
    @PreAuthorize("hasAuthority('content:competition:view')")
    public R<String> reviewByTeacher(
            @PathVariable Long id,
            @RequestBody CompetitionReviewDTO dto,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!isAdminRole(roleId) && !Objects.equals(roleId, 8L)) {
                return R.error("无比赛审核权限");
            }
            boolean success = competitionService.reviewCompetitionByTeacher(
                    id,
                    Boolean.TRUE.equals(dto.getApproved()),
                    dto.getReviewComment(),
                    UserContext.getUser(),
                    Objects.equals(roleId, 1L)
            );
            return success ? R.ok("比赛审核完成") : R.error("比赛审核失败");
        } catch (Exception e) {
            log.error("指导老师审核比赛失败", e);
            return R.error("指导老师审核比赛失败：" + e.getMessage());
        }
    }
}
