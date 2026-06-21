package com.tianji.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.annotation.PostConstruct;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.utils.CacheKeyBuilder;
import com.tianji.common.utils.SearchIndexSyncUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.content.domain.po.Work;
import com.tianji.content.mapper.WorkMapper;
import com.tianji.content.service.IWorksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 作品展示服务实现类
 * 基于master分支的WorkServiceImpl功能
 * 
 * 缓存策略：
 * - 热门作品缓存：30分钟
 * - 作品详情缓存：1小时
 * - 作品列表缓存：10分钟
 * - 所有缓存Key包含tenant_id实现多租户隔离
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorksServiceImpl extends ServiceImpl<WorkMapper, Work>
        implements IWorksService {

    private final StringRedisTemplate redisTemplate;
    private final SearchClient searchClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // 缓存过期时间配置（单位：分钟）
    private static final long CACHE_EXPIRE_DETAIL = 60;      // 详情缓存1小时
    private static final long CACHE_EXPIRE_LIST = 10;        // 列表缓存10分钟
    private static final long CACHE_EXPIRE_HOT = 30;         // 热门缓存30分钟
    private static final String SEARCH_ENTITY_TYPE_WORKS = "WORKS";

    @Override
    public boolean save(Work entity) {
        if (entity == null) {
            throw new IllegalArgumentException("work is required");
        }
        if (entity.getManagerReviewerId() == null || entity.getTeacherReviewerId() == null) {
            throw new IllegalArgumentException("manager and teacher approvers are required");
        }
        entity.setManagerReviewStatus(Work.REVIEW_PENDING);
        entity.setManagerReviewComment(null);
        entity.setManagerReviewTime(null);
        entity.setTeacherReviewStatus(Work.REVIEW_PENDING);
        entity.setTeacherReviewComment(null);
        entity.setTeacherReviewTime(null);
        boolean result = super.save(entity);
        if (result) {
            clearEntityCaches(entity == null ? null : entity.getId());
            syncWorkSearchIndex(entity == null ? null : entity.getId());
        }
        return result;
    }

    @Override
    public boolean updateById(Work entity) {
        Long workId = entity == null ? null : entity.getId();
        boolean result = super.updateById(entity);
        if (result) {
            clearEntityCaches(workId);
            syncWorkSearchIndex(workId);
        }
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        Long workId = toLongId(id);
        Work existing = workId == null ? null : getById(workId);
        Long tenantId = existing == null ? null : existing.getTenantId();
        boolean result = super.removeById(id);
        if (result) {
            clearEntityCaches(workId);
            deleteWorkSearchIndex(workId, tenantId);
        }
        return result;
    }

    @Override
    public boolean removeByIds(Collection<? extends Serializable> list) {
        Map<Long, Long> tenantIdsByWorkId = new HashMap<>();
        if (list != null) {
            for (Object id : list) {
                Long workId = toLongId(id);
                Work existing = workId == null ? null : getById(workId);
                if (existing != null && existing.getId() != null) {
                    tenantIdsByWorkId.put(existing.getId(), existing.getTenantId());
                }
            }
        }
        boolean result = super.removeByIds(list);
        if (result) {
            if (list != null) {
                for (Object id : list) {
                    Long workId = toLongId(id);
                    clearDetailCache(workId);
                    deleteWorkSearchIndex(workId, tenantIdsByWorkId.get(workId));
                }
            }
            clearListCache();
        }
        return result;
    }

    /**
     * 获取所有作品（前端展示用，带缓存）
     */
    @Override
    public List<Map<String, Object>> getAllWorks() {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_WORKS, "all", "list");
        
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取所有作品列表");
                return objectMapper.readValue(cachedJson, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("读取所有作品缓存失败: {}", e.getMessage());
        }
        
        List<Work> works = getAllActiveWorks();
        List<Map<String, Object>> result = convertWorksToMapList(works);
        
        // 写入缓存
        if (result != null && !result.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_LIST, TimeUnit.MINUTES);
                log.debug("所有作品列表已缓存");
            } catch (Exception e) {
                log.warn("写入所有作品缓存失败: {}", e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * 按分类获取作品（带缓存）
     */
    @Override
    public List<Map<String, Object>> getWorksByCategory(String category) {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_WORKS, "category_" + category, "list");
        
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取分类作品列表: {}", category);
                return objectMapper.readValue(cachedJson, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("读取分类作品缓存失败: {}", e.getMessage());
        }
        
        LambdaQueryWrapper<Work> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Work::getCategory, category)
                .eq(Work::getIsDeleted, 0)
                .orderByAsc(Work::getDisplayOrder)
                .orderByDesc(Work::getCreateTime);
        applyApprovedFilter(queryWrapper);
        List<Work> works = list(queryWrapper);
        List<Map<String, Object>> result = convertWorksToMapList(works);
        
        // 写入缓存
        if (result != null && !result.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_LIST, TimeUnit.MINUTES);
                log.debug("分类作品列表已缓存: {}", category);
            } catch (Exception e) {
                log.warn("写入分类作品缓存失败: {}", e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * 获取精选作品（带缓存）
     */
    @Override
    public List<Map<String, Object>> getFeaturedWorks() {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_WORKS, "featured", "list");
        
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取精选作品列表");
                return objectMapper.readValue(cachedJson, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("读取精选作品缓存失败: {}", e.getMessage());
        }
        
        LambdaQueryWrapper<Work> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Work::getIsFeatured, true)
                .eq(Work::getIsDeleted, 0)
                .orderByAsc(Work::getDisplayOrder)
                .orderByDesc(Work::getLikeCount);
        applyApprovedFilter(queryWrapper);
        List<Work> works = list(queryWrapper);
        List<Map<String, Object>> result = convertWorksToMapList(works);
        
        // 写入缓存
        if (result != null && !result.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_HOT, TimeUnit.MINUTES);
                log.debug("精选作品列表已缓存");
            } catch (Exception e) {
                log.warn("写入精选作品缓存失败: {}", e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * 搜索作品
     * 搜索结果不缓存，每次实时查询
     */
    @Override
    public List<Map<String, Object>> searchWorks(String title, String category) {
        LambdaQueryWrapper<Work> queryWrapper = new LambdaQueryWrapper<>();

        if (title != null && !title.trim().isEmpty()) {
            queryWrapper.like(Work::getTitle, title);
        }
        if (category != null && !category.trim().isEmpty()) {
            queryWrapper.eq(Work::getCategory, category);
        }

        queryWrapper.eq(Work::getIsDeleted, 0)
                .orderByDesc(Work::getLikeCount)
                .orderByDesc(Work::getViewCount);
        applyApprovedFilter(queryWrapper);

        List<Work> works = list(queryWrapper);
        return convertWorksToMapList(works);
    }

    /**
     * 获取作品详情（带缓存）
     */
    @Override
    public Map<String, Object> getWorkDetail(Long id) {
        // 尝试从缓存获取
        String cacheKey = CacheKeyBuilder.buildDetailKey(CacheKeyBuilder.MODULE_WORKS, id);
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取作品详情: {}", id);
                return objectMapper.readValue(cachedJson, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("读取作品详情缓存失败: {}", e.getMessage());
        }
        
        Work work = getById(id);
        if (work == null) {
            return null;
        }
        if (!Objects.equals(work.getManagerReviewStatus(), Work.REVIEW_APPROVED)
                || !Objects.equals(work.getTeacherReviewStatus(), Work.REVIEW_APPROVED)) {
            return null;
        }
        
        Map<String, Object> result = convertWorkToMap(work);
        
        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_DETAIL, TimeUnit.MINUTES);
            log.debug("作品详情已缓存: {}", id);
        } catch (Exception e) {
            log.warn("写入作品详情缓存失败: {}", e.getMessage());
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
        String countKey = CacheKeyBuilder.buildCountKey(CacheKeyBuilder.MODULE_WORKS, "view", id);
        Long newCount = redisTemplate.opsForValue().increment(countKey);
        
        // 设置计数器过期时间（7天）
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(countKey, 7, TimeUnit.DAYS);
        }
        
        // 每100次浏览更新一次数据库
        if (newCount != null && newCount % 100 == 0) {
            Work work = getById(id);
            if (work != null) {
                work.setViewCount(work.getViewCount() + 100);
                work.setUpdateTime(LocalDateTime.now());
                updateById(work);
                // 重置计数器
                redisTemplate.opsForValue().set(countKey, "0", 7, TimeUnit.DAYS);
            }
        }
        
        return 1;
    }

    /**
     * 点赞作品
     */
    @Override
    public boolean likeWork(Long id) {
        Work work = getById(id);
        if (work != null) {
            work.setLikeCount(work.getLikeCount() + 1);
            work.setUpdateTime(LocalDateTime.now());
            boolean result = updateById(work);
            
            // 清除详情缓存
            if (result) {
                clearDetailCache(id);
            }
            
            return result;
        }
        return false;
    }

    /**
     * 取消点赞
     */
    @Override
    public boolean unlikeWork(Long id) {
        Work work = getById(id);
        if (work != null && work.getLikeCount() > 0) {
            work.setLikeCount(work.getLikeCount() - 1);
            work.setUpdateTime(LocalDateTime.now());
            boolean result = updateById(work);
            
            // 清除详情缓存
            if (result) {
                clearDetailCache(id);
            }
            
            return result;
        }
        return false;
    }

    /**
     * 上传封面图片
     */
    @Override
    public String uploadCoverImage(Long id, String imageUrl) {
        Work work = getById(id);
        if (work != null) {
            work.setCoverImage(imageUrl);
            work.setUpdateTime(LocalDateTime.now());
            updateById(work);
            
            // 清除详情缓存
            clearDetailCache(id);
            
            return imageUrl;
        }
        return null;
    }

    /**
     * 上传演示视频
     */
    @Override
    public String uploadDemoVideo(Long id, String videoUrl) {
        Work work = getById(id);
        if (work != null) {
            work.setDemoVideo(videoUrl);
            work.setUpdateTime(LocalDateTime.now());
            updateById(work);
            
            // 清除详情缓存
            clearDetailCache(id);
            
            return videoUrl;
        }
        return null;
    }

    /**
     * 获取作品统计（带缓存）
     */
    @Override
    public Map<String, Object> getWorkStatistics() {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_WORKS, "statistics", "total");
        
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取作品统计");
                return objectMapper.readValue(cachedJson, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("读取作品统计缓存失败: {}", e.getMessage());
        }
        
        List<Work> allWorks = getAllActiveWorks();

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total", allWorks.size());
        statistics.put("totalViews", allWorks.stream().mapToInt(Work::getViewCount).sum());
        statistics.put("totalLikes", allWorks.stream().mapToInt(Work::getLikeCount).sum());

        // 按分类统计
        Map<String, Long> categoryStats = allWorks.stream()
                .collect(Collectors.groupingBy(work ->
                        work.getCategory() != null ? work.getCategory() : "未分类",
                        Collectors.counting()));
        statistics.put("categoryDistribution", categoryStats);
        
        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(statistics);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_LIST, TimeUnit.MINUTES);
            log.debug("作品统计已缓存");
        } catch (Exception e) {
            log.warn("写入作品统计缓存失败: {}", e.getMessage());
        }

        return statistics;
    }

    /**
     * 获取热门作品（带缓存）
     */
    @Override
    public List<Map<String, Object>> getPopularWorks() {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_WORKS, "popular", "top10");
        
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取热门作品列表");
                return objectMapper.readValue(cachedJson, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("读取热门作品缓存失败: {}", e.getMessage());
        }
        
        LambdaQueryWrapper<Work> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Work::getIsDeleted, 0)
                .orderByDesc(Work::getViewCount)
                .orderByDesc(Work::getLikeCount)
                .last("LIMIT 10");
        applyApprovedFilter(queryWrapper);
        List<Work> works = list(queryWrapper);
        List<Map<String, Object>> result = convertWorksToMapList(works);
        
        // 写入缓存
        if (result != null && !result.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_HOT, TimeUnit.MINUTES);
                log.debug("热门作品列表已缓存");
            } catch (Exception e) {
                log.warn("写入热门作品缓存失败: {}", e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * 获取最新作品（带缓存）
     */
    @Override
    public List<Map<String, Object>> getLatestWorks() {
        // 构建缓存Key
        String cacheKey = CacheKeyBuilder.build(CacheKeyBuilder.MODULE_WORKS, "latest", "top10");
        
        // 尝试从缓存获取
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("从缓存获取最新作品列表");
                return objectMapper.readValue(cachedJson, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("读取最新作品缓存失败: {}", e.getMessage());
        }
        
        LambdaQueryWrapper<Work> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Work::getIsDeleted, 0)
                .orderByDesc(Work::getCreateTime)
                .last("LIMIT 10");
        applyApprovedFilter(queryWrapper);
        List<Work> works = list(queryWrapper);
        List<Map<String, Object>> result = convertWorksToMapList(works);
        
        // 写入缓存
        if (result != null && !result.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_EXPIRE_LIST, TimeUnit.MINUTES);
                log.debug("最新作品列表已缓存");
            } catch (Exception e) {
                log.warn("写入最新作品缓存失败: {}", e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * 获取所有活跃作品
     */
    @Override
    public boolean approveWorkByManager(Long id, String remark, Long reviewerId, boolean allowProxyReview) {
        return reviewWorkByManager(id, true, remark, reviewerId, allowProxyReview);
    }

    @Override
    public boolean reviewWorkByManager(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview) {
        Work work = getById(id);
        if (work == null || !Objects.equals(work.getManagerReviewStatus(), Work.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, work.getManagerReviewerId())) {
            return false;
        }
        work.setManagerReviewStatus(approved ? Work.REVIEW_APPROVED : Work.REVIEW_REJECTED);
        work.setManagerReviewComment(remark);
        work.setManagerReviewTime(LocalDateTime.now());
        return updateById(work);
    }

    @Override
    public boolean approveWorkByTeacher(Long id, String remark, Long reviewerId, boolean allowProxyReview) {
        return reviewWorkByTeacher(id, true, remark, reviewerId, allowProxyReview);
    }

    @Override
    public boolean reviewWorkByTeacher(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview) {
        Work work = getById(id);
        if (work == null
                || !Objects.equals(work.getManagerReviewStatus(), Work.REVIEW_APPROVED)
                || !Objects.equals(work.getTeacherReviewStatus(), Work.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, work.getTeacherReviewerId())) {
            return false;
        }
        work.setTeacherReviewStatus(approved ? Work.REVIEW_APPROVED : Work.REVIEW_REJECTED);
        work.setTeacherReviewComment(remark);
        work.setTeacherReviewTime(LocalDateTime.now());
        return updateById(work);
    }

    private List<Work> getAllActiveWorks() {
        LambdaQueryWrapper<Work> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Work::getIsDeleted, 0)
                .orderByAsc(Work::getDisplayOrder)
                .orderByDesc(Work::getCreateTime);
        applyApprovedFilter(queryWrapper);
        return list(queryWrapper);
    }

    private void applyApprovedFilter(LambdaQueryWrapper<Work> queryWrapper) {
        queryWrapper.eq(Work::getManagerReviewStatus, Work.REVIEW_APPROVED)
                .eq(Work::getTeacherReviewStatus, Work.REVIEW_APPROVED);
    }

    private boolean isPublicWork(Work work) {
        return work != null
                && Objects.equals(work.getManagerReviewStatus(), Work.REVIEW_APPROVED)
                && Objects.equals(work.getTeacherReviewStatus(), Work.REVIEW_APPROVED)
                && Integer.valueOf(0).equals(work.getIsDeleted());
    }

    private void syncWorkSearchIndex(Long id) {
        if (id == null) {
            return;
        }
        try {
            SearchIndexSyncUtils.runAfterCommit(() -> {
                Work work = null;
                try {
                    work = getById(id);
                    if (work == null || !isPublicWork(work)) {
                        searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_WORKS, id, work == null ? TenantContext.getTenantId() : work.getTenantId());
                        return;
                    }
                    searchClient.upsertGlobalDocument(buildWorkSearchDocument(work));
                } catch (Exception e) {
                    log.warn("sync work global search index failed, workId={}, tenantId={}, reason={}",
                            id, work == null ? null : work.getTenantId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("schedule work global search index sync failed, workId={}, tenantId={}, reason={}",
                    id, TenantContext.getTenantId(), e.getMessage());
        }
    }

    private void deleteWorkSearchIndex(Long id, Long tenantId) {
        if (id == null) {
            return;
        }
        Long resolvedTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        try {
            SearchIndexSyncUtils.runAfterCommit(() -> {
                try {
                    searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_WORKS, id, resolvedTenantId);
                } catch (Exception e) {
                    log.warn("delete work global search index failed, workId={}, tenantId={}, reason={}",
                            id, resolvedTenantId, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("schedule work global search index delete failed, workId={}, tenantId={}, reason={}",
                    id, resolvedTenantId, e.getMessage());
        }
    }

    @Override
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        LambdaQueryWrapper<Work> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Work::getIsDeleted, 0)
                .eq(Work::getManagerReviewStatus, Work.REVIEW_APPROVED)
                .eq(Work::getTeacherReviewStatus, Work.REVIEW_APPROVED)
                .orderByDesc(Work::getUpdateTime);
        return list(queryWrapper).stream()
                .filter(this::isPublicWork)
                .map(this::buildWorkSearchDocument)
                .collect(Collectors.toList());
    }

    private GlobalSearchDocumentDTO buildWorkSearchDocument(Work work) {
        return new GlobalSearchDocumentDTO()
                .setEntityType(SEARCH_ENTITY_TYPE_WORKS)
                .setEntityId(work.getId())
                .setTenantId(work.getTenantId())
                .setTitle(work.getTitle())
                .setSummary(work.getDescription())
                .setContent(work.getContent())
                .setTags(work.getTechnologies())
                .setRoute("/works/" + work.getId())
                .setCoverUrl(work.getCoverImage())
                .setUpdatedAt(formatUpdatedAt(work.getUpdateTime(), work.getCreateTime()))
                .setVisible(isPublicWork(work));
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
     * 转换作品列表为Map列表
     */
    private List<Map<String, Object>> convertWorksToMapList(List<Work> works) {
        return works.stream()
                .map(this::convertWorkToMap)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个作品为Map
     */
    private Map<String, Object> convertWorkToMap(Work work) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", work.getId());
        map.put("title", work.getTitle());
        map.put("description", work.getDescription());
        map.put("content", work.getContent());
        map.put("category", work.getCategory());
        map.put("coverImage", work.getCoverImage());
        map.put("demoVideo", work.getDemoVideo());
        map.put("isFeatured", work.getIsFeatured());
        map.put("viewCount", work.getViewCount());
        map.put("likeCount", work.getLikeCount());
        map.put("displayOrder", work.getDisplayOrder());
        map.put("createTime", work.getCreateTime());

        // 解析JSON字段
        try {
            if (work.getProjectLinks() != null) {
                map.put("projectLinks", work.getProjectLinks());
            }
            if (work.getTechnologies() != null) {
                map.put("technologies", work.getTechnologies());
            }
            if (work.getAuthors() != null) {
                map.put("authors", work.getAuthors());
            }
        } catch (Exception e) {
            log.warn("解析作品JSON字段失败", e);
        }

        return map;
    }

    /**
     * 清除详情缓存
     */
    private void clearDetailCache(Long id) {
        if (id == null) {
            return;
        }
        String cacheKey = CacheKeyBuilder.buildDetailKey(CacheKeyBuilder.MODULE_WORKS, id);
        redisTemplate.delete(cacheKey);
        log.debug("已清除作品详情缓存: {}", id);
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
            String pattern = CacheKeyBuilder.MODULE_WORKS + ":" + tenantId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                keys.removeIf(key -> key.contains(":detail:"));
                if (!keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.debug("已清除作品列表缓存，共{}个key", keys.size());
                }
            }
        } catch (Exception e) {
            log.warn("清除作品列表缓存失败: {}", e.getMessage());
        }
    }

    private void clearEntityCaches(Long id) {
        clearDetailCache(id);
        clearListCache();
    }

    private Long toLongId(Object id) {
        if (id == null) {
            return null;
        }
        if (id instanceof Long) {
            return (Long) id;
        }
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(id));
        } catch (NumberFormatException e) {
            log.warn("无法解析作品ID为Long: {}", id);
            return null;
        }
    }
}
