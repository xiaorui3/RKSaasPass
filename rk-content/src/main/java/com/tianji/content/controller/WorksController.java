package com.tianji.content.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.R;
import com.tianji.common.utils.UserContext;
import com.tianji.content.converter.WorksConverter;
import com.tianji.content.domain.dto.WorksQueryDTO;
import com.tianji.content.domain.dto.WorksSaveDTO;
import com.tianji.content.domain.dto.WorksUpdateDTO;
import com.tianji.content.domain.po.Work;
import com.tianji.content.domain.vo.WorksDetailVO;
import com.tianji.content.domain.vo.WorksListVO;
import com.tianji.content.domain.vo.WorksSimpleVO;
import com.tianji.content.service.IWorksService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 作品展示控制器
 * 使用DTO/VO分层架构
 * 包含前端展示和后台管理CRUD接口
 * 权限控制：
 * - 作品管理（增删改）: super_admin, manager
 * - 作品浏览（查看）: super_admin, manager, teacher, member
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Slf4j
@Api(tags = "作品展示接口")
@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
@Validated
public class WorksController {

    private final IWorksService worksService;
    private final WorksConverter worksConverter;

    // ==================== 管理接口（CRUD） ====================

    /**
     * 新增作品
     * 权限: super_admin, manager
     */
    @ApiOperation("新增作品")
    @PostMapping
    @PreAuthorize("hasAuthority('content:works:add')")
    public R<String> addWork(@Valid @RequestBody WorksSaveDTO dto) {
        try {
            Work work = worksConverter.toEntity(dto);
            boolean success = worksService.save(work);
            if (success) {
                log.info("新增作品成功，标题: {}", work.getTitle());
                return R.ok("新增成功");
            }
            return R.error("新增失败");
        } catch (Exception e) {
            log.error("新增作品失败", e);
            return R.error("新增失败：" + e.getMessage());
        }
    }

    /**
     * 更新作品
     * 权限: super_admin, manager
     */
    @ApiOperation("更新作品")
    @PutMapping
    @PreAuthorize("hasAuthority('content:works:edit')")
    public R<String> updateWork(@Valid @RequestBody WorksUpdateDTO dto) {
        try {
            if (dto.getId() == null) {
                return R.error("作品ID不能为空");
            }
            
            Work existingWork = worksService.getById(dto.getId());
            if (existingWork == null) {
                return R.error("作品不存在");
            }
            
            // 使用转换器更新实体
            worksConverter.updateEntity(existingWork, dto);
            
            boolean success = worksService.updateById(existingWork);
            if (success) {
                log.info("更新作品成功，ID: {}", dto.getId());
                return R.ok("更新成功");
            }
            return R.error("更新失败");
        } catch (Exception e) {
            log.error("更新作品失败", e);
            return R.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除作品（逻辑删除）
     * 权限: super_admin, manager
     */
    @ApiOperation("删除作品")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('content:works:remove')")
    public R<String> deleteWork(@PathVariable Long id) {
        try {
            Work work = worksService.getById(id);
            if (work == null) {
                return R.error("作品不存在");
            }
            
            boolean success = worksService.removeById(id);
            if (success) {
                log.info("删除作品成功，ID: {}", id);
                return R.ok("删除成功");
            }
            return R.error("删除失败");
        } catch (Exception e) {
            log.error("删除作品失败", e);
            return R.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除作品
     * 权限: super_admin, manager
     */
    @ApiOperation("批量删除作品")
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('content:works:remove')")
    public R<String> deleteWorksBatch(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return R.error("请选择要删除的作品");
            }
            
            boolean success = worksService.removeByIds(ids);
            if (success) {
                log.info("批量删除作品成功，数量: {}", ids.size());
                return R.ok("批量删除成功");
            }
            return R.error("批量删除失败");
        } catch (Exception e) {
            log.error("批量删除作品失败", e);
            return R.error("批量删除失败：" + e.getMessage());
        }
    }

    // ==================== 查询接口（使用VO） ====================

    /**
     * 根路径重定向到分页列表
     * 解决直接访问 /api/works 返回500错误的问题
     */
    @ApiOperation("获取作品列表（重定向到分页接口）")
    @GetMapping
    public R<IPage<WorksListVO>> getWorksRoot(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Page<Work> pageParam = new Page<>(page, size);
            IPage<Work> workPage = worksService.page(pageParam, buildWorksPageQuery(null));
            IPage<WorksListVO> voPage = workPage.convert(worksConverter::toListVO);
            return R.ok(voPage);
        } catch (Exception e) {
            log.error("获取作品列表失败", e);
            return R.error("获取作品列表失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询作品列表
     * 公开接口：无需登录即可查看作品列表
     */
    @ApiOperation("分页查询作品列表")
    @GetMapping("/page")
    public R<IPage<WorksListVO>> getWorksPage(WorksQueryDTO queryDTO) {
        try {
            Page<Work> page = new Page<>(resolvePage(queryDTO), resolveSize(queryDTO));
            IPage<Work> workPage = worksService.page(page, buildWorksPageQuery(queryDTO));
            
            // 转换为VO
            IPage<WorksListVO> voPage = workPage.convert(worksConverter::toListVO);
            return R.ok(voPage);
        } catch (Exception e) {
            log.error("分页查询作品列表失败", e);
            return R.error("分页查询作品列表失败：" + e.getMessage());
        }
    }

    /**
     * Build the public works query used by root and paged list endpoints.
     */
    private LambdaQueryWrapper<Work> buildWorksPageQuery(WorksQueryDTO queryDTO) {
        LambdaQueryWrapper<Work> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Work::getIsDeleted, 0)
                .eq(Work::getManagerReviewStatus, Work.REVIEW_APPROVED)
                .eq(Work::getTeacherReviewStatus, Work.REVIEW_APPROVED);

        if (queryDTO == null) {
            applyWorksPageSorting(queryWrapper, null);
            return queryWrapper;
        }

        String category = trimToNull(queryDTO.getCategory());
        if (category != null) {
            queryWrapper.eq(Work::getCategory, category);
        }

        String keyword = trimToNull(queryDTO.getKeyword());
        if (keyword != null) {
            queryWrapper.and(wrapper -> wrapper.like(Work::getTitle, keyword)
                    .or()
                    .like(Work::getDescription, keyword)
                    .or()
                    .like(Work::getContent, keyword)
                    .or()
                    .like(Work::getAuthors, keyword)
                    .or()
                    .like(Work::getTechnologies, keyword));
        }

        String author = trimToNull(queryDTO.getAuthor());
        if (author != null) {
            queryWrapper.like(Work::getAuthors, author);
        }

        String technology = trimToNull(queryDTO.getTechnology());
        if (technology != null) {
            queryWrapper.like(Work::getTechnologies, technology);
        }

        if (queryDTO.getIsFeatured() != null) {
            queryWrapper.eq(Work::getIsFeatured, queryDTO.getIsFeatured());
        }

        applyWorksPageSorting(queryWrapper, queryDTO);
        return queryWrapper;
    }

    private void applyWorksPageSorting(LambdaQueryWrapper<Work> queryWrapper, WorksQueryDTO queryDTO) {
        boolean ascending = queryDTO != null && "asc".equalsIgnoreCase(queryDTO.getSortOrder());
        String sortBy = trimToNull(queryDTO == null ? null : queryDTO.getSortBy());
        if (sortBy == null) {
            queryWrapper.orderByAsc(Work::getDisplayOrder)
                    .orderByDesc(Work::getCreateTime);
            return;
        }

        switch (sortBy) {
            case "updateTime":
                applySortOrder(queryWrapper, ascending, Work::getUpdateTime);
                break;
            case "viewCount":
                applySortOrder(queryWrapper, ascending, Work::getViewCount);
                break;
            case "likeCount":
                applySortOrder(queryWrapper, ascending, Work::getLikeCount);
                break;
            case "displayOrder":
                applySortOrder(queryWrapper, ascending, Work::getDisplayOrder);
                break;
            case "createTime":
                applySortOrder(queryWrapper, ascending, Work::getCreateTime);
                break;
            default:
                queryWrapper.orderByAsc(Work::getDisplayOrder)
                        .orderByDesc(Work::getCreateTime);
                return;
        }

        if (!"displayOrder".equals(sortBy)) {
            queryWrapper.orderByAsc(Work::getDisplayOrder);
        }
        if (!"createTime".equals(sortBy)) {
            queryWrapper.orderByDesc(Work::getCreateTime);
        }
    }

    private void applySortOrder(LambdaQueryWrapper<Work> queryWrapper, boolean ascending, SFunction<Work, ?> column) {
        if (ascending) {
            queryWrapper.orderByAsc(column);
        } else {
            queryWrapper.orderByDesc(column);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private long resolvePage(WorksQueryDTO queryDTO) {
        Integer page = queryDTO == null ? null : queryDTO.getPage();
        return page == null || page < 1 ? 1L : page.longValue();
    }

    private long resolveSize(WorksQueryDTO queryDTO) {
        Integer size = queryDTO == null ? null : queryDTO.getSize();
        if (size == null || size < 1) {
            return 10L;
        }
        return Math.min(size.longValue(), 100L);
    }

    @ApiOperation("获取所有作品")
    @GetMapping("/list")
    public R<List<WorksListVO>> getAllWorks() {
        try {
            List<Map<String, Object>> result = worksService.getAllWorks();
            // 将Map转换为VO
            List<WorksListVO> voList = result.stream()
                    .map(this::mapToWorksListVO)
                    .collect(Collectors.toList());
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取作品列表失败", e);
            return R.error("获取作品列表失败：" + e.getMessage());
        }
    }

    /**
     * 按分类获取作品
     */
    @ApiOperation("按分类获取作品")
    @GetMapping("/category/{category}")
    public R<List<WorksSimpleVO>> getWorksByCategory(@PathVariable String category) {
        try {
            List<Map<String, Object>> result = worksService.getWorksByCategory(category);
            List<WorksSimpleVO> voList = result.stream()
                    .map(this::mapToWorksSimpleVO)
                    .collect(Collectors.toList());
            return R.ok(voList);
        } catch (Exception e) {
            log.error("按分类获取作品失败", e);
            return R.error("按分类获取作品失败：" + e.getMessage());
        }
    }

    /**
     * 获取精选作品
     */
    @ApiOperation("获取精选作品")
    @GetMapping("/featured")
    public R<List<WorksSimpleVO>> getFeaturedWorks() {
        try {
            List<Map<String, Object>> result = worksService.getFeaturedWorks();
            List<WorksSimpleVO> voList = result.stream()
                    .map(this::mapToWorksSimpleVO)
                    .collect(Collectors.toList());
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取精选作品失败", e);
            return R.error("获取精选作品失败：" + e.getMessage());
        }
    }

    /**
     * 搜索作品
     */
    @ApiOperation("搜索作品")
    @GetMapping("/search")
    public R<List<WorksListVO>> searchWorks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        try {
            List<Map<String, Object>> result = worksService.searchWorks(title, category);
            List<WorksListVO> voList = result.stream()
                    .map(this::mapToWorksListVO)
                    .collect(Collectors.toList());
            return R.ok(voList);
        } catch (Exception e) {
            log.error("搜索作品失败", e);
            return R.error("搜索作品失败：" + e.getMessage());
        }
    }

    /**
     * 获取作品详情
     */
    @ApiOperation("获取作品详情")
    @GetMapping("/{id}")
    public R<WorksDetailVO> getWorkDetail(@PathVariable Long id) {
        try {
            Map<String, Object> result = worksService.getWorkDetail(id);
            if (result == null) {
                return R.error("作品不存在");
            }

            // 增加浏览量
            worksService.incrementViewCount(id);

            WorksDetailVO vo = mapToWorksDetailVO(result);
            return R.ok(vo);
        } catch (Exception e) {
            log.error("获取作品详情失败", e);
            return R.error("获取作品详情失败：" + e.getMessage());
        }
    }

    /**
     * 点赞作品
     */
    @ApiOperation("点赞作品")
    @PostMapping("/{id}/like")
    public R<String> likeWork(@PathVariable Long id) {
        try {
            boolean success = worksService.likeWork(id);
            if (success) {
                log.info("作品点赞成功，ID: {}", id);
                return R.ok("点赞成功");
            }
            return R.error("点赞失败");
        } catch (Exception e) {
            log.error("点赞作品失败", e);
            return R.error("点赞失败：" + e.getMessage());
        }
    }

    /**
     * 取消点赞
     */
    @ApiOperation("取消点赞")
    @PostMapping("/{id}/unlike")
    public R<String> unlikeWork(@PathVariable Long id) {
        try {
            boolean success = worksService.unlikeWork(id);
            if (success) {
                log.info("取消点赞成功，ID: {}", id);
                return R.ok("取消点赞成功");
            }
            return R.error("取消点赞失败");
        } catch (Exception e) {
            log.error("取消点赞失败", e);
            return R.error("取消点赞失败：" + e.getMessage());
        }
    }

    /**
     * 获取热门作品
     */
    @ApiOperation("获取热门作品")
    @GetMapping("/popular")
    public R<List<WorksSimpleVO>> getPopularWorks() {
        try {
            List<Map<String, Object>> result = worksService.getPopularWorks();
            List<WorksSimpleVO> voList = result.stream()
                    .map(this::mapToWorksSimpleVO)
                    .collect(Collectors.toList());
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取热门作品失败", e);
            return R.error("获取热门作品失败：" + e.getMessage());
        }
    }

    /**
     * 获取最新作品
     */
    @ApiOperation("获取最新作品")
    @GetMapping("/latest")
    public R<List<WorksSimpleVO>> getLatestWorks() {
        try {
            List<Map<String, Object>> result = worksService.getLatestWorks();
            List<WorksSimpleVO> voList = result.stream()
                    .map(this::mapToWorksSimpleVO)
                    .collect(Collectors.toList());
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取最新作品失败", e);
            return R.error("获取最新作品失败：" + e.getMessage());
        }
    }

    /**
     * 获取作品统计信息
     */
    @ApiOperation("获取作品统计信息")
    @GetMapping("/statistics")
    public R<Map<String, Object>> getWorkStatistics() {
        try {
            Map<String, Object> statistics = worksService.getWorkStatistics();
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取作品统计信息失败", e);
            return R.error("获取作品统计信息失败：" + e.getMessage());
        }
    }

    // ==================== 私有方法：Map转VO ====================

    /**
     * Map转WorksListVO
     */
    private WorksListVO mapToWorksListVO(Map<String, Object> map) {
        WorksListVO vo = new WorksListVO();
        vo.setId(getLongValue(map, "id"));
        vo.setTitle((String) map.get("title"));
        vo.setDescription((String) map.get("description"));
        vo.setCategory((String) map.get("category"));
        vo.setCoverImage((String) map.get("coverImage"));
        vo.setDemoVideo((String) map.get("demoVideo"));
        vo.setIsFeatured(getBooleanValue(map, "isFeatured"));
        vo.setDisplayOrder(getIntValue(map, "displayOrder"));
        vo.setViewCount(getIntValue(map, "viewCount"));
        vo.setLikeCount(getIntValue(map, "likeCount"));
        vo.setTechnologies((String) map.get("technologies"));
        vo.setAuthors((String) map.get("authors"));
        vo.setCreateTime(getLocalDateTimeValue(map, "createTime"));
        return vo;
    }

    /**
     * Map转WorksSimpleVO
     */
    private WorksSimpleVO mapToWorksSimpleVO(Map<String, Object> map) {
        WorksSimpleVO vo = new WorksSimpleVO();
        vo.setId(getLongValue(map, "id"));
        vo.setTitle((String) map.get("title"));
        vo.setDescription((String) map.get("description"));
        vo.setCoverImage((String) map.get("coverImage"));
        vo.setCategory((String) map.get("category"));
        vo.setIsFeatured(getBooleanValue(map, "isFeatured"));
        vo.setViewCount(getIntValue(map, "viewCount"));
        vo.setLikeCount(getIntValue(map, "likeCount"));
        vo.setCreateTime(getLocalDateTimeValue(map, "createTime"));
        return vo;
    }

    /**
     * Map转WorksDetailVO
     */
    private WorksDetailVO mapToWorksDetailVO(Map<String, Object> map) {
        WorksDetailVO vo = new WorksDetailVO();
        vo.setId(getLongValue(map, "id"));
        vo.setTitle((String) map.get("title"));
        vo.setDescription((String) map.get("description"));
        vo.setContent((String) map.get("content"));
        vo.setCategory((String) map.get("category"));
        vo.setCoverImage((String) map.get("coverImage"));
        vo.setDemoVideo((String) map.get("demoVideo"));
        vo.setProjectLinks((String) map.get("projectLinks"));
        vo.setTechnologies((String) map.get("technologies"));
        vo.setAuthors((String) map.get("authors"));
        vo.setIsFeatured(getBooleanValue(map, "isFeatured"));
        vo.setDisplayOrder(getIntValue(map, "displayOrder"));
        vo.setViewCount(getIntValue(map, "viewCount"));
        vo.setLikeCount(getIntValue(map, "likeCount"));
        vo.setCreatedBy((String) map.get("createdBy"));
        vo.setCreateTime(getLocalDateTimeValue(map, "createTime"));
        return vo;
    }

    // ==================== 辅助方法 ====================

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        return Long.parseLong(value.toString());
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        return Integer.parseInt(value.toString());
    }

    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    private java.time.LocalDateTime getLocalDateTimeValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof java.time.LocalDateTime) return (java.time.LocalDateTime) value;
        return null;
    }

    @ApiOperation("负责人审核作品")
    @PostMapping("/{id}/review/manager")
    @PreAuthorize("hasAnyAuthority('content:works:edit', 'content:works:add')")
    public R<String> reviewWorkByManager(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!Objects.equals(roleId, 1L) && !Objects.equals(roleId, 7L)) {
                return R.error("no manager review permission");
            }
            boolean success = worksService.reviewWorkByManager(id, extractApproved(body), extractRemark(body), UserContext.getUser(), Objects.equals(roleId, 1L));
            return success ? R.ok("manager review approved") : R.error("manager review failed");
        } catch (Exception e) {
            log.error("manager review work failed, id: {}", id, e);
            return R.error("manager review failed: " + e.getMessage());
        }
    }

    @ApiOperation("指导老师审核作品")
    @PostMapping("/{id}/review/teacher")
    @PreAuthorize("hasAuthority('content:works:edit')")
    public R<String> reviewWorkByTeacher(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!Objects.equals(roleId, 1L) && !Objects.equals(roleId, 8L)) {
                return R.error("no teacher review permission");
            }
            boolean success = worksService.reviewWorkByTeacher(id, extractApproved(body), extractRemark(body), UserContext.getUser(), Objects.equals(roleId, 1L));
            return success ? R.ok("teacher review approved") : R.error("teacher review failed");
        } catch (Exception e) {
            log.error("teacher review work failed, id: {}", id, e);
            return R.error("teacher review failed: " + e.getMessage());
        }
    }

    private String extractRemark(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object value = body.get("reviewComment");
        if (value == null) {
            value = body.get("remark");
        }
        return value == null ? null : String.valueOf(value);
    }

    private boolean extractApproved(Map<String, Object> body) {
        if (body == null || !body.containsKey("approved")) {
            return true;
        }
        Object value = body.get("approved");
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
