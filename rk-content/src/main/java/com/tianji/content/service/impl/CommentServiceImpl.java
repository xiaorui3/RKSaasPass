package com.tianji.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.content.domain.dto.CommentCreateDTO;
import com.tianji.content.domain.po.Comment;
import com.tianji.content.domain.vo.CommentVO;
import com.tianji.content.mapper.CommentMapper;
import com.tianji.content.service.ICommentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {

    private final UserClient userClient;

    public CommentServiceImpl(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    public List<CommentVO> listVisibleComments(String targetType, Long targetId) {
        String normalizedType = normalizeTargetType(targetType);
        if (targetId == null) {
            throw new IllegalArgumentException("targetId is required");
        }
        return baseMapper.selectList(new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetType, normalizedType)
                        .eq(Comment::getTargetId, targetId)
                        .eq(Comment::getStatus, Comment.STATUS_VISIBLE)
                        .eq(Comment::getIsDeleted, 0)
                        .orderByDesc(Comment::getFeatured)
                        .orderByDesc(Comment::getCreateTime))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public CommentVO createComment(CommentCreateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("comment payload is required");
        }
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("comment content is required");
        }

        LocalDateTime now = LocalDateTime.now();
        Long userId = UserContext.getUser() != null ? UserContext.getUser() : dto.getUserId();
        UserDTO profile = resolveUserProfile(userId);
        Comment comment = new Comment()
                .setTenantId(TenantContext.getTenantId())
                .setTargetType(normalizeTargetType(dto.getTargetType()))
                .setTargetId(dto.getTargetId())
                .setUserId(userId)
                .setUserName(resolveUserName(dto.getUserName(), profile, userId))
                .setUserAvatar(resolveUserAvatar(dto.getUserAvatar(), profile))
                .setContent(content)
                .setFeatured(false)
                .setStatus(Comment.STATUS_VISIBLE)
                .setIsDeleted(0)
                .setCreateTime(now)
                .setUpdateTime(now);
        baseMapper.insert(comment);
        return toVO(comment);
    }

    private String normalizeTargetType(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!"news".equals(normalized) && !"activity".equals(normalized)) {
            throw new IllegalArgumentException("unsupported comment target type: " + value);
        }
        return normalized;
    }

    private UserDTO resolveUserProfile(Long authUserId) {
        if (authUserId == null || userClient == null) {
            return null;
        }
        try {
            List<UserDTO> users = userClient.queryUsersByAuthIds(Collections.singletonList(authUserId));
            if (users == null || users.isEmpty()) {
                return null;
            }
            return users.stream()
                    .filter(user -> user != null)
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveUserName(String userName, UserDTO profile, Long userId) {
        if (StringUtils.hasText(userName)) {
            return userName.trim();
        }
        if (profile != null) {
            if (StringUtils.hasText(profile.getName())) {
                return profile.getName().trim();
            }
            if (StringUtils.hasText(profile.getUsername())) {
                return profile.getUsername().trim();
            }
        }
        return userId == null ? "匿名用户" : "用户-" + userId;
    }

    private String resolveUserAvatar(String avatar, UserDTO profile) {
        if (StringUtils.hasText(avatar)) {
            return avatar.trim();
        }
        return profile == null || !StringUtils.hasText(profile.getIcon()) ? null : profile.getIcon().trim();
    }

    private CommentVO toVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setTenantId(comment.getTenantId());
        vo.setTargetType(comment.getTargetType());
        vo.setTargetId(comment.getTargetId());
        vo.setUserId(comment.getUserId());
        vo.setUserName(comment.getUserName());
        vo.setUserAvatar(comment.getUserAvatar());
        vo.setContent(comment.getContent());
        vo.setFeatured(Boolean.TRUE.equals(comment.getFeatured()));
        vo.setStatus(comment.getStatus());
        vo.setDr(comment.getIsDeleted() == null ? 0L : comment.getIsDeleted().longValue());
        vo.setCreateTime(comment.getCreateTime());
        vo.setUpdateTime(comment.getUpdateTime());
        return vo;
    }
}
