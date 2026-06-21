package com.tianji.content.controller;

import com.tianji.common.domain.R;
import com.tianji.common.utils.UserContext;
import com.tianji.content.converter.NewsConverter;
import com.tianji.content.domain.dto.NewsApproveDTO;
import com.tianji.content.domain.dto.NewsQueryDTO;
import com.tianji.content.domain.dto.NewsRejectDTO;
import com.tianji.content.domain.dto.NewsSaveDTO;
import com.tianji.content.domain.dto.NewsUpdateDTO;
import com.tianji.content.domain.po.News;
import com.tianji.content.domain.vo.NewsDetailVO;
import com.tianji.content.domain.vo.NewsListVO;
import com.tianji.content.domain.vo.NewsSimpleVO;
import com.tianji.content.service.INewsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 新闻控制器
 * 基于master分支的核心业务逻辑，完整保留原接口功能
 * 使用DTO/VO分层架构
 */
@Slf4j
@Api(tags = "新闻管理接口")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Validated
public class NewsController {

    private final INewsService newsService;
    private final NewsConverter newsConverter;

    /**
     * 获取最新新闻
     * 基于master分支的接口设计
     */
    @ApiOperation("获取最新新闻")
    @GetMapping("/latest")
    public R<List<NewsSimpleVO>> getLatestNews(@RequestParam(defaultValue = "5") Integer limit) {
        try {
            List<News> newsList = newsService.selectLatestNews(limit);
            List<NewsSimpleVO> voList = newsConverter.toSimpleVO(newsList);
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取最新新闻失败", e);
            return R.error("获取新闻失败");
        }
    }

    /**
     * 获取置顶新闻
     * 基于master分支的接口设计
     */
    @ApiOperation("获取置顶新闻")
    @GetMapping("/top")
    public R<List<NewsSimpleVO>> getTopNews(@RequestParam(defaultValue = "3") Integer limit) {
        try {
            List<News> newsList = newsService.selectTopNews(limit);
            List<NewsSimpleVO> voList = newsConverter.toSimpleVO(newsList);
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取置顶新闻失败", e);
            return R.error("获取新闻失败");
        }
    }

    @ApiOperation("获取其他租户共享新闻")
    @GetMapping("/shared")
    public R<List<NewsListVO>> getSharedNews(@RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<News> newsList = newsService.selectSharedNews(limit);
            return R.ok(newsConverter.toListVO(newsList));
        } catch (Exception e) {
            log.error("获取共享新闻失败", e);
            return R.error("获取共享新闻失败");
        }
    }

    /**
     * 根据分类获取新闻
     * 基于master分支的接口设计
     */
    @ApiOperation("根据分类获取新闻")
    @GetMapping("/category/{category}")
    public R<List<NewsListVO>> getNewsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<News> newsList = newsService.selectNewsByCategory(category, limit);
            List<NewsListVO> voList = newsConverter.toListVO(newsList);
            return R.ok(voList);
        } catch (Exception e) {
            log.error("根据分类获取新闻失败", e);
            return R.error("获取新闻失败");
        }
    }

    /**
     * 搜索新闻
     * 基于master分支的接口设计
     */
    @ApiOperation("搜索新闻")
    @GetMapping("/search")
    public R<List<NewsListVO>> searchNews(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") Integer limit) {
        try {
            List<News> newsList = newsService.searchNews(keyword, limit);
            List<NewsListVO> voList = newsConverter.toListVO(newsList);
            return R.ok(voList);
        } catch (Exception e) {
            log.error("搜索新闻失败", e);
            return R.error("搜索新闻失败");
        }
    }

    /**
     * 获取新闻详情
     * 基于master分支的接口设计
     */
    @ApiOperation("获取新闻详情")
    @GetMapping("/{id}")
    public R<NewsDetailVO> getNewsDetail(@PathVariable Long id) {
        try {
            News news = newsService.selectNewsById(id);
            if (news != null) {
                // 增加浏览量
                newsService.incrementViewCount(id);
                // 更新返回对象中的浏览量
                news.setViewCount(news.getViewCount() + 1);
                NewsDetailVO vo = newsConverter.toDetailVO(news);
                return R.ok(vo);
            } else {
                return R.error("新闻不存在");
            }
        } catch (Exception e) {
            log.error("获取新闻详情失败", e);
            return R.error("获取新闻详情失败");
        }
    }

    /**
     * 获取新闻列表
     * 基于master分支的接口设计
     */
    @ApiOperation("获取新闻列表")
    @GetMapping("/list")
    public R<List<NewsListVO>> getNewsList(NewsQueryDTO queryDTO) {
        try {
            News queryNews = new News();
            queryNews.setCategory(queryDTO.getCategory());
            queryNews.setAuthor(queryDTO.getAuthor());
            queryNews.setIsPublished(queryDTO.getIsPublished());
            queryNews.setIsFeatured(queryDTO.getIsFeatured());
            
            List<News> newsList = newsService.selectNewsList(queryNews);
            List<NewsListVO> voList = newsConverter.toListVO(newsList);
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取新闻列表失败", e);
            return R.error("获取新闻列表失败");
        }
    }

    /**
     * 分页获取新闻列表
     * 基于master分支的接口设计，支持搜索和筛选
     */
    @ApiOperation("分页获取新闻列表")
    @GetMapping
    public R<Object> getNewsWithPagination(NewsQueryDTO queryDTO) {

        try {
            // 获取所有新闻
            List<News> allNews = newsService.selectNewsList(new News());
            log.info("📊 从数据库获取到 {} 条新闻", allNews.size());

            // 筛选
            if (queryDTO.getCategory() != null && !queryDTO.getCategory().isEmpty() && !queryDTO.getCategory().equals("all")) {
                final String category = queryDTO.getCategory();
                allNews = allNews.stream()
                        .filter(news -> news.getCategory() != null && news.getCategory().contains(category))
                        .collect(Collectors.toList());
            }

            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
                final String keyword = queryDTO.getKeyword();
                allNews = allNews.stream()
                        .filter(news ->
                            (news.getTitle() != null && news.getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                            (news.getSummary() != null && news.getSummary().toLowerCase().contains(keyword.toLowerCase())) ||
                            (news.getContent() != null && news.getContent().toLowerCase().contains(keyword.toLowerCase()))
                        )
                        .collect(Collectors.toList());
            }

            // 按发布状态筛选
            if (queryDTO.getIsPublished() != null) {
                final Integer isPublished = queryDTO.getIsPublished();
                allNews = allNews.stream()
                        .filter(news -> news.getIsPublished() != null && news.getIsPublished().equals(isPublished))
                        .collect(Collectors.toList());
            }

            // 按发布时间倒序排序
            allNews.sort((a, b) -> {
                if (a.getPublishTime() == null) return 1;
                if (b.getPublishTime() == null) return -1;
                return b.getPublishTime().compareTo(a.getPublishTime());
            });

            // 分页
            int total = allNews.size();
            int page = queryDTO.getPage();
            int size = queryDTO.getSize();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, total);
            List<News> pageData = allNews.subList(startIndex, endIndex);

            // 转换为VO
            List<NewsListVO> voList = newsConverter.toListVO(pageData);

            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("records", voList);
            result.put("total", total);
            result.put("current", page);
            result.put("size", size);
            result.put("pages", (int) Math.ceil((double) total / size));

            return R.ok(result);
        } catch (Exception e) {
            log.error("分页获取新闻列表失败", e);
            return R.error("获取新闻列表失败：" + e.getMessage());
        }
    }

    /**
     * 添加新闻
     * 基于master分支的接口设计
     * 权限控制：super_admin(1), manager(7) - 社团负责人可发布新闻
     */
    @ApiOperation("添加新闻")
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('content:news:add')")
    public R<String> addNews(@RequestBody @Validated NewsSaveDTO saveDTO) {
        try {
            News news = newsConverter.toEntity(saveDTO);
            int result = newsService.insertNews(news);
            if (result > 0) {
                log.info("✅ 添加新闻成功");
                return R.ok("添加新闻成功");
            } else {
                return R.error("添加新闻失败");
            }
        } catch (Exception e) {
            log.error("添加新闻失败", e);
            return R.error("添加新闻失败：" + e.getMessage());
        }
    }

    /**
     * 修改新闻
     * 基于master分支的接口设计
     * 权限控制：super_admin(1), manager(7) - 社团负责人可编辑新闻
     */
    @ApiOperation("修改新闻")
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('content:news:edit')")
    public R<String> updateNews(@RequestBody @Validated NewsUpdateDTO updateDTO) {
        try {
            News news = newsConverter.toEntity(updateDTO);
            int result = newsService.updateNews(news);
            if (result > 0) {
                log.info("✅ 修改新闻成功，ID: {}", updateDTO.getId());
                return R.ok("修改新闻成功");
            } else {
                return R.error("修改新闻失败");
            }
        } catch (Exception e) {
            log.error("修改新闻失败", e);
            return R.error("修改新闻失败：" + e.getMessage());
        }
    }

    /**
     * 删除新闻
     * 基于master分支的接口设计
     */
    @ApiOperation("删除新闻")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('content:news:remove')")
    public R<String> deleteNews(@PathVariable Long id) {
        try {
            int result = newsService.deleteNewsById(id);
            if (result > 0) {
                log.info("✅ 删除新闻成功，ID: {}", id);
                return R.ok("删除新闻成功");
            } else {
                return R.error("删除新闻失败");
            }
        } catch (Exception e) {
            log.error("删除新闻失败，ID: {}", id, e);
            return R.error("删除新闻失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除新闻
     * 基于master分支的接口设计
     */
    @ApiOperation("批量删除新闻")
    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('content:news:remove')")
    public R<String> deleteNewsByIds(@RequestBody List<Long> ids) {
        try {
            int result = newsService.deleteNewsByIds(ids);
            if (result > 0) {
                log.info("✅ 批量删除新闻成功，删除数量: {}", result);
                return R.ok("批量删除新闻成功");
            } else {
                return R.error("批量删除新闻失败");
            }
        } catch (Exception e) {
            log.error("批量删除新闻失败", e);
            return R.error("批量删除新闻失败：" + e.getMessage());
        }
    }

    /**
     * 获取新闻统计信息
     * 基于master分支的接口设计
     */
    @ApiOperation("获取新闻统计信息")
    @GetMapping("/statistics")
    public R<Object> getNewsStatistics() {
        try {
            int totalNews = newsService.countNews();
            return R.ok(totalNews);
        } catch (Exception e) {
            log.error("获取新闻统计信息失败", e);
            return R.error("获取统计信息失败");
        }
    }

    /**
     * 获取待审核新闻列表
     * 权限：super_admin, manager（可查看）, teacher（可审核）
     */
    @ApiOperation("获取待审核新闻列表")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('content:news:audit', 'content:news:view')")
    public R<List<NewsListVO>> getPendingNews() {
        try {
            List<News> pendingList = newsService.selectPendingNews();
            List<NewsListVO> voList = newsConverter.toListVO(pendingList);
            log.info("📋 获取待审核新闻列表，数量: {}", voList.size());
            return R.ok(voList);
        } catch (Exception e) {
            log.error("获取待审核新闻列表失败", e);
            return R.error("获取待审核列表失败：" + e.getMessage());
        }
    }

    /**
     * 审核通过新闻
     * 权限：super_admin, teacher
     */
    @ApiOperation("审核通过新闻")
    @PutMapping("/approve")
    @PreAuthorize("hasAuthority('content:news:approve')")
    public R<Void> approveNews(@RequestBody @Validated NewsApproveDTO approveDTO) {
        try {
            newsService.approveNews(approveDTO.getId(), approveDTO.getRemark());
            log.info("✅ 新闻审核通过，ID: {}", approveDTO.getId());
            return R.ok();
        } catch (Exception e) {
            log.error("新闻审核失败，ID: {}", approveDTO.getId(), e);
            return R.error("审核失败：" + e.getMessage());
        }
    }

    /**
     * 审核拒绝新闻
     * 权限：super_admin, teacher
     */
    @ApiOperation("审核拒绝新闻")
    @PutMapping("/reject")
    @PreAuthorize("hasAuthority('content:news:reject')")
    public R<Void> rejectNews(@RequestBody @Validated NewsRejectDTO rejectDTO) {
        try {
            newsService.rejectNews(rejectDTO.getId(), rejectDTO.getRejectReason());
            log.info("❌ 新闻审核拒绝，ID: {}, 原因: {}", rejectDTO.getId(), rejectDTO.getRejectReason());
            return R.ok();
        } catch (Exception e) {
            log.error("新闻拒绝失败，ID: {}", rejectDTO.getId(), e);
            return R.error("拒绝失败：" + e.getMessage());
        }
    }

    @ApiOperation("负责人审核新闻")
    @PostMapping("/{id}/review/manager")
    @PreAuthorize("hasAnyAuthority('content:news:edit', 'content:news:approve')")
    public R<Void> reviewNewsByManager(
            @PathVariable Long id,
            @RequestBody(required = false) NewsApproveDTO approveDTO,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!Objects.equals(roleId, 1L) && !Objects.equals(roleId, 7L)) {
                return R.error("no manager review permission");
            }
            String remark = approveDTO == null ? null : approveDTO.getRemark();
            boolean approved = approveDTO == null || !Boolean.FALSE.equals(approveDTO.getApproved());
            boolean success = newsService.reviewNewsByManager(id, approved, remark, UserContext.getUser(), Objects.equals(roleId, 1L));
            return success ? R.ok() : R.error("manager review failed");
        } catch (Exception e) {
            log.error("manager review news failed, id: {}", id, e);
            return R.error("manager review failed: " + e.getMessage());
        }
    }

    @ApiOperation("指导老师审核新闻")
    @PostMapping("/{id}/review/teacher")
    @PreAuthorize("hasAuthority('content:news:approve')")
    public R<Void> reviewNewsByTeacher(
            @PathVariable Long id,
            @RequestBody(required = false) NewsApproveDTO approveDTO,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!Objects.equals(roleId, 1L) && !Objects.equals(roleId, 8L)) {
                return R.error("no teacher review permission");
            }
            String remark = approveDTO == null ? null : approveDTO.getRemark();
            boolean approved = approveDTO == null || !Boolean.FALSE.equals(approveDTO.getApproved());
            boolean success = newsService.reviewNewsByTeacher(id, approved, remark, UserContext.getUser(), Objects.equals(roleId, 1L));
            return success ? R.ok() : R.error("teacher review failed");
        } catch (Exception e) {
            log.error("teacher review news failed, id: {}", id, e);
            return R.error("teacher review failed: " + e.getMessage());
        }
    }
}
