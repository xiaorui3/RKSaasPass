package com.tianji.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.activity.domain.po.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 活动Mapper接口
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 增加浏览次数
     *
     * @param id 活动ID
     * @return 影响行数
     */
    @Update("UPDATE rk_activity SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 增加参与人数
     *
     * @param id 活动ID
     * @return 影响行数
     */
    @Update("UPDATE rk_activity SET current_participants = current_participants + 1 WHERE id = #{id} AND current_participants < max_participants")
    int incrementParticipants(@Param("id") Long id);

    /**
     * 减少参与人数
     *
     * @param id 活动ID
     * @return 影响行数
     */
    @Update("UPDATE rk_activity SET current_participants = current_participants - 1 WHERE id = #{id} AND current_participants > 0")
    int decrementParticipants(@Param("id") Long id);
}
