package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.NotificationRead;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知阅读状态Mapper接口
 */
@Mapper
public interface NotificationReadMapper extends BaseMapper<NotificationRead> {
}
