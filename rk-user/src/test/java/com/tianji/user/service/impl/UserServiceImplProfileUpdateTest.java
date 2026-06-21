package com.tianji.user.service.impl;

import com.tianji.api.dto.auth.CurrentUserPasswordUpdateDTO;
import com.tianji.common.autoconfigure.media.MediaPathHelper;
import com.tianji.common.autoconfigure.media.MediaPathProperties;
import com.tianji.common.enums.UserType;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.UserFormDTO;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.po.UserDetail;
import com.tianji.user.enums.UserStatus;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.ICodeService;
import com.tianji.user.service.IUserDetailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplProfileUpdateTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ICodeService codeService;
    @Mock
    private com.tianji.api.client.auth.AuthClient authClient;
    @Mock
    private IUserDetailService detailService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        ReflectionTestUtils.setField(userService, "mediaPathHelper", mediaPathHelper());
        UserContext.setUser(8L);
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    private MediaPathHelper mediaPathHelper() {
        MediaPathProperties properties = new MediaPathProperties();
        properties.setEndpoint("http://cdn.test");
        properties.setBucketName("rk-bucket");
        properties.setPublicBaseUrl("http://cdn.test/rk-bucket/");
        return new MediaPathHelper(properties);
    }

    @Test
    void updateUserWithPassword_shouldSyncAvatarAndProfileFieldsToLocalUserTables() {
        User currentUser = new User();
        currentUser.setId(8L);
        currentUser.setAuthUserId(8L);
        currentUser.setTenantId(1L);
        currentUser.setUsername("member_a");
        currentUser.setRealName("旧名字");
        currentUser.setNickname("旧名字");
        currentUser.setAvatar("http://old/avatar.png");
        currentUser.setCellPhone("13800138003");
        currentUser.setEmail("old@example.com");
        currentUser.setGender(0);
        currentUser.setType(UserType.STUDENT);
        currentUser.setStatus(UserStatus.NORMAL);

        when(userMapper.selectOne(any())).thenReturn(currentUser);
        UserDetail existingDetail = new UserDetail();
        existingDetail.setId(18L);
        existingDetail.setUserId(8L);
        when(detailService.getOne(any())).thenReturn(existingDetail);

        UserFormDTO dto = new UserFormDTO();
        dto.setName("新名字");
        dto.setUsername("member_a");
        dto.setCellPhone("13900139000");
        dto.setEmail("new@example.com");
        dto.setGender(1);
        dto.setIcon("http://minio/rk-files/users/avatars/new-avatar.png");
        dto.setIntro("新的个人介绍");

        userService.updateUserWithPassword(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(8L, userCaptor.getValue().getId());
        assertEquals("新名字", userCaptor.getValue().getRealName());
        assertEquals("新名字", userCaptor.getValue().getNickname());
        assertEquals("13900139000", userCaptor.getValue().getCellPhone());
        assertEquals("new@example.com", userCaptor.getValue().getEmail());
        assertEquals(Integer.valueOf(1), userCaptor.getValue().getGender());
        assertEquals("rk-files/users/avatars/new-avatar.png", userCaptor.getValue().getAvatar());

        ArgumentCaptor<UserDetail> detailCaptor = ArgumentCaptor.forClass(UserDetail.class);
        verify(detailService).updateById(detailCaptor.capture());
        assertEquals(18L, detailCaptor.getValue().getId());
        assertEquals(8L, detailCaptor.getValue().getUserId());
        assertEquals("新的个人介绍", detailCaptor.getValue().getIntroduction());
    }

    @Test
    void updateUserWithPassword_shouldNotBlankOutExistingPhoneWhenCellPhoneIsEmpty() {
        User currentUser = new User();
        currentUser.setId(8L);
        currentUser.setAuthUserId(8L);
        currentUser.setTenantId(1L);
        currentUser.setUsername("member_a");
        currentUser.setRealName("旧名字");
        currentUser.setNickname("旧名字");
        currentUser.setAvatar("http://old/avatar.png");
        currentUser.setCellPhone("13800138003");
        currentUser.setEmail("old@example.com");
        currentUser.setGender(0);
        currentUser.setType(UserType.STUDENT);
        currentUser.setStatus(UserStatus.NORMAL);

        when(userMapper.selectOne(any())).thenReturn(currentUser);
        UserDetail existingDetail = new UserDetail();
        existingDetail.setId(18L);
        existingDetail.setUserId(8L);
        when(detailService.getOne(any())).thenReturn(existingDetail);

        UserFormDTO dto = new UserFormDTO();
        dto.setUsername("member_a");
        dto.setName("新名字");
        dto.setCellPhone("");
        dto.setIcon("http://minio/rk-bucket/users/avatars/new-avatar.png");

        userService.updateUserWithPassword(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals("13800138003", userCaptor.getValue().getCellPhone());
        assertEquals("rk-bucket/users/avatars/new-avatar.png", userCaptor.getValue().getAvatar());
    }

    @Test
    void updateUserWithPassword_shouldPersistRelativeAvatarPathWithoutConvertingIt() {
        User currentUser = new User();
        currentUser.setId(8L);
        currentUser.setAuthUserId(8L);
        currentUser.setTenantId(1L);
        currentUser.setUsername("member_a");
        currentUser.setRealName("old-name");
        currentUser.setNickname("old-name");
        currentUser.setAvatar("rk-user/avatar/2026/04/20/old.png");
        currentUser.setCellPhone("13800138003");
        currentUser.setEmail("old@example.com");
        currentUser.setGender(0);
        currentUser.setType(UserType.STUDENT);
        currentUser.setStatus(UserStatus.NORMAL);

        when(userMapper.selectOne(any())).thenReturn(currentUser);
        UserDetail existingDetail = new UserDetail();
        existingDetail.setId(18L);
        existingDetail.setUserId(8L);
        when(detailService.getOne(any())).thenReturn(existingDetail);

        UserFormDTO dto = new UserFormDTO();
        dto.setUsername("member_a");
        dto.setName("new-name");
        dto.setIcon("rk-user/avatar/2026/04/24/new-avatar.png");

        userService.updateUserWithPassword(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals("rk-user/avatar/2026/04/24/new-avatar.png", userCaptor.getValue().getAvatar());
    }

    @Test
    void updateUserWithPassword_shouldDelegatePasswordChangeToAuthServiceAndSyncLocalPassword() {
        User currentUser = new User();
        currentUser.setId(8L);
        currentUser.setAuthUserId(108L);
        currentUser.setTenantId(1L);
        currentUser.setUsername("member_a");
        currentUser.setRealName("old-name");
        currentUser.setNickname("old-name");
        currentUser.setAvatar("rk-user/avatar/2026/04/20/old.png");
        currentUser.setCellPhone("13800138003");
        currentUser.setEmail("old@example.com");
        currentUser.setGender(0);
        currentUser.setType(UserType.STUDENT);
        currentUser.setStatus(UserStatus.NORMAL);

        when(userMapper.selectOne(any())).thenReturn(currentUser);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new-pass");
        UserDetail existingDetail = new UserDetail();
        existingDetail.setId(18L);
        existingDetail.setUserId(8L);
        when(detailService.getOne(any())).thenReturn(existingDetail);

        UserFormDTO dto = new UserFormDTO();
        dto.setUsername("member_a");
        dto.setName("new-name");
        dto.setOldPassword("old-pass");
        dto.setPassword("new-pass");

        userService.updateUserWithPassword(dto);

        verify(authClient).updateCurrentUserPassword(eq(108L), argThat((CurrentUserPasswordUpdateDTO payload) ->
                payload != null
                        && "old-pass".equals(payload.getOldPassword())
                        && "new-pass".equals(payload.getNewPassword())
        ));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, atLeast(2)).updateById(userCaptor.capture());
        assertEquals("encoded-new-pass", userCaptor.getAllValues().get(0).getPassword());
        assertNotNull(userCaptor.getAllValues().get(0).getUpdateTime());
    }

    @Test
    void updateUserWithPassword_shouldSyncSharedProfileToAllLinkedTenantUsers() {
        User currentUser = new User();
        currentUser.setId(8L);
        currentUser.setAuthUserId(108L);
        currentUser.setTenantId(1L);
        currentUser.setUsername("member_a");
        currentUser.setRealName("old-name");
        currentUser.setNickname("old-name");
        currentUser.setAvatar("rk-user/avatar/2026/04/20/old.png");
        currentUser.setCellPhone("13800138003");
        currentUser.setEmail("old@example.com");
        currentUser.setGender(0);
        currentUser.setType(UserType.STUDENT);
        currentUser.setStatus(UserStatus.NORMAL);

        User linkedTenantUser = new User();
        linkedTenantUser.setId(18L);
        linkedTenantUser.setAuthUserId(108L);
        linkedTenantUser.setTenantId(2L);
        linkedTenantUser.setUsername("member_a");
        linkedTenantUser.setRealName("tenant-two-old");
        linkedTenantUser.setNickname("tenant-two-old");
        linkedTenantUser.setAvatar("rk-user/avatar/2026/04/20/old-2.png");
        linkedTenantUser.setCellPhone("13800138004");
        linkedTenantUser.setEmail("old-2@example.com");
        linkedTenantUser.setGender(0);
        linkedTenantUser.setType(UserType.STUDENT);
        linkedTenantUser.setStatus(UserStatus.NORMAL);

        when(userMapper.selectOne(any())).thenReturn(currentUser);
        when(userMapper.selectList(any())).thenReturn(java.util.List.of(currentUser, linkedTenantUser));

        UserDetail currentDetail = new UserDetail();
        currentDetail.setId(28L);
        currentDetail.setUserId(8L);
        currentDetail.setTenantId(1L);
        currentDetail.setIntroduction("old-intro");

        UserDetail linkedDetail = new UserDetail();
        linkedDetail.setId(38L);
        linkedDetail.setUserId(18L);
        linkedDetail.setTenantId(2L);
        linkedDetail.setIntroduction("tenant-two-old-intro");

        when(detailService.getOne(any())).thenReturn(currentDetail, linkedDetail);

        UserFormDTO dto = new UserFormDTO();
        dto.setUsername("member_a");
        dto.setName("shared-name");
        dto.setCellPhone("13900139000");
        dto.setEmail("shared@example.com");
        dto.setGender(1);
        dto.setIcon("rk-user/avatar/2026/04/25/shared.png");
        dto.setIntro("shared-intro");

        userService.updateUserWithPassword(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, atLeast(2)).updateById(userCaptor.capture());
        assertTrue(userCaptor.getAllValues().stream().anyMatch(item ->
                Long.valueOf(8L).equals(item.getId())
                        && "shared-name".equals(item.getRealName())
                        && "shared@example.com".equals(item.getEmail())
        ));
        assertTrue(userCaptor.getAllValues().stream().anyMatch(item ->
                Long.valueOf(18L).equals(item.getId())
                        && "shared-name".equals(item.getRealName())
                        && "13900139000".equals(item.getCellPhone())
                        && "rk-user/avatar/2026/04/25/shared.png".equals(item.getAvatar())
        ), "linked tenant profile should be updated too");

        ArgumentCaptor<UserDetail> detailCaptor = ArgumentCaptor.forClass(UserDetail.class);
        verify(detailService, atLeast(2)).updateById(detailCaptor.capture());
        assertTrue(detailCaptor.getAllValues().stream().anyMatch(item ->
                Long.valueOf(28L).equals(item.getId()) && "shared-intro".equals(item.getIntroduction())
        ));
        assertTrue(detailCaptor.getAllValues().stream().anyMatch(item ->
                Long.valueOf(38L).equals(item.getId()) && "shared-intro".equals(item.getIntroduction())
        ), "linked tenant detail should share the same introduction");
    }
}
