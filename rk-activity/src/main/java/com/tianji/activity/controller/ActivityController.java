package com.tianji.activity.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.activity.domain.dto.ActivityQueryDTO;
import com.tianji.activity.domain.dto.ActivityReviewDTO;
import com.tianji.activity.domain.po.Activity;
import com.tianji.activity.domain.po.ActivityAlbumPhoto;
import com.tianji.activity.domain.po.ActivityRegistration;
import com.tianji.activity.domain.vo.ActivityDetailVO;
import com.tianji.activity.domain.vo.ActivityListVO;
import com.tianji.activity.domain.vo.ActivityRegistrationVO;
import com.tianji.activity.mapper.ActivityAlbumPhotoMapper;
import com.tianji.activity.mapper.ActivityRegistrationMapper;
import com.tianji.activity.service.IActivityService;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.CreditGrantDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "活动管理接口")
@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final IActivityService activityService;
    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityAlbumPhotoMapper albumPhotoMapper;
    private final UserClient userClient;

    @ApiOperation("获取所有活动列表")
    @GetMapping("/list")
    public R<List<Activity>> getAllActivities() {
        try {
            return R.ok(activityService.getAllActivities());
        } catch (Exception e) {
            log.error("获取活动列表失败", e);
            return R.error("获取活动列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("分页获取活动列表")
    @GetMapping
    public R<Page<Activity>> getActivityPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type) {
        try {
            return R.ok(activityService.getActivityPage(page, size, status, type));
        } catch (Exception e) {
            log.error("分页获取活动列表失败", e);
            return R.error("获取活动列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取活动详情")
    @GetMapping("/{id}")
    public R<Activity> getActivityDetail(@PathVariable Long id) {
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity == null) {
                return R.error("活动不存在");
            }
            activityService.incrementViewCount(id);
            return R.ok(activity);
        } catch (Exception e) {
            log.error("获取活动详情失败", e);
            return R.error("获取活动详情失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取活动详情VO")
    @GetMapping("/{id}/detail")
    public R<ActivityDetailVO> getActivityDetailVO(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUser();
            ActivityDetailVO vo = activityService.getActivityDetailVO(id, userId);
            return vo == null ? R.error("活动不存在") : R.ok(vo);
        } catch (Exception e) {
            log.error("获取活动详情失败", e);
            return R.error("获取活动详情失败：" + e.getMessage());
        }
    }

    @ApiOperation("分页获取活动列表VO")
    @PostMapping("/page")
    public R<Page<ActivityListVO>> getActivityPageVO(@RequestBody ActivityQueryDTO queryDTO) {
        try {
            return R.ok(activityService.getActivityPageVO(queryDTO));
        } catch (Exception e) {
            log.error("分页获取活动列表失败", e);
            return R.error("获取活动列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("根据状态获取活动")
    @GetMapping("/status/{status}")
    public R<List<Activity>> getActivitiesByStatus(@PathVariable Integer status) {
        try {
            return R.ok(activityService.getActivitiesByStatus(status));
        } catch (Exception e) {
            log.error("根据状态获取活动失败", e);
            return R.error("获取活动列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取热门活动")
    @GetMapping("/hot")
    public R<List<Activity>> getHotActivities(@RequestParam(defaultValue = "10") Integer limit) {
        try {
            return R.ok(activityService.getHotActivities(limit));
        } catch (Exception e) {
            log.error("获取热门活动失败", e);
            return R.error("获取热门活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取推荐活动")
    @GetMapping("/top")
    public R<List<Activity>> getTopActivities(@RequestParam(defaultValue = "5") Integer limit) {
        try {
            return R.ok(activityService.getTopActivities(limit));
        } catch (Exception e) {
            log.error("获取推荐活动失败", e);
            return R.error("获取推荐活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("管理后台分页获取活动列表")
    @GetMapping("/admin/page")
    @PreAuthorize("hasAuthority('content:activity:view')")
    public R<Page<Activity>> getAdminActivityPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer recentDays,
            @RequestParam(required = false) String startTimeBegin,
            @RequestParam(required = false) String startTimeEnd) {
        try {
            return R.ok(activityService.getAdminActivityPage(
                    page,
                    size,
                    status,
                    type,
                    title,
                    parseDateTimeParam(startTimeBegin, true),
                    parseDateTimeParam(startTimeEnd, false),
                    keyword,
                    recentDays
            ));
        } catch (Exception e) {
            log.error("管理后台分页获取活动列表失败", e);
            return R.error("获取活动列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取待审核活动列表")
    @GetMapping("/review/pending")
    @PreAuthorize("hasAuthority('content:activity:view')")
    public R<Page<Activity>> getPendingReviewPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String applicant,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            return R.ok(activityService.getPendingReviewPage(page, size, title, applicant, roleId));
        } catch (Exception e) {
            log.error("获取待审核活动列表失败", e);
            return R.error("获取待审核活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取其他租户共享活动")
    @GetMapping("/shared")
    public R<List<Activity>> getSharedActivities(@RequestParam(defaultValue = "10") Integer limit) {
        try {
            return R.ok(activityService.getSharedActivities(limit));
        } catch (Exception e) {
            log.error("获取共享活动失败", e);
            return R.error("获取共享活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("搜索活动")
    @GetMapping("/search")
    public R<List<Activity>> searchActivities(@RequestParam String keyword) {
        try {
            return R.ok(activityService.searchActivities(keyword));
        } catch (Exception e) {
            log.error("搜索活动失败", e);
            return R.error("搜索活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取活动统计信息")
    @GetMapping("/statistics")
    public R<Map<String, Object>> getActivityStatistics() {
        try {
            return R.ok(activityService.getActivityStatistics());
        } catch (Exception e) {
            log.error("获取活动统计信息失败", e);
            return R.error("获取统计信息失败：" + e.getMessage());
        }
    }

    @ApiOperation("创建活动")
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('content:activity:add')")
    public R<Long> createActivity(@RequestBody Activity activity) {
        try {
            if (activity.getActivityName() == null || activity.getActivityName().isEmpty()) {
                return R.error("活动名称不能为空");
            }
            Long userId = UserContext.getUser();
            activity.setOrganizer(activity.getOrganizer() == null || activity.getOrganizer().isBlank() ? "用户-" + userId : activity.getOrganizer());
            return R.ok(activityService.createActivity(activity));
        } catch (Exception e) {
            log.error("创建活动失败", e);
            return R.error("创建活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("普通成员发起活动申请")
    @PostMapping("/submit")
    public R<Long> submitActivity(@RequestBody Activity activity) {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }
            if (activity.getActivityName() == null || activity.getActivityName().isEmpty()) {
                return R.error("活动名称不能为空");
            }
            activity.setOrganizer(activity.getOrganizer() == null || activity.getOrganizer().isBlank() ? "用户-" + userId : activity.getOrganizer());
            return R.ok(activityService.submitActivity(activity));
        } catch (Exception e) {
            log.error("发起活动申请失败", e);
            return R.error("发起活动申请失败：" + e.getMessage());
        }
    }

    @ApiOperation("更新活动")
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('content:activity:edit')")
    public R<String> updateActivity(@RequestBody Activity activity) {
        try {
            if (activity.getId() == null) {
                return R.error("活动ID不能为空");
            }
            return activityService.updateActivity(activity) > 0 ? R.ok("更新活动成功") : R.error("更新活动失败");
        } catch (Exception e) {
            log.error("更新活动失败", e);
            return R.error("更新活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("负责人审核活动")
    @PostMapping("/{id}/review/manager")
    @PreAuthorize("hasAnyAuthority('content:activity:edit')")
    public R<String> reviewByManager(
            @PathVariable Long id,
            @RequestBody ActivityReviewDTO dto,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!java.util.Objects.equals(roleId, 1L) && !java.util.Objects.equals(roleId, 7L)) {
                return R.error("无负责人审核权限");
            }
            Long reviewerId = UserContext.getUser();
            boolean allowProxyReview = java.util.Objects.equals(roleId, 1L);
            boolean success = activityService.reviewActivityByManager(id, Boolean.TRUE.equals(dto.getApproved()), dto.getReviewComment(), reviewerId, allowProxyReview);
            return success ? R.ok("负责人审核完成") : R.error("负责人审核失败");
        } catch (Exception e) {
            log.error("负责人审核活动失败", e);
            return R.error("负责人审核活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("指导老师审核活动")
    @PostMapping("/{id}/review/teacher")
    @PreAuthorize("hasAnyAuthority('content:activity:view')")
    public R<String> reviewByTeacher(
            @PathVariable Long id,
            @RequestBody ActivityReviewDTO dto,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!java.util.Objects.equals(roleId, 1L) && !java.util.Objects.equals(roleId, 8L)) {
                return R.error("无指导老师审核权限");
            }
            Long reviewerId = UserContext.getUser();
            boolean allowProxyReview = java.util.Objects.equals(roleId, 1L);
            boolean success = activityService.reviewActivityByTeacher(id, Boolean.TRUE.equals(dto.getApproved()), dto.getReviewComment(), reviewerId, allowProxyReview);
            return success ? R.ok("指导老师审核完成") : R.error("指导老师审核失败");
        } catch (Exception e) {
            log.error("指导老师审核活动失败", e);
            return R.error("指导老师审核活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("删除活动")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('content:activity:remove')")
    public R<String> deleteActivity(@PathVariable Long id) {
        try {
            return activityService.deleteActivity(id) > 0 ? R.ok("删除活动成功") : R.error("删除活动失败");
        } catch (Exception e) {
            log.error("删除活动失败", e);
            return R.error("删除活动失败：" + e.getMessage());
        }
    }

    @ApiOperation("发放活动学分")
    @PostMapping("/{id}/credits/grant")
    @PreAuthorize("hasAuthority('content:activity:edit')")
    public R<String> grantActivityCredits(@PathVariable Long id) {
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity == null) {
                return R.error("活动不存在");
            }
            if (!canGrantActivityCredits(activity)) {
                return R.error("活动结束后才能发放学分");
            }
            Integer points = activity.getPoints();
            if (points == null || points <= 0) {
                return R.error("当前活动未配置有效学分");
            }

            List<CreditGrantDTO> grants = activityService.getRegistrationList(id).stream()
                    .filter(item -> item.getUserId() != null)
                    .filter(item -> item.getRegistrationStatus() != null
                            && item.getRegistrationStatus() == ActivityRegistration.STATUS_CHECKED_IN)
                    .map(item -> {
                        CreditGrantDTO dto = new CreditGrantDTO();
                        dto.setUserId(item.getUserId());
                        dto.setTenantId(activity.getTenantId());
                        dto.setSourceType("activity");
                        dto.setSourceId(id);
                        dto.setCreditTypeCode("activity");
                        dto.setCreditHours(BigDecimal.ZERO);
                        dto.setCreditScore(BigDecimal.valueOf(points));
                        dto.setDescription("活动《" + activity.getActivityName() + "》签到学分发放");
                        return dto;
                    })
                    .collect(Collectors.toList());
            if (grants.isEmpty()) {
                return R.error("当前活动没有可发放学分的签到成员");
            }

            Integer processed = userClient.grantCredits(grants);
            return R.ok("已为 " + (processed == null ? 0 : processed) + " 名成员发放活动学分");
        } catch (Exception e) {
            log.error("发放活动学分失败", e);
            return R.error("发放活动学分失败: " + e.getMessage());
        }
    }

    private List<ActivityRegistrationVO> enrichRegistrationUsers(Long activityId, List<ActivityRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return List.of();
        }
        Activity activity = activityService.getActivityById(activityId);
        List<Long> authUserIds = registrations.stream()
                .map(ActivityRegistration::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserDTO> usersByAuthId = new HashMap<>();
        if (!authUserIds.isEmpty()) {
            try {
                usersByAuthId = userClient.queryUsersByAuthIds(authUserIds).stream()
                        .filter(item -> item.getAuthUserId() != null)
                        .collect(Collectors.toMap(UserDTO::getAuthUserId, Function.identity(), (left, right) -> left));
            } catch (Exception e) {
                log.warn("enrich activity registrations users failed, activityId={}, reason={}", activityId, e.getMessage());
            }
        }
        Map<Long, UserDTO> finalUsersByAuthId = usersByAuthId;
        return registrations.stream().map(registration -> {
            ActivityRegistrationVO vo = new ActivityRegistrationVO();
            vo.setId(registration.getId());
            vo.setActivityId(registration.getActivityId());
            vo.setActivityName(activity == null ? null : activity.getActivityName());
            vo.setUserId(registration.getUserId());
            vo.setRegistrationStatus(registration.getRegistrationStatus());
            vo.setRegistrationTime(registration.getRegistrationTime());
            vo.setCheckInTime(registration.getCheckInTime());
            vo.setCancelTime(registration.getCancelTime());
            vo.setCancelReason(registration.getCancelReason());
            vo.setRemark(registration.getRemark());
            UserDTO user = finalUsersByAuthId.get(registration.getUserId());
            if (user != null) {
                vo.setUserName(firstNonBlank(user.getName(), user.getUsername(), "用户-" + registration.getUserId()));
                vo.setUserAvatar(user.getIcon());
                vo.setStudentId(firstNonBlank(user.getStudentId(), "-"));
                vo.setEmail(firstNonBlank(user.getEmail(), "-"));
                vo.setCellPhone(firstNonBlank(user.getCellPhone(), "-"));
            } else {
                vo.setUserName("用户-" + registration.getUserId());
                vo.setStudentId("-");
                vo.setEmail("-");
                vo.setCellPhone("-");
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : 1L;
    }

    private boolean canGrantActivityCredits(Activity activity) {
        if (activity == null) {
            return false;
        }
        if (activity.getActivityStatus() != null && activity.getActivityStatus() == Activity.STATUS_ENDED) {
            return true;
        }
        return activity.getEndTime() != null && !activity.getEndTime().isAfter(LocalDateTime.now());
    }

    private LocalDateTime parseDateTimeParam(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim().replace("T", " ");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            if (text.length() == 10) {
                return LocalDateTime.parse(text + (startOfDay ? " 00:00:00" : " 23:59:59"), formatter);
            }
            if (text.length() == 16) {
                return LocalDateTime.parse(text + ":00", formatter);
            }
            if (text.length() >= 19) {
                return LocalDateTime.parse(text.substring(0, 19), formatter);
            }
        } catch (Exception e) {
            log.warn("ignore invalid activity date filter: {}", value);
        }
        return null;
    }

    @ApiOperation("活动报名")
    @PostMapping("/{id}/register")
    public R<Map<String, Object>> registerActivity(@PathVariable Long id, @RequestParam(required = false) String remark) {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }
            Map<String, Object> result = activityService.registerActivity(id, userId, remark);
            return Boolean.TRUE.equals(result.get("success")) ? R.ok(result) : R.error((String) result.get("message"));
        } catch (Exception e) {
            log.error("活动报名失败", e);
            return R.error("报名失败：" + e.getMessage());
        }
    }

    @ApiOperation("取消报名")
    @PostMapping("/{id}/cancel")
    public R<Map<String, Object>> cancelRegistration(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }
            Map<String, Object> result = activityService.cancelRegistration(id, userId, reason);
            return Boolean.TRUE.equals(result.get("success")) ? R.ok(result) : R.error((String) result.get("message"));
        } catch (Exception e) {
            log.error("取消报名失败", e);
            return R.error("取消报名失败：" + e.getMessage());
        }
    }

    @ApiOperation("活动签到")
    @PostMapping("/{id}/checkin")
    public R<Map<String, Object>> checkIn(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }
            Map<String, Object> result = activityService.checkIn(id, userId);
            return Boolean.TRUE.equals(result.get("success")) ? R.ok(result) : R.error((String) result.get("message"));
        } catch (Exception e) {
            log.error("活动签到失败", e);
            return R.error("签到失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取活动的报名列表")
    @GetMapping("/{id}/registrations")
    public R<List<ActivityRegistrationVO>> getRegistrationList(@PathVariable Long id) {
        try {
            List<ActivityRegistration> registrations = activityService.getRegistrationList(id);
            return R.ok(enrichRegistrationUsers(id, registrations));
        } catch (Exception e) {
            log.error("获取报名列表失败", e);
            return R.error("获取报名列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("查询活动相册照片")
    @GetMapping("/{id}/albums")
    @PreAuthorize("hasAuthority('content:activity:view')")
    public R<List<ActivityAlbumPhoto>> getActivityAlbums(@PathVariable Long id) {
        try {
            Activity activity = activityService.getActivityById(id);
            if (activity == null || !Objects.equals(activity.getTenantId(), currentTenantId())) {
                return R.error("娲诲姩涓嶅瓨鍦ㄦ垨鏃犳潈璁块棶");
            }
            return R.ok(albumPhotoMapper.selectList(
                    new QueryWrapper<ActivityAlbumPhoto>()
                            .eq("activity_id", id)
                            .eq("tenant_id", currentTenantId())
                            .eq("is_deleted", 0)
                            .orderByAsc("sort_order")
                            .orderByDesc("create_time")
            ));
        } catch (Exception e) {
            log.error("查询活动相册失败", e);
            return R.error("查询活动相册失败：" + e.getMessage());
        }
    }

    @ApiOperation("新增活动相册照片")
    @PostMapping("/{id}/albums")
    @PreAuthorize("hasAuthority('content:activity:edit')")
    public R<ActivityAlbumPhoto> createActivityAlbum(@PathVariable Long id, @RequestBody ActivityAlbumPhoto photo) {
        try {
            if (photo == null || photo.getPhotoUrl() == null || photo.getPhotoUrl().isBlank()) {
                return R.error("照片路径不能为空");
            }
            Activity activity = activityService.getActivityById(id);
            if (activity == null) {
                return R.error("活动不存在");
            }
            Long operatorId = UserContext.getUser();
            LocalDateTime now = LocalDateTime.now();
            photo.setId(null);
            photo.setActivityId(id);
            photo.setTenantId(activity.getTenantId() != null ? activity.getTenantId() : currentTenantId());
            photo.setUploaderId(operatorId);
            photo.setCreator(operatorId);
            photo.setUpdater(operatorId);
            photo.setCreateTime(now);
            photo.setUpdateTime(now);
            photo.setIsDeleted(0);
            if (photo.getSortOrder() == null) {
                photo.setSortOrder(0);
            }
            albumPhotoMapper.insert(photo);
            return R.ok(photo);
        } catch (Exception e) {
            log.error("保存活动相册失败", e);
            return R.error("保存活动相册失败：" + e.getMessage());
        }
    }

    @ApiOperation("删除活动相册照片")
    @DeleteMapping("/albums/{photoId}")
    @PreAuthorize("hasAuthority('content:activity:edit')")
    public R<String> deleteActivityAlbum(@PathVariable Long photoId) {
        try {
            ActivityAlbumPhoto photo = albumPhotoMapper.selectById(photoId);
            if (photo == null || !Objects.equals(photo.getTenantId(), currentTenantId())) {
                return R.error("鐓х墖涓嶅瓨鍦ㄦ垨鏃犳潈鍒犻櫎");
            }
            return albumPhotoMapper.deleteById(photoId) > 0 ? R.ok("删除成功") : R.error("照片不存在或已删除");
        } catch (Exception e) {
            log.error("删除活动相册失败", e);
            return R.error("删除活动相册失败：" + e.getMessage());
        }
    }

    @ApiOperation("检查是否已报名")
    @GetMapping("/{id}/registered")
    public R<Map<String, Object>> checkRegistered(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUser();
            Map<String, Object> result = new HashMap<>();
            if (userId == null) {
                result.put("registered", false);
                return R.ok(result);
            }
            result.put("registered", activityService.isRegistered(id, userId));
            return R.ok(result);
        } catch (Exception e) {
            log.error("检查报名状态失败", e);
            return R.error("检查报名状态失败：" + e.getMessage());
        }
    }

    @ApiOperation("更新活动状态")
    @PostMapping("/update-status")
    public R<String> updateActivityStatus() {
        try {
            activityService.updateActivityStatus();
            return R.ok("活动状态更新成功");
        } catch (Exception e) {
            log.error("更新活动状态失败", e);
            return R.error("更新活动状态失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取我的活动列表")
    @GetMapping("/my")
    public R<List<Map<String, Object>>> getMyActivities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }

            List<ActivityRegistration> registrations = registrationMapper.findByUserId(userId);
            List<Long> activityIds = registrations.stream()
                    .filter(r -> r.getRegistrationStatus() != ActivityRegistration.STATUS_CANCELLED)
                    .map(ActivityRegistration::getActivityId)
                    .collect(Collectors.toList());

            if (activityIds.isEmpty()) {
                return R.ok(new ArrayList<>());
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Long activityId : activityIds) {
                Activity activity = activityService.getActivityById(activityId);
                if (activity == null) {
                    continue;
                }
                ActivityRegistration registration = registrations.stream()
                        .filter(r -> r.getActivityId().equals(activityId))
                        .findFirst()
                        .orElse(null);

                Map<String, Object> item = new HashMap<>();
                item.put("id", activity.getId());
                item.put("activityName", activity.getActivityName());
                item.put("startTime", activity.getStartTime());
                item.put("endTime", activity.getEndTime());
                item.put("location", activity.getLocation());
                item.put("cover", activity.getCoverImage());
                item.put("status", activity.getActivityStatus());
                item.put("registrationStatus", registration != null ? registration.getRegistrationStatus() : null);
                item.put("registrationTime", registration != null ? registration.getRegistrationTime() : null);
                result.add(item);
            }
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取我的活动列表失败", e);
            return R.error("获取我的活动列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取我的活动报名记录")
    @GetMapping("/my/registrations")
    public R<List<ActivityRegistration>> getMyRegistrations() {
        try {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return R.error("请先登录");
            }
            return R.ok(registrationMapper.findByUserId(userId));
        } catch (Exception e) {
            log.error("获取我的报名记录失败", e);
            return R.error("获取报名记录失败：" + e.getMessage());
        }
    }
}
