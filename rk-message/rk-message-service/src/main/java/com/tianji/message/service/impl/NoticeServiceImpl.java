package com.tianji.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.api.dto.user.NotificationInternalSaveDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.autoconfigure.media.MediaPathHelper;
import com.tianji.common.utils.SearchIndexSyncUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.message.domain.dto.NoticeDTO;
import com.tianji.message.domain.po.Notice;
import com.tianji.message.mapper.NoticeMapper;
import com.tianji.message.service.INoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements INoticeService {

    private final MediaPathHelper mediaPathHelper;
    private final UserClient userClient;
    private final SearchClient searchClient;

    private static final String SEARCH_ENTITY_TYPE_NOTICE = "NOTICE";

    @Override
    public void createNotice(NoticeDTO noticeDTO) {
        if (noticeDTO == null) {
            throw new IllegalArgumentException("notice is required");
        }
        if (noticeDTO.getManagerReviewerId() == null || noticeDTO.getTeacherReviewerId() == null) {
            throw new IllegalArgumentException("manager and teacher approvers are required");
        }
        Notice notice = new Notice();
        BeanUtils.copyProperties(noticeDTO, notice);
        if (noticeDTO.getPriority() != null) {
            notice.setNoticeLevel(noticeDTO.getPriority());
        }
        normalizeNoticeMediaForStorage(notice);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        notice.setViewCount(0);
        boolean requestPublish = Boolean.TRUE.equals(noticeDTO.getIsPublished());
        notice.setIsPublished(requestPublish);
        notice.setPublishTime(requestPublish ? LocalDateTime.now() : null);
        notice.setManagerReviewStatus(Notice.REVIEW_PENDING);
        notice.setManagerReviewComment(null);
        notice.setManagerReviewTime(null);
        notice.setTeacherReviewStatus(Notice.REVIEW_PENDING);
        notice.setTeacherReviewComment(null);
        notice.setTeacherReviewTime(null);
        if (notice.getTenantId() == null) {
            Long tenantId = TenantContext.getTenantId();
            notice.setTenantId(tenantId != null ? tenantId : 1L);
        }

        baseMapper.insert(notice);
        syncNoticeSearchIndex(notice.getId());
        if (Boolean.TRUE.equals(notice.getIsPublished())) {
            sendNoticePublishedNotification(notice);
        }
        log.info("notice created: {}, published={}", notice.getTitle(), notice.getIsPublished());
    }

    @Override
    public void updateNotice(Long id, NoticeDTO noticeDTO) {
        Notice notice = baseMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("notice not found");
        }

        applyNoticeUpdates(notice, noticeDTO);
        normalizeNoticeMediaForStorage(notice);
        notice.setUpdateTime(LocalDateTime.now());

        baseMapper.updateById(notice);
        syncNoticeSearchIndex(notice.getId());
        log.info("notice updated: {}", id);
    }

    @Override
    public void deleteNotice(Long id) {
        Notice notice = baseMapper.selectById(id);
        if (notice != null) {
            Long tenantId = notice.getTenantId();
            baseMapper.deleteById(id);
            deleteNoticeSearchIndex(id, tenantId);
            log.info("notice deleted: {}", id);
        }
    }

    @Override
    public void publishNotice(Long id) {
        Notice notice = baseMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("notice not found");
        }

        notice.setIsPublished(true);
        notice.setPublishTime(LocalDateTime.now());
        notice.setManagerReviewStatus(Notice.REVIEW_APPROVED);
        notice.setTeacherReviewStatus(Notice.REVIEW_APPROVED);
        notice.setUpdateTime(LocalDateTime.now());

        baseMapper.updateById(notice);
        syncNoticeSearchIndex(notice.getId());
        if (Boolean.TRUE.equals(notice.getIsPublished())) {
            sendNoticePublishedNotification(notice);
        }
        log.info("notice published: {}", id);
    }

    @Override
    public void withdrawNotice(Long id) {
        Notice notice = baseMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("notice not found");
        }
        notice.setIsPublished(false);
        notice.setPublishTime(null);
        notice.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(notice);
        syncNoticeSearchIndex(notice.getId());
        log.info("notice withdrawn: {}", id);
    }

    @Override
    public void updateTopStatus(Long id, Boolean isTop) {
        Notice notice = baseMapper.selectById(id);
        if (notice == null) {
            throw new RuntimeException("notice not found");
        }
        notice.setIsTop(Boolean.TRUE.equals(isTop) ? 1 : 0);
        notice.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(notice);
        syncNoticeSearchIndex(notice.getId());
        log.info("notice top status updated: {}, isTop={}", id, isTop);
    }

    @Override
    public Notice getNoticeDetail(Long id) {
        Notice notice = baseMapper.selectById(id);
        if (notice != null) {
            incrementViewCount(id);
            expandNoticeMedia(notice);
        }
        return notice;
    }

    @Override
    public List<Notice> getNoticeList() {
        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Notice::getCreateTime);
        return expandNoticeMedia(baseMapper.selectList(queryWrapper));
    }

    @Override
    public PageDTO<Notice> getNoticePage(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(Math.max(1, pageSize), 100);
        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Notice::getCreateTime);
        Page<Notice> result = baseMapper.selectPage(new Page<>(safePage, safePageSize), queryWrapper);
        expandNoticeMedia(result.getRecords());
        return PageDTO.of(result);
    }

    @Override
    public List<Notice> getPublishedNotices() {
        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notice::getIsPublished, true)
                .eq(Notice::getManagerReviewStatus, Notice.REVIEW_APPROVED)
                .eq(Notice::getTeacherReviewStatus, Notice.REVIEW_APPROVED)
                .orderByDesc(Notice::getPublishTime);
        return expandNoticeMedia(baseMapper.selectList(queryWrapper));
    }

    @Override
    public boolean approveNoticeByManager(Long id, String remark, Long reviewerId, boolean allowProxyReview) {
        return reviewNoticeByManager(id, true, remark, reviewerId, allowProxyReview);
    }

    @Override
    public boolean reviewNoticeByManager(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview) {
        Notice notice = baseMapper.selectById(id);
        if (notice == null || !Objects.equals(notice.getManagerReviewStatus(), Notice.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, notice.getManagerReviewerId())) {
            return false;
        }
        notice.setManagerReviewStatus(approved ? Notice.REVIEW_APPROVED : Notice.REVIEW_REJECTED);
        notice.setManagerReviewComment(remark);
        notice.setManagerReviewTime(LocalDateTime.now());
        notice.setIsPublished(false);
        if (!approved) {
            notice.setPublishTime(null);
        }
        notice.setUpdateTime(LocalDateTime.now());
        boolean result = baseMapper.updateById(notice) > 0;
        if (result) {
            syncNoticeSearchIndex(notice.getId());
        }
        return result;
    }

    @Override
    public boolean approveNoticeByTeacher(Long id, String remark, Long reviewerId, boolean allowProxyReview) {
        return reviewNoticeByTeacher(id, true, remark, reviewerId, allowProxyReview);
    }

    @Override
    public boolean reviewNoticeByTeacher(Long id, boolean approved, String remark, Long reviewerId, boolean allowProxyReview) {
        Notice notice = baseMapper.selectById(id);
        if (notice == null
                || !Objects.equals(notice.getManagerReviewStatus(), Notice.REVIEW_APPROVED)
                || !Objects.equals(notice.getTeacherReviewStatus(), Notice.REVIEW_PENDING)) {
            return false;
        }
        if (!allowProxyReview && !Objects.equals(reviewerId, notice.getTeacherReviewerId())) {
            return false;
        }
        notice.setTeacherReviewStatus(approved ? Notice.REVIEW_APPROVED : Notice.REVIEW_REJECTED);
        notice.setTeacherReviewComment(remark);
        notice.setTeacherReviewTime(LocalDateTime.now());
        notice.setIsPublished(approved);
        notice.setPublishTime(approved ? LocalDateTime.now() : null);
        notice.setUpdateTime(LocalDateTime.now());
        boolean result = baseMapper.updateById(notice) > 0;
        if (result) {
            syncNoticeSearchIndex(notice.getId());
        }
        return result;
    }

    @Override
    public void incrementViewCount(Long id) {
        Notice notice = baseMapper.selectById(id);
        if (notice != null) {
            notice.setViewCount((notice.getViewCount() != null ? notice.getViewCount() : 0) + 1);
            baseMapper.updateById(notice);
        }
    }

    private void applyNoticeUpdates(Notice notice, NoticeDTO noticeDTO) {
        boolean reviewerChanged = false;
        if (noticeDTO.getTitle() != null) notice.setTitle(noticeDTO.getTitle());
        if (noticeDTO.getContent() != null) notice.setContent(noticeDTO.getContent());
        if (noticeDTO.getCoverImage() != null) notice.setCoverImage(noticeDTO.getCoverImage());
        if (noticeDTO.getAttachmentUrl() != null) notice.setAttachmentUrl(noticeDTO.getAttachmentUrl());
        if (noticeDTO.getNoticeType() != null) notice.setNoticeType(noticeDTO.getNoticeType());
        if (noticeDTO.getPriority() != null) notice.setNoticeLevel(noticeDTO.getPriority());
        if (noticeDTO.getIsPublished() != null) notice.setIsPublished(noticeDTO.getIsPublished());
        if (noticeDTO.getIsTop() != null) notice.setIsTop(noticeDTO.getIsTop());
        if (noticeDTO.getPublishTime() != null) notice.setPublishTime(noticeDTO.getPublishTime());
        if (noticeDTO.getTargetType() != null) notice.setTargetType(noticeDTO.getTargetType());
        if (noticeDTO.getTenantId() != null) notice.setTenantId(noticeDTO.getTenantId());
        if (noticeDTO.getManagerReviewerId() != null && !Objects.equals(notice.getManagerReviewerId(), noticeDTO.getManagerReviewerId())) {
            notice.setManagerReviewerId(noticeDTO.getManagerReviewerId());
            reviewerChanged = true;
        }
        if (noticeDTO.getTeacherReviewerId() != null && !Objects.equals(notice.getTeacherReviewerId(), noticeDTO.getTeacherReviewerId())) {
            notice.setTeacherReviewerId(noticeDTO.getTeacherReviewerId());
            reviewerChanged = true;
        }
        if (reviewerChanged) {
            notice.setManagerReviewStatus(Notice.REVIEW_PENDING);
            notice.setManagerReviewComment(null);
            notice.setManagerReviewTime(null);
            notice.setTeacherReviewStatus(Notice.REVIEW_PENDING);
            notice.setTeacherReviewComment(null);
            notice.setTeacherReviewTime(null);
            notice.setIsPublished(false);
            notice.setPublishTime(null);
        }
    }

    private void normalizeNoticeMediaForStorage(Notice notice) {
        notice.setCoverImage(mediaPathHelper.normalizeForStorage(notice.getCoverImage()));
        notice.setAttachmentUrl(mediaPathHelper.normalizeForStorage(notice.getAttachmentUrl()));
    }

    private List<Notice> expandNoticeMedia(List<Notice> notices) {
        return notices.stream().map(this::expandNoticeMedia).collect(Collectors.toList());
    }

    private Notice expandNoticeMedia(Notice notice) {
        if (notice == null) {
            return null;
        }
        notice.setCoverImage(mediaPathHelper.toPublicUrl(notice.getCoverImage()));
        notice.setAttachmentUrl(mediaPathHelper.toPublicUrl(notice.getAttachmentUrl()));
        return notice;
    }

    private boolean isFullyApproved(Notice notice) {
        return notice != null
                && Objects.equals(notice.getManagerReviewStatus(), Notice.REVIEW_APPROVED)
                && Objects.equals(notice.getTeacherReviewStatus(), Notice.REVIEW_APPROVED);
    }

    private boolean isPublicNotice(Notice notice) {
        return notice != null
                && Boolean.TRUE.equals(notice.getIsPublished())
                && isFullyApproved(notice)
                && !Integer.valueOf(1).equals(notice.getIsDeleted());
    }

    private void syncNoticeSearchIndex(Long id) {
        if (id == null) {
            return;
        }
        SearchIndexSyncUtils.runAfterCommit(() -> {
            Notice notice = null;
            try {
                notice = baseMapper.selectById(id);
                if (notice == null || !isPublicNotice(notice)) {
                    searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_NOTICE, id,
                            notice == null ? TenantContext.getTenantId() : notice.getTenantId());
                    return;
                }
                searchClient.upsertGlobalDocument(buildNoticeSearchDocument(notice));
            } catch (Exception e) {
                log.warn("sync notice global search index failed, noticeId={}, tenantId={}, reason={}",
                        id, notice == null ? null : notice.getTenantId(), e.getMessage());
            }
        });
    }

    private void deleteNoticeSearchIndex(Long id, Long tenantId) {
        if (id == null) {
            return;
        }
        Long resolvedTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        SearchIndexSyncUtils.runAfterCommit(() -> {
            try {
                searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_NOTICE, id, resolvedTenantId);
            } catch (Exception e) {
                log.warn("delete notice global search index failed, noticeId={}, tenantId={}, reason={}",
                        id, resolvedTenantId, e.getMessage());
            }
        });
    }

    @Override
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notice::getIsDeleted, 0)
                .eq(Notice::getIsPublished, true)
                .eq(Notice::getManagerReviewStatus, Notice.REVIEW_APPROVED)
                .eq(Notice::getTeacherReviewStatus, Notice.REVIEW_APPROVED)
                .orderByDesc(Notice::getUpdateTime);
        return baseMapper.selectList(queryWrapper).stream()
                .filter(this::isPublicNotice)
                .map(this::buildNoticeSearchDocument)
                .collect(Collectors.toList());
    }

    private GlobalSearchDocumentDTO buildNoticeSearchDocument(Notice notice) {
        return new GlobalSearchDocumentDTO()
                .setEntityType(SEARCH_ENTITY_TYPE_NOTICE)
                .setEntityId(notice.getId())
                .setTenantId(notice.getTenantId())
                .setTitle(notice.getTitle())
                .setSummary(notice.getContent())
                .setContent(joinSearchText(notice.getContent(), notice.getTargetType(), notice.getNoticeLevel()))
                .setTags(resolveNoticeTypeTag(notice.getNoticeType()))
                .setRoute("/notices")
                .setCoverUrl(notice.getCoverImage())
                .setUpdatedAt(formatUpdatedAt(notice.getUpdateTime(), notice.getPublishTime(), notice.getCreateTime()))
                .setVisible(isPublicNotice(notice));
    }

    private String joinSearchText(String... values) {
        if (values == null) {
            return null;
        }
        return java.util.Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private String resolveNoticeTypeTag(Integer noticeType) {
        if (noticeType == null) {
            return null;
        }
        switch (noticeType) {
            case 1:
                return "system notice";
            case 2:
                return "activity notice";
            case 3:
                return "competition notice";
            default:
                return "notice";
        }
    }

    private String formatUpdatedAt(LocalDateTime... times) {
        if (times == null) {
            return null;
        }
        for (LocalDateTime time : times) {
            if (time != null) {
                return time.toString();
            }
        }
        return null;
    }

    private void sendNoticePublishedNotification(Notice notice) {
        try {
            NotificationInternalSaveDTO dto = new NotificationInternalSaveDTO();
            dto.setTenantId(notice.getTenantId());
            dto.setTitle("新公告已发布");
            dto.setContent("公告《" + safeText(notice.getTitle()) + "》已发布，请及时查看。");
            dto.setType("message");
            dto.setPriority(resolveNoticePriority(notice.getNoticeLevel()));
            dto.setTargetType(0);
            dto.setSenderName("notice-service");
            dto.setMetadata(buildNoticePublishMetadata(notice));
            userClient.saveNotificationInternal(dto);
        } catch (Exception e) {
            log.warn("send notice published notification failed, noticeId={}, reason={}", notice == null ? null : notice.getId(), e.getMessage());
        }
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private int resolveNoticePriority(String noticeLevel) {
        if (noticeLevel == null || noticeLevel.isBlank()) {
            return 1;
        }
        try {
            return Math.max(0, Integer.parseInt(noticeLevel.trim()));
        } catch (NumberFormatException e) {
            String normalized = noticeLevel.trim().toLowerCase();
            return normalized.contains("important") || normalized.contains("urgent") || normalized.contains("重要") || normalized.contains("紧急") ? 2 : 1;
        }
    }

    private Map<String, Object> buildNoticePublishMetadata(Notice notice) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "notice_publish");
        if (notice != null && notice.getId() != null) {
            metadata.put("noticeId", notice.getId());
        }
        return metadata;
    }
}
