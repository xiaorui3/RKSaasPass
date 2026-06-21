package com.tianji.activity.controller;

import com.tianji.activity.domain.po.Activity;
import com.tianji.activity.domain.po.ActivityAlbumPhoto;
import com.tianji.activity.domain.po.ActivityRegistration;
import com.tianji.activity.domain.vo.ActivityRegistrationVO;
import com.tianji.activity.mapper.ActivityAlbumPhotoMapper;
import com.tianji.activity.mapper.ActivityRegistrationMapper;
import com.tianji.activity.service.IActivityService;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityControllerRegistrationEnrichmentTest {

    @Mock
    private IActivityService activityService;
    @Mock
    private ActivityRegistrationMapper registrationMapper;
    @Mock
    private ActivityAlbumPhotoMapper albumPhotoMapper;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private ActivityController controller;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.removeUser();
    }

    @Test
    void getRegistrationList_shouldReturnFallbackIdentityFieldsWhenUserProfileIsPartial() {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setActivityName("Smoke Activity");

        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(20L);
        registration.setActivityId(10L);
        registration.setUserId(99L);
        registration.setRegistrationStatus(ActivityRegistration.STATUS_REGISTERED);
        registration.setRegistrationTime(LocalDateTime.now());

        UserDTO user = new UserDTO();
        user.setAuthUserId(99L);
        user.setUsername("admin_a");
        user.setName("Admin A");

        when(activityService.getActivityById(10L)).thenReturn(activity);
        when(activityService.getRegistrationList(10L)).thenReturn(List.of(registration));
        when(userClient.queryUsersByAuthIds(List.of(99L))).thenReturn(List.of(user));

        R<List<ActivityRegistrationVO>> response = controller.getRegistrationList(10L);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        ActivityRegistrationVO vo = response.getData().get(0);
        assertEquals("Admin A", vo.getUserName());
        assertEquals("-", vo.getStudentId());
        assertEquals("-", vo.getEmail());
        assertEquals("-", vo.getCellPhone());
    }

    @Test
    void getRegistrationList_shouldReturnFallbackIdentityFieldsWhenUserServiceHasNoProfile() {
        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(21L);
        registration.setActivityId(11L);
        registration.setUserId(100L);
        registration.setRegistrationStatus(ActivityRegistration.STATUS_REGISTERED);

        when(activityService.getActivityById(11L)).thenReturn(null);
        when(activityService.getRegistrationList(11L)).thenReturn(List.of(registration));
        when(userClient.queryUsersByAuthIds(List.of(100L))).thenReturn(List.of());

        R<List<ActivityRegistrationVO>> response = controller.getRegistrationList(11L);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        ActivityRegistrationVO vo = response.getData().get(0);
        assertEquals("用户-100", vo.getUserName());
        assertEquals("-", vo.getStudentId());
        assertEquals("-", vo.getEmail());
        assertEquals("-", vo.getCellPhone());
    }

    @Test
    void getActivityAlbums_shouldReturnMappedPhotoRecords() {
        ActivityAlbumPhoto photo = new ActivityAlbumPhoto();
        photo.setId(30L);
        photo.setActivityId(10L);
        photo.setPhotoUrl("gallery/demo.png");
        when(albumPhotoMapper.selectList(any())).thenReturn(List.of(photo));

        R<List<ActivityAlbumPhoto>> response = controller.getActivityAlbums(10L);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("gallery/demo.png", response.getData().get(0).getPhotoUrl());
    }

    @Test
    void createActivityAlbum_shouldPersistTenantAndAuditFields() {
        TenantContext.setTenantId(3L);
        UserContext.setUser(12L);
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setTenantId(5L);
        when(activityService.getActivityById(10L)).thenReturn(activity);

        ActivityAlbumPhoto photo = new ActivityAlbumPhoto();
        photo.setPhotoUrl("gallery/demo.png");
        photo.setFileName("demo.png");

        R<ActivityAlbumPhoto> response = controller.createActivityAlbum(10L, photo);

        assertEquals(200, response.getCode());
        ArgumentCaptor<ActivityAlbumPhoto> captor = ArgumentCaptor.forClass(ActivityAlbumPhoto.class);
        verify(albumPhotoMapper).insert(captor.capture());
        ActivityAlbumPhoto saved = captor.getValue();
        assertEquals(10L, saved.getActivityId());
        assertEquals(5L, saved.getTenantId());
        assertEquals(12L, saved.getUploaderId());
        assertEquals(12L, saved.getCreator());
        assertEquals(12L, saved.getUpdater());
        assertEquals(0, saved.getSortOrder());
        assertEquals(0, saved.getIsDeleted());
    }

    @Test
    void deleteActivityAlbum_shouldReturnSuccessWhenMapperDeletesRecord() {
        when(albumPhotoMapper.deleteById(30L)).thenReturn(1);

        R<String> response = controller.deleteActivityAlbum(30L);

        assertEquals(200, response.getCode());
        verify(albumPhotoMapper).deleteById(30L);
    }
}
