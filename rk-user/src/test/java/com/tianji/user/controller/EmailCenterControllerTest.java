package com.tianji.user.controller;

import com.tianji.user.service.IEmailCenterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailCenterControllerTest {

    @Mock
    private IEmailCenterService emailCenterService;

    @InjectMocks
    private EmailCenterController controller;

    @Test
    void sendInternal_shouldMapApiDtoAndDelegateToService() {
        com.tianji.api.dto.user.EmailCenterSendDTO request = new com.tianji.api.dto.user.EmailCenterSendDTO();
        request.setRoleIds(List.of(3L, 8L));
        request.setActivityId(100L);
        request.setManualEmails(List.of("admin_a@test.com"));
        request.setSubject("活动发布通知");
        request.setContent("<p>content</p>");
        request.setHtml(true);

        when(emailCenterService.send(any(com.tianji.user.domain.dto.EmailCenterSendDTO.class)))
                .thenReturn(Map.of("taskId", 1L));

        Boolean result = controller.sendInternal(request);

        assertTrue(Boolean.TRUE.equals(result));

        ArgumentCaptor<com.tianji.user.domain.dto.EmailCenterSendDTO> captor =
                ArgumentCaptor.forClass(com.tianji.user.domain.dto.EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        com.tianji.user.domain.dto.EmailCenterSendDTO actual = captor.getValue();
        assertEquals(request.getRoleIds(), actual.getRoleIds());
        assertEquals(request.getActivityId(), actual.getActivityId());
        assertEquals(request.getManualEmails(), actual.getManualEmails());
        assertEquals(request.getSubject(), actual.getSubject());
        assertEquals(request.getContent(), actual.getContent());
        assertEquals(request.getHtml(), actual.getHtml());
    }
}
