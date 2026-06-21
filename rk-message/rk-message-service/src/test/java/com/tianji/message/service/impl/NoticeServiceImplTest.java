package com.tianji.message.service.impl;

import com.tianji.common.autoconfigure.media.MediaPathHelper;
import com.tianji.common.autoconfigure.media.MediaPathProperties;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.api.dto.user.NotificationInternalSaveDTO;
import com.tianji.message.domain.dto.NoticeDTO;
import com.tianji.message.domain.po.Notice;
import com.tianji.message.mapper.NoticeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock
    private NoticeMapper noticeMapper;
    @Mock
    private UserClient userClient;
    @Mock
    private SearchClient searchClient;

    private NoticeServiceImpl service;

    @BeforeEach
    void setUp() {
        MediaPathProperties properties = new MediaPathProperties();
        properties.setEndpoint("http://127.0.0.1:9000");
        properties.setBucketName("rk-bucket");
        properties.setPublicBaseUrl("http://203.0.113.10:9000/rk-bucket/");
        service = new NoticeServiceImpl(new MediaPathHelper(properties), userClient, searchClient);
        ReflectionTestUtils.setField(service, "baseMapper", noticeMapper);
    }

    @Test
    void createNotice_shouldNormalizeAbsoluteManagedMediaUrlsBeforeInsert() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle("notice");
        dto.setContent("content");
        dto.setCoverImage("http://127.0.0.1:9000/rk-bucket/notices/cover.png");
        dto.setAttachmentUrl("http://127.0.0.1:9000/rk-bucket/notices/file.txt");
        dto.setManagerReviewerId(7001L);
        dto.setTeacherReviewerId(8001L);

        service.createNotice(dto);

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(captor.capture());
        assertEquals("rk-bucket/notices/cover.png", captor.getValue().getCoverImage());
        assertEquals("rk-bucket/notices/file.txt", captor.getValue().getAttachmentUrl());
        assertEquals(Notice.REVIEW_PENDING, captor.getValue().getManagerReviewStatus());
        assertEquals(Notice.REVIEW_PENDING, captor.getValue().getTeacherReviewStatus());
        assertEquals(false, captor.getValue().getIsPublished());
        verify(userClient, never()).saveNotificationInternal(any(NotificationInternalSaveDTO.class));
    }

    @Test
    void noticeDto_shouldAcceptBooleanTopFlagFromFrontend() throws Exception {
        NoticeDTO dto = new ObjectMapper().readValue("{\"title\":\"notice\",\"content\":\"content\",\"isTop\":true}", NoticeDTO.class);

        assertEquals(1, dto.getIsTop());

        NoticeDTO falseDto = new ObjectMapper().readValue("{\"title\":\"notice\",\"content\":\"content\",\"isTop\":false}", NoticeDTO.class);
        assertEquals(0, falseDto.getIsTop());
    }

    @Test
    void createNotice_shouldRespectImmediatePublishAndSendNotification() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle("notice");
        dto.setContent("content");
        dto.setIsPublished(true);
        dto.setPriority("3");
        dto.setManagerReviewerId(7001L);
        dto.setTeacherReviewerId(8001L);

        service.createNotice(dto);

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(noticeCaptor.capture());
        assertEquals(true, noticeCaptor.getValue().getIsPublished());

        ArgumentCaptor<NotificationInternalSaveDTO> notificationCaptor = ArgumentCaptor.forClass(NotificationInternalSaveDTO.class);
        verify(userClient).saveNotificationInternal(notificationCaptor.capture());
        assertEquals("鏂板叕鍛婂凡鍙戝竷", notificationCaptor.getValue().getTitle());
        assertEquals(3, notificationCaptor.getValue().getPriority());
    }

    @Test
    void getNoticeDetail_shouldExpandLegacyInternalUrlsToPublicHost() {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setCoverImage("http://127.0.0.1:9000/rk-bucket/notices/cover.png");
        notice.setAttachmentUrl("http://127.0.0.1:9000/rk-bucket/notices/file.txt");
        when(noticeMapper.selectById(1L)).thenReturn(notice);

        Notice detail = service.getNoticeDetail(1L);

        assertEquals("http://203.0.113.10:9000/rk-bucket/notices/cover.png", detail.getCoverImage());
        assertEquals("http://203.0.113.10:9000/rk-bucket/notices/file.txt", detail.getAttachmentUrl());
        verify(noticeMapper).updateById(notice);
    }

    @Test
    void reviewNoticeByManager_shouldRejectCurrentStageWithoutPublishing() {
        Notice notice = new Notice();
        notice.setId(2L);
        notice.setManagerReviewerId(7001L);
        notice.setTeacherReviewerId(8001L);
        notice.setManagerReviewStatus(Notice.REVIEW_PENDING);
        notice.setTeacherReviewStatus(Notice.REVIEW_PENDING);
        notice.setIsPublished(true);
        when(noticeMapper.selectById(2L)).thenReturn(notice);
        when(noticeMapper.updateById(any(Notice.class))).thenReturn(1);

        boolean success = service.reviewNoticeByManager(2L, false, "not ready", 7001L, false);

        assertEquals(true, success);
        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).updateById(captor.capture());
        Notice saved = captor.getValue();
        assertEquals(Notice.REVIEW_REJECTED, saved.getManagerReviewStatus());
        assertEquals(Notice.REVIEW_PENDING, saved.getTeacherReviewStatus());
        assertEquals(false, saved.getIsPublished());
        assertEquals("not ready", saved.getManagerReviewComment());
    }

    @Test
    void withdrawNotice_shouldPersistDraftStateAndClearPublishTime() {
        Notice notice = new Notice();
        notice.setId(3L);
        notice.setIsPublished(true);
        notice.setPublishTime(java.time.LocalDateTime.now());
        when(noticeMapper.selectById(3L)).thenReturn(notice);

        service.withdrawNotice(3L);

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).updateById(captor.capture());
        assertEquals(false, captor.getValue().getIsPublished());
        assertNull(captor.getValue().getPublishTime());
    }

    @Test
    void updateTopStatus_shouldPersistRequestedTopValue() {
        Notice notice = new Notice();
        notice.setId(4L);
        notice.setIsTop(0);
        when(noticeMapper.selectById(4L)).thenReturn(notice);

        service.updateTopStatus(4L, true);

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getIsTop());
    }

    @Test
    void publishNotice_shouldMarkLegacyDraftAsFullyPublishedAndNotify() {
        Notice notice = new Notice();
        notice.setId(5L);
        notice.setTenantId(2L);
        notice.setTitle("Release notice");
        notice.setIsPublished(false);
        notice.setManagerReviewStatus(null);
        notice.setTeacherReviewStatus(null);
        when(noticeMapper.selectById(5L)).thenReturn(notice);

        service.publishNotice(5L);

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).updateById(captor.capture());
        Notice saved = captor.getValue();
        assertEquals(true, saved.getIsPublished());
        assertEquals(Notice.REVIEW_APPROVED, saved.getManagerReviewStatus());
        assertEquals(Notice.REVIEW_APPROVED, saved.getTeacherReviewStatus());

        ArgumentCaptor<NotificationInternalSaveDTO> notificationCaptor = ArgumentCaptor.forClass(NotificationInternalSaveDTO.class);
        verify(userClient).saveNotificationInternal(notificationCaptor.capture());
        assertEquals(2L, notificationCaptor.getValue().getTenantId());

        ArgumentCaptor<GlobalSearchDocumentDTO> searchCaptor = ArgumentCaptor.forClass(GlobalSearchDocumentDTO.class);
        verify(searchClient).upsertGlobalDocument(searchCaptor.capture());
        assertEquals("NOTICE", searchCaptor.getValue().getEntityType());
        assertEquals(5L, searchCaptor.getValue().getEntityId());
        assertEquals(2L, searchCaptor.getValue().getTenantId());
        assertEquals("/notices", searchCaptor.getValue().getRoute());
    }
}
