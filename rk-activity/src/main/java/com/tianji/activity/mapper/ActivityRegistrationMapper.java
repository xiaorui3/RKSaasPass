package com.tianji.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.activity.domain.po.ActivityRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 活动报名Mapper接口
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Mapper
public interface ActivityRegistrationMapper extends BaseMapper<ActivityRegistration> {

    /**
     * 根据活动ID和用户ID查询报名记录
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 报名记录
     */
    @Select("SELECT * FROM rk_activity_registration WHERE activity_id = #{activityId} AND user_id = #{userId} AND is_deleted = 0")
    ActivityRegistration findByActivityIdAndUserId(@Param("activityId") Long activityId, @Param("userId") Long userId);

    /**
     * 统计活动的报名人数
     *
     * @param activityId 活动ID
     * @return 报名人数
     */
    @Select("SELECT COUNT(*) FROM rk_activity_registration WHERE activity_id = #{activityId} AND registration_status != 3 AND is_deleted = 0")
    int countByActivityId(@Param("activityId") Long activityId);

    /**
     * 根据用户ID查询报名记录列表
     *
     * @param userId 用户ID
     * @return 报名记录列表
     */
    @Select("SELECT * FROM rk_activity_registration WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY registration_time DESC")
    List<ActivityRegistration> findByUserId(@Param("userId") Long userId);
}
