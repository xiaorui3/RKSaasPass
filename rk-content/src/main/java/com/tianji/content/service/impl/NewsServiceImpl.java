package com.tianji.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.CacheKeyBuilder;
import com.tianji.common.utils.SearchIndexSyncUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.content.domain.po.News;
import com.tianji.content.mapper.NewsMapper;
import com.tianji.content.service.INewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

/**
 * 新闻服务实现类
 * 基于master分支的核心业务逻辑实现
 * 
 * 缓存策略：
 * - 热门新闻缓存：30分钟
 * - 新闻详情缓存：1小时
 * - 新闻列表缓存：10分钟
 * - 所有缓存Key包含tenant_id实现多租户隔离
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements INewsService {

    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;
    private final SearchClient searchClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存过期时间配置（单位：分钟）
    private static final long CACHE_EXPIRE_DETAIL = 60;      // 详情缓存1小时
    private static final long CACHE_EXPIRE_LIST = 10;        // 列表缓存10分钟
    private static final long CACHE_EXPIRE_HOT = 30;         // 热门缓存30分钟
    private static final String SEARCH_ENTITY_TYPE_NEWS = "NEWS";
    private static final List<Long> DEFAULT_WORKFLOW_NOTIFY_ROLE_IDS = Arrays.asList(1L, 3L, 5L, 7L, 8L);

    /**
     * 查询新闻详情（带缓存）
     */
    @Override
    public News selectNewsById(Long id) {
        // 尝试从缓存获取
        String cacheKey = CacheKeyBuilder.buildDetailKey(CacheKeyBuilder.MODULE_NEWS, id);
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取新闻详情: {}", id);
                return objectMapper.readValue(cachedJson, News.class);
            }
        } catch (Exception e) {
            log.warn("读取新闻详情缓存失败: {}", e.getMessage());
        }

        // 从数据库查询
        News news = baseMapper.selectById(id);
        if (news == null) {
            news = getSharedNewsById(id);
        }
        
        // 写入缓存
        if (news != null) {
            try {
                String json = objectMapper.writeValueAsString(news);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_DETAIL, TimeUnit.MINUTES);
                log.debug("新闻详情已缓存: {}", id);
            } catch (Exception e) {
                log.warn("写入新闻详情缓存失败: {}", e.getMessage());
            }
        }
        
        return news;
    }

    /**
     * 查询新闻列表（带缓存）
     */
    @Override
    public List<News> selectNewsList(News news) {
        // 列表查询条件复杂，不缓存条件查询结果
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(News::getIsDeleted, 0);
        if (news.getTenantId() != null) {
            queryWrapper.eq(News::getTenantId, news.getTenantId());
        }
        if (news.getCategory() != null) {
            queryWrapper.eq(News::getCategory, news.getCategory());
        }
        if (news.getIsPublished() != null) {
            queryWrapper.eq(News::getIsPublished, news.getIsPublished());
            if (news.getIsPublished() == 1) {
                queryWrapper.eq(News::getApprovalStatus, 2)
                        .eq(News::getManagerReviewStatus, News.REVIEW_APPROVED)
                        .eq(News::getTeacherReviewStatus, News.REVIEW_APPROVED);
            }
        }
        queryWrapper.orderByDesc(News::getPublishTime);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 查询最新新闻（带缓存）
     */
    @Override
    public List<News> selectLatestNews(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_NEWS, "latest", "limit" + limit);
        
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取最新新闻列表");
                return objectMapper.readValue(cachedJson, new TypeReference<List<News>>() {});
            }
        } catch (Exception e) {
            log.warn("读取最新新闻缓存失败: {}", e.getMessage());
        }

        // 从数据库查询
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(News::getIsPublished, 1)
                .eq(News::getApprovalStatus, 2)
                .eq(News::getManagerReviewStatus, News.REVIEW_APPROVED)
                .eq(News::getTeacherReviewStatus, News.REVIEW_APPROVED)
                .eq(News::getIsDeleted, 0)
                .orderByDesc(News::getPublishTime)
                .last("LIMIT " + limit);
        List<News> newsList = baseMapper.selectList(queryWrapper);
        
        // 写入缓存
        if (newsList != null && !newsList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(newsList);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_LIST, TimeUnit.MINUTES);
                log.debug("最新新闻列表已缓存");
            } catch (Exception e) {
                log.warn("写入最新新闻缓存失败: {}", e.getMessage());
            }
        }
        
        return newsList;
    }

    /**
     * 查询置顶新闻（带缓存）
     */
    @Override
    public List<News> selectTopNews(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 5;
        }
        
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_NEWS, "top", "limit" + limit);
        
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取置顶新闻列表");
                return objectMapper.readValue(cachedJson, new TypeReference<List<News>>() {});
            }
        } catch (Exception e) {
            log.warn("读取置顶新闻缓存失败: {}", e.getMessage());
        }

        // 从数据库查询
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(News::getIsFeatured, 1)
                .eq(News::getIsPublished, 1)
                .eq(News::getApprovalStatus, 2)
                .eq(News::getManagerReviewStatus, News.REVIEW_APPROVED)
                .eq(News::getTeacherReviewStatus, News.REVIEW_APPROVED)
                .eq(News::getIsDeleted, 0)
                .orderByDesc(News::getPublishTime)
                .last("LIMIT " + limit);
        List<News> newsList = baseMapper.selectList(queryWrapper);
        
        // 写入缓存
        if (newsList != null && !newsList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(newsList);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_HOT, TimeUnit.MINUTES);
                log.debug("置顶新闻列表已缓存");
            } catch (Exception e) {
                log.warn("写入置顶新闻缓存失败: {}", e.getMessage());
            }
        }
        
        return newsList;
    }

    @Override
    public List<News> selectSharedNews(Integer limit) {
        Long currentTenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L;
        int finalLimit = (limit == null || limit <= 0) ? 10 : limit;
        return runWithoutTenantIsolation(() -> baseMapper.selectList(
                new LambdaQueryWrapper<News>()
                        .eq(News::getIsCrossTenant, 1)
                        .eq(News::getIsPublished, 1)
                        .eq(News::getApprovalStatus, 2)
                        .eq(News::getManagerReviewStatus, News.REVIEW_APPROVED)
                        .eq(News::getTeacherReviewStatus, News.REVIEW_APPROVED)
                        .eq(News::getIsDeleted, 0)
                        .ne(News::getTenantId, currentTenantId)
                        .orderByDesc(News::getPublishTime)
                        .last("LIMIT " + finalLimit)
        ));
    }

    /**
     * 根据分类查询新闻（带缓存）
     */
    @Override
    public List<News> selectNewsByCategory(String category, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_NEWS, "category_" + category, "limit" + limit);
        
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取分类新闻列表: {}", category);
                return objectMapper.readValue(cachedJson, new TypeReference<List<News>>() {});
            }
        } catch (Exception e) {
            log.warn("读取分类新闻缓存失败: {}", e.getMessage());
        }

        // 从数据库查询
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(News::getCategory, category)
                .eq(News::getIsPublished, 1)
                .eq(News::getApprovalStatus, 2)
                .eq(News::getManagerReviewStatus, News.REVIEW_APPROVED)
                .eq(News::getTeacherReviewStatus, News.REVIEW_APPROVED)
                .eq(News::getIsDeleted, 0)
                .orderByDesc(News::getPublishTime)
                .last("LIMIT " + limit);
        List<News> newsList = baseMapper.selectList(queryWrapper);
        
        // 写入缓存
        if (newsList != null && !newsList.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(newsList);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_LIST, TimeUnit.MINUTES);
                log.debug("分类新闻列表已缓存: {}", category);
            } catch (Exception e) {
                log.warn("写入分类新闻缓存失败: {}", e.getMessage());
            }
        }
        
        return newsList;
    }

    /**
     * 搜索新闻
     * 搜索结果不缓存，每次实时查询
     */
    @Override
    public List<News> searchNews(String keyword, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .like(News::getTitle, keyword)
                .or()
                .like(News::getSummary, keyword)
                .or()
                .like(News::getContent, keyword)
        )
        .eq(News::getIsPublished, 1)
        .eq(News::getApprovalStatus, 2)
        .eq(News::getManagerReviewStatus, News.REVIEW_APPROVED)
        .eq(News::getTeacherReviewStatus, News.REVIEW_APPROVED)
        .eq(News::getIsDeleted, 0)
        .orderByDesc(News::getPublishTime)
        .last("LIMIT " + limit);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 新增新闻（清除相关缓存）
     */
    @Override
    public int insertNews(News news) {
        if (news == null) {
            throw new IllegalArgumentException("news is required");
        }
        Long tenantId = news.getTenantId() != null ? news.getTenantId() : (TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
        news.setTenantId(tenantId);
        TenantSelfServiceAdmissionPolicyDTO selfServicePolicy = loadTenantSelfServicePolicy(tenantId);
        boolean requireManagerReview = requireClubManagerReview(selfServicePolicy);
        boolean requireTeacherReview = requireTeacherReview(selfServicePolicy);
        if ((requireManagerReview && news.getManagerReviewerId() == null)
                || (requireTeacherReview && news.getTeacherReviewerId() == null)) {
            throw new IllegalArgumentException("manager and teacher approvers are required");
        }
        ensureMonthlyNewsQuota(tenantId, selfServicePolicy);
        // 设置默认值
        if (news.getIsPublished() == null) {
            news.setIsPublished(0);
        }
        if (news.getIsFeatured() == null) {
            news.setIsFeatured(0);
        }
        if (news.getViewCount() == null) {
            news.setViewCount(0);
        }
        if (news.getIsCrossTenant() == null) {
            news.setIsCrossTenant(0);
        }
        if (news.getAuthor() == null || news.getAuthor().trim().isEmpty()) {
            news.setAuthor("软开社团");
        }
        if (news.getCategory() == null || news.getCategory().trim().isEmpty()) {
            news.setCategory("社团新闻");
        }
        if (news.getPublishTime() == null && news.getIsPublished() == 1) {
            news.setPublishTime(java.time.LocalDateTime.now());
        }
        if (news.getIsDeleted() == null) {
            news.setIsDeleted(0);
        }
        if (news.getCreateTime() == null) {
            news.setCreateTime(LocalDateTime.now());
        }
        if (news.getUpdateTime() == null) {
            news.setUpdateTime(LocalDateTime.now());
        }
        if (news.getApprovalStatus() == null) {
            news.setApprovalStatus(0); // 默认草稿状态
        }

        boolean fullyApproved = !requireManagerReview && !requireTeacherReview;
        news.setIsPublished(fullyApproved ? 1 : 0);
        news.setApprovalStatus(fullyApproved ? 2 : 1);
        news.setPublishTime(fullyApproved ? LocalDateTime.now() : null);
        news.setManagerReviewStatus(requireManagerReview ? News.REVIEW_PENDING : News.REVIEW_APPROVED);
        news.setManagerReviewComment(null);
        news.setManagerReviewTime(requireManagerReview ? null : LocalDateTime.now());
        news.setTeacherReviewStatus(requireTeacherReview ? News.REVIEW_PENDING : News.REVIEW_APPROVED);
        news.setTeacherReviewComment(null);
        news.setTeacherReviewTime(requireTeacherReview ? null : LocalDateTime.now());

        int result = baseMapper.insert(news);
        
        // 清除列表相关缓存
        if (result > 0) {
            if (fullyApproved) {
                notifyNewsPublishSuccess(news);
            }
            clearListCache();
            syncNewsSearchIndex(news);
        }
        
        return result;
    }

    /**
     * 修改新闻（清除相关缓存）
     */
    @Override
    public int updateNews(News news) {
        if (news == null || news.getId() == null) {
            return 0;
        }

        News existing = baseMapper.selectById(news.getId());
        if (existing == null) {
            return 0;
        }

        mergeUpdatableFields(existing, news);
        existing.setUpdateTime(LocalDateTime.now());
        applyTenantSelfServiceReviewPolicy(existing);
        if (news.getIsPublished() != null && news.getIsPublished() == 1 && !isFullyApproved(existing)) {
            existing.setIsPublished(0);
            existing.setApprovalStatus(1);
            existing.setPublishTime(null);
        } else if (news.getIsPublished() != null
                && news.getIsPublished() == 1
                && existing.getPublishTime() == null) {
            existing.setPublishTime(LocalDateTime.now());
        }

        int result = baseMapper.updateById(existing);
        
        // 清除详情缓存和列表缓存
        if (result > 0) {
            clearDetailCache(news.getId());
            clearListCache();
            syncNewsSearchIndex(existing);
        }
        
        return result;
    }

    private void mergeUpdatableFields(News target, News source) {
        if (source.getTitle() != null) {
            target.setTitle(source.getTitle());
        }
        if (source.getSummary() != null) {
            target.setSummary(source.getSummary());
        }
        if (source.getContent() != null) {
            target.setContent(source.getContent());
        }
        if (source.getCoverImage() != null) {
            target.setCoverImage(source.getCoverImage());
        }
        if (source.getVideoUrl() != null) {
            target.setVideoUrl(source.getVideoUrl());
        }
        if (source.getAttachmentUrl() != null) {
            target.setAttachmentUrl(source.getAttachmentUrl());
        }
        if (source.getAuthor() != null) {
            target.setAuthor(source.getAuthor());
        }
        if (source.getCategory() != null) {
            target.setCategory(source.getCategory());
        }
        if (source.getTags() != null) {
            target.setTags(source.getTags());
        }
        if (source.getIsPublished() != null) {
            target.setIsPublished(source.getIsPublished());
        }
        if (source.getIsFeatured() != null) {
            target.setIsFeatured(source.getIsFeatured());
        }
        if (source.getIsCrossTenant() != null) {
            target.setIsCrossTenant(source.getIsCrossTenant());
        }
        if (source.getApprovalStatus() != null) {
            target.setApprovalStatus(source.getApprovalStatus());
        }
        if (source.getManagerReviewerId() != null) {
            target.setManagerReviewerId(source.getManagerReviewerId());
        }
        if (source.getTeacherReviewerId() != null) {
            target.setTeacherReviewerId(source.getTeacherReviewerId());
        }
    }

    private boolean isFullyApproved(News news) {
        return news != null
                && Objects.equals(news.getManagerReviewStatus(), News.REVIEW_APPROVED)
                && Objects.equals(news.getTeacherReviewStatus(), News.REVIEW_APPROVED)
                && Integer.valueOf(2).equals(news.getApprovalStatus());
    }

    private boolean isPublicNews(News news) {
        return isFullyApproved(news)
                && Integer.valueOf(1).equals(news.getIsPublished())
                && Integer.valueOf(0).equals(news.getIsDeleted());
    }

    private void syncNewsSearchIndex(News news) {
        if (news == null || news.getId() == null) {
            return;
        }
        try {
            SearchIndexSyncUtils.runAfterCommit(() -> {
                try {
                    if (!isPublicNews(news)) {
                        searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_NEWS, news.getId(), news.getTenantId());
                        return;
                    }
                    searchClient.upsertGlobalDocument(buildNewsSearchDocument(news));
                } catch (Exception e) {
                    log.warn("sync news global search index failed, newsId={}, tenantId={}, reason={}",
                            news.getId(), news.getTenantId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("schedule news global search index sync failed, newsId={}, tenantId={}, reason={}",
                    news.getId(), news.getTenantId(), e.getMessage());
        }
    }

    private void deleteNewsSearchIndex(Long id, Long tenantId) {
        if (id == null) {
            return;
        }
        Long resolvedTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        try {
            SearchIndexSyncUtils.runAfterCommit(() -> {
                try {
                    searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_NEWS, id, resolvedTenantId);
                } catch (Exception e) {
                    log.warn("delete news global search index failed, newsId={}, tenantId={}, reason={}",
                            id, resolvedTenantId, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("schedule news global search index delete failed, newsId={}, tenantId={}, reason={}",
                    id, resolvedTenantId, e.getMessage());
        }
    }

    @Override
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(News::getIsDeleted, 0)
                .eq(News::getIsPublished, 1)
                .eq(News::getApprovalStatus, 2)
                .eq(News::getManagerReviewStatus, News.REVIEW_APPROVED)
                .eq(News::getTeacherReviewStatus, News.REVIEW_APPROVED)
                .orderByDesc(News::getUpdateTime);
        return baseMapper.selectList(queryWrapper).stream()
                .filter(this::isPublicNews)
                .map(this::buildNewsSearchDocument)
                .collect(java.util.stream.Collectors.toList());
    }

    private GlobalSearchDocumentDTO buildNewsSearchDocument(News news) {
        return new GlobalSearchDocumentDTO()
                .setEntityType(SEARCH_ENTITY_TYPE_NEWS)
                .setEntityId(news.getId())
                .setTenantId(news.getTenantId())
                .setTitle(news.getTitle())
                .setSummary(news.getSummary())
                .setContent(news.getContent())
                .setTags(news.getTags())
                .setRoute("/news/" + news.getId())
                .setCoverUrl(news.getCoverImage())
                .setUpdatedAt(formatUpdatedAt(news.getUpdateTime(), news.getPublishTime(), news.getCreateTime()))
                .setVisible(isPublicNews(news));
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

    /**
     * 删除新闻（清除相关缓存）
     */
    @Override
    public int deleteNewsById(Long id) {
        News existing = baseMapper.selectById(id);
        Long tenantId = existing == null ? null : existing.getTenantId();
        int result = baseMapper.deleteById(id);
        
        // 清除详情缓存和列表缓存
        if (result > 0) {
            clearDetailCache(id);
            clearListCache();
            deleteNewsSearchIndex(id, tenantId);
        }
        
        return result;
    }

    /**
     * 批量删除新闻（清除相关缓存）
     */
    @Override
    public int deleteNewsByIds(List<Long> ids) {
        Map<Long, Long> tenantIdsByNewsId = new HashMap<>();
        if (ids != null) {
            for (Long id : ids) {
                News existing = baseMapper.selectById(id);
                if (existing != null && existing.getId() != null) {
                    tenantIdsByNewsId.put(existing.getId(), existing.getTenantId());
                }
            }
        }
        int result = baseMapper.deleteBatchIds(ids);
        
        // 清除详情缓存和列表缓存
        if (result > 0) {
            for (Long id : ids) {
                clearDetailCache(id);
                deleteNewsSearchIndex(id, tenantIdsByNewsId.get(id));
            }
            clearListCache();
        }
        
        return result;
    }

    /**
     * 增加浏览量
     * 使用Redis计数器，定时同步到数据库
     */
    @Override
    public int incrementViewCount(Long id) {
        // 使用Redis INCR操作增加计数
        String countKey = CacheKeyBuilder.buildCountKey(CacheKeyBuilder.MODULE_NEWS, "view", id);
        Long newCount = redisTemplate.opsForValue().increment(countKey);
        
        // 设置计数器过期时间（7天）
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(countKey, 7, TimeUnit.DAYS);
        }
        
        // 每100次浏览更新一次数据库
        if (newCount != null && newCount % 100 == 0) {
            News news = baseMapper.selectById(id);
            if (news != null) {
                news.setViewCount(news.getViewCount() + 100);
                baseMapper.updateById(news);
                // 重置计数器
                redisTemplate.opsForValue().set(countKey, "0", 7, TimeUnit.DAYS);
            }
        }
        
        return 1;
    }

    /**
     * 查询新闻总数（带缓存）
     */
    @Override
    public int countNews() {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_NEWS, "count", "total");
        
        // 尝试从缓存获取
        try {
            String cachedCount = redisTemplate.opsForValue().get(cacheKey);
            if (cachedCount != null) {
                return Integer.parseInt(cachedCount);
            }
        } catch (Exception e) {
            log.warn("读取新闻总数缓存失败: {}", e.getMessage());
        }

        // 从数据库查询
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(News::getIsPublished, 1)
                .eq(News::getApprovalStatus, 2)
                .eq(News::getManagerReviewStatus, News.REVIEW_APPROVED)
                .eq(News::getTeacherReviewStatus, News.REVIEW_APPROVED)
                .eq(News::getIsDeleted, 0);
        int count = Math.toIntExact(baseMapper.selectCount(queryWrapper));
        
        // 写入缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(count), CACHE_EXPIRE_LIST, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入新闻总数缓存失败: {}", e.getMessage());
        }
        
        return count;
    }

    /**
     * 清除详情缓存
     */
    private void clearDetailCache(Long id) {
        String cacheKey = CacheKeyBuilder.buildDetailKey(CacheKeyBuilder.MODULE_NEWS, id);
        redisTemplate.delete(cacheKey);
        log.debug("已清除新闻详情缓存: {}", id);
    }

    /**
     * 清除列表相关缓存
     */
    private void clearListCache() {
        try {
            // 获取当前租户的所有新闻相关缓存Key
            Long tenantId = com.tianji.common.utils.TenantContext.getTenantId();
            if (tenantId == null) {
                tenantId = 0L;
            }
            String pattern = CacheKeyBuilder.MODULE_NEWS + ":" + tenantId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                // 只删除列表和计数相关的缓存，保留详情缓存
                keys.removeIf(key -> key.contains(":detail:"));
                if (!keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.debug("已清除新闻列表缓存，共{}个key", keys.size());
                }
            }
        } catch (Exception e) {
            log.warn("清除新闻列表缓存失败: {}", e.getMessage());
        }
    }

    private News getSharedNewsById(Long id) {
        return runWithoutTenantIsolation(() -> {
            News news = baseMapper.selectById(id);
            if (news != null
                    && Integer.valueOf(1).equals(news.getIsCrossTenant())
                    && Integer.valueOf(1).equals(news.getIsPublished())
                    && Integer.valueOf(2).equals(news.getApprovalStatus())
                    && Objects.equals(news.getManagerReviewStatus(), News.REVIEW_APPROVED)
                    && Objects.equals(news.getTeacherReviewStatus(), News.REVIEW_APPROVED)) {
                return news;
            }
            return null;
        });
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

    /**
     * 查询待审核新闻列表
     */
    private TenantWorkflowConfigDTO loadNewsWorkflowConfig(Long tenantId) {
        try {
            Long resolvedTenantId = tenantId != null
                    ? tenantId
                    : (TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
            return userClient.queryCurrentTenantWorkflowConfig(resolvedTenantId);
        } catch (Exception e) {
            log.warn("load news workflow config failed, tenantId={}, reason={}", tenantId, e.getMessage());
            return null;
        }
    }

    private WorkflowPolicyDTO loadNewsPublishPolicy(Long tenantId) {
        TenantWorkflowConfigDTO config = loadNewsWorkflowConfig(tenantId);
        return config == null ? null : config.getNewsPublish();
    }

    private boolean shouldNotifyOnSuccess(WorkflowPolicyDTO policy) {
        if (policy == null) {
            return true;
        }
        return !Boolean.FALSE.equals(policy.getNotifyOnSuccess());
    }

    private TenantSelfServiceAdmissionPolicyDTO loadTenantSelfServicePolicy(Long tenantId) {
        try {
            Long resolvedTenantId = tenantId != null
                    ? tenantId
                    : (TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
            return userClient.queryTenantSelfServiceAdmissionPolicy(resolvedTenantId);
        } catch (Exception e) {
            log.warn("load tenant self service policy failed, tenantId={}, reason={}", tenantId, e.getMessage());
            return null;
        }
    }

    private void applyTenantSelfServiceReviewPolicy(News news) {
        if (news == null) {
            return;
        }
        TenantSelfServiceAdmissionPolicyDTO policy = loadTenantSelfServicePolicy(news.getTenantId());
        if (!requireClubManagerReview(policy)) {
            news.setManagerReviewStatus(News.REVIEW_APPROVED);
            news.setManagerReviewTime(news.getManagerReviewTime() == null ? LocalDateTime.now() : news.getManagerReviewTime());
        }
        if (!requireTeacherReview(policy)) {
            news.setTeacherReviewStatus(News.REVIEW_APPROVED);
            news.setTeacherReviewTime(news.getTeacherReviewTime() == null ? LocalDateTime.now() : news.getTeacherReviewTime());
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

    private void ensureMonthlyNewsQuota(Long tenantId, TenantSelfServiceAdmissionPolicyDTO policy) {
        Integer maxMonthlyNews = policy == null ? null : policy.getMaxMonthlyNews();
        if (maxMonthlyNews == null || maxMonthlyNews <= 0) {
            return;
        }
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        Integer currentCount = baseMapper.selectCount(new LambdaQueryWrapper<News>()
                .eq(News::getTenantId, tenantId == null ? 1L : tenantId)
                .eq(News::getIsDeleted, 0)
                .ge(News::getCreateTime, monthStart)
                .lt(News::getCreateTime, nextMonthStart));
        if (currentCount != null && currentCount >= maxMonthlyNews) {
            throw new BadRequestException("当前租户本月新闻数量已达到上限：" + maxMonthlyNews);
        }
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

    private void notifyNewsPublishSuccess(News news) {
        if (news == null || news.getId() == null) {
            return;
        }
        if (!Integer.valueOf(2).equals(news.getApprovalStatus()) || !Integer.valueOf(1).equals(news.getIsPublished())) {
            return;
        }
        WorkflowPolicyDTO policy = loadNewsPublishPolicy(news.getTenantId());
        if (!shouldNotifyOnSuccess(policy)) {
            return;
        }
        try {
            if (isEmailNoticeEnabled(news.getTenantId())) {
                EmailCenterSendDTO dto = new EmailCenterSendDTO();
                dto.setRoleIds(resolveWorkflowRoleIds(policy));
                dto.setHtml(true);
                dto.setSubject("新闻发布成功通知");
                dto.setContent("<p>新闻审核已通过并发布：</p>"
                        + "<p>新闻ID：" + news.getId() + "</p>"
                        + "<p>新闻标题：" + safeText(news.getTitle()) + "</p>");
                Boolean sent = userClient.sendEmailCenterInternal(dto);
                if (!Boolean.TRUE.equals(sent)) {
                    log.warn("send news publish success notification failed, newsId={}, tenantId={}",
                            news.getId(), news.getTenantId());
                }
            }
            if (isSiteNoticeEnabled(news.getTenantId())) {
                NotificationInternalSaveDTO notification = new NotificationInternalSaveDTO();
                notification.setTenantId(news.getTenantId());
                notification.setTitle("新新闻已发布");
                notification.setContent("新闻《" + safeText(news.getTitle()) + "》已发布，请及时查看。");
                notification.setType("message");
                notification.setPriority(1);
                notification.setTargetType(0);
                notification.setSenderName("news-service");
                notification.setMetadata(Map.of("newsId", news.getId(), "source", "news_publish"));
                userClient.saveNotificationInternal(notification);
            }
        } catch (Exception e) {
            log.warn("send news publish success notification exception, newsId={}, reason={}",
                    news.getId(), e.getMessage());
        }
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    @Override
    public List<News> selectPendingNews() {
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(News::getApprovalStatus, 1) // 1-待审核
                .eq(News::getIsDeleted, 0)
                .orderByDesc(News::getCreateTime);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 审核通过新闻
     */
    @Override
    public boolean approveNewsByManager(Long id, String remark, Long reviewerId, boolean allowProxyReview) {
        return reviewNewsByManager(id, true, remark, reviewerId, allowProxyReview);
    }

    @Override
    public boolean reviewNewsByManager(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview) {
        News news = baseMapper.selectById(id);
        if (news == null || !Objects.equals(news.getManagerReviewStatus(), News.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, news.getManagerReviewerId())) {
            return false;
        }
        news.setManagerReviewStatus(approved ? News.REVIEW_APPROVED : News.REVIEW_REJECTED);
        news.setManagerReviewComment(remark);
        news.setManagerReviewTime(LocalDateTime.now());
        news.setApprovalStatus(approved ? 1 : 3);
        news.setIsPublished(0);
        if (!approved) {
            Long resolvedReviewerId = reviewerId != null ? reviewerId : com.tianji.common.utils.UserContext.getUser();
            news.setTeacherReviewStatus(News.REVIEW_PENDING);
            news.setRejectReason(remark);
            news.setApprover(resolvedReviewerId != null ? "用户-" + resolvedReviewerId : "系统");
            news.setApprovalTime(LocalDateTime.now());
            news.setPublishTime(null);
        } else {
            applyTenantSelfServiceReviewPolicy(news);
            if (isFullyApproved(news)) {
                Long resolvedReviewerId = reviewerId != null ? reviewerId : com.tianji.common.utils.UserContext.getUser();
                news.setApprovalStatus(2);
                news.setApprover(resolvedReviewerId != null ? "用户-" + resolvedReviewerId : "系统");
                news.setApprovalTime(LocalDateTime.now());
                news.setIsPublished(1);
                if (news.getPublishTime() == null) {
                    news.setPublishTime(LocalDateTime.now());
                }
            }
        }
        int result = baseMapper.updateById(news);
        if (result > 0) {
            if (approved && isFullyApproved(news)) {
                notifyNewsPublishSuccess(news);
            }
            clearDetailCache(id);
            clearListCache();
            syncNewsSearchIndex(news);
        }
        return result > 0;
    }

    @Override
    public boolean approveNewsByTeacher(Long id, String remark, Long reviewerId, boolean allowProxyReview) {
        return reviewNewsByTeacher(id, true, remark, reviewerId, allowProxyReview);
    }

    @Override
    public boolean reviewNewsByTeacher(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview) {
        News news = baseMapper.selectById(id);
        if (news == null
                || !Objects.equals(news.getManagerReviewStatus(), News.REVIEW_APPROVED)
                || !Objects.equals(news.getTeacherReviewStatus(), News.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, news.getTeacherReviewerId())) {
            return false;
        }
        Long resolvedReviewerId = reviewerId != null ? reviewerId : com.tianji.common.utils.UserContext.getUser();
        String approver = resolvedReviewerId != null ? "用户-" + resolvedReviewerId : "系统";
        news.setTeacherReviewStatus(approved ? News.REVIEW_APPROVED : News.REVIEW_REJECTED);
        news.setTeacherReviewComment(remark);
        news.setTeacherReviewTime(LocalDateTime.now());
        news.setApprovalStatus(approved ? 2 : 3);
        news.setApprover(approver);
        news.setApprovalTime(LocalDateTime.now());
        news.setIsPublished(approved ? 1 : 0);
        if (approved && news.getPublishTime() == null) {
            news.setPublishTime(LocalDateTime.now());
        }
        if (!approved) {
            news.setRejectReason(remark);
            news.setPublishTime(null);
        }
        int result = baseMapper.updateById(news);
        if (result > 0 && approved) {
            notifyNewsPublishSuccess(news);
            clearDetailCache(id);
            clearListCache();
            syncNewsSearchIndex(news);
        } else if (result > 0) {
            clearDetailCache(id);
            clearListCache();
            syncNewsSearchIndex(news);
        }
        return result > 0;
    }

    public void approveNews(Long id, String remark) {
        Long userId = com.tianji.common.utils.UserContext.getUser();
        if (!approveNewsByTeacher(id, remark, userId, true)) {
            throw new RuntimeException("审核失败：新闻不存在或当前审批阶段不正确");
        }
    }

    private boolean rejectNewsAtCurrentStage(Long id, News news, String rejectReason, Long userId) {
        if (Objects.equals(news.getManagerReviewStatus(), News.REVIEW_PENDING)) {
            return reviewNewsByManager(id, false, rejectReason, userId, true);
        }
        if (Objects.equals(news.getManagerReviewStatus(), News.REVIEW_APPROVED)
                && Objects.equals(news.getTeacherReviewStatus(), News.REVIEW_PENDING)) {
            return reviewNewsByTeacher(id, false, rejectReason, userId, true);
        }
        return false;
    }

    private void legacyApproveNews(Long id, String remark) {
        News news = baseMapper.selectById(id);
        if (news == null) {
            throw new RuntimeException("新闻不存在，ID: " + id);
        }

        // 获取当前用户ID
        Long userId = com.tianji.common.utils.UserContext.getUser();
        String approver = userId != null ? "用户-" + userId : "系统";

        // 更新审核状态
        news.setApprovalStatus(2); // 2-已通过
        news.setApprover(approver);
        news.setApprovalTime(java.time.LocalDateTime.now());

        // 如果新闻未发布，审核通过后自动发布
        if (news.getIsPublished() == 0) {
            news.setIsPublished(1);
            if (news.getPublishTime() == null) {
                news.setPublishTime(java.time.LocalDateTime.now());
            }
        }

        int result = baseMapper.updateById(news);
        if (result > 0) {
            notifyNewsPublishSuccess(news);
            // 清除缓存
            clearDetailCache(id);
            clearListCache();
            syncNewsSearchIndex(news);
            log.info("✅ 新闻审核通过: ID={}, 标题={}, 审核人={}", id, news.getTitle(), approver);
        } else {
            throw new RuntimeException("审核失败：更新数据库失败");
        }
    }

    /**
     * 审核拒绝新闻
     */
    @Override
    public void rejectNews(Long id, String rejectReason) {
        {
            News stageNews = baseMapper.selectById(id);
            Long stageUserId = com.tianji.common.utils.UserContext.getUser();
            if (stageNews != null && rejectNewsAtCurrentStage(id, stageNews, rejectReason, stageUserId)) {
                return;
            }
        }
        News news = baseMapper.selectById(id);
        if (news == null) {
            throw new RuntimeException("新闻不存在，ID: " + id);
        }

        // 获取当前用户ID
        Long userId = com.tianji.common.utils.UserContext.getUser();
        String approver = userId != null ? "用户-" + userId : "系统";

        // 更新审核状态
        news.setApprovalStatus(3); // 3-已拒绝
        news.setApprover(approver);
        news.setApprovalTime(java.time.LocalDateTime.now());
        news.setRejectReason(rejectReason);

        int result = baseMapper.updateById(news);
        if (result > 0) {
            // 清除缓存
            clearDetailCache(id);
            clearListCache();
            syncNewsSearchIndex(news);
            log.info("❌ 新闻审核拒绝: ID={}, 标题={}, 审核人={}, 原因={}", id, news.getTitle(), approver, rejectReason);
        } else {
            throw new RuntimeException("拒绝失败：更新数据库失败");
        }
    }
}
