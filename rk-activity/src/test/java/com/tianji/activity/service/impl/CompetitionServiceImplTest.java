package com.tianji.activity.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.activity.domain.po.Competition;
import com.tianji.activity.mapper.CompetitionMapper;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.EmailCenterSendDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.tianji.common.utils.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceImplTest {

    @Mock
    private CompetitionMapper competitionMapper;
    @Mock
    private UserClient userClient;
    @Mock
    private SearchClient searchClient;

    @InjectMocks
    private CompetitionServiceImpl competitionService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Competition.class);
        ReflectionTestUtils.setField(competitionService, "baseMapper", competitionMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Competition buildCompetition() {
        Competition competition = new Competition();
        competition.setTitle("workflow-competition");
        competition.setTenantId(1L);
        competition.setManagerReviewerId(7001L);
        competition.setTeacherReviewerId(8001L);
        competition.setIsPublished(true);
        competition.setStatus("PUBLISHED");
        return competition;
    }

    @Test
    void createCompetition_shouldRejectWhenManagerOrTeacherApproverMissing() {
        Competition missingManager = buildCompetition();
        missingManager.setManagerReviewerId(null);
        assertThrows(IllegalArgumentException.class, () -> competitionService.createCompetition(missingManager));

        Competition missingTeacher = buildCompetition();
        missingTeacher.setTeacherReviewerId(null);
        assertThrows(IllegalArgumentException.class, () -> competitionService.createCompetition(missingTeacher));
    }

    @Test
    void createCompetition_shouldStartPendingAndSuppressPublishingUntilBothApprovals() {
        Competition competition = buildCompetition();
        when(competitionMapper.insert(any(Competition.class))).thenAnswer(invocation -> {
            Competition inserted = invocation.getArgument(0);
            inserted.setId(902L);
            return 1;
        });

        Long id = competitionService.createCompetition(competition);

        assertEquals(902L, id);
        ArgumentCaptor<Competition> captor = ArgumentCaptor.forClass(Competition.class);
        verify(competitionMapper).insert(captor.capture());
        Competition inserted = captor.getValue();
        assertEquals(7001L, inserted.getManagerReviewerId());
        assertEquals(8001L, inserted.getTeacherReviewerId());
        assertEquals(Competition.REVIEW_PENDING, inserted.getManagerReviewStatus());
        assertEquals(Competition.REVIEW_PENDING, inserted.getTeacherReviewStatus());
        assertFalse(Boolean.TRUE.equals(inserted.getIsPublished()));
        assertEquals("DRAFT", inserted.getStatus());
        ArgumentCaptor<EmailCenterSendDTO> emailCaptor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(userClient, times(1)).sendEmailCenterInternal(emailCaptor.capture());
        EmailCenterSendDTO email = emailCaptor.getValue();
        assertEquals(1L, email.getTenantId());
        assertEquals(List.of(7001L), email.getAuthUserIds());
        assertEquals("比赛待负责人审核通知", email.getSubject());
        assertTrue(email.getContent().contains("/admin/activity/competition-approval"));
        assertTrue(email.getContent().contains("/competition/902"));
    }

    @Test
    void createCompetition_shouldUseCurrentTenantWhenTenantIdMissing() {
        TenantContext.setTenantId(30L);
        Competition competition = buildCompetition();
        competition.setTenantId(null);
        when(competitionMapper.insert(any(Competition.class))).thenAnswer(invocation -> {
            Competition inserted = invocation.getArgument(0);
            inserted.setId(905L);
            return 1;
        });

        competitionService.createCompetition(competition);

        ArgumentCaptor<Competition> captor = ArgumentCaptor.forClass(Competition.class);
        verify(competitionMapper).insert(captor.capture());
        assertEquals(30L, captor.getValue().getTenantId());
    }

    @Test
    void getCompetitionsByStatus_shouldOnlyReturnPublicApprovedCompetitions() {
        when(competitionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of());

        competitionService.getCompetitionsByStatus("PUBLISHED");

        ArgumentCaptor<LambdaQueryWrapper<Competition>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(competitionMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getCustomSqlSegment().toUpperCase();
        assertTrue(sqlSegment.contains("STATUS"));
        assertTrue(sqlSegment.contains("IS_PUBLISHED"));
        assertTrue(sqlSegment.contains("MANAGER_REVIEW_STATUS"));
        assertTrue(sqlSegment.contains("TEACHER_REVIEW_STATUS"));
    }

    @Test
    void reviewCompetitionByManager_shouldRejectWhenReviewerIsNotAssignedApprover() {
        Competition stored = buildCompetition();
        stored.setId(903L);
        stored.setManagerReviewStatus(Competition.REVIEW_PENDING);
        stored.setTeacherReviewStatus(Competition.REVIEW_PENDING);

        when(competitionMapper.selectById(903L)).thenReturn(stored);

        boolean success = competitionService.reviewCompetitionByManager(903L, true, "ok", 9009L);

        assertFalse(success);
    }

    @Test
    void reviewCompetitionByTeacher_shouldRejectBeforeManagerApproval() {
        Competition stored = buildCompetition();
        stored.setId(904L);
        stored.setManagerReviewStatus(Competition.REVIEW_PENDING);
        stored.setTeacherReviewStatus(Competition.REVIEW_PENDING);

        when(competitionMapper.selectById(904L)).thenReturn(stored);

        boolean success = competitionService.reviewCompetitionByTeacher(904L, true, "ok", 8001L);

        assertFalse(success);
    }

    @Test
    void reviewCompetitionByTeacher_shouldSendPublishSuccessNotificationWhenApprovedAndNotifyOnSuccessEnabled() {
        Competition stored = new Competition();
        stored.setId(901L);
        stored.setTitle("AI 比赛");
        stored.setTenantId(1L);
        stored.setManagerReviewStatus(Competition.REVIEW_APPROVED);
        stored.setManagerReviewerId(7L);
        stored.setTeacherReviewerId(8L);
        stored.setTeacherReviewStatus(Competition.REVIEW_PENDING);
        stored.setIsPublished(false);
        stored.setStatus("DRAFT");
        stored.setCreatedBy("9");
        stored.setContactEmail("contact@example.com");

        TenantWorkflowConfigDTO config = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO policy = new WorkflowPolicyDTO();
        policy.setNotifyOnSuccess(true);
        config.setCompetitionPublish(policy);

        when(competitionMapper.selectById(901L)).thenReturn(stored);
        when(competitionMapper.updateById(any(Competition.class))).thenReturn(1);
        when(userClient.queryCurrentTenantWorkflowConfig(eq(1L))).thenReturn(config);
        when(userClient.sendEmailCenterInternal(any(EmailCenterSendDTO.class))).thenReturn(true);

        boolean success = competitionService.reviewCompetitionByTeacher(901L, true, "ok", 8L);

        assertTrue(success);
        ArgumentCaptor<EmailCenterSendDTO> emailCaptor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(userClient, times(1)).sendEmailCenterInternal(emailCaptor.capture());
        EmailCenterSendDTO email = emailCaptor.getValue();
        assertEquals(1L, email.getTenantId());
        assertEquals(List.of(7L, 9L), email.getAuthUserIds());
        assertEquals(List.of("contact@example.com"), email.getManualEmails());
        assertEquals("比赛发布成功通知", email.getSubject());
        assertTrue(email.getContent().contains("/competition/901"));
    }

    @Test
    void deleteCompetition_shouldDeleteGlobalSearchIndex() {
        Competition stored = buildCompetition();
        stored.setId(902L);
        stored.setTenantId(11L);

        when(competitionMapper.selectById(902L)).thenReturn(stored);
        when(competitionMapper.deleteById(902L)).thenReturn(1);

        int deleted = competitionService.deleteCompetition(902L);

        assertEquals(1, deleted);
        verify(searchClient).deleteGlobalDocument("COMPETITION", 902L, 11L);
    }

    @Test
    void reviewCompetitionByManager_shouldSendTeacherPendingReviewNotificationWhenApproved() {
        Competition stored = buildCompetition();
        stored.setId(906L);
        stored.setTenantId(1L);
        stored.setManagerReviewStatus(Competition.REVIEW_PENDING);
        stored.setTeacherReviewStatus(Competition.REVIEW_PENDING);

        when(competitionMapper.selectById(906L)).thenReturn(stored);
        when(competitionMapper.updateById(any(Competition.class))).thenReturn(1);

        boolean success = competitionService.reviewCompetitionByManager(906L, true, "ok", 7001L);

        assertTrue(success);
        ArgumentCaptor<EmailCenterSendDTO> emailCaptor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(userClient, times(1)).sendEmailCenterInternal(emailCaptor.capture());
        EmailCenterSendDTO email = emailCaptor.getValue();
        assertEquals(1L, email.getTenantId());
        assertEquals(List.of(8001L), email.getAuthUserIds());
        assertEquals("比赛待指导老师审核通知", email.getSubject());
        assertTrue(email.getContent().contains("/admin/activity/competition-approval"));
    }
}
