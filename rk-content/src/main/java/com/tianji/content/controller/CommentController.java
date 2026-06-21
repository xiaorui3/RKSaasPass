package com.tianji.content.controller;

import com.tianji.common.domain.R;
import com.tianji.content.domain.dto.CommentCreateDTO;
import com.tianji.content.domain.vo.CommentVO;
import com.tianji.content.service.ICommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final ICommentService commentService;

    @GetMapping
    public R<List<CommentVO>> listComments(
            @RequestParam @NotBlank String targetType,
            @RequestParam @NotNull Long targetId) {
        try {
            return R.ok(commentService.listVisibleComments(targetType, targetId));
        } catch (Exception e) {
            log.error("list comments failed, targetType={}, targetId={}", targetType, targetId, e);
            return R.error("获取评论失败：" + e.getMessage());
        }
    }

    @PostMapping
    public R<CommentVO> createComment(@RequestBody @Valid CommentCreateDTO dto) {
        try {
            return R.ok(commentService.createComment(dto));
        } catch (Exception e) {
            log.error("create comment failed, targetType={}, targetId={}", dto.getTargetType(), dto.getTargetId(), e);
            return R.error("发表评论失败：" + e.getMessage());
        }
    }
}
