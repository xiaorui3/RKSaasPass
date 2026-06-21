package com.tianji.content.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.EmailCenterSendDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.tianji.common.utils.TenantContext;
import com.tianji.content.domain.po.News;
import com.tianji.content.mapper.NewsMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private NewsMapper newsMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private NewsServiceImpl newsService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        ReflectionTestUtils.setField(newsService, "baseMapper", newsMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private News buildNews() {
        News news = new News();
        news.setTitle("workflow-news");
        news.setContent("content");
        news.setTenantId(1L);
        news.setIsPublished(1);
        news.setManagerReviewerId(7001L);
        news.setTeacherReviewerId(8001L);
        return news;
    }

    @Test
    void insertNews_shouldRejectWhenManagerOrTeacherApproverMissing() {
        News missingManager = buildNews();
        missingManager.setManagerReviewerId(null);
        assertThrows(IllegalArgumentException.class, () -> newsService.insertNews(missingManager));

        News missingTeacher = buildNews();
        missingTeacher.setTeacherReviewerId(null);
        assertThrows(IllegalArgumentException.class, () -> newsService.insertNews(missingTeacher));
    }

    @Test
    void insertNews_shouldStartPendingAndSuppressPublishingUntilBothApprovals() {
        News news = buildNews();
        when(newsMapper.insert(any(News.class))).thenReturn(1);

        int result = newsService.insertNews(news);

        assertEquals(1, result);
        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsMapper).insert(captor.capture());
        News inserted = captor.getValue();
        assertEquals(7001L, inserted.getManagerReviewerId());
        assertEquals(8001L, inserted.getTeacherReviewerId());
        assertEquals(News.REVIEW_PENDING, inserted.getManagerReviewStatus());
        assertEquals(News.REVIEW_PENDING, inserted.getTeacherReviewStatus());
        assertEquals(1, inserted.getApprovalStatus());
        assertFalse(Integer.valueOf(1).equals(inserted.getIsPublished()));
    }

    @Test
    void insertNews_shouldUseCurrentTenantWhenTenantIdMissing() {
        TenantContext.setTenantId(30L);
        News news = buildNews();
        news.setTenantId(null);
        when(newsMapper.insert(any(News.class))).thenReturn(1);

        newsService.insertNews(news);

        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsMapper).insert(captor.capture());
        assertEquals(30L, captor.getValue().getTenantId());
    }

    @Test
    void approveNewsByManager_shouldRejectWhenReviewerIsNotAssignedApprover() {
        News news = buildNews();
        news.setId(301L);
        news.setManagerReviewStatus(News.REVIEW_PENDING);
        news.setTeacherReviewStatus(News.REVIEW_PENDING);
        when(newsMapper.selectById(301L)).thenReturn(news);

        boolean success = newsService.approveNewsByManager(301L, "ok", 9009L, false);

        assertFalse(success);
    }

    @Test
    void rejectNews_shouldRejectCurrentManagerStageAndKeepNewsUnpublished() {
        News news = buildNews();
        news.setId(302L);
        news.setApprovalStatus(1);
        news.setManagerReviewStatus(News.REVIEW_PENDING);
        news.setTeacherReviewStatus(News.REVIEW_PENDING);
        news.setIsPublished(1);

        when(newsMapper.selectById(302L)).thenReturn(news);
        when(newsMapper.updateById(any(News.class))).thenReturn(1);

        newsService.rejectNews(302L, "not ready");

        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsMapper).updateById(captor.capture());
        News saved = captor.getValue();
        assertEquals(News.REVIEW_REJECTED, saved.getManagerReviewStatus());
        assertEquals(News.REVIEW_PENDING, saved.getTeacherReviewStatus());
        assertEquals(3, saved.getApprovalStatus());
        assertEquals(0, saved.getIsPublished());
        assertEquals("not ready", saved.getRejectReason());
    }

    @Test
    void insertNews_shouldPopulateUpdateTimeBeforeInsert() {
        News news = new News();
        news.setTitle("insert");
        news.setContent("content");
        news.setIsPublished(0);
        news.setUpdateTime(null);
        news.setManagerReviewerId(7001L);
        news.setTeacherReviewerId(8001L);

        when(newsMapper.insert(news)).thenReturn(1);

        newsService.insertNews(news);

        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getCreateTime());
        assertNotNull(captor.getValue().getUpdateTime());
    }

    @Test
    void updateNews_shouldMergeExistingRecordAndPopulatePublishAndUpdateTime() {
        News existing = new News();
        existing.setId(267L);
        existing.setTitle("old-title");
        existing.setContent("old-content");
        existing.setCoverImage("old-cover");
        existing.setVideoUrl("old-video");
        existing.setAttachmentUrl("old-attachment");
        existing.setIsPublished(0);
        existing.setApprovalStatus(2);
        existing.setManagerReviewStatus(News.REVIEW_APPROVED);
        existing.setTeacherReviewStatus(News.REVIEW_APPROVED);
        existing.setPublishTime(null);
        existing.setUpdateTime(null);

        News patch = new News();
        patch.setId(267L);
        patch.setCoverImage("rk-content/news-image/new.png");
        patch.setVideoUrl("rk-content/news-video/new.mp4");
        patch.setAttachmentUrl("rk-content/news-attachment/new.txt");
        patch.setIsPublished(1);

        when(newsMapper.selectById(267L)).thenReturn(existing);
        when(newsMapper.updateById(existing)).thenReturn(1);

        newsService.updateNews(patch);

        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsMapper).updateById(captor.capture());
        News saved = captor.getValue();
        assertEquals(267L, saved.getId());
        assertEquals("old-title", saved.getTitle());
        assertEquals("old-content", saved.getContent());
        assertEquals("rk-content/news-image/new.png", saved.getCoverImage());
        assertEquals("rk-content/news-video/new.mp4", saved.getVideoUrl());
        assertEquals("rk-content/news-attachment/new.txt", saved.getAttachmentUrl());
        assertEquals(1, saved.getIsPublished());
        assertNotNull(saved.getPublishTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    void approveNews_shouldSendPublishSuccessNotificationWhenNotifyOnSuccessEnabled() {
        News news = new News();
        news.setId(267L);
        news.setTitle("新闻发布");
        news.setIsPublished(0);
        news.setTenantId(1L);
        news.setApprovalStatus(1);
        news.setManagerReviewStatus(News.REVIEW_APPROVED);
        news.setTeacherReviewStatus(News.REVIEW_PENDING);
        news.setTeacherReviewerId(8L);

        TenantWorkflowConfigDTO config = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO policy = new WorkflowPolicyDTO();
        policy.setNotifyOnSuccess(true);
        config.setNewsPublish(policy);

        when(newsMapper.selectById(267L)).thenReturn(news);
        when(newsMapper.updateById(any(News.class))).thenReturn(1);
        when(userClient.queryCurrentTenantWorkflowConfig(eq(1L))).thenReturn(config);
        when(userClient.sendEmailCenterInternal(any(EmailCenterSendDTO.class))).thenReturn(true);

        newsService.approveNews(267L, "ok");

        verify(userClient, times(1)).sendEmailCenterInternal(any(EmailCenterSendDTO.class));
    }
}
