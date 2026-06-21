package com.tianji.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.activity.domain.po.ActivityCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 活动分类Mapper接口
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Mapper
public interface ActivityCategoryMapper extends BaseMapper<ActivityCategory> {

    /**
     * 根据父ID查询子分类列表
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Select("SELECT * FROM rk_activity_category WHERE parent_id = #{parentId} AND status = 1 AND is_deleted = 0 ORDER BY sort ASC")
    List<ActivityCategory> findByParentId(@Param("parentId") Long parentId);

    /**
     * 查询所有启用的分类
     *
     * @return 分类列表
     */
    @Select("SELECT * FROM rk_activity_category WHERE status = 1 AND is_deleted = 0 ORDER BY sort ASC")
    List<ActivityCategory> findAllEnabled();

    /**
     * 检查分类名称是否存在
     *
     * @param parentId 父分类ID
     * @param categoryName 分类名称
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) FROM rk_activity_category WHERE parent_id = #{parentId} AND category_name = #{categoryName} AND is_deleted = 0")
    int countByName(@Param("parentId") Long parentId, @Param("categoryName") String categoryName);
}
