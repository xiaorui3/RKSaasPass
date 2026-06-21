package com.tianji.activity.controller;

import com.tianji.activity.domain.dto.ActivityVoteSaveDTO;
import com.tianji.activity.domain.po.Activity;
import com.tianji.activity.domain.po.ActivityRegistration;
import com.tianji.activity.domain.po.ActivityVote;
import com.tianji.activity.domain.po.ActivityVoteOption;
import com.tianji.activity.domain.vo.ActivityVoteVO;
import com.tianji.activity.mapper.ActivityMapper;
import com.tianji.activity.mapper.ActivityRegistrationMapper;
import com.tianji.activity.mapper.ActivityVoteMapper;
import com.tianji.activity.mapper.ActivityVoteOptionMapper;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.NotificationInternalSaveDTO;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityVoteControllerTest {

    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivityRegistrationMapper registrationMapper;
    @Mock
    private ActivityVoteMapper voteMapper;
    @Mock
    private ActivityVoteOptionMapper optionMapper;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private ActivityVoteController controller;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.removeUser();
    }

    @Test
    void listActivities_shouldReturnCurrentTenantActivitiesForBinding() {
        TenantContext.setTenantId(5L);
        Activity activity = new Activity()
                .setId(11L)
                .setActivityName("Project Night");
        activity.setTenantId(5L);
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));

        R<List<Activity>> response = controller.listActivities("Project");

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("Project Night", response.getData().get(0).getActivityName());
    }

    @Test
    void listRegistrations_shouldReturnActivityRegistrationRecipients() {
        TenantContext.setTenantId(5L);
        ActivityRegistration registration = new ActivityRegistration()
                .setId(21L)
                .setActivityId(11L)
                .setUserId(901L)
                .setRegistrationStatus(ActivityRegistration.STATUS_REGISTERED)
                .setRegistrationTime(LocalDateTime.now());
        when(registrationMapper.selectList(any())).thenReturn(List.of(registration));

        R<List<Map<String, Object>>> response = controller.listRegistrations(11L);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(901L, response.getData().get(0).get("userId"));
        assertEquals(ActivityRegistration.STATUS_REGISTERED, response.getData().get(0).get("registrationStatus"));
    }

    @Test
    void createVote_shouldBindActivityRecipientsAndExtraUsersThenNotify() {
        TenantContext.setTenantId(5L);
        UserContext.setUser(77L);
        Activity activity = new Activity()
                .setId(11L)
                .setActivityName("Project Night");
        activity.setTenantId(5L);
        ActivityRegistration first = new ActivityRegistration()
                .setActivityId(11L)
                .setUserId(901L)
                .setRegistrationStatus(ActivityRegistration.STATUS_REGISTERED);
        ActivityRegistration second = new ActivityRegistration()
                .setActivityId(11L)
                .setUserId(902L)
                .setRegistrationStatus(ActivityRegistration.STATUS_CHECKED_IN);
        ActivityVoteSaveDTO dto = new ActivityVoteSaveDTO();
        dto.setActivityId(11L);
        dto.setTitle("Choose time");
        dto.setDescription("Pick the best slot");
        dto.setOptions(List.of("Friday", "Saturday"));
        dto.setExtraUserIds(List.of(902L, 903L));
        dto.setNotifyUsers(true);

        when(activityMapper.selectOne(any())).thenReturn(activity);
        when(registrationMapper.selectList(any())).thenReturn(List.of(first, second));
        doAnswer(invocation -> {
            ActivityVote vote = invocation.getArgument(0);
            vote.setId(31L);
            return 1;
        }).when(voteMapper).insert(any(ActivityVote.class));
        when(activityMapper.selectById(11L)).thenReturn(activity);
        when(optionMapper.selectList(any())).thenReturn(List.of());

        R<ActivityVoteVO> response = controller.createVote(dto);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(3, response.getData().getTargetCount());

        ArgumentCaptor<ActivityVote> voteCaptor = ArgumentCaptor.forClass(ActivityVote.class);
        verify(voteMapper).insert(voteCaptor.capture());
        ActivityVote savedVote = voteCaptor.getValue();
        assertEquals(5L, savedVote.getTenantId());
        assertEquals(77L, savedVote.getCreator());
        assertEquals("[901,902,903]", savedVote.getTargetUserIds());
        assertEquals(1, savedVote.getNotifySent());

        ArgumentCaptor<ActivityVoteOption> optionCaptor = ArgumentCaptor.forClass(ActivityVoteOption.class);
        verify(optionMapper, org.mockito.Mockito.times(2)).insert(optionCaptor.capture());
        assertEquals(List.of("Friday", "Saturday"), optionCaptor.getAllValues().stream().map(ActivityVoteOption::getOptionLabel).collect(Collectors.toList()));

        ArgumentCaptor<NotificationInternalSaveDTO> notificationCaptor = ArgumentCaptor.forClass(NotificationInternalSaveDTO.class);
        verify(userClient).saveNotificationInternal(notificationCaptor.capture());
        NotificationInternalSaveDTO notification = notificationCaptor.getValue();
        assertEquals(5L, notification.getTenantId());
        assertEquals("activity", notification.getType());
        assertEquals(1, notification.getTargetType());
        assertEquals(List.of(901L, 902L, 903L), notification.getTargetIds());
    }

    @Test
    void createVote_shouldNotBroadcastWhenNotifyEnabledButNoTargets() {
        TenantContext.setTenantId(5L);
        UserContext.setUser(77L);
        Activity activity = new Activity()
                .setId(11L)
                .setActivityName("Project Night");
        activity.setTenantId(5L);
        ActivityVoteSaveDTO dto = new ActivityVoteSaveDTO();
        dto.setActivityId(11L);
        dto.setTitle("Choose time");
        dto.setOptions(List.of("Friday", "Saturday"));
        dto.setNotifyUsers(true);

        when(activityMapper.selectOne(any())).thenReturn(activity);
        when(registrationMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            ActivityVote vote = invocation.getArgument(0);
            vote.setId(32L);
            return 1;
        }).when(voteMapper).insert(any(ActivityVote.class));
        when(activityMapper.selectById(11L)).thenReturn(activity);
        when(optionMapper.selectList(any())).thenReturn(List.of());

        R<ActivityVoteVO> response = controller.createVote(dto);

        assertEquals(200, response.getCode());
        ArgumentCaptor<ActivityVote> voteCaptor = ArgumentCaptor.forClass(ActivityVote.class);
        verify(voteMapper).insert(voteCaptor.capture());
        assertEquals(0, voteCaptor.getValue().getNotifySent());
        verify(userClient, never()).saveNotificationInternal(any(NotificationInternalSaveDTO.class));
    }

    @Test
    void closeVote_shouldPersistClosedStatusForCurrentTenant() {
        TenantContext.setTenantId(5L);
        UserContext.setUser(77L);
        ActivityVote vote = new ActivityVote()
                .setId(31L)
                .setActivityId(11L)
                .setTitle("Choose time")
                .setStatus(ActivityVote.STATUS_ACTIVE)
                .setTargetUserIds("[901]");
        vote.setTenantId(5L);
        when(voteMapper.selectOne(any())).thenReturn(vote);
        when(activityMapper.selectById(11L)).thenReturn(new Activity().setId(11L).setActivityName("Project Night"));
        when(optionMapper.selectList(any())).thenReturn(List.of());

        R<ActivityVoteVO> response = controller.closeVote(31L);

        assertEquals(200, response.getCode());
        assertEquals(ActivityVote.STATUS_CLOSED, response.getData().getStatus());
        verify(voteMapper).updateById(vote);
        assertEquals(77L, vote.getUpdater());
        assertNotNull(vote.getClosedTime());
    }
}
