package com.tianji.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.api.dto.user.EmailCenterSendDTO;
import com.tianji.api.dto.user.NotificationInternalSaveDTO;
import com.tianji.api.dto.user.TenantSelfServiceAdmissionPolicyDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.activity.domain.converter.CompetitionConverter;
import com.tianji.activity.domain.dto.CompetitionQueryDTO;
import com.tianji.activity.domain.po.Competition;
import com.tianji.activity.mapper.CompetitionMapper;
import com.tianji.activity.service.ICompetitionService;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.SearchIndexSyncUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.common.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.tianji.common.utils.TenantContext;

/**
 * 比赛服务实现类
 * 基于master分支的核心业务逻辑实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl extends ServiceImpl<CompetitionMapper, Competition>
        implements ICompetitionService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<Long> DEFAULT_WORKFLOW_NOTIFY_ROLE_IDS = Arrays.asList(1L, 3L, 5L, 7L, 8L);
    private static final String SEARCH_ENTITY_TYPE_COMPETITION = "COMPETITION";

    @Value("${rk.web.public-base-url:}")
    private String publicBaseUrl;
    private final UserClient userClient;
    private final SearchClient searchClient;

    /**
     * 分页查询比赛
     */
    @Override
    public IPage<Competition> pageCompetitions(Page<Competition> page, CompetitionQueryDTO queryDTO) {
        LambdaQueryWrapper<Competition> queryWrapper = new LambdaQueryWrapper<>();

        // 状态筛选
        if (StringUtils.hasText(queryDTO.getStatus())) {
            queryWrapper.eq(Competition::getStatus, queryDTO.getStatus());
        }

        // 比赛类型筛选
        if (StringUtils.hasText(queryDTO.getCompetitionType())) {
            queryWrapper.eq(Competition::getCompetitionType, queryDTO.getCompetitionType());
        }

        // 比赛级别筛选
        if (StringUtils.hasText(queryDTO.getLevel())) {
            queryWrapper.eq(Competition::getLevel, queryDTO.getLevel());
        }

        // 关键词搜索
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Competition::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(Competition::getOrganizer, queryDTO.getKeyword())
                    .or()
                    .like(Competition::getDescription, queryDTO.getKeyword())
            );
        }

        // 推荐筛选
        if (Boolean.TRUE.equals(queryDTO.getFeaturedOnly())) {
            queryWrapper.eq(Competition::getIsFeatured, true);
        }

        // 只查询已发布
        if (Boolean.TRUE.equals(queryDTO.getPublishedOnly())) {
            queryWrapper.eq(Competition::getIsPublished, true)
                    .eq(Competition::getManagerReviewStatus, Competition.REVIEW_APPROVED)
                    .eq(Competition::getTeacherReviewStatus, Competition.REVIEW_APPROVED);
        }

        // 报名时间筛选
        if (StringUtils.hasText(queryDTO.getRegistrationStartBegin())) {
            try {
                LocalDateTime start = LocalDateTime.parse(queryDTO.getRegistrationStartBegin(), DATE_FORMATTER);
                queryWrapper.ge(Competition::getRegistrationStart, start);
            } catch (Exception ignored) {}
        }

        if (StringUtils.hasText(queryDTO.getRegistrationEndEnd())) {
            try {
                LocalDateTime end = LocalDateTime.parse(queryDTO.getRegistrationEndEnd(), DATE_FORMATTER);
                queryWrapper.le(Competition::getRegistrationEnd, end);
            } catch (Exception ignored) {}
        }

        // 排序
        String sortBy = queryDTO.getSortBy();
        String sortOrder = queryDTO.getSortOrder();

        if ("create_time".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                queryWrapper.orderByAsc(Competition::getCreateTime);
            } else {
                queryWrapper.orderByDesc(Competition::getCreateTime);
            }
        } else if ("competition_start".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                queryWrapper.orderByAsc(Competition::getCompetitionStart);
            } else {
                queryWrapper.orderByDesc(Competition::getCompetitionStart);
            }
        } else if ("priority".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                queryWrapper.orderByAsc(Competition::getPriority);
            } else {
                queryWrapper.orderByDesc(Competition::getPriority);
            }
        } else if ("registration_count".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                queryWrapper.orderByAsc(Competition::getRegistrationCount);
            } else {
                queryWrapper.orderByDesc(Competition::getRegistrationCount);
            }
        } else if ("view_count".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                queryWrapper.orderByAsc(Competition::getViewCount);
            } else {
                queryWrapper.orderByDesc(Competition::getViewCount);
            }
        } else {
            queryWrapper.orderByDesc(Competition::getCreateTime);
        }

        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 获取所有比赛
     * 注意：@TableLogic 会自动过滤已删除记录
     */
    @Override
    public List<Competition> getAllCompetitions() {
        LambdaQueryWrapper<Competition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Competition::getCreateTime);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 获取已发布的比赛
     * 基于master分支的实现逻辑，支持排序
     * 注意：@TableLogic 会自动过滤已删除记录
     */
    @Override
    public List<Competition> getPublishedCompetitions(String sortBy, String sortOrder) {
        LambdaQueryWrapper<Competition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Competition::getIsPublished, true)
                .eq(Competition::getManagerReviewStatus, Competition.REVIEW_APPROVED)
                .eq(Competition::getTeacherReviewStatus, Competition.REVIEW_APPROVED);

        // 排序处理
        if ("create_time".equals(sortBy)) {
            if ("desc".equals(sortOrder)) {
                queryWrapper.orderByDesc(Competition::getCreateTime);
            } else {
                queryWrapper.orderByAsc(Competition::getCreateTime);
            }
        } else if ("competition_start".equals(sortBy)) {
            if ("desc".equals(sortOrder)) {
                queryWrapper.orderByDesc(Competition::getCompetitionStart);
            } else {
                queryWrapper.orderByAsc(Competition::getCompetitionStart);
            }
        } else if ("priority".equals(sortBy)) {
            if ("desc".equals(sortOrder)) {
                queryWrapper.orderByDesc(Competition::getPriority);
            } else {
                queryWrapper.orderByAsc(Competition::getPriority);
            }
        }

        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 获取推荐比赛
     * 注意：@TableLogic 会自动过滤已删除记录
     */
    @Override
    public List<Competition> getFeaturedCompetitions() {
        LambdaQueryWrapper<Competition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Competition::getIsFeatured, true)
                .eq(Competition::getIsPublished, true)
                .eq(Competition::getManagerReviewStatus, Competition.REVIEW_APPROVED)
                .eq(Competition::getTeacherReviewStatus, Competition.REVIEW_APPROVED)
                .orderByDesc(Competition::getPriority)
                .last("LIMIT 10");
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<Competition> getSharedCompetitions(Integer limit) {
        Long currentTenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        int finalLimit = (limit == null || limit <= 0) ? 10 : limit;
        return runWithoutTenantIsolation(() -> baseMapper.selectList(
                new LambdaQueryWrapper<Competition>()
                        .eq(Competition::getIsCrossTenant, true)
                        .eq(Competition::getIsPublished, true)
                        .eq(Competition::getManagerReviewStatus, Competition.REVIEW_APPROVED)
                        .eq(Competition::getTeacherReviewStatus, Competition.REVIEW_APPROVED)
                        .ne(Competition::getTenantId, currentTenantId)
                        .orderByDesc(Competition::getPriority)
                        .orderByDesc(Competition::getCreateTime)
                        .last("LIMIT " + finalLimit)
        ));
    }

    /**
     * 根据状态获取比赛
     * 注意：@TableLogic 会自动过滤已删除记录
     */
    @Override
    public List<Competition> getCompetitionsByStatus(String status) {
        LambdaQueryWrapper<Competition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Competition::getStatus, status)
                .eq(Competition::getIsPublished, true)
                .eq(Competition::getManagerReviewStatus, Competition.REVIEW_APPROVED)
                .eq(Competition::getTeacherReviewStatus, Competition.REVIEW_APPROVED)
                .orderByDesc(Competition::getCompetitionStart);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 根据ID获取比赛
     */
    @Override
    public Competition getCompetitionById(Long id) {
        Competition competition = baseMapper.selectById(id);
        if (competition == null) {
            competition = runWithoutTenantIsolation(() -> {
                Competition item = baseMapper.selectById(id);
                if (item != null && Boolean.TRUE.equals(item.getIsCrossTenant())) {
                    return item;
                }
                return null;
            });
        }
        return competition;
    }

    @Override
    public Page<Competition> getPendingReviewPage(Integer page, Integer size, String title) {
        return getPendingReviewPage(page, size, title, null);
    }

    @Override
    public Page<Competition> getPendingReviewPage(Integer page, Integer size, String title, Long roleId) {
        LambdaQueryWrapper<Competition> queryWrapper = new LambdaQueryWrapper<>();
        if (Objects.equals(roleId, 7L)) {
            queryWrapper.eq(Competition::getManagerReviewStatus, Competition.REVIEW_PENDING);
        } else if (Objects.equals(roleId, 8L)) {
            queryWrapper.eq(Competition::getManagerReviewStatus, Competition.REVIEW_APPROVED)
                    .eq(Competition::getTeacherReviewStatus, Competition.REVIEW_PENDING);
        } else {
            queryWrapper.and(wrapper -> wrapper
                    .eq(Competition::getManagerReviewStatus, Competition.REVIEW_PENDING)
                    .or(nested -> nested
                            .eq(Competition::getManagerReviewStatus, Competition.REVIEW_APPROVED)
                            .eq(Competition::getTeacherReviewStatus, Competition.REVIEW_PENDING)));
        }
        if (StringUtils.hasText(title)) {
            queryWrapper.like(Competition::getTitle, title);
        }
        queryWrapper.orderByDesc(Competition::getCreateTime);
        return baseMapper.selectPage(new Page<>(page, size), queryWrapper);
    }

    /**
     * 创建比赛
     * 基于master分支的实现逻辑，包含完整的验证和默认值设置
     */
    @Override
    @Transactional
    public Long createCompetition(Competition competition) {
        if (competition == null) {
            throw new IllegalArgumentException("competition is required");
        }
        Long tenantId = competition.getTenantId() != null ? competition.getTenantId() : (TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
        competition.setTenantId(tenantId);
        TenantSelfServiceAdmissionPolicyDTO selfServicePolicy = loadTenantSelfServicePolicy(tenantId);
        boolean requireManagerReview = requireClubManagerReview(selfServicePolicy);
        boolean requireTeacherReview = requireTeacherReview(selfServicePolicy);
        if ((requireManagerReview && competition.getManagerReviewerId() == null)
                || (requireTeacherReview && competition.getTeacherReviewerId() == null)) {
            throw new IllegalArgumentException("manager and teacher approvers are required");
        }
        ensureActiveActivityQuota(tenantId, selfServicePolicy);
        if (!StringUtils.hasText(competition.getCreatedBy()) && UserContext.getUser() != null) {
            competition.setCreatedBy(String.valueOf(UserContext.getUser()));
        }
        // 设置默认值
        if (competition.getIsPublished() == null) {
            competition.setIsPublished(false);
        }
        if (competition.getIsFeatured() == null) {
            competition.setIsFeatured(false);
        }
        if (competition.getIsCrossTenant() == null) {
            competition.setIsCrossTenant(false);
        }
        if (competition.getViewCount() == null) {
            competition.setViewCount(0);
        }
        if (competition.getRegistrationCount() == null) {
            competition.setRegistrationCount(0);
        }
        competition.setManagerReviewStatus(requireManagerReview ? Competition.REVIEW_PENDING : Competition.REVIEW_APPROVED);
        competition.setManagerReviewComment(null);
        competition.setManagerReviewTime(requireManagerReview ? null : LocalDateTime.now());
        competition.setTeacherReviewStatus(requireTeacherReview ? Competition.REVIEW_PENDING : Competition.REVIEW_APPROVED);
        competition.setTeacherReviewComment(null);
        competition.setTeacherReviewTime(requireTeacherReview ? null : LocalDateTime.now());
        if (competition.getPriority() == null) {
            competition.setPriority(0);
        }
        if (competition.getStatus() == null) {
            competition.setStatus("UPCOMING");
        }
        if (competition.getIsDeleted() == null) {
            competition.setIsDeleted(0);
        }
        if (competition.getCreateTime() == null) {
            competition.setCreateTime(LocalDateTime.now());
        }
        if (competition.getUpdateTime() == null) {
            competition.setUpdateTime(LocalDateTime.now());
        }
        boolean fullyApproved = !requireManagerReview && !requireTeacherReview;
        competition.setIsPublished(fullyApproved);
        competition.setStatus(fullyApproved ? "PUBLISHED" : "DRAFT");
        if (fullyApproved) {
            CompetitionConverter.updateStatusByTime(competition);
        }

        baseMapper.insert(competition);
        WorkflowPolicyDTO policy = loadCompetitionPublishPolicy(competition.getTenantId());
        if (shouldNotifyAdmins(policy)) {
            notifyCompetitionNextWorkflowStep(competition);
        }
        syncCompetitionSearchIndex(competition.getId());
        return competition.getId();
    }

    /**
     * 更新比赛
     */
    @Override
    @Transactional
    public int updateCompetition(Competition competition) {
        if (competition.getUpdateTime() == null) {
            competition.setUpdateTime(LocalDateTime.now());
        }
        applyTenantSelfServiceReviewPolicy(competition);
        gatePublishUntilApproved(competition);
        int result = baseMapper.updateById(competition);
        if (result > 0) {
            syncCompetitionSearchIndex(competition.getId());
        }
        return result;
    }

    /**
     * 删除比赛
     */
    @Override
    @Transactional
    public int deleteCompetition(Long id) {
        Competition existing = baseMapper.selectById(id);
        Long tenantId = existing == null ? null : existing.getTenantId();
        int result = baseMapper.deleteById(id);
        if (result > 0) {
            deleteCompetitionSearchIndex(id, tenantId);
        }
        return result;
    }

    @Override
    public boolean reviewCompetitionByManager(Long id, boolean approved, String reviewComment, Long reviewerId) {
        return reviewCompetitionByManager(id, approved, reviewComment, reviewerId, false);
    }

    @Override
    @Transactional
    public boolean reviewCompetitionByManager(Long id, boolean approved, String reviewComment, Long reviewerId, boolean allowProxyReview) {
        Competition competition = baseMapper.selectById(id);
        if (competition == null || !Objects.equals(competition.getManagerReviewStatus(), Competition.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, competition.getManagerReviewerId())) {
            return false;
        }
        competition.setManagerReviewStatus(approved ? Competition.REVIEW_APPROVED : Competition.REVIEW_REJECTED);
        competition.setManagerReviewComment(reviewComment);
        competition.setManagerReviewTime(LocalDateTime.now());
        if (!approved) {
            competition.setTeacherReviewStatus(Competition.REVIEW_PENDING);
            competition.setIsPublished(false);
            competition.setStatus("DRAFT");
        }
        applyTenantSelfServiceReviewPolicy(competition);
        gatePublishUntilApproved(competition);
        boolean updated = baseMapper.updateById(competition) > 0;
        if (updated && approved) {
            if (isFullyApproved(competition)) {
                if (!Boolean.TRUE.equals(competition.getIsPublished())) {
                    competition.setIsPublished(true);
                    if (competition.getStatus() == null || "DRAFT".equals(competition.getStatus())) {
                        competition.setStatus("PUBLISHED");
                    }
                    CompetitionConverter.updateStatusByTime(competition);
                    baseMapper.updateById(competition);
                }
                notifyCompetitionPublishSuccess(competition);
            } else {
                notifyCompetitionTeacherPendingReview(competition);
            }
        }
        if (updated) {
            syncCompetitionSearchIndex(competition.getId());
        }
        return updated;
    }

    @Override
    public boolean reviewCompetitionByTeacher(Long id, boolean approved, String reviewComment, Long reviewerId) {
        return reviewCompetitionByTeacher(id, approved, reviewComment, reviewerId, false);
    }

    @Override
    @Transactional
    public boolean reviewCompetitionByTeacher(Long id, boolean approved, String reviewComment, Long reviewerId, boolean allowProxyReview) {
        Competition competition = baseMapper.selectById(id);
        if (competition == null
                || !Objects.equals(competition.getManagerReviewStatus(), Competition.REVIEW_APPROVED)
                || !Objects.equals(competition.getTeacherReviewStatus(), Competition.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, competition.getTeacherReviewerId())) {
            return false;
        }
        competition.setTeacherReviewStatus(approved ? Competition.REVIEW_APPROVED : Competition.REVIEW_REJECTED);
        competition.setTeacherReviewComment(reviewComment);
        competition.setTeacherReviewTime(LocalDateTime.now());
        if (approved) {
            competition.setIsPublished(true);
            if (competition.getStatus() == null || "DRAFT".equals(competition.getStatus())) {
                competition.setStatus("PUBLISHED");
            }
            CompetitionConverter.updateStatusByTime(competition);
        } else {
            competition.setIsPublished(false);
            competition.setStatus("DRAFT");
        }
        boolean updated = baseMapper.updateById(competition) > 0;
        if (updated && approved) {
            notifyCompetitionPublishSuccess(competition);
        }
        if (updated) {
            syncCompetitionSearchIndex(competition.getId());
        }
        return updated;
    }

    /**
     * 搜索比赛
     * 基于master分支的实现逻辑
     * 注意：@TableLogic 会自动过滤已删除记录
     */
    @Override
    public List<Competition> searchCompetitions(String keyword) {
        LambdaQueryWrapper<Competition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .like(Competition::getTitle, keyword)
                .or()
                .like(Competition::getDescription, keyword)
                .or()
                .like(Competition::getContent, keyword)
        )
        .eq(Competition::getIsPublished, true)
        .eq(Competition::getManagerReviewStatus, Competition.REVIEW_APPROVED)
        .eq(Competition::getTeacherReviewStatus, Competition.REVIEW_APPROVED)
        .orderByDesc(Competition::getPriority)
        .last("LIMIT 20");
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 增加浏览量
     */
    @Override
    public int incrementViewCount(Long id) {
        Competition competition = baseMapper.selectById(id);
        if (competition != null) {
            competition.setViewCount(competition.getViewCount() + 1);
            return baseMapper.updateById(competition);
        }
        return 0;
    }

    /**
     * 增加报名人数
     */
    @Override
    public int incrementRegistrationCount(Long id) {
        Competition competition = baseMapper.selectById(id);
        if (competition != null) {
            competition.setRegistrationCount(competition.getRegistrationCount() + 1);
            return baseMapper.updateById(competition);
        }
        return 0;
    }

    private void gatePublishUntilApproved(Competition competition) {
        if (competition == null || !Boolean.TRUE.equals(competition.getIsPublished())) {
            return;
        }
        if (isFullyApproved(competition)) {
            return;
        }
        competition.setIsPublished(false);
        if (competition.getStatus() == null || "PUBLISHED".equals(competition.getStatus())) {
            competition.setStatus("DRAFT");
        }
    }

    private boolean isFullyApproved(Competition competition) {
        return competition != null
                && Objects.equals(competition.getManagerReviewStatus(), Competition.REVIEW_APPROVED)
                && Objects.equals(competition.getTeacherReviewStatus(), Competition.REVIEW_APPROVED);
    }

    private boolean isPublicCompetition(Competition competition) {
        return competition != null
                && isFullyApproved(competition)
                && Boolean.TRUE.equals(competition.getIsPublished())
                && !Integer.valueOf(1).equals(competition.getIsDeleted());
    }

    private void syncCompetitionSearchIndex(Long id) {
        if (id == null) {
            return;
        }
        SearchIndexSyncUtils.runAfterCommit(() -> {
            Competition competition = null;
            try {
                competition = baseMapper.selectById(id);
                if (competition == null || !isPublicCompetition(competition)) {
                    searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_COMPETITION, id,
                            competition == null ? TenantContext.getTenantId() : competition.getTenantId());
                    return;
                }
                searchClient.upsertGlobalDocument(buildCompetitionSearchDocument(competition));
            } catch (Exception e) {
                log.warn("sync competition global search index failed, competitionId={}, tenantId={}, reason={}",
                        id, competition == null ? null : competition.getTenantId(), e.getMessage());
            }
        });
    }

    private void deleteCompetitionSearchIndex(Long id, Long tenantId) {
        if (id == null) {
            return;
        }
        Long resolvedTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        SearchIndexSyncUtils.runAfterCommit(() -> {
            try {
                searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_COMPETITION, id, resolvedTenantId);
            } catch (Exception e) {
                log.warn("delete competition global search index failed, competitionId={}, tenantId={}, reason={}",
                        id, resolvedTenantId, e.getMessage());
            }
        });
    }

    @Override
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        LambdaQueryWrapper<Competition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Competition::getIsDeleted, 0)
                .eq(Competition::getIsPublished, true)
                .eq(Competition::getManagerReviewStatus, Competition.REVIEW_APPROVED)
                .eq(Competition::getTeacherReviewStatus, Competition.REVIEW_APPROVED)
                .orderByDesc(Competition::getUpdateTime);
        return list(queryWrapper).stream()
                .filter(this::isPublicCompetition)
                .map(this::buildCompetitionSearchDocument)
                .collect(Collectors.toList());
    }

    private GlobalSearchDocumentDTO buildCompetitionSearchDocument(Competition competition) {
        return new GlobalSearchDocumentDTO()
                .setEntityType(SEARCH_ENTITY_TYPE_COMPETITION)
                .setEntityId(competition.getId())
                .setTenantId(competition.getTenantId())
                .setTitle(competition.getTitle())
                .setSummary(firstNonBlank(competition.getSummary(), competition.getSubtitle(), competition.getOrganizer()))
                .setContent(firstNonBlank(competition.getContent(), competition.getDescription(), competition.getAwards()))
                .setTags(firstNonBlank(competition.getTags(), competition.getCompetitionType(), competition.getLevel()))
                .setRoute("/competition/" + competition.getId())
                .setCoverUrl(competition.getCoverImage())
                .setUpdatedAt(formatUpdatedAt(competition.getUpdateTime(), competition.getCreateTime()))
                .setVisible(isPublicCompetition(competition));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
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

    private void applyTeacherApprovalWorkflow(Competition competition) {
        Long roleId = currentRoleId();
        if (!Boolean.TRUE.equals(competition.getIsPublished())) {
            if (competition.getTeacherReviewStatus() == null) {
                competition.setTeacherReviewStatus(Competition.REVIEW_APPROVED);
            }
            return;
        }

        if (!requiresCompetitionApproval(competition.getTenantId())) {
            competition.setTeacherReviewStatus(Competition.REVIEW_APPROVED);
            competition.setTeacherReviewerId(UserContext.getUser());
            competition.setTeacherReviewTime(LocalDateTime.now());
            if (competition.getStatus() == null || "DRAFT".equals(competition.getStatus())) {
                competition.setStatus("PUBLISHED");
            }
            competition.setIsPublished(true);
            CompetitionConverter.updateStatusByTime(competition);
            return;
        }

        if (isAdminRole(roleId)) {
            competition.setTeacherReviewStatus(Competition.REVIEW_APPROVED);
            competition.setTeacherReviewerId(UserContext.getUser());
            competition.setTeacherReviewTime(LocalDateTime.now());
            if (competition.getStatus() == null || "DRAFT".equals(competition.getStatus())) {
                competition.setStatus("PUBLISHED");
            }
            CompetitionConverter.updateStatusByTime(competition);
            return;
        }

        competition.setTeacherReviewStatus(Competition.REVIEW_PENDING);
        competition.setTeacherReviewComment(null);
        competition.setTeacherReviewTime(null);
        competition.setTeacherReviewerId(null);
        competition.setIsPublished(false);
        competition.setStatus("DRAFT");
    }

    private Long currentRoleId() {
        String roleIdHeader = WebUtils.getHeader("X-Role-Id");
        if (!StringUtils.hasText(roleIdHeader)) {
            return null;
        }
        try {
            return Long.parseLong(roleIdHeader);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isAdminRole(Long roleId) {
        return Objects.equals(roleId, 1L)
                || Objects.equals(roleId, 3L)
                || Objects.equals(roleId, 5L);
    }

    private boolean requiresCompetitionApproval(Long tenantId) {
        WorkflowPolicyDTO policy = loadCompetitionPublishPolicy(tenantId);
        return shouldRequireCompetitionApproval(policy);
    }

    private boolean shouldRequireCompetitionApproval(WorkflowPolicyDTO policy) {
        if (policy == null) {
            return true;
        }
        return !Boolean.FALSE.equals(policy.getRequireApproval());
    }

    private boolean shouldNotifyOnSuccess(WorkflowPolicyDTO policy) {
        if (policy == null) {
            return true;
        }
        return !Boolean.FALSE.equals(policy.getNotifyOnSuccess());
    }

    private boolean shouldNotifyAdmins(WorkflowPolicyDTO policy) {
        if (policy == null) {
            return true;
        }
        return !Boolean.FALSE.equals(policy.getNotifyAdmins());
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

    private void applyTenantSelfServiceReviewPolicy(Competition competition) {
        if (competition == null) {
            return;
        }
        TenantSelfServiceAdmissionPolicyDTO policy = loadTenantSelfServicePolicy(competition.getTenantId());
        if (!requireClubManagerReview(policy)) {
            competition.setManagerReviewStatus(Competition.REVIEW_APPROVED);
            competition.setManagerReviewTime(competition.getManagerReviewTime() == null ? LocalDateTime.now() : competition.getManagerReviewTime());
        }
        if (!requireTeacherReview(policy)) {
            competition.setTeacherReviewStatus(Competition.REVIEW_APPROVED);
            competition.setTeacherReviewTime(competition.getTeacherReviewTime() == null ? LocalDateTime.now() : competition.getTeacherReviewTime());
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
        Integer currentCount = baseMapper.selectCount(new LambdaQueryWrapper<Competition>()
                .eq(Competition::getTenantId, tenantId == null ? 1L : tenantId)
                .eq(Competition::getIsDeleted, 0)
                .notIn(Competition::getStatus, List.of("COMPLETED", "CANCELLED", "ENDED")));
        if (currentCount != null && currentCount >= maxActiveActivities) {
            throw new BadRequestException("当前租户活动/比赛数量已达到上限：" + maxActiveActivities);
        }
    }

    private TenantWorkflowConfigDTO loadCompetitionWorkflowConfig(Long tenantId) {
        try {
            Long resolvedTenantId = tenantId != null ? tenantId : (TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
            return userClient.queryCurrentTenantWorkflowConfig(resolvedTenantId);
        } catch (Exception e) {
            log.warn("load competition workflow config failed, tenantId={}, reason={}", tenantId, e.getMessage());
            return null;
        }
    }

    private WorkflowPolicyDTO loadCompetitionPublishPolicy(Long tenantId) {
        TenantWorkflowConfigDTO config = loadCompetitionWorkflowConfig(tenantId);
        return config == null ? null : config.getCompetitionPublish();
    }

    private void notifyCompetitionPublishSuccess(Competition competition) {
        if (competition == null) {
            return;
        }
        WorkflowPolicyDTO policy = loadCompetitionPublishPolicy(competition.getTenantId());
        if (!shouldNotifyOnSuccess(policy)) {
            return;
        }
        try {
            sendCompetitionWorkflowEmail(
                    competition,
                    competitionSuccessRecipientIds(competition),
                    competitionManualSuccessEmails(competition),
                    "比赛发布成功通知",
                    "比赛审核已通过并发布",
                    competitionViewLink(competition.getId()),
                    "查看比赛详情"
            );
            if (isSiteNoticeEnabled(competition.getTenantId())) {
                NotificationInternalSaveDTO notification = new NotificationInternalSaveDTO();
                notification.setTenantId(competition.getTenantId());
                notification.setTitle("新比赛已发布");
                notification.setContent("比赛《" + safeText(competition.getTitle()) + "》已发布，请及时查看。");
                notification.setType("activity");
                notification.setPriority(1);
                notification.setTargetType(0);
                notification.setSenderName("competition-service");
                notification.setMetadata(Map.of("competitionId", competition.getId(), "source", "competition_publish"));
                userClient.saveNotificationInternal(notification);
            }
        } catch (Exception e) {
            log.warn("send competition publish success notification exception, competitionId={}, reason={}",
                    competition.getId(), e.getMessage());
        }
    }

    private void notifyCompetitionPendingReview(Competition competition) {
        if (competition == null || competition.getManagerReviewerId() == null) {
            return;
        }
        if (!isEmailNoticeEnabled(competition.getTenantId())) {
            return;
        }
        sendCompetitionWorkflowEmail(
                competition,
                List.of(competition.getManagerReviewerId()),
                null,
                "比赛待负责人审核通知",
                "有新的比赛待负责人审核",
                competitionApprovalLink(),
                "进入比赛审核"
        );
    }

    private void notifyCompetitionNextWorkflowStep(Competition competition) {
        if (competition == null) {
            return;
        }
        if (isFullyApproved(competition)) {
            notifyCompetitionPublishSuccess(competition);
            return;
        }
        if (Objects.equals(competition.getManagerReviewStatus(), Competition.REVIEW_APPROVED)
                && Objects.equals(competition.getTeacherReviewStatus(), Competition.REVIEW_PENDING)) {
            notifyCompetitionTeacherPendingReview(competition);
            return;
        }
        notifyCompetitionPendingReview(competition);
    }

    private void notifyCompetitionTeacherPendingReview(Competition competition) {
        if (competition == null || competition.getTeacherReviewerId() == null) {
            return;
        }
        if (!isEmailNoticeEnabled(competition.getTenantId())) {
            return;
        }
        sendCompetitionWorkflowEmail(
                competition,
                List.of(competition.getTeacherReviewerId()),
                null,
                "比赛待指导老师审核通知",
                "负责人已通过比赛申请，请指导老师继续审核",
                competitionApprovalLink(),
                "进入比赛审核"
        );
    }

    private void sendCompetitionWorkflowEmail(Competition competition, List<Long> authUserIds, List<String> manualEmails,
                                              String subject, String headline, String actionUrl, String actionLabel) {
        if (competition == null) {
            return;
        }
        List<Long> recipients = authUserIds == null ? List.of() : authUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        List<String> emails = manualEmails == null ? List.of() : manualEmails.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (recipients.isEmpty() && emails.isEmpty()) {
            return;
        }
        try {
            EmailCenterSendDTO dto = new EmailCenterSendDTO();
            dto.setTenantId(competition.getTenantId());
            dto.setAuthUserIds(recipients);
            dto.setManualEmails(emails);
            dto.setHtml(true);
            dto.setSubject(subject);
            dto.setContent(buildCompetitionWorkflowEmailContent(competition, headline, actionUrl, actionLabel));
            Boolean sent = userClient.sendEmailCenterInternal(dto);
            if (!Boolean.TRUE.equals(sent)) {
                log.warn("send competition workflow email failed, competitionId={}, tenantId={}, subject={}, users={}, emails={}",
                        competition.getId(), competition.getTenantId(), subject, recipients, emails);
            }
        } catch (Exception e) {
            log.warn("send competition workflow email exception, competitionId={}, subject={}, reason={}",
                    competition.getId(), subject, e.getMessage());
        }
    }

    private List<Long> competitionSuccessRecipientIds(Competition competition) {
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        ids.add(competition.getManagerReviewerId());
        Long creator = parseLong(competition.getCreatedBy());
        if (creator != null) {
            ids.add(creator);
        }
        ids.remove(null);
        return new java.util.ArrayList<>(ids);
    }

    private List<String> competitionManualSuccessEmails(Competition competition) {
        if (competition == null || !StringUtils.hasText(competition.getContactEmail())) {
            return List.of();
        }
        return List.of(competition.getContactEmail().trim());
    }

    private String buildCompetitionWorkflowEmailContent(Competition competition, String headline, String actionUrl, String actionLabel) {
        String detailUrl = competitionViewLink(competition.getId());
        return "<p>" + safeText(headline) + "：</p>"
                + "<p>比赛ID：" + competition.getId() + "</p>"
                + "<p>比赛标题：" + safeText(competition.getTitle()) + "</p>"
                + "<p>比赛时间：" + safeText(competition.getCompetitionStart() == null ? null : competition.getCompetitionStart().toString())
                + " - " + safeText(competition.getCompetitionEnd() == null ? null : competition.getCompetitionEnd().toString()) + "</p>"
                + "<p><a href=\"" + actionUrl + "\" target=\"_blank\">" + safeText(actionLabel) + "</a></p>"
                + "<p><a href=\"" + detailUrl + "\" target=\"_blank\">查看前台比赛详情</a></p>"
                + "<p style=\"color:#666;font-size:12px;\">" + actionUrl + "</p>";
    }

    private String competitionApprovalLink() {
        return publicLink("/admin/activity/competition-approval");
    }

    private String competitionViewLink(Long competitionId) {
        return publicLink("/competition/" + competitionId);
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

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (Exception ignored) {
            return null;
        }
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
}
