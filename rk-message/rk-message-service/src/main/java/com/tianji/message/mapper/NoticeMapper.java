package com.tianji.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.message.domain.po.Notice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}