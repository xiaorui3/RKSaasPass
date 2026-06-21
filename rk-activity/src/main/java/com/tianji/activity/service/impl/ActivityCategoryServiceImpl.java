package com.tianji.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.activity.domain.po.ActivityCategory;
import com.tianji.activity.mapper.ActivityCategoryMapper;
import com.tianji.activity.service.IActivityCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 活动分类服务实现类
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityCategoryServiceImpl implements IActivityCategoryService {

    private final ActivityCategoryMapper categoryMapper;

    @Override
    public List<ActivityCategory> getAllCategories() {
        return categoryMapper.findAllEnabled();
    }

    @Override
    public List<ActivityCategory> getCategoriesByParentId(Long parentId) {
        if (parentId == null) {
            parentId = ActivityCategory.ROOT_PARENT_ID;
        }
        return categoryMapper.findByParentId(parentId);
    }

    @Override
    public ActivityCategory getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    @Transactional
    public Long createCategory(ActivityCategory category) {
        // 设置默认值
        if (category.getParentId() == null) {
            category.setParentId(ActivityCategory.ROOT_PARENT_ID);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(ActivityCategory.STATUS_ENABLED);
        }
        
        categoryMapper.insert(category);
        log.info("✅ 创建活动分类成功，ID: {}", category.getId());
        return category.getId();
    }

    @Override
    @Transactional
    public int updateCategory(ActivityCategory category) {
        int result = categoryMapper.updateById(category);
        if (result > 0) {
            log.info("✅ 更新活动分类成功，ID: {}", category.getId());
        }
        return result;
    }

    @Override
    @Transactional
    public int deleteCategory(Long id) {
        // 检查是否有子分类
        List<ActivityCategory> children = categoryMapper.findByParentId(id);
        if (!children.isEmpty()) {
            log.warn("❌ 删除活动分类失败，存在子分类，ID: {}", id);
            return -1;
        }
        
        int result = categoryMapper.deleteById(id);
        if (result > 0) {
            log.info("✅ 删除活动分类成功，ID: {}", id);
        }
        return result;
    }

    @Override
    public List<ActivityCategory> getCategoryTree() {
        // 获取所有启用的分类
        List<ActivityCategory> allCategories = categoryMapper.findAllEnabled();
        
        // 构建树结构
        Map<Long, List<ActivityCategory>> parentMap = allCategories.stream()
                .collect(Collectors.groupingBy(ActivityCategory::getParentId));
        
        // 获取顶级分类
        List<ActivityCategory> rootCategories = parentMap.getOrDefault(
                ActivityCategory.ROOT_PARENT_ID, new ArrayList<>());
        
        // 递归设置子分类
        buildCategoryTree(rootCategories, parentMap);
        
        return rootCategories;
    }

    /**
     * 递归构建分类树
     */
    private void buildCategoryTree(List<ActivityCategory> categories, 
                                    Map<Long, List<ActivityCategory>> parentMap) {
        if (categories == null) return;
        
        for (ActivityCategory category : categories) {
            List<ActivityCategory> children = parentMap.get(category.getId());
            if (children != null && !children.isEmpty()) {
                // 如果有子分类，递归处理
                buildCategoryTree(children, parentMap);
            }
        }
    }
}
