package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.HistoryEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社团历史事件Mapper接口
 * 基于master分支的club_history表
 */
@Mapper
public interface HistoryEventMapper extends BaseMapper<HistoryEvent> {
}