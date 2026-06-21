package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.Banner;
import org.apache.ibatis.annotations.Mapper;

/**
 * 轮播图Mapper接口
 */
@Mapper
public interface BannerMapper extends BaseMapper<Banner> {
}
