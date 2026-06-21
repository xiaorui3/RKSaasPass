package com.tianji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.user.domain.po.Navigation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导航菜单Mapper接口
 */
@Mapper
public interface NavigationMapper extends BaseMapper<Navigation> {
}
