package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper接口
 *
 * @author RK-Web
 * @since 2026-03-29
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
