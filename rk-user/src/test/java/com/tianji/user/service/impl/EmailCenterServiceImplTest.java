package com.tianji.user.service.impl;

import com.tianji.api.client.activity.ActivityRecipientClient;
import com.tianji.api.client.auth.AuthClient;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.message.api.client.AsyncEmailClient;
import com.tianji.message.domain.dto.EmailInfoDTO;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.dto.EmailCenterSendDTO;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.po.EmailSendRecipient;
import com.tianji.user.domain.po.EmailSendTask;
import com.tianji.user.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailCenterServiceImplTest {

    @Mock private AuthClient authClient;
    @Mock private ActivityRecipientClient activityRecipientClient;
    @Mock private UserMapper userMapper;
    @Mock private EmailInvitationMapper emailInvitationMapper;
    @Mock private EmailInvitationEventMapper emailInvitationEventMapper;
    @Mock private ReferralConversionRecordMapper referralConversionRecordMapper;
    @Mock private ReferralCodeMapper referralCodeMapper;
    @Mock private EmailSendTaskMapper emailSendTaskMapper;
    @Mock private EmailSendRecipientMapper emailSendRecipientMapper;
    @Mock private AsyncEmailClient asyncEmailClient;

    @InjectMocks
    private EmailCenterServiceImpl service;

    @Test
    void resolveTenantRecipientEmails_shouldReturnTenantScopedDistinctEmailsForAuthUsersAndUsernames() {
        User authLinkedUser = new User();
        authLinkedUser.setAuthUserId(2001L);
        authLinkedUser.setTenantId(9L);
        authLinkedUser.setUsername("admin_a");
        authLinkedUser.setEmail("tenant-admin@example.com");

        User usernameMappedUser = new User();
        usernameMappedUser.setTenantId(9L);
        usernameMappedUser.setUsername("teacher_b");
        usernameMappedUser.setEmail("teacher@example.com");

        when(userMapper.selectList(any())).thenReturn(List.of(authLinkedUser, usernameMappedUser, authLinkedUser));

        List<String> emails = service.resolveTenantRecipientEmails(9L, List.of(2001L), List.of("teacher_b", "admin_a"));

        assertIterableEquals(List.of("tenant-admin@example.com", "teacher@example.com"), emails);
    }

    @Test
    void send_shouldCreateAuditTaskAndRecipientSnapshotsBeforeQueueing() {
        EmailCenterSendDTO dto = new EmailCenterSendDTO();
        dto.setManualEmails(List.of("a@example.com", "b@example.com"));
        dto.setSubject("subject");
        dto.setContent("content");
        dto.setHtml(false);

        doAnswer(invocation -> {
            EmailSendTask task = invocation.getArgument(0);
            task.setId(10L);
            return 1;
        }).when(emailSendTaskMapper).insert(any(EmailSendTask.class));
        doAnswer(invocation -> {
            EmailSendRecipient recipient = invocation.getArgument(0);
            if (recipient.getId() == null) {
                recipient.setId(recipient.getRecipientEmail().startsWith("a") ? 101L : 102L);
            }
            return 1;
        }).when(emailSendRecipientMapper).insert(any(EmailSendRecipient.class));

        Map<String, Object> result = service.send(dto);

        assertNotNull(result.get("taskId"));
        assertEquals(2, result.get("queuedCount"));
        verify(emailSendTaskMapper).insert(any(EmailSendTask.class));
        verify(emailSendRecipientMapper, times(2)).insert(any(EmailSendRecipient.class));

        ArgumentCaptor<EmailInfoDTO> captor = ArgumentCaptor.forClass(EmailInfoDTO.class);
        verify(asyncEmailClient).sendMessage(captor.capture());
        assertEquals(10L, captor.getValue().getTaskId());
        assertEquals(List.of(101L, 102L), captor.getValue().getRecipientIds());
    }

    @Test
    void send_shouldUseExplicitTenantIdForInternalAuditRecords() {
        TenantContext.setTenantId(1L);
        EmailCenterSendDTO dto = new EmailCenterSendDTO();
        dto.setTenantId(9L);
        dto.setManualEmails(List.of("tenant9@example.com"));
        dto.setSubject("subject");
        dto.setContent("content");
        dto.setHtml(false);

        doAnswer(invocation -> {
            EmailSendTask task = invocation.getArgument(0);
            task.setId(20L);
            return 1;
        }).when(emailSendTaskMapper).insert(any(EmailSendTask.class));
        doAnswer(invocation -> {
            EmailSendRecipient recipient = invocation.getArgument(0);
            recipient.setId(201L);
            return 1;
        }).when(emailSendRecipientMapper).insert(any(EmailSendRecipient.class));

        service.send(dto);

        ArgumentCaptor<EmailSendTask> taskCaptor = ArgumentCaptor.forClass(EmailSendTask.class);
        ArgumentCaptor<EmailSendRecipient> recipientCaptor = ArgumentCaptor.forClass(EmailSendRecipient.class);
        verify(emailSendTaskMapper).insert(taskCaptor.capture());
        verify(emailSendRecipientMapper).insert(recipientCaptor.capture());
        assertEquals(9L, taskCaptor.getValue().getTenantId());
        assertEquals(9L, recipientCaptor.getValue().getTenantId());
        TenantContext.clear();
    }

    @Test
    void send_shouldResolveExplicitAuthUserRecipientsInsideExplicitTenant() {
        EmailCenterSendDTO dto = new EmailCenterSendDTO();
        dto.setTenantId(9L);
        dto.setAuthUserIds(List.of(2001L));
        dto.setSubject("subject");
        dto.setContent("content");
        dto.setHtml(false);

        User reviewer = new User();
        reviewer.setAuthUserId(2001L);
        reviewer.setTenantId(9L);
        reviewer.setUsername("teacher_a");
        reviewer.setEmail("teacher9@example.com");

        when(userMapper.selectList(any())).thenReturn(List.of(reviewer));
        doAnswer(invocation -> {
            EmailSendTask task = invocation.getArgument(0);
            task.setId(30L);
            return 1;
        }).when(emailSendTaskMapper).insert(any(EmailSendTask.class));
        doAnswer(invocation -> {
            EmailSendRecipient recipient = invocation.getArgument(0);
            recipient.setId(301L);
            return 1;
        }).when(emailSendRecipientMapper).insert(any(EmailSendRecipient.class));

        Map<String, Object> result = service.send(dto);

        assertEquals(1, result.get("queuedCount"));
        assertEquals(List.of("teacher9@example.com"), result.get("recipients"));
    }

    @Test
    void send_shouldResolveExplicitAuthUserRecipientByAuthUsernameWhenLocalAuthLinkMissing() {
        EmailCenterSendDTO dto = new EmailCenterSendDTO();
        dto.setTenantId(1L);
        dto.setAuthUserIds(List.of(6L));
        dto.setSubject("subject");
        dto.setContent("content");
        dto.setHtml(false);

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setAuthUserId(6L);
        authUser.setTenantId(1L);
        authUser.setUsername("manager_a");
        when(authClient.queryAdminUserById(6L)).thenReturn(authUser);

        User usernameMappedUser = new User();
        usernameMappedUser.setTenantId(1L);
        usernameMappedUser.setUsername("manager_a");
        usernameMappedUser.setEmail("manager@example.com");

        when(userMapper.selectList(any())).thenReturn(List.of(), List.of(usernameMappedUser));
        doAnswer(invocation -> {
            EmailSendTask task = invocation.getArgument(0);
            task.setId(40L);
            return 1;
        }).when(emailSendTaskMapper).insert(any(EmailSendTask.class));
        doAnswer(invocation -> {
            EmailSendRecipient recipient = invocation.getArgument(0);
            recipient.setId(401L);
            return 1;
        }).when(emailSendRecipientMapper).insert(any(EmailSendRecipient.class));

        Map<String, Object> result = service.send(dto);

        assertEquals(1, result.get("queuedCount"));
        assertEquals(List.of("manager@example.com"), result.get("recipients"));
    }

    @Test
    void send_shouldResolveExplicitAuthUserRecipientFromDeletedLocalProfileAfterAuthTenantCheck() {
        EmailCenterSendDTO dto = new EmailCenterSendDTO();
        dto.setTenantId(1L);
        dto.setAuthUserIds(List.of(6L));
        dto.setSubject("subject");
        dto.setContent("content");
        dto.setHtml(false);

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setAuthUserId(6L);
        authUser.setTenantId(1L);
        authUser.setUsername("manager_a");
        when(authClient.queryAdminUserById(6L)).thenReturn(authUser);

        User deletedProfile = new User();
        deletedProfile.setAuthUserId(6L);
        deletedProfile.setTenantId(1L);
        deletedProfile.setUsername("manager_a");
        deletedProfile.setEmail("manager_a@example.com");
        deletedProfile.setIsDeleted(1);

        when(userMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(userMapper.selectAnyByTenantAndUsername(1L, "manager_a")).thenReturn(deletedProfile);
        doAnswer(invocation -> {
            EmailSendTask task = invocation.getArgument(0);
            task.setId(41L);
            return 1;
        }).when(emailSendTaskMapper).insert(any(EmailSendTask.class));
        doAnswer(invocation -> {
            EmailSendRecipient recipient = invocation.getArgument(0);
            recipient.setId(411L);
            return 1;
        }).when(emailSendRecipientMapper).insert(any(EmailSendRecipient.class));

        Map<String, Object> result = service.send(dto);

        assertEquals(1, result.get("queuedCount"));
        assertEquals(List.of("manager_a@example.com"), result.get("recipients"));
    }

    @Test
    void markEmailSendRecipientSuccess_shouldUpdateRecipientAndTaskCounters() {
        EmailSendTask task = new EmailSendTask();
        task.setId(10L);
        task.setQueuedCount(1);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setStatus("QUEUED");

        EmailSendRecipient recipient = new EmailSendRecipient();
        recipient.setId(101L);
        recipient.setTaskId(10L);
        recipient.setSendStatus("QUEUED");

        when(emailSendTaskMapper.selectById(10L)).thenReturn(task);
        when(emailSendRecipientMapper.selectById(101L)).thenReturn(recipient);

        boolean result = service.markEmailSendRecipientSuccess(10L, 101L);

        assertTrue(result);
        assertEquals("SUCCESS", recipient.getSendStatus());
        assertEquals(1, task.getSuccessCount());
        assertEquals("SUCCESS", task.getStatus());
        verify(emailSendRecipientMapper).updateById(recipient);
        verify(emailSendTaskMapper).updateById(task);
    }

    @Test
    void markEmailSendRecipientFailure_shouldUpdateRecipientAndTaskCounters() {
        EmailSendTask task = new EmailSendTask();
        task.setId(11L);
        task.setQueuedCount(2);
        task.setSuccessCount(1);
        task.setFailCount(0);
        task.setStatus("QUEUED");

        EmailSendRecipient recipient = new EmailSendRecipient();
        recipient.setId(202L);
        recipient.setTaskId(11L);
        recipient.setSendStatus("QUEUED");

        when(emailSendTaskMapper.selectById(11L)).thenReturn(task);
        when(emailSendRecipientMapper.selectById(202L)).thenReturn(recipient);

        boolean result = service.markEmailSendRecipientFailure(11L, 202L, "smtp failed");

        assertTrue(result);
        assertEquals("FAILED", recipient.getSendStatus());
        assertEquals("smtp failed", recipient.getErrorMessage());
        assertEquals(1, task.getFailCount());
        assertEquals("PARTIAL_SUCCESS", task.getStatus());
        verify(emailSendRecipientMapper).updateById(recipient);
        verify(emailSendTaskMapper).updateById(task);
    }
}
