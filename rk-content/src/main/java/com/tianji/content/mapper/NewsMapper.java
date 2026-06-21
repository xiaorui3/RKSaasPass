package com.tianji.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.content.domain.po.News;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新闻信息Mapper接口
 * 基于master分支的news表
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {
}