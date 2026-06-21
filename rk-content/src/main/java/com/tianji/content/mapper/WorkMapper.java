package com.tianji.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.content.domain.po.Work;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作品展示Mapper接口
 * 基于master分支的works表
 */
@Mapper
public interface WorkMapper extends BaseMapper<Work> {
}