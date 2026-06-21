package com.tianji.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.api.dto.user.EmailCenterSendDTO;
import com.tianji.api.dto.user.NotificationInternalSaveDTO;
import com.tianji.api.dto.user.TenantSelfServiceAdmissionPolicyDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.tianji.activity.domain.dto.ActivityQueryDTO;
import com.tianji.activity.domain.dto.ActivitySaveDTO;
import com.tianji.activity.domain.po.Activity;
import com.tianji.activity.domain.po.ActivityCategory;
import com.tianji.activity.domain.po.ActivityRegistration;
import com.tianji.activity.domain.vo.ActivityDetailVO;
import com.tianji.activity.domain.vo.ActivityListVO;
import com.tianji.activity.domain.vo.ActivityRegistrationVO;
import com.tianji.activity.mapper.ActivityCategoryMapper;
import com.tianji.activity.mapper.ActivityMapper;
import com.tianji.activity.mapper.ActivityRegistrationMapper;
import com.tianji.activity.service.IActivityService;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.CacheKeyBuilder;
import com.tianji.common.utils.SearchIndexSyncUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 活动服务实现类
 *
 * 缓存策略：
 * - 活动详情缓存：1小时
 * - 活动列表缓存：10分钟
 * - 热门活动缓存：30分钟
 * - 所有缓存Key包含tenant_id实现多租户隔离
 *
 * @author RK-Web Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements IActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;
    private final SearchClient searchClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存过期时间配置（单位：分钟）
    private static final long CACHE_EXPIRE_DETAIL = 60;      // 详情缓存1小时
    private static final long CACHE_EXPIRE_LIST = 10;        // 列表缓存10分钟
    private static final long CACHE_EXPIRE_HOT = 30;         // 热门缓存30分钟
    private static final List<Long> DEFAULT_WORKFLOW_NOTIFY_ROLE_IDS = Arrays.asList(1L, 3L, 5L, 7L, 8L);
    private static final String SEARCH_ENTITY_TYPE_ACTIVITY = "ACTIVITY";

    @Value("${rk.web.public-base-url:}")
    private String publicBaseUrl;

    @Override
    public List<Activity> getAllActivities() {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        applyPublicVisibilityFilter(wrapper);
        wrapper.orderByDesc(Activity::getIsTop)
                .orderByDesc(Activity::getCreateTime);
        return activityMapper.selectList(wrapper);
    }

    @Override
    public Page<Activity> getActivityPage(Integer page, Integer size, Integer status, Integer type) {
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        applyPublicVisibilityFilter(wrapper);

        if (status != null) {
            wrapper.eq(Activity::getActivityStatus, status);
        }
        if (type != null) {
            wrapper.eq(Activity::getActivityType, type);
        }

        wrapper.orderByDesc(Activity::getIsTop)
               .orderByDesc(Activity::getCreateTime);

        return activityMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<Activity> getAdminActivityPage(Integer page, Integer size, Integer status, Integer type, String title,
                                               LocalDateTime startTimeBegin, LocalDateTime startTimeEnd) {
        return getAdminActivityPage(page, size, status, type, title, startTimeBegin, startTimeEnd, null, null);
    }

    @Override
    public Page<Activity> getAdminActivityPage(Integer page, Integer size, Integer status, Integer type, String title,
                                               LocalDateTime startTimeBegin, LocalDateTime startTimeEnd,
                                               String keyword, Integer recentDays) {
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Activity::getActivityStatus, status);
        }
        if (type != null) {
            wrapper.eq(Activity::getActivityType, type);
        }
        String searchText = firstNonBlank(keyword, title);
        if (searchText != null) {
            wrapper.and(w -> w.like(Activity::getActivityName, searchText)
                    .or().like(Activity::getOrganizer, searchText)
                    .or().like(Activity::getLocation, searchText)
                    .or().like(Activity::getActivityCode, searchText));
        }
        LocalDateTime resolvedStart = startTimeBegin;
        LocalDateTime resolvedEnd = startTimeEnd;
        if ((resolvedStart == null || resolvedEnd == null) && recentDays != null && recentDays > 0) {
            LocalDateTime now = LocalDateTime.now();
            resolvedStart = resolvedStart == null ? now.minusDays(Math.min(recentDays, 365)) : resolvedStart;
            resolvedEnd = resolvedEnd == null ? now.plusDays(Math.min(recentDays, 365)) : resolvedEnd;
        }
        if (resolvedStart != null) {
            wrapper.ge(Activity::getStartTime, resolvedStart);
        }
        if (resolvedEnd != null) {
            wrapper.le(Activity::getStartTime, resolvedEnd);
        }

        wrapper.orderByDesc(Activity::getIsTop)
                .orderByDesc(Activity::getStartTime)
                .orderByDesc(Activity::getCreateTime);

        return activityMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<Activity> getPendingReviewPage(Integer page, Integer size, String title, String applicant, Long roleId) {
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();

        if (title != null && !title.isBlank()) {
            wrapper.like(Activity::getActivityName, title);
        }
        if (applicant != null && !applicant.isBlank()) {
            wrapper.like(Activity::getOrganizer, applicant);
        }

        if (Objects.equals(roleId, 7L)) {
            wrapper.eq(Activity::getManagerReviewStatus, Activity.REVIEW_PENDING);
        } else if (Objects.equals(roleId, 8L)) {
            wrapper.eq(Activity::getManagerReviewStatus, Activity.REVIEW_APPROVED)
                    .eq(Activity::getTeacherReviewStatus, Activity.REVIEW_PENDING);
        } else {
            wrapper.and(w -> w.eq(Activity::getManagerReviewStatus, Activity.REVIEW_PENDING)
                    .or()
                    .eq(Activity::getManagerReviewStatus, Activity.REVIEW_APPROVED)
                    .eq(Activity::getTeacherReviewStatus, Activity.REVIEW_PENDING));
        }

        wrapper.orderByDesc(Activity::getCreateTime);
        return activityMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 获取活动详情（带缓存）
     */
    @Override
    public Activity getActivityById(Long id) {
        // 尝试从缓存获取
        String cacheKey = CacheKeyBuilder.buildDetailKey(CacheKeyBuilder.MODULE_ACTIVITY, id);
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取活动详情: {}", id);
                return objectMapper.readValue(cachedJson, Activity.class);
            }
        } catch (Exception e) {
            log.warn("读取活动详情缓存失败: {}", e.getMessage());
        }

        // 从数据库查询
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            activity = getSharedActivityById(id);
        }

        // 写入缓存
        if (activity != null) {
            try {
                String json = objectMapper.writeValueAsString(activity);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_DETAIL, TimeUnit.MINUTES);
                log.debug("活动详情已缓存: {}", id);
            } catch (Exception e) {
                log.warn("写入活动详情缓存失败: {}", e.getMessage());
            }
        }

        return activity;
    }

    @Override
    public List<Activity> getActivitiesByStatus(Integer status) {
        updateActivityStatus();
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        applyPublicVisibilityFilter(wrapper);
        wrapper.eq(Activity::getActivityStatus, status)
                .orderByDesc(Activity::getCreateTime);
        return activityMapper.selectList(wrapper);
    }

    /**
     * 获取热门活动（带缓存）
     */
    @Override
    public List<Activity> getHotActivities(Integer limit) {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_ACTIVITY, "hot", "limit" + limit);

        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取热门活动列表");
                return objectMapper.readValue(cachedJson, new TypeReference<List<Activity>>() {});
            }
        } catch (Exception e) {
            log.warn("读取热门活动缓存失败: {}", e.getMessage());
        }

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        applyPublicVisibilityFilter(wrapper);
        wrapper.eq(Activity::getIsHot, 1)
                .orderByDesc(Activity::getViewCount)
                .last("LIMIT " + limit);
        List<Activity> activities = activityMapper.selectList(wrapper);

        // 写入缓存
        if (activities != null && !activities.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(activities);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_HOT, TimeUnit.MINUTES);
                log.debug("热门活动列表已缓存");
            } catch (Exception e) {
                log.warn("写入热门活动缓存失败: {}", e.getMessage());
            }
        }

        return activities;
    }

    /**
     * 获取置顶活动（带缓存）
     */
    @Override
    public List<Activity> getTopActivities(Integer limit) {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_ACTIVITY, "top", "limit" + limit);

        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取置顶活动列表");
                return objectMapper.readValue(cachedJson, new TypeReference<List<Activity>>() {});
            }
        } catch (Exception e) {
            log.warn("读取置顶活动缓存失败: {}", e.getMessage());
        }

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        applyPublicVisibilityFilter(wrapper);
        wrapper.eq(Activity::getIsTop, 1)
                .orderByDesc(Activity::getCreateTime)
                .last("LIMIT " + limit);
        List<Activity> activities = activityMapper.selectList(wrapper);

        // 写入缓存
        if (activities != null && !activities.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(activities);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_HOT, TimeUnit.MINUTES);
                log.debug("置顶活动列表已缓存");
            } catch (Exception e) {
                log.warn("写入置顶活动缓存失败: {}", e.getMessage());
            }
        }

        return activities;
    }

    @Override
    public List<Activity> getSharedActivities(Integer limit) {
        Long currentTenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        int finalLimit = (limit == null || limit <= 0) ? 10 : limit;
        return runWithoutTenantIsolation(() -> activityMapper.selectList(
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getIsCrossTenant, 1)
                        .eq(Activity::getManagerReviewStatus, Activity.REVIEW_APPROVED)
                        .eq(Activity::getTeacherReviewStatus, Activity.REVIEW_APPROVED)
                        .ne(Activity::getTenantId, currentTenantId)
                        .orderByDesc(Activity::getIsTop)
                        .orderByDesc(Activity::getCreateTime)
                        .last("LIMIT " + finalLimit)
        ));
    }

    /**
     * 搜索活动
     * 搜索结果不缓存，每次实时查询
     */
    @Override
    public List<Activity> searchActivities(String keyword) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        applyPublicVisibilityFilter(wrapper);
        wrapper.and(w -> w.like(Activity::getActivityName, keyword)
                        .or()
                        .like(Activity::getContent, keyword)
                        .or()
                        .like(Activity::getOrganizer, keyword))
                .orderByDesc(Activity::getCreateTime);
        return activityMapper.selectList(wrapper);
    }

    /**
     * 创建活动（清除相关缓存）
     */
    @Override
    @Transactional
    public Long createActivity(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity is required");
        }
        Long tenantId = activity.getTenantId() != null ? activity.getTenantId() : (TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
        activity.setTenantId(tenantId);
        TenantSelfServiceAdmissionPolicyDTO selfServicePolicy = loadTenantSelfServicePolicy(tenantId);
        boolean requireManagerReview = requireClubManagerReview(selfServicePolicy);
        boolean requireTeacherReview = requireTeacherReview(selfServicePolicy);
        if ((requireManagerReview && activity.getManagerReviewerId() == null)
                || (requireTeacherReview && activity.getTeacherReviewerId() == null)) {
            throw new IllegalArgumentException("manager and teacher approvers are required");
        }
        ensureActiveActivityQuota(tenantId, selfServicePolicy);
        // 设置默认值
        if (activity.getActivityStatus() == null) {
            activity.setActivityStatus(Activity.STATUS_NOT_STARTED);
        }
        if (activity.getCurrentParticipants() == null) {
            activity.setCurrentParticipants(0);
        }
        if (activity.getViewCount() == null) {
            activity.setViewCount(0);
        }
        if (activity.getIsTop() == null) {
            activity.setIsTop(0);
        }
        if (activity.getIsHot() == null) {
            activity.setIsHot(0);
        }
        if (activity.getIsCrossTenant() == null) {
            activity.setIsCrossTenant(0);
        }
        activity.setManagerReviewStatus(requireManagerReview ? Activity.REVIEW_PENDING : Activity.REVIEW_APPROVED);
        activity.setTeacherReviewStatus(requireTeacherReview ? Activity.REVIEW_PENDING : Activity.REVIEW_APPROVED);
        activity.setManagerReviewComment(null);
        activity.setManagerReviewTime(requireManagerReview ? null : LocalDateTime.now());
        activity.setTeacherReviewComment(null);
        activity.setTeacherReviewTime(requireTeacherReview ? null : LocalDateTime.now());
        if (activity.getCreateTime() == null) {
            activity.setCreateTime(LocalDateTime.now());
        }
        if (activity.getUpdateTime() == null) {
            activity.setUpdateTime(LocalDateTime.now());
        }
        if (activity.getIsDeleted() == null) {
            activity.setIsDeleted(0);
        }
        if (activity.getCreator() == null) {
            activity.setCreator(UserContext.getUser());
        }
        normalizeActivityStatus(activity);

        activityMapper.insert(activity);
        log.info("✅ 创建活动成功，ID: {}", activity.getId());

        // 清除列表缓存
        clearListCache();
        syncActivitySearchIndex(activity.getId());

        return activity.getId();
    }

    @Override
    @Transactional
    public Long submitActivity(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity is required");
        }
        activity.setIsCrossTenant(0);
        TenantWorkflowConfigDTO workflowConfig = loadActivityWorkflowConfig(activity.getTenantId());
        WorkflowPolicyDTO policy = workflowConfig == null ? null : workflowConfig.getActivityPublish();
        Long activityId = createActivity(activity);
        if (shouldNotifyAdmins(policy)) {
            notifyActivityNextWorkflowStep(activity, activityId, policy);
        }
        return activityId;
    }

    /**
     * 更新活动（清除相关缓存）
     */
    @Override
    @Transactional
    public int updateActivity(Activity activity) {
        applyTenantSelfServiceReviewPolicy(activity);
        normalizeActivityStatus(activity);
        if (activity.getUpdateTime() == null) {
            activity.setUpdateTime(LocalDateTime.now());
        }
        int result = activityMapper.updateById(activity);
        if (result > 0) {
            log.info("✅ 更新活动成功，ID: {}", activity.getId());
            // 清除详情缓存和列表缓存
            clearDetailCache(activity.getId());
            clearListCache();
            syncActivitySearchIndex(activity.getId());
        }
        return result;
    }

    @Override
    @Transactional
    public boolean reviewActivityByManager(Long activityId, boolean approved, String reviewComment, Long reviewerId) {
        return reviewActivityByManager(activityId, approved, reviewComment, reviewerId, false);
    }

    @Override
    @Transactional
    public boolean reviewActivityByManager(Long activityId, boolean approved, String reviewComment, Long reviewerId, boolean allowProxyReview) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !Objects.equals(activity.getManagerReviewStatus(), Activity.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, activity.getManagerReviewerId())) {
            return false;
        }

        activity.setManagerReviewStatus(approved ? Activity.REVIEW_APPROVED : Activity.REVIEW_REJECTED);
        activity.setManagerReviewComment(reviewComment);
        activity.setManagerReviewTime(LocalDateTime.now());
        if (!approved) {
            activity.setTeacherReviewStatus(Activity.REVIEW_PENDING);
        }
        int result = updateActivity(activity);
        if (result > 0 && approved) {
            if (isFullyApproved(activity)) {
                notifyActivityPublishSuccess(activity);
            } else {
                notifyActivityTeacherPendingReview(activity);
            }
        }
        return result > 0;
    }

    @Override
    @Transactional
    public boolean reviewActivityByTeacher(Long activityId, boolean approved, String reviewComment, Long reviewerId) {
        return reviewActivityByTeacher(activityId, approved, reviewComment, reviewerId, false);
    }

    @Override
    @Transactional
    public boolean reviewActivityByTeacher(Long activityId, boolean approved, String reviewComment, Long reviewerId, boolean allowProxyReview) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null
                || !Objects.equals(activity.getManagerReviewStatus(), Activity.REVIEW_APPROVED)
                || !Objects.equals(activity.getTeacherReviewStatus(), Activity.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, activity.getTeacherReviewerId())) {
            return false;
        }

        activity.setTeacherReviewStatus(approved ? Activity.REVIEW_APPROVED : Activity.REVIEW_REJECTED);
        activity.setTeacherReviewComment(reviewComment);
        activity.setTeacherReviewTime(LocalDateTime.now());
        int result = updateActivity(activity);
        if (result > 0 && approved) {
            notifyActivityPublishSuccess(activity);
        }
        return result > 0;
    }

    /**
     * 删除活动（清除相关缓存）
     */
    @Override
    @Transactional
    public int deleteActivity(Long id) {
        Activity existing = activityMapper.selectById(id);
        Long tenantId = existing == null ? null : existing.getTenantId();
        int result = activityMapper.deleteById(id);
        if (result > 0) {
            log.info("✅ 删除活动成功，ID: {}", id);
            // 清除详情缓存和列表缓存
            clearDetailCache(id);
            clearListCache();
            deleteActivitySearchIndex(id, tenantId);
        }
        return result;
    }

    /**
     * 增加浏览量
     * 使用Redis计数器，定时同步到数据库
     */
    @Override
    public void incrementViewCount(Long id) {
        // 使用Redis INCR操作增加计数
        String countKey = CacheKeyBuilder.buildCountKey(CacheKeyBuilder.MODULE_ACTIVITY, "view", id);
        Long newCount = redisTemplate.opsForValue().increment(countKey);

        // 设置计数器过期时间（7天）
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(countKey, 7, TimeUnit.DAYS);
        }

        // 每100次浏览更新一次数据库
        if (newCount != null && newCount % 100 == 0) {
            Activity activity = activityMapper.selectById(id);
            if (activity != null) {
                activity.setViewCount(activity.getViewCount() + 100);
                activityMapper.updateById(activity);
                // 重置计数器
                redisTemplate.opsForValue().set(countKey, "0", 7, TimeUnit.DAYS);
            }
        }
    }

    /**
     * 报名活动
     */
    @Override
    @Transactional
    public Map<String, Object> registerActivity(Long activityId, Long userId, String remark) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 检查活动是否存在
        Activity activity = getActivityById(activityId);
        if (activity == null) {
            result.put("success", false);
            result.put("message", "活动不存在");
            return result;
        }

        // 2. 检查活动报名时间与状态
        String cannotRegisterReason = getCannotRegisterReason(activity, now);
        if (cannotRegisterReason != null) {
            result.put("success", false);
            result.put("message", cannotRegisterReason);
            return result;
        }

        // 3. 检查是否已报名
        ActivityRegistration existing = registrationMapper.findByActivityIdAndUserId(activityId, userId);
        if (existing != null) {
            result.put("success", false);
            result.put("message", "您已报名该活动");
            return result;
        }

        Long currentTenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        boolean crossTenantRequest = !Objects.equals(activity.getTenantId(), currentTenantId);
        if (crossTenantRequest && !Objects.equals(activity.getIsCrossTenant(), 1)) {
            result.put("success", false);
            result.put("message", "该活动不允许跨租户报名");
            return result;
        }

        // 4. 检查人数限制
        if (activity.getMaxParticipants() != null &&
            activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            result.put("success", false);
            result.put("message", "活动报名人数已满");
            return result;
        }

        // 5. 创建报名记录
        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(activityId);
        registration.setUserId(userId);
        registration.setRegistrationStatus(ActivityRegistration.STATUS_REGISTERED);
        registration.setRegistrationTime(now);
        registration.setRemark(remark);
        registration.setTenantId(currentTenantId);
        registration.setCreateTime(now);
        registration.setUpdateTime(now);
        registration.setCreator(userId);
        registration.setUpdater(userId);
        registration.setIsDeleted(0);
        registrationMapper.insert(registration);

        // 6. 增加参与人数
        if (crossTenantRequest) {
            runWithoutTenantIsolation(() -> {
                activityMapper.incrementParticipants(activityId);
                return null;
            });
        } else {
            activityMapper.incrementParticipants(activityId);
        }

        // 7. 清除活动详情缓存
        clearDetailCache(activityId);

        log.info("✅ 用户 {} 报名活动 {} 成功", userId, activityId);
        result.put("success", true);
        result.put("message", "报名成功");
        result.put("registrationId", registration.getId());
        return result;
    }

    /**
     * 取消报名
     */
    @Override
    @Transactional
    public Map<String, Object> cancelRegistration(Long activityId, Long userId, String reason) {
        Map<String, Object> result = new HashMap<>();
        Activity activity = getActivityById(activityId);
        Long currentTenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        boolean crossTenantRequest = activity != null && !Objects.equals(activity.getTenantId(), currentTenantId);

        // 1. 查找报名记录
        ActivityRegistration registration = registrationMapper.findByActivityIdAndUserId(activityId, userId);
        if (registration == null) {
            result.put("success", false);
            result.put("message", "未找到报名记录");
            return result;
        }

        // 2. 检查是否已取消
        if (registration.getRegistrationStatus() == ActivityRegistration.STATUS_CANCELLED) {
            result.put("success", false);
            result.put("message", "报名已取消");
            return result;
        }

        // 3. 检查是否已签到
        if (registration.getRegistrationStatus() == ActivityRegistration.STATUS_CHECKED_IN) {
            result.put("success", false);
            result.put("message", "已签到的活动不能取消报名");
            return result;
        }

        // 4. 更新报名状态
        registration.setRegistrationStatus(ActivityRegistration.STATUS_CANCELLED);
        registration.setCancelTime(LocalDateTime.now());
        registration.setCancelReason(reason);
        registrationMapper.updateById(registration);

        // 5. 减少参与人数
        if (crossTenantRequest) {
            runWithoutTenantIsolation(() -> {
                activityMapper.decrementParticipants(activityId);
                return null;
            });
        } else {
            activityMapper.decrementParticipants(activityId);
        }

        // 6. 清除活动详情缓存
        clearDetailCache(activityId);

        log.info("✅ 用户 {} 取消报名活动 {} 成功", userId, activityId);
        result.put("success", true);
        result.put("message", "取消报名成功");
        return result;
    }

    /**
     * 签到
     */
    @Override
    @Transactional
    public Map<String, Object> checkIn(Long activityId, Long userId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查找报名记录
        ActivityRegistration registration = registrationMapper.findByActivityIdAndUserId(activityId, userId);
        if (registration == null) {
            result.put("success", false);
            result.put("message", "未找到报名记录");
            return result;
        }

        // 2. 检查是否已签到
        if (registration.getRegistrationStatus() == ActivityRegistration.STATUS_CHECKED_IN) {
            result.put("success", false);
            result.put("message", "已签到，请勿重复签到");
            return result;
        }

        // 3. 检查是否已取消
        if (registration.getRegistrationStatus() == ActivityRegistration.STATUS_CANCELLED) {
            result.put("success", false);
            result.put("message", "报名已取消，无法签到");
            return result;
        }

        // 4. 更新签到状态
        registration.setRegistrationStatus(ActivityRegistration.STATUS_CHECKED_IN);
        registration.setCheckInTime(LocalDateTime.now());
        registrationMapper.updateById(registration);

        log.info("✅ 用户 {} 签到活动 {} 成功", userId, activityId);
        result.put("success", true);
        result.put("message", "签到成功");
        return result;
    }

    @Override
    public List<ActivityRegistration> getRegistrationList(Long activityId) {
        return runWithoutTenantIsolation(() -> registrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .ne(ActivityRegistration::getRegistrationStatus, ActivityRegistration.STATUS_CANCELLED)
                        .orderByDesc(ActivityRegistration::getRegistrationTime)
        ));
    }

    @Override
    public boolean isRegistered(Long activityId, Long userId) {
        ActivityRegistration registration = registrationMapper.findByActivityIdAndUserId(activityId, userId);
        return registration != null && registration.getRegistrationStatus() != ActivityRegistration.STATUS_CANCELLED;
    }

    /**
     * 获取活动统计（带缓存）
     */
    @Override
    public Map<String, Object> getActivityStatistics() {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_ACTIVITY, "statistics", "total");

        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取活动统计");
                return objectMapper.readValue(cachedJson, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("读取活动统计缓存失败: {}", e.getMessage());
        }

        Map<String, Object> statistics = new HashMap<>();

        // 总活动数
        Integer total = activityMapper.selectCount(new LambdaQueryWrapper<>());
        statistics.put("total", total);

        // 各状态活动数
        Integer notStarted = activityMapper.selectCount(
                new LambdaQueryWrapper<Activity>().eq(Activity::getActivityStatus, Activity.STATUS_NOT_STARTED));
        Integer registering = activityMapper.selectCount(
                new LambdaQueryWrapper<Activity>().eq(Activity::getActivityStatus, Activity.STATUS_REGISTERING));
        Integer ongoing = activityMapper.selectCount(
                new LambdaQueryWrapper<Activity>().eq(Activity::getActivityStatus, Activity.STATUS_ONGOING));
        Integer ended = activityMapper.selectCount(
                new LambdaQueryWrapper<Activity>().eq(Activity::getActivityStatus, Activity.STATUS_ENDED));

        statistics.put("notStarted", notStarted);
        statistics.put("registering", registering);
        statistics.put("ongoing", ongoing);
        statistics.put("ended", ended);

        // 热门活动数
        Integer hotCount = activityMapper.selectCount(
                new LambdaQueryWrapper<Activity>().eq(Activity::getIsHot, 1));
        statistics.put("hotCount", hotCount);

        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(statistics);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_LIST, TimeUnit.MINUTES);
            log.debug("活动统计已缓存");
        } catch (Exception e) {
            log.warn("写入活动统计缓存失败: {}", e.getMessage());
        }

        return statistics;
    }

    /**
     * 更新活动状态
     */
    @Override
    @Transactional
    public void updateActivityStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 更新为报名中状态
        activityMapper.selectList(
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getActivityStatus, Activity.STATUS_NOT_STARTED)
                        .le(Activity::getRegistrationStartTime, now)
                        .gt(Activity::getRegistrationEndTime, now)
        ).forEach(activity -> {
            activity.setActivityStatus(Activity.STATUS_REGISTERING);
            activityMapper.updateById(activity);
            log.info("活动 {} 状态更新为：报名中", activity.getId());
            clearDetailCache(activity.getId());
        });

        // 更新为进行中状态
        activityMapper.selectList(
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getActivityStatus, Activity.STATUS_REGISTERING)
                        .le(Activity::getStartTime, now)
                        .gt(Activity::getEndTime, now)
        ).forEach(activity -> {
            activity.setActivityStatus(Activity.STATUS_ONGOING);
            activityMapper.updateById(activity);
            log.info("活动 {} 状态更新为：进行中", activity.getId());
            clearDetailCache(activity.getId());
        });

        // 更新为已结束状态
        activityMapper.selectList(
                new LambdaQueryWrapper<Activity>()
                        .in(Activity::getActivityStatus, Arrays.asList(Activity.STATUS_NOT_STARTED, Activity.STATUS_REGISTERING, Activity.STATUS_ONGOING))
                        .le(Activity::getEndTime, now)
        ).forEach(activity -> {
            activity.setActivityStatus(Activity.STATUS_ENDED);
            activityMapper.updateById(activity);
            log.info("活动 {} 状态更新为：已结束", activity.getId());
            clearDetailCache(activity.getId());
        });

        // 清除列表缓存
        clearListCache();
    }

    // ==================== VO转换方法 ====================

    /**
     * 获取活动分页VO（带缓存）
     */
    @Override
    public Page<ActivityListVO> getActivityPageVO(ActivityQueryDTO queryDTO) {
        Page<Activity> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Activity::getActivityStatus, queryDTO.getStatus());
        }
        if (queryDTO.getType() != null) {
            wrapper.eq(Activity::getActivityType, queryDTO.getType());
        }
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Activity::getCategoryId, queryDTO.getCategoryId());
        }
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(Activity::getActivityName, queryDTO.getKeyword())
                    .or().like(Activity::getOrganizer, queryDTO.getKeyword()));
        }
        if (Boolean.TRUE.equals(queryDTO.getHotOnly())) {
            wrapper.eq(Activity::getIsHot, 1);
        }
        if (Boolean.TRUE.equals(queryDTO.getTopOnly())) {
            wrapper.eq(Activity::getIsTop, 1);
        }

        wrapper.orderByDesc(Activity::getIsTop)
               .orderByDesc(Activity::getCreateTime);

        Page<Activity> activityPage = activityMapper.selectPage(pageParam, wrapper);

        // 转换为VO
        Page<ActivityListVO> voPage = new Page<>(activityPage.getCurrent(), activityPage.getSize(), activityPage.getTotal());
        List<ActivityListVO> voList = activityPage.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 获取活动详情VO（带缓存）
     */
    @Override
    public ActivityDetailVO getActivityDetailVO(Long id, Long userId) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            return null;
        }

        // 增加浏览次数
        incrementViewCount(id);

        ActivityDetailVO vo = new ActivityDetailVO();
        BeanUtils.copyProperties(activity, vo);

        // 设置分类名称
        if (activity.getCategoryId() != null) {
            ActivityCategory category = categoryMapper.selectById(activity.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }

        // 设置报名状态
        if (userId != null) {
            ActivityRegistration registration = registrationMapper.findByActivityIdAndUserId(id, userId);
            if (registration != null) {
                vo.setRegistered(true);
                vo.setRegistrationStatus(registration.getRegistrationStatus());
            } else {
                vo.setRegistered(false);
            }
        } else {
            vo.setRegistered(false);
        }

        // 设置操作权限
        LocalDateTime now = LocalDateTime.now();
        vo.setCanRegister(canRegister(activity, now));
        vo.setCanCheckIn(canCheckIn(activity, now));
        vo.setCanCancel(canCancel(activity, now));
        vo.setCannotRegisterReason(getCannotRegisterReason(activity, now));

        return vo;
    }

    /**
     * 转换为列表VO
     */
    private ActivityListVO convertToListVO(Activity activity) {
        ActivityListVO vo = new ActivityListVO();
        BeanUtils.copyProperties(activity, vo);
        vo.setIsTop(activity.getIsTop() == 1);
        vo.setIsHot(activity.getIsHot() == 1);
        return vo;
    }

    /**
     * 判断是否可以报名
     */
    private boolean canRegister(Activity activity, LocalDateTime now) {
        if (activity == null) {
            return false;
        }
        if (!isFullyApproved(activity)) {
            return false;
        }
        if (activity.getActivityStatus() == Activity.STATUS_ENDED) {
            return false;
        }
        if (activity.getStartTime() != null && !now.isBefore(activity.getStartTime())) {
            return false;
        }
        if (activity.getRegistrationStartTime() != null && now.isBefore(activity.getRegistrationStartTime())) {
            return false;
        }
        if (activity.getRegistrationEndTime() != null && now.isAfter(activity.getRegistrationEndTime())) {
            return false;
        }
        if (activity.getMaxParticipants() != null &&
            activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            return false;
        }
        return true;
    }

    /**
     * 判断是否可以签到
     */
    private boolean canCheckIn(Activity activity, LocalDateTime now) {
        // 活动状态必须是进行中
        return activity.getActivityStatus() == Activity.STATUS_ONGOING;
    }

    /**
     * 判断是否可以取消报名
     */
    private boolean canCancel(Activity activity, LocalDateTime now) {
        // 活动未开始或报名中可以取消
        return activity.getActivityStatus() == Activity.STATUS_NOT_STARTED ||
               activity.getActivityStatus() == Activity.STATUS_REGISTERING;
    }

    /**
     * 获取不能报名的原因
     */
    private String getCannotRegisterReason(Activity activity, LocalDateTime now) {
        if (activity == null) {
            return "活动不存在";
        }
        if (!isFullyApproved(activity)) {
            return "活动尚未对外开放报名";
        }
        if (activity.getActivityStatus() == Activity.STATUS_ENDED) {
            return "活动已结束";
        }
        if (activity.getStartTime() != null && !now.isBefore(activity.getStartTime())) {
            return "活动进行中，无法报名";
        }
        if (activity.getRegistrationStartTime() != null && now.isBefore(activity.getRegistrationStartTime())) {
            return "报名尚未开始";
        }
        if (activity.getRegistrationEndTime() != null && now.isAfter(activity.getRegistrationEndTime())) {
            return "报名已截止";
        }
        if (activity.getMaxParticipants() != null &&
            activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            return "报名人数已满";
        }
        return null;
    }

    /**
     * 清除详情缓存
     */
    private void clearDetailCache(Long id) {
        String cacheKey = CacheKeyBuilder.buildDetailKey(CacheKeyBuilder.MODULE_ACTIVITY, id);
        redisTemplate.delete(cacheKey);
        log.debug("已清除活动详情缓存: {}", id);
    }

    /**
     * 清除列表相关缓存
     */
    private void clearListCache() {
        try {
            Long tenantId = com.tianji.common.utils.TenantContext.getTenantId();
            if (tenantId == null) {
                tenantId = 0L;
            }
            String pattern = CacheKeyBuilder.MODULE_ACTIVITY + ":" + tenantId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                keys.removeIf(key -> key.contains(":detail:"));
                if (!keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.debug("已清除活动列表缓存，共{}个key", keys.size());
                }
            }
        } catch (Exception e) {
            log.warn("清除活动列表缓存失败: {}", e.getMessage());
        }
    }

    private Activity getSharedActivityById(Long id) {
        return runWithoutTenantIsolation(() -> {
            Activity activity = activityMapper.selectById(id);
            if (activity != null
                    && Objects.equals(activity.getIsCrossTenant(), 1)
                    && isFullyApproved(activity)) {
                return activity;
            }
            return null;
        });
    }

    private void applyPublicVisibilityFilter(LambdaQueryWrapper<Activity> wrapper) {
        wrapper.eq(Activity::getManagerReviewStatus, Activity.REVIEW_APPROVED)
                .eq(Activity::getTeacherReviewStatus, Activity.REVIEW_APPROVED);
    }

    private boolean isFullyApproved(Activity activity) {
        return Objects.equals(activity.getManagerReviewStatus(), Activity.REVIEW_APPROVED)
                && Objects.equals(activity.getTeacherReviewStatus(), Activity.REVIEW_APPROVED);
    }

    private boolean isPublicActivity(Activity activity) {
        return activity != null
                && isFullyApproved(activity)
                && !Integer.valueOf(1).equals(activity.getIsDeleted());
    }

    private void syncActivitySearchIndex(Long id) {
        if (id == null) {
            return;
        }
        SearchIndexSyncUtils.runAfterCommit(() -> {
            Activity activity = null;
            try {
                activity = activityMapper.selectById(id);
                if (activity == null || !isPublicActivity(activity)) {
                    searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_ACTIVITY, id,
                            activity == null ? TenantContext.getTenantId() : activity.getTenantId());
                    return;
                }
                searchClient.upsertGlobalDocument(buildActivitySearchDocument(activity));
            } catch (Exception e) {
                log.warn("sync activity global search index failed, activityId={}, tenantId={}, reason={}",
                        id, activity == null ? null : activity.getTenantId(), e.getMessage());
            }
        });
    }

    private void deleteActivitySearchIndex(Long id, Long tenantId) {
        if (id == null) {
            return;
        }
        Long resolvedTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        SearchIndexSyncUtils.runAfterCommit(() -> {
            try {
                searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_ACTIVITY, id, resolvedTenantId);
            } catch (Exception e) {
                log.warn("delete activity global search index failed, activityId={}, tenantId={}, reason={}",
                        id, resolvedTenantId, e.getMessage());
            }
        });
    }

    @Override
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getIsDeleted, 0);
        applyPublicVisibilityFilter(wrapper);
        wrapper.orderByDesc(Activity::getUpdateTime);
        return activityMapper.selectList(wrapper).stream()
                .filter(this::isPublicActivity)
                .map(this::buildActivitySearchDocument)
                .collect(Collectors.toList());
    }

    private GlobalSearchDocumentDTO buildActivitySearchDocument(Activity activity) {
        return new GlobalSearchDocumentDTO()
                .setEntityType(SEARCH_ENTITY_TYPE_ACTIVITY)
                .setEntityId(activity.getId())
                .setTenantId(activity.getTenantId())
                .setTitle(activity.getActivityName())
                .setSummary(activitySummary(activity))
                .setContent(activity.getContent())
                .setTags(activityTags(activity))
                .setRoute("/activities/" + activity.getId())
                .setCoverUrl(activity.getCoverImage())
                .setUpdatedAt(formatUpdatedAt(activity.getUpdateTime(), activity.getCreateTime()))
                .setVisible(isPublicActivity(activity));
    }

    private String activitySummary(Activity activity) {
        if (activity == null) {
            return null;
        }
        return firstNonBlank(activity.getOrganizer(), activity.getLocation(), plainTextExcerpt(activity.getContent(), 180));
    }

    private String activityTags(Activity activity) {
        if (activity == null) {
            return null;
        }
        List<String> tags = new ArrayList<>();
        if (activity.getActivityType() != null) {
            tags.add("type:" + activity.getActivityType());
        }
        if (StringUtils.hasText(activity.getOrganizer())) {
            tags.add(activity.getOrganizer().trim());
        }
        if (StringUtils.hasText(activity.getLocation())) {
            tags.add(activity.getLocation().trim());
        }
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    private String plainTextExcerpt(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String formatUpdatedAt(LocalDateTime... times) {
        if (times == null) {
            return null;
        }
        for (LocalDateTime time : times) {
            if (time != null) {
                return time.toString();
            }
        }
        return null;
    }

    private boolean requiresActivityApproval(Long tenantId) {
        WorkflowPolicyDTO policy = loadActivityPublishPolicy(tenantId);
        return shouldRequireActivityApproval(policy);
    }

    private boolean shouldRequireActivityApproval(WorkflowPolicyDTO policy) {
        if (policy == null) {
            return true;
        }
        return !Boolean.FALSE.equals(policy.getRequireApproval());
    }

    private boolean shouldNotifyAdmins(WorkflowPolicyDTO policy) {
        if (policy == null) {
            return true;
        }
        return !Boolean.FALSE.equals(policy.getNotifyAdmins());
    }

    private boolean shouldNotifyOnSuccess(WorkflowPolicyDTO policy) {
        if (policy == null) {
            return true;
        }
        return !Boolean.FALSE.equals(policy.getNotifyOnSuccess());
    }

    private TenantSelfServiceAdmissionPolicyDTO loadTenantSelfServicePolicy(Long tenantId) {
        try {
            Long resolvedTenantId = tenantId != null ? tenantId : (TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
            return userClient.queryTenantSelfServiceAdmissionPolicy(resolvedTenantId);
        } catch (Exception e) {
            log.warn("load tenant self service policy failed, tenantId={}, reason={}", tenantId, e.getMessage());
            return null;
        }
    }

    private void applyTenantSelfServiceReviewPolicy(Activity activity) {
        if (activity == null) {
            return;
        }
        TenantSelfServiceAdmissionPolicyDTO policy = loadTenantSelfServicePolicy(activity.getTenantId());
        if (!requireClubManagerReview(policy)) {
            activity.setManagerReviewStatus(Activity.REVIEW_APPROVED);
            activity.setManagerReviewTime(activity.getManagerReviewTime() == null ? LocalDateTime.now() : activity.getManagerReviewTime());
        }
        if (!requireTeacherReview(policy)) {
            activity.setTeacherReviewStatus(Activity.REVIEW_APPROVED);
            activity.setTeacherReviewTime(activity.getTeacherReviewTime() == null ? LocalDateTime.now() : activity.getTeacherReviewTime());
        }
    }

    private boolean requireClubManagerReview(TenantSelfServiceAdmissionPolicyDTO policy) {
        return policy == null || !Boolean.FALSE.equals(policy.getRequireClubManagerReview());
    }

    private boolean requireTeacherReview(TenantSelfServiceAdmissionPolicyDTO policy) {
        return policy == null || !Boolean.FALSE.equals(policy.getRequireTeacherReview());
    }

    private boolean isEmailNoticeEnabled(Long tenantId) {
        TenantSelfServiceAdmissionPolicyDTO policy = loadTenantSelfServicePolicy(tenantId);
        return policy == null || !Boolean.FALSE.equals(policy.getEmailNoticeEnabled());
    }

    private boolean isSiteNoticeEnabled(Long tenantId) {
        TenantSelfServiceAdmissionPolicyDTO policy = loadTenantSelfServicePolicy(tenantId);
        return policy == null || !Boolean.FALSE.equals(policy.getSiteNoticeEnabled());
    }

    private void ensureActiveActivityQuota(Long tenantId, TenantSelfServiceAdmissionPolicyDTO policy) {
        Integer maxActiveActivities = policy == null ? null : policy.getMaxActiveActivities();
        if (maxActiveActivities == null || maxActiveActivities <= 0) {
            return;
        }
        Integer currentCount = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getTenantId, tenantId == null ? 1L : tenantId)
                .eq(Activity::getIsDeleted, 0)
                .ne(Activity::getActivityStatus, Activity.STATUS_ENDED));
        if (currentCount != null && currentCount >= maxActiveActivities) {
            throw new BadRequestException("当前租户活动数量已达到上限：" + maxActiveActivities);
        }
    }

    private TenantWorkflowConfigDTO loadActivityWorkflowConfig(Long tenantId) {
        try {
            Long resolvedTenantId = tenantId != null ? tenantId : (TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
            return userClient.queryCurrentTenantWorkflowConfig(resolvedTenantId);
        } catch (Exception e) {
            log.warn("load activity workflow config failed, tenantId={}, reason={}", tenantId, e.getMessage());
            return null;
        }
    }

    private WorkflowPolicyDTO loadActivityPublishPolicy(Long tenantId) {
        TenantWorkflowConfigDTO config = loadActivityWorkflowConfig(tenantId);
        return config == null ? null : config.getActivityPublish();
    }

    private void notifyActivityPendingReview(Activity activity, Long activityId, WorkflowPolicyDTO policy) {
        if (activity == null || activity.getManagerReviewerId() == null) {
            return;
        }
        if (!isEmailNoticeEnabled(activity.getTenantId())) {
            return;
        }
        sendActivityWorkflowEmail(
                activity,
                List.of(activity.getManagerReviewerId()),
                "活动待负责人审核通知",
                "有新的活动待负责人审核",
                activityApprovalLink(),
                "进入活动审核"
        );
    }

    private void notifyActivityNextWorkflowStep(Activity activity, Long activityId, WorkflowPolicyDTO policy) {
        if (activity == null) {
            return;
        }
        if (isFullyApproved(activity)) {
            notifyActivityPublishSuccess(activity);
            return;
        }
        if (Objects.equals(activity.getManagerReviewStatus(), Activity.REVIEW_APPROVED)
                && Objects.equals(activity.getTeacherReviewStatus(), Activity.REVIEW_PENDING)) {
            notifyActivityTeacherPendingReview(activity);
            return;
        }
        notifyActivityPendingReview(activity, activityId, policy);
    }

    private void notifyActivityTeacherPendingReview(Activity activity) {
        if (activity == null || activity.getTeacherReviewerId() == null) {
            return;
        }
        if (!isEmailNoticeEnabled(activity.getTenantId())) {
            return;
        }
        sendActivityWorkflowEmail(
                activity,
                List.of(activity.getTeacherReviewerId()),
                "活动待指导老师审核通知",
                "负责人已通过活动申请，请指导老师继续审核",
                activityApprovalLink(),
                "进入活动审核"
        );
    }

    private void notifyActivityPublishSuccess(Activity activity) {
        if (activity == null) {
            return;
        }
        WorkflowPolicyDTO policy = loadActivityPublishPolicy(activity.getTenantId());
        if (!shouldNotifyOnSuccess(policy)) {
            return;
        }
        try {
            sendActivityWorkflowEmail(
                    activity,
                    activitySuccessRecipientIds(activity),
                    "活动发布成功通知",
                    "活动审核已通过并发布",
                    activityViewLink(activity.getId()),
                    "查看活动详情"
            );
            if (isSiteNoticeEnabled(activity.getTenantId())) {
                NotificationInternalSaveDTO notification = new NotificationInternalSaveDTO();
                notification.setTenantId(activity.getTenantId());
                notification.setTitle("新活动已发布");
                notification.setContent("活动《" + safeText(activity.getActivityName()) + "》已发布，请及时查看。");
                notification.setType("activity");
                notification.setPriority(1);
                notification.setTargetType(0);
                notification.setSenderName("activity-service");
                notification.setMetadata(Map.of("activityId", activity.getId(), "source", "activity_publish"));
                userClient.saveNotificationInternal(notification);
            }
        } catch (Exception e) {
            log.warn("send activity publish success notification exception, activityId={}, reason={}",
                    activity.getId(), e.getMessage());
        }
    }

    private void sendActivityWorkflowEmail(Activity activity, List<Long> authUserIds, String subject, String headline, String actionUrl, String actionLabel) {
        if (activity == null || authUserIds == null || authUserIds.stream().filter(Objects::nonNull).findAny().isEmpty()) {
            return;
        }
        try {
            EmailCenterSendDTO dto = new EmailCenterSendDTO();
            dto.setTenantId(activity.getTenantId());
            dto.setAuthUserIds(authUserIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList()));
            dto.setHtml(true);
            dto.setSubject(subject);
            dto.setContent(buildActivityWorkflowEmailContent(activity, headline, actionUrl, actionLabel));
            Boolean sent = userClient.sendEmailCenterInternal(dto);
            if (!Boolean.TRUE.equals(sent)) {
                log.warn("send activity workflow email failed, activityId={}, tenantId={}, subject={}, recipients={}",
                        activity.getId(), activity.getTenantId(), subject, authUserIds);
            }
        } catch (Exception e) {
            log.warn("send activity workflow email exception, activityId={}, subject={}, reason={}",
                    activity.getId(), subject, e.getMessage());
        }
    }

    private List<Long> activitySuccessRecipientIds(Activity activity) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ids.add(activity.getManagerReviewerId());
        ids.add(activity.getCreator());
        ids.remove(null);
        return new ArrayList<>(ids);
    }

    private String buildActivityWorkflowEmailContent(Activity activity, String headline, String actionUrl, String actionLabel) {
        String detailUrl = activityViewLink(activity.getId());
        return "<p>" + safeText(headline) + "：</p>"
                + "<p>活动ID：" + activity.getId() + "</p>"
                + "<p>活动名称：" + safeText(activity.getActivityName()) + "</p>"
                + "<p>活动时间：" + safeText(activity.getStartTime() == null ? null : activity.getStartTime().toString())
                + " - " + safeText(activity.getEndTime() == null ? null : activity.getEndTime().toString()) + "</p>"
                + "<p><a href=\"" + actionUrl + "\" target=\"_blank\">" + safeText(actionLabel) + "</a></p>"
                + "<p><a href=\"" + detailUrl + "\" target=\"_blank\">查看前台活动详情</a></p>"
                + "<p style=\"color:#666;font-size:12px;\">" + actionUrl + "</p>";
    }

    private String activityApprovalLink() {
        return publicLink("/admin/activity/approval");
    }

    private String activityViewLink(Long activityId) {
        return publicLink("/activities/" + activityId);
    }

    private String publicLink(String path) {
        String baseUrl = resolvePublicBaseUrl();
        String normalizedPath = path == null ? "" : (path.startsWith("/") ? path : "/" + path);
        return StringUtils.hasText(baseUrl) ? trimTrailingSlash(baseUrl) + normalizedPath : normalizedPath;
    }

    private String resolvePublicBaseUrl() {
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl.trim();
        }
        HttpServletRequest request = WebUtils.getRequest();
        if (request == null) {
            return "";
        }
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin)) {
            return origin.trim();
        }
        String forwardedHost = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        if (StringUtils.hasText(forwardedHost)) {
            String proto = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
            return (StringUtils.hasText(proto) ? proto : "https") + "://" + forwardedHost;
        }
        String host = request.getHeader("Host");
        if (StringUtils.hasText(host)) {
            return request.getScheme() + "://" + host;
        }
        return "";
    }

    private String firstHeaderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int comma = value.indexOf(',');
        return (comma >= 0 ? value.substring(0, comma) : value).trim();
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }

    private List<Long> resolveWorkflowRoleIds(WorkflowPolicyDTO policy) {
        if (policy != null
                && "DESIGNATED".equalsIgnoreCase(policy.getAdvisorMode())
                && policy.getDesignatedAdvisorRoleIds() != null
                && !policy.getDesignatedAdvisorRoleIds().isEmpty()) {
            return policy.getDesignatedAdvisorRoleIds();
        }
        return DEFAULT_WORKFLOW_NOTIFY_ROLE_IDS;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private <T> T runWithoutTenantIsolation(Supplier<T> action) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setSuperAdmin(true);
            TenantContext.setTenantId(previousTenantId != null ? previousTenantId : 1L);
            return action.get();
        } finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(previousTenantId);
            }
            TenantContext.setSuperAdmin(previousSuperAdmin);
        }
    }

    private void normalizeActivityStatus(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            activity.setActivityStatus(Activity.STATUS_ENDED);
            return;
        }
        if (activity.getStartTime() != null && activity.getEndTime() != null
                && !now.isBefore(activity.getStartTime())
                && now.isBefore(activity.getEndTime())) {
            activity.setActivityStatus(Activity.STATUS_ONGOING);
            return;
        }
        if (activity.getRegistrationStartTime() != null && activity.getRegistrationEndTime() != null
                && !now.isBefore(activity.getRegistrationStartTime())
                && now.isBefore(activity.getRegistrationEndTime())) {
            activity.setActivityStatus(Activity.STATUS_REGISTERING);
            return;
        }
        if (activity.getActivityStatus() == null) {
            activity.setActivityStatus(Activity.STATUS_NOT_STARTED);
        }
    }
}
