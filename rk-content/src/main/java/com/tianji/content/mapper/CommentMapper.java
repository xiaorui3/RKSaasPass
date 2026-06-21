package com.tianji.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.content.domain.po.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
