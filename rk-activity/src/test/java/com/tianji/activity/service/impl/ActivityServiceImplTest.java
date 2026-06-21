package com.tianji.activity.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.api.dto.user.EmailCenterSendDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.tianji.activity.domain.po.Activity;
import com.tianji.activity.mapper.ActivityCategoryMapper;
import com.tianji.activity.mapper.ActivityMapper;
import com.tianji.activity.mapper.ActivityRegistrationMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivityRegistrationMapper registrationMapper;
    @Mock
    private ActivityCategoryMapper categoryMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private UserClient userClient;
    @Mock
    private SearchClient searchClient;

    @InjectMocks
    private ActivityServiceImpl activityService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Activity.class);
    }

    private Activity buildValidActivity() {
        Activity activity = new Activity();
        activity.setActivityName("workflow-test");
        activity.setTenantId(1L);
        activity.setManagerReviewerId(7001L);
        activity.setTeacherReviewerId(8001L);
        return activity;
    }

    @Test
    void updateActivity_shouldPopulateUpdateTimeBeforeCallingMapper() {
        Activity activity = new Activity();
        activity.setId(12L);
        activity.setActivityName("Test Activity");
        activity.setActivityStatus(Activity.STATUS_NOT_STARTED);
        activity.setUpdateTime(null);
        when(activityMapper.updateById(activity)).thenReturn(1);

        activityService.updateActivity(activity);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateById(captor.capture());
        assertEquals(12L, captor.getValue().getId());
        assertNotNull(captor.getValue().getUpdateTime());
    }

    @Test
    void createActivity_shouldRejectWhenManagerOrTeacherApproverMissing() {
        Activity missingManager = buildValidActivity();
        missingManager.setManagerReviewerId(null);
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(missingManager));

        Activity missingTeacher = buildValidActivity();
        missingTeacher.setTeacherReviewerId(null);
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(missingTeacher));
    }

    @Test
    void createActivity_shouldKeepAssignedApproversAndStartFromPendingManagerReview() {
        Activity activity = buildValidActivity();
        when(activityMapper.insert(any(Activity.class))).thenAnswer(invocation -> {
            Activity inserted = invocation.getArgument(0);
            inserted.setId(88L);
            return 1;
        });

        Long id = activityService.createActivity(activity);

        assertEquals(88L, id);
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(captor.capture());
        Activity inserted = captor.getValue();
        assertEquals(7001L, inserted.getManagerReviewerId());
        assertEquals(8001L, inserted.getTeacherReviewerId());
        assertEquals(Activity.REVIEW_PENDING, inserted.getManagerReviewStatus());
        assertEquals(Activity.REVIEW_PENDING, inserted.getTeacherReviewStatus());
    }

    @Test
    void submitActivity_shouldSendPendingReviewNotificationWhenApprovalRequiredAndNotifyAdminsEnabled() {
        Activity activity = new Activity();
        activity.setActivityName("待审核活动");
        activity.setTenantId(1L);
        activity.setManagerReviewerId(7001L);
        activity.setTeacherReviewerId(8001L);

        TenantWorkflowConfigDTO config = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO policy = new WorkflowPolicyDTO();
        policy.setRequireApproval(true);
        policy.setNotifyAdmins(true);
        config.setActivityPublish(policy);

        when(userClient.queryCurrentTenantWorkflowConfig(1L)).thenReturn(config);
        when(activityMapper.insert(any(Activity.class))).thenAnswer(invocation -> {
            Activity inserted = invocation.getArgument(0);
            inserted.setId(1001L);
            return 1;
        });
        when(userClient.sendEmailCenterInternal(any(EmailCenterSendDTO.class))).thenReturn(true);

        Long id = activityService.submitActivity(activity);

        assertEquals(1001L, id);
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(captor.capture());
        Activity inserted = captor.getValue();
        assertEquals(7001L, inserted.getManagerReviewerId());
        assertEquals(8001L, inserted.getTeacherReviewerId());
        assertEquals(Activity.REVIEW_PENDING, inserted.getManagerReviewStatus());
        assertEquals(Activity.REVIEW_PENDING, inserted.getTeacherReviewStatus());
        ArgumentCaptor<EmailCenterSendDTO> emailCaptor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(userClient, org.mockito.Mockito.times(1)).sendEmailCenterInternal(emailCaptor.capture());
        EmailCenterSendDTO email = emailCaptor.getValue();
        assertEquals(1L, email.getTenantId());
        assertEquals(List.of(7001L), email.getAuthUserIds());
        assertEquals("活动待负责人审核通知", email.getSubject());
        assertTrue(email.getContent().contains("/admin/activity/approval"));
        assertTrue(email.getContent().contains("/activities/1001"));
    }

    @Test
    void reviewActivityByTeacher_shouldSendPublishSuccessNotificationWhenApprovedAndNotifyOnSuccessEnabled() {
        Activity stored = new Activity();
        stored.setId(202L);
        stored.setTenantId(1L);
        stored.setActivityName("审批通过活动");
        stored.setManagerReviewStatus(Activity.REVIEW_APPROVED);
        stored.setTeacherReviewStatus(Activity.REVIEW_PENDING);
        stored.setManagerReviewerId(7L);
        stored.setTeacherReviewerId(8L);
        stored.setCreator(9L);

        TenantWorkflowConfigDTO config = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO policy = new WorkflowPolicyDTO();
        policy.setNotifyOnSuccess(true);
        config.setActivityPublish(policy);

        when(activityMapper.selectById(202L)).thenReturn(stored);
        when(activityMapper.updateById(any(Activity.class))).thenReturn(1);
        when(userClient.queryCurrentTenantWorkflowConfig(eq(1L))).thenReturn(config);
        when(userClient.sendEmailCenterInternal(any(EmailCenterSendDTO.class))).thenReturn(true);

        boolean success = activityService.reviewActivityByTeacher(202L, true, "ok", 8L);

        assertEquals(true, success);
        ArgumentCaptor<EmailCenterSendDTO> emailCaptor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(userClient, org.mockito.Mockito.times(1)).sendEmailCenterInternal(emailCaptor.capture());
        EmailCenterSendDTO email = emailCaptor.getValue();
        assertEquals(1L, email.getTenantId());
        assertEquals(List.of(7L, 9L), email.getAuthUserIds());
        assertEquals("活动发布成功通知", email.getSubject());
        assertTrue(email.getContent().contains("/activities/202"));
    }

    @Test
    void reviewActivityByTeacher_shouldUpsertGlobalSearchIndexWhenApproved() {
        Activity stored = new Activity();
        stored.setId(502L);
        stored.setTenantId(1L);
        stored.setActivityName("search activity");
        stored.setOrganizer("club");
        stored.setLocation("room 1");
        stored.setContent("<p>activity search content</p>");
        stored.setCoverImage("/activity-cover.png");
        stored.setCreateTime(LocalDateTime.now().minusDays(1));
        stored.setUpdateTime(LocalDateTime.now());
        stored.setManagerReviewStatus(Activity.REVIEW_APPROVED);
        stored.setTeacherReviewStatus(Activity.REVIEW_PENDING);
        stored.setManagerReviewerId(7L);
        stored.setTeacherReviewerId(8L);

        TenantWorkflowConfigDTO config = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO policy = new WorkflowPolicyDTO();
        policy.setNotifyOnSuccess(false);
        config.setActivityPublish(policy);

        when(activityMapper.selectById(502L)).thenReturn(stored);
        when(activityMapper.updateById(any(Activity.class))).thenReturn(1);
        when(userClient.queryCurrentTenantWorkflowConfig(eq(1L))).thenReturn(config);

        boolean success = activityService.reviewActivityByTeacher(502L, true, "ok", 8L);

        assertTrue(success);
        ArgumentCaptor<GlobalSearchDocumentDTO> documentCaptor = ArgumentCaptor.forClass(GlobalSearchDocumentDTO.class);
        verify(searchClient).upsertGlobalDocument(documentCaptor.capture());
        GlobalSearchDocumentDTO document = documentCaptor.getValue();
        assertEquals("ACTIVITY", document.getEntityType());
        assertEquals(502L, document.getEntityId());
        assertEquals(1L, document.getTenantId());
        assertEquals("search activity", document.getTitle());
        assertEquals("/activities/502", document.getRoute());
        assertEquals("/activity-cover.png", document.getCoverUrl());
        assertTrue(Boolean.TRUE.equals(document.getVisible()));
    }

    @Test
    void deleteActivity_shouldDeleteGlobalSearchIndex() {
        Activity stored = new Activity();
        stored.setId(503L);
        stored.setTenantId(12L);
        stored.setManagerReviewStatus(Activity.REVIEW_APPROVED);
        stored.setTeacherReviewStatus(Activity.REVIEW_APPROVED);

        when(activityMapper.selectById(503L)).thenReturn(stored);
        when(activityMapper.deleteById(503L)).thenReturn(1);

        int deleted = activityService.deleteActivity(503L);

        assertEquals(1, deleted);
        verify(searchClient).deleteGlobalDocument("ACTIVITY", 503L, 12L);
    }

    @Test
    void reviewActivityByManager_shouldRejectWhenReviewerIsNotAssignedApprover() {
        Activity stored = new Activity();
        stored.setId(302L);
        stored.setManagerReviewStatus(Activity.REVIEW_PENDING);
        stored.setTeacherReviewStatus(Activity.REVIEW_PENDING);
        stored.setManagerReviewerId(7001L);
        stored.setTeacherReviewerId(8001L);

        when(activityMapper.selectById(302L)).thenReturn(stored);

        boolean success = activityService.reviewActivityByManager(302L, true, "ok", 9009L);

        assertFalse(success);
    }

    @Test
    void reviewActivityByManager_shouldAllowProxyReviewWhenExplicitlyEnabled() {
        Activity stored = new Activity();
        stored.setId(303L);
        stored.setTenantId(1L);
        stored.setManagerReviewStatus(Activity.REVIEW_PENDING);
        stored.setTeacherReviewStatus(Activity.REVIEW_PENDING);
        stored.setManagerReviewerId(7001L);
        stored.setTeacherReviewerId(8001L);

        when(activityMapper.selectById(303L)).thenReturn(stored);
        when(activityMapper.updateById(any(Activity.class))).thenReturn(1);

        boolean success = activityService.reviewActivityByManager(303L, true, "proxy", 9009L, true);

        assertEquals(true, success);
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateById(captor.capture());
        Activity updated = captor.getValue();
        assertEquals(7001L, updated.getManagerReviewerId());
        assertEquals(Activity.REVIEW_APPROVED, updated.getManagerReviewStatus());
        ArgumentCaptor<EmailCenterSendDTO> emailCaptor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(userClient, org.mockito.Mockito.times(1)).sendEmailCenterInternal(emailCaptor.capture());
        EmailCenterSendDTO email = emailCaptor.getValue();
        assertEquals(1L, email.getTenantId());
        assertEquals(List.of(8001L), email.getAuthUserIds());
        assertEquals("活动待指导老师审核通知", email.getSubject());
        assertTrue(email.getContent().contains("/admin/activity/approval"));
    }

    @Test
    void getActivitiesByStatus_shouldRefreshExpiredRowsBeforeSelectingStatusList() {
        Activity expired = new Activity();
        expired.setId(404L);
        expired.setActivityStatus(Activity.STATUS_NOT_STARTED);
        expired.setEndTime(LocalDateTime.now().minusDays(1));

        when(activityMapper.selectList(any())).thenReturn(
                List.of(),
                List.of(),
                List.of(expired),
                List.of()
        );
        when(activityMapper.updateById(any(Activity.class))).thenReturn(1);

        activityService.getActivitiesByStatus(Activity.STATUS_NOT_STARTED);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateById(captor.capture());
        assertEquals(404L, captor.getValue().getId());
        assertEquals(Activity.STATUS_ENDED, captor.getValue().getActivityStatus());
        verify(activityMapper, times(4)).selectList(any());
    }
}
