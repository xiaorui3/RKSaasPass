package com.tianji.activity.controller;

import com.tianji.activity.domain.po.Activity;
import com.tianji.activity.domain.po.ActivityRegistration;
import com.tianji.activity.mapper.ActivityRegistrationMapper;
import com.tianji.activity.service.IActivityService;
import com.tianji.api.client.user.UserClient;
import com.tianji.common.domain.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityControllerCreditGrantTest {

    @Mock
    private IActivityService activityService;

    @Mock
    private ActivityRegistrationMapper registrationMapper;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private ActivityController controller;

    @Test
    void grantActivityCredits_shouldOnlyGrantCheckedInMembers() {
        Activity activity = new Activity();
        activity.setId(15L);
        activity.setActivityName("春季志愿活动");
        activity.setPoints(3);
        activity.setActivityStatus(Activity.STATUS_ENDED);
        activity.setEndTime(LocalDateTime.now().minusHours(1));

        ActivityRegistration checkedIn = new ActivityRegistration();
        checkedIn.setActivityId(15L);
        checkedIn.setUserId(101L);
        checkedIn.setRegistrationStatus(ActivityRegistration.STATUS_CHECKED_IN);

        ActivityRegistration registered = new ActivityRegistration();
        registered.setActivityId(15L);
        registered.setUserId(202L);
        registered.setRegistrationStatus(ActivityRegistration.STATUS_REGISTERED);

        when(activityService.getActivityById(15L)).thenReturn(activity);
        when(activityService.getRegistrationList(15L)).thenReturn(List.of(checkedIn, registered));
        when(userClient.grantCredits(anyList())).thenReturn(1);

        R<String> response = controller.grantActivityCredits(15L);

        assertEquals(200, response.getCode());
        verify(userClient).grantCredits(argThat(grants ->
                grants.size() == 1
                        && Long.valueOf(101L).equals(grants.get(0).getUserId())
                        && "activity".equals(grants.get(0).getSourceType())
                        && Long.valueOf(15L).equals(grants.get(0).getSourceId())
                        && "activity".equals(grants.get(0).getCreditTypeCode())
                        && BigDecimal.valueOf(3).compareTo(grants.get(0).getCreditScore()) == 0
        ));
    }

    @Test
    void grantActivityCredits_shouldRejectWhenActivityNotEnded() {
        Activity activity = new Activity();
        activity.setId(15L);
        activity.setActivityName("春季志愿活动");
        activity.setPoints(3);
        activity.setActivityStatus(Activity.STATUS_ONGOING);
        activity.setEndTime(LocalDateTime.now().plusHours(2));

        when(activityService.getActivityById(15L)).thenReturn(activity);

        R<String> response = controller.grantActivityCredits(15L);

        assertEquals(0, response.getCode());
        assertEquals("活动结束后才能发放学分", response.getMsg());
        verify(userClient, never()).grantCredits(anyList());
    }
}
