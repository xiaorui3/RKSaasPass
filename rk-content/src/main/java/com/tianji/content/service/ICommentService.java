package com.tianji.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.content.domain.dto.CommentCreateDTO;
import com.tianji.content.domain.po.Comment;
import com.tianji.content.domain.vo.CommentVO;

import java.util.List;

public interface ICommentService extends IService<Comment> {
    List<CommentVO> listVisibleComments(String targetType, Long targetId);

    CommentVO createComment(CommentCreateDTO dto);
}
