package com.tianji.activity.service;

import com.tianji.activity.domain.po.ActivityCategory;

import java.util.List;

/**
 * 活动分类服务接口
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
public interface IActivityCategoryService {

    /**
     * 获取所有启用的分类
     *
     * @return 分类列表
     */
    List<ActivityCategory> getAllCategories();

    /**
     * 根据父ID获取子分类
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<ActivityCategory> getCategoriesByParentId(Long parentId);

    /**
     * 根据ID获取分类详情
     *
     * @param id 分类ID
     * @return 分类详情
     */
    ActivityCategory getCategoryById(Long id);

    /**
     * 创建分类
     *
     * @param category 分类信息
     * @return 分类ID
     */
    Long createCategory(ActivityCategory category);

    /**
     * 更新分类
     *
     * @param category 分类信息
     * @return 影响行数
     */
    int updateCategory(ActivityCategory category);

    /**
     * 删除分类
     *
     * @param id 分类ID
     * @return 影响行数
     */
    int deleteCategory(Long id);

    /**
     * 获取分类树结构
     *
     * @return 分类树
     */
    List<ActivityCategory> getCategoryTree();
}
