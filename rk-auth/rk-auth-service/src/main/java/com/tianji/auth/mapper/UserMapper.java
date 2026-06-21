package com.tianji.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.auth.domain.po.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}