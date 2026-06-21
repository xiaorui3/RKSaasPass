package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.JoinRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社团成员申请Mapper接口
 * 基于master分支的join_requests表
 */
@Mapper
public interface JoinRequestMapper extends BaseMapper<JoinRequest> {
}