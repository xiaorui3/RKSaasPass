package com.tianji.activity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.activity.domain.po.MemberAchievement;
import com.tianji.activity.mapper.MemberAchievementMapper;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Api(tags = "成就管理接口")
@RestController
@RequestMapping("/api/achievement")
@RequiredArgsConstructor
public class AchievementController {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    private final MemberAchievementMapper memberAchievementMapper;

    @ApiOperation("分页获取成就列表")
    @GetMapping("/list")
    public R<Page<MemberAchievement>> listAchievements(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer achievementType,
            @RequestParam(required = false) Integer achievementLevel,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        try {
            Page<MemberAchievement> pageParam = new Page<>(page, size);
            QueryWrapper<MemberAchievement> wrapper = new QueryWrapper<>();
            wrapper.eq("tenant_id", currentTenantId());
            if (achievementType != null) {
                wrapper.eq("achievement_type", achievementType);
            }
            if (achievementLevel != null) {
                wrapper.eq("achievement_level", achievementLevel);
            }
            if (status != null) {
                wrapper.eq("status", status);
            }
            if (keyword != null && !keyword.isBlank()) {
                wrapper.and(q -> q.like("achievement_title", keyword)
                        .or()
                        .like("description", keyword)
                        .or()
                        .like("award_organization", keyword));
            }
            wrapper.orderByDesc("award_date")
                    .orderByDesc("create_time")
                    .orderByDesc("id");
            return R.ok(memberAchievementMapper.selectPage(pageParam, wrapper));
        } catch (Exception e) {
            log.error("list achievements failed", e);
            return R.error("获取成就列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取成就详情")
    @GetMapping("/{id}")
    public R<MemberAchievement> getAchievement(@PathVariable Long id) {
        MemberAchievement achievement = memberAchievementMapper.selectById(id);
        return achievement == null ? R.error("成就不存在") : R.ok(achievement);
    }

    @ApiOperation("按类型获取成就")
    @GetMapping("/type/{type}")
    public R<List<MemberAchievement>> getByType(@PathVariable Integer type) {
        LambdaQueryWrapper<MemberAchievement> wrapper = scopedWrapper()
                .eq(MemberAchievement::getAchievementType, type)
                .orderByDesc(MemberAchievement::getAwardDate)
                .orderByDesc(MemberAchievement::getCreateTime);
        return R.ok(memberAchievementMapper.selectList(wrapper));
    }

    @ApiOperation("按级别获取成就")
    @GetMapping("/level/{level}")
    public R<List<MemberAchievement>> getByLevel(@PathVariable Integer level) {
        LambdaQueryWrapper<MemberAchievement> wrapper = scopedWrapper()
                .eq(MemberAchievement::getAchievementLevel, level)
                .orderByDesc(MemberAchievement::getAwardDate)
                .orderByDesc(MemberAchievement::getCreateTime);
        return R.ok(memberAchievementMapper.selectList(wrapper));
    }

    @ApiOperation("获取公开成就")
    @GetMapping("/public")
    public R<List<MemberAchievement>> getPublicAchievements() {
        LambdaQueryWrapper<MemberAchievement> wrapper = scopedWrapper()
                .eq(MemberAchievement::getStatus, STATUS_APPROVED)
                .eq(MemberAchievement::getManagerReviewStatus, MemberAchievement.REVIEW_APPROVED)
                .eq(MemberAchievement::getTeacherReviewStatus, MemberAchievement.REVIEW_APPROVED)
                .orderByDesc(MemberAchievement::getAwardDate)
                .orderByDesc(MemberAchievement::getCreateTime);
        return R.ok(memberAchievementMapper.selectList(wrapper));
    }

    @ApiOperation("搜索成就")
    @GetMapping("/search")
    public R<List<MemberAchievement>> searchAchievements(@RequestParam String keyword) {
        LambdaQueryWrapper<MemberAchievement> wrapper = scopedWrapper()
                .and(q -> q.like(MemberAchievement::getAchievementTitle, keyword)
                        .or()
                        .like(MemberAchievement::getDescription, keyword)
                        .or()
                        .like(MemberAchievement::getAwardOrganization, keyword))
                .orderByDesc(MemberAchievement::getAwardDate)
                .orderByDesc(MemberAchievement::getCreateTime);
        return R.ok(memberAchievementMapper.selectList(wrapper));
    }

    @ApiOperation("获取成就统计")
    @GetMapping("/statistics")
    public R<Map<String, Object>> getAchievementStatistics() {
        List<MemberAchievement> achievements = memberAchievementMapper.selectList(scopedWrapper());
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", achievements.size());
        stats.put("competition", achievements.stream().filter(item -> Integer.valueOf(1).equals(item.getAchievementType())).count());
        stats.put("project", achievements.stream().filter(item -> Integer.valueOf(2).equals(item.getAchievementType())).count());
        stats.put("paper", achievements.stream().filter(item -> Integer.valueOf(3).equals(item.getAchievementType())).count());
        stats.put("patent", achievements.stream().filter(item -> Integer.valueOf(4).equals(item.getAchievementType())).count());
        stats.put("national", achievements.stream().filter(item -> Integer.valueOf(1).equals(item.getAchievementLevel())).count());
        stats.put("provincial", achievements.stream().filter(item -> Integer.valueOf(2).equals(item.getAchievementLevel())).count());
        return R.ok(stats);
    }

    @ApiOperation("获取我的成就")
    @GetMapping("/my")
    public R<List<MemberAchievement>> getMyAchievements() {
        Long userId = UserContext.getUser();
        LambdaQueryWrapper<MemberAchievement> wrapper = scopedWrapper()
                .eq(MemberAchievement::getUserId, userId)
                .orderByDesc(MemberAchievement::getAwardDate)
                .orderByDesc(MemberAchievement::getCreateTime);
        return R.ok(memberAchievementMapper.selectList(wrapper));
    }

    @ApiOperation("新增成就")
    @PostMapping
    public R<String> addAchievement(@RequestBody MemberAchievement achievement) {
        try {
            if (achievement.getTenantId() == null) {
                achievement.setTenantId(currentTenantId());
            }
            if (achievement.getUserId() == null) {
                achievement.setUserId(UserContext.getUser());
            }
            if (achievement.getStatus() == null) {
                achievement.setStatus(STATUS_PENDING);
            }
            if (achievement.getManagerReviewerId() == null || achievement.getTeacherReviewerId() == null) {
                return R.error("负责人和指导老师审批人不能为空");
            }
            achievement.setManagerReviewStatus(MemberAchievement.REVIEW_PENDING);
            achievement.setManagerReviewComment(null);
            achievement.setManagerReviewTime(null);
            achievement.setTeacherReviewStatus(MemberAchievement.REVIEW_PENDING);
            achievement.setTeacherReviewComment(null);
            achievement.setTeacherReviewTime(null);
            memberAchievementMapper.insert(achievement);
            return R.ok("新增成功");
        } catch (Exception e) {
            log.error("add achievement failed", e);
            return R.error("新增成就失败：" + e.getMessage());
        }
    }

    @ApiOperation("更新成就")
    @PutMapping
    public R<String> updateAchievement(@RequestBody MemberAchievement achievement) {
        if (achievement.getId() == null) {
            return R.error("成就ID不能为空");
        }
        try {
            memberAchievementMapper.updateById(achievement);
            return R.ok("更新成功");
        } catch (Exception e) {
            log.error("update achievement failed", e);
            return R.error("更新成就失败：" + e.getMessage());
        }
    }

    @ApiOperation("删除成就")
    @DeleteMapping("/{id}")
    public R<String> deleteAchievement(@PathVariable Long id) {
        memberAchievementMapper.deleteById(id);
        return R.ok("删除成功");
    }

    @ApiOperation("批量删除成就")
    @DeleteMapping("/batch")
    public R<String> deleteAchievementBatch(@RequestBody List<Long> ids) {
        memberAchievementMapper.deleteBatchIds(ids);
        return R.ok("批量删除成功");
    }

    @ApiOperation("通过成就审核")
    @PostMapping("/{id}/approve")
    public R<String> approveAchievement(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        MemberAchievement achievement = memberAchievementMapper.selectById(id);
        if (achievement == null) {
            return R.error("成就不存在");
        }
        Long effectiveRoleId = roleId == null ? 1L : roleId;
        if (Objects.equals(effectiveRoleId, 1L)
                && Objects.equals(achievement.getManagerReviewStatus(), MemberAchievement.REVIEW_PENDING)) {
            R<String> managerResult = reviewAchievementByManager(id, remark, effectiveRoleId);
            if (managerResult.failed()) {
                return managerResult;
            }
        }
        R<String> teacherResult = reviewAchievementByTeacher(id, remark, effectiveRoleId);
        if (teacherResult.failed()) {
            return teacherResult;
        }
        return R.ok("审核通过");
    }

    @ApiOperation("拒绝成就审核")
    @PostMapping("/{id}/reject")
    public R<String> rejectAchievement(@PathVariable Long id, @RequestParam(required = false) String remark) {
        MemberAchievement achievement = memberAchievementMapper.selectById(id);
        if (achievement == null) {
            return R.error("成就不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        achievement.setStatus(STATUS_REJECTED);
        if (Objects.equals(achievement.getManagerReviewStatus(), MemberAchievement.REVIEW_PENDING)) {
            achievement.setManagerReviewStatus(MemberAchievement.REVIEW_REJECTED);
            achievement.setManagerReviewComment(remark);
            achievement.setManagerReviewTime(now);
        } else if (Objects.equals(achievement.getTeacherReviewStatus(), MemberAchievement.REVIEW_PENDING)) {
            achievement.setTeacherReviewStatus(MemberAchievement.REVIEW_REJECTED);
            achievement.setTeacherReviewComment(remark);
            achievement.setTeacherReviewTime(now);
        }
        achievement.setAuditRemark(remark);
        achievement.setAuditTime(now);
        achievement.setAuditUserId(UserContext.getUser());
        memberAchievementMapper.updateById(achievement);
        return R.ok("已拒绝");
    }

    @ApiOperation("负责人审核成就")
    @PostMapping("/{id}/review/manager")
    public R<String> reviewAchievementByManager(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!Objects.equals(roleId, 1L) && !Objects.equals(roleId, 7L)) {
                return R.error("no manager review permission");
            }
            MemberAchievement achievement = memberAchievementMapper.selectById(id);
            if (achievement == null) {
                return R.error("achievement not found");
            }
            if (!Objects.equals(achievement.getManagerReviewStatus(), MemberAchievement.REVIEW_PENDING)) {
                return R.error("manager review is not pending");
            }
            Long reviewerId = UserContext.getUser();
            boolean allowProxyReview = Objects.equals(roleId, 1L);
            if (!allowProxyReview && !Objects.equals(reviewerId, achievement.getManagerReviewerId())) {
                return R.error("only designated manager reviewer can approve");
            }
            achievement.setManagerReviewStatus(MemberAchievement.REVIEW_APPROVED);
            achievement.setManagerReviewComment(remark);
            achievement.setManagerReviewTime(LocalDateTime.now());
            achievement.setStatus(STATUS_PENDING);
            memberAchievementMapper.updateById(achievement);
            return R.ok("manager review approved");
        } catch (Exception e) {
            log.error("manager review achievement failed", e);
            return R.error("manager review achievement failed: " + e.getMessage());
        }
    }

    @ApiOperation("指导老师审核成就")
    @PostMapping("/{id}/review/teacher")
    public R<String> reviewAchievementByTeacher(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!Objects.equals(roleId, 1L) && !Objects.equals(roleId, 8L)) {
                return R.error("no teacher review permission");
            }
            MemberAchievement achievement = memberAchievementMapper.selectById(id);
            if (achievement == null) {
                return R.error("achievement not found");
            }
            if (!Objects.equals(achievement.getManagerReviewStatus(), MemberAchievement.REVIEW_APPROVED)) {
                return R.error("manager review must be approved first");
            }
            if (!Objects.equals(achievement.getTeacherReviewStatus(), MemberAchievement.REVIEW_PENDING)) {
                return R.error("teacher review is not pending");
            }
            Long reviewerId = UserContext.getUser();
            boolean allowProxyReview = Objects.equals(roleId, 1L);
            if (!allowProxyReview && !Objects.equals(reviewerId, achievement.getTeacherReviewerId())) {
                return R.error("only designated teacher reviewer can approve");
            }
            LocalDateTime now = LocalDateTime.now();
            achievement.setTeacherReviewStatus(MemberAchievement.REVIEW_APPROVED);
            achievement.setTeacherReviewComment(remark);
            achievement.setTeacherReviewTime(now);
            achievement.setStatus(STATUS_APPROVED);
            achievement.setAuditRemark(remark);
            achievement.setAuditTime(now);
            achievement.setAuditUserId(reviewerId);
            memberAchievementMapper.updateById(achievement);
            return R.ok("teacher review approved");
        } catch (Exception e) {
            log.error("teacher review achievement failed", e);
            return R.error("teacher review achievement failed: " + e.getMessage());
        }
    }

    private LambdaQueryWrapper<MemberAchievement> scopedWrapper() {
        LambdaQueryWrapper<MemberAchievement> wrapper = new LambdaQueryWrapper<>();
        Long tenantId = currentTenantId();
        if (tenantId != null) {
            wrapper.eq(MemberAchievement::getTenantId, tenantId);
        }
        return wrapper;
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
