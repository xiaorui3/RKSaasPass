package com.tianji.activity.controller;

import com.tianji.activity.domain.po.ActivityCategory;
import com.tianji.activity.service.IActivityCategoryService;
import com.tianji.common.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动分类管理控制器
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Slf4j
@Api(tags = "活动分类管理接口")
@RestController
@RequestMapping("/api/activity/category")
@RequiredArgsConstructor
public class ActivityCategoryController {

    private final IActivityCategoryService categoryService;

    /**
     * 获取所有分类列表
     */
    @ApiOperation("获取所有活动分类")
    @GetMapping("/list")
    public R<List<ActivityCategory>> getAllCategories() {
        try {
            List<ActivityCategory> categories = categoryService.getAllCategories();
            return R.ok(categories);
        } catch (Exception e) {
            log.error("获取活动分类列表失败", e);
            return R.error("获取活动分类列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取分类树结构
     */
    @ApiOperation("获取活动分类树")
    @GetMapping("/tree")
    public R<List<ActivityCategory>> getCategoryTree() {
        try {
            List<ActivityCategory> tree = categoryService.getCategoryTree();
            return R.ok(tree);
        } catch (Exception e) {
            log.error("获取活动分类树失败", e);
            return R.error("获取活动分类树失败：" + e.getMessage());
        }
    }

    /**
     * 获取子分类列表
     */
    @ApiOperation("获取子分类列表")
    @GetMapping("/children/{parentId}")
    public R<List<ActivityCategory>> getChildren(@PathVariable Long parentId) {
        try {
            List<ActivityCategory> children = categoryService.getCategoriesByParentId(parentId);
            return R.ok(children);
        } catch (Exception e) {
            log.error("获取子分类列表失败", e);
            return R.error("获取子分类列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取分类详情
     */
    @ApiOperation("获取分类详情")
    @GetMapping("/{id}")
    public R<ActivityCategory> getCategoryDetail(@PathVariable Long id) {
        try {
            ActivityCategory category = categoryService.getCategoryById(id);
            if (category == null) {
                return R.error("分类不存在");
            }
            return R.ok(category);
        } catch (Exception e) {
            log.error("获取分类详情失败", e);
            return R.error("获取分类详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建分类
     */
    @ApiOperation("创建活动分类")
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('content:category:add')")
    public R<Long> createCategory(@RequestBody ActivityCategory category) {
        try {
            if (category.getCategoryName() == null || category.getCategoryName().isEmpty()) {
                return R.error("分类名称不能为空");
            }
            Long id = categoryService.createCategory(category);
            log.info("✅ 创建活动分类成功，ID: {}", id);
            return R.ok(id);
        } catch (Exception e) {
            log.error("创建活动分类失败", e);
            return R.error("创建活动分类失败：" + e.getMessage());
        }
    }

    /**
     * 更新分类
     */
    @ApiOperation("更新活动分类")
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('content:category:edit')")
    public R<String> updateCategory(@RequestBody ActivityCategory category) {
        try {
            if (category.getId() == null) {
                return R.error("分类ID不能为空");
            }
            int result = categoryService.updateCategory(category);
            if (result > 0) {
                log.info("✅ 更新活动分类成功，ID: {}", category.getId());
                return R.ok("更新分类成功");
            }
            return R.error("更新分类失败");
        } catch (Exception e) {
            log.error("更新活动分类失败", e);
            return R.error("更新活动分类失败：" + e.getMessage());
        }
    }

    /**
     * 删除分类
     */
    @ApiOperation("删除活动分类")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('content:category:remove')")
    public R<String> deleteCategory(@PathVariable Long id) {
        try {
            int result = categoryService.deleteCategory(id);
            if (result == -1) {
                return R.error("该分类下存在子分类，无法删除");
            }
            if (result > 0) {
                log.info("✅ 删除活动分类成功，ID: {}", id);
                return R.ok("删除分类成功");
            }
            return R.error("删除分类失败");
        } catch (Exception e) {
            log.error("删除活动分类失败", e);
            return R.error("删除活动分类失败：" + e.getMessage());
        }
    }
}
