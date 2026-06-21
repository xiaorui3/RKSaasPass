package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 通知Mapper接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Update("UPDATE rk_notification SET is_deleted = 1, updater = #{updater}, update_time = #{updateTime} WHERE id = #{id} AND is_deleted = 0")
    int softDeleteById(@Param("id") Long id, @Param("updater") Long updater, @Param("updateTime") LocalDateTime updateTime);
}
