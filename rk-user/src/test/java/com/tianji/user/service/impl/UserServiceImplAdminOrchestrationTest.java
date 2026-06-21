package com.tianji.user.service.impl;

import com.tianji.api.client.auth.AuthClient;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.User;
import com.tianji.user.enums.UserStatus;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.ICodeService;
import com.tianji.user.service.IUserDetailService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static com.tianji.user.constants.UserConstants.DEFAULT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplAdminOrchestrationTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ICodeService codeService;
    @Mock
    private AuthClient authClient;
    @Mock
    private IUserDetailService detailService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ClubMemberMapper clubMemberMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void provisionManagedUser_shouldProvisionAuthAccountAndPersistLocalProfile() {
        AdminUserProvisionDTO dto = new AdminUserProvisionDTO();
        dto.setUsername("admin_local_create");
        dto.setPassword("Passw0rd!");
        dto.setType(1);
        dto.setRoleId(7L);
        dto.setCellPhone("13800138000");
        dto.setName("Admin Local");
        dto.setEmail("admin-local@example.com");
        dto.setStatus(1);

        when(authClient.provisionAdminUser(any(AdminUserProvisionDTO.class))).thenReturn(101L);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        doAnswer(invocation -> {
            User entity = invocation.getArgument(0);
            entity.setId(201L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        Long localUserId = userService.provisionManagedUser(dto);

        assertEquals(201L, localUserId);

        ArgumentCaptor<AdminUserProvisionDTO> authCaptor = ArgumentCaptor.forClass(AdminUserProvisionDTO.class);
        verify(authClient).provisionAdminUser(authCaptor.capture());
        assertEquals("admin_local_create", authCaptor.getValue().getUsername());
        assertEquals("Passw0rd!", authCaptor.getValue().getPassword());
        assertEquals(7L, authCaptor.getValue().getRoleId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(101L, userCaptor.getValue().getAuthUserId());

        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberMapper).insert(memberCaptor.capture());
        assertEquals("AUTH-101", memberCaptor.getValue().getStudentId());
        assertEquals("admin-local@example.com", memberCaptor.getValue().getEmail());
    }

    @Test
    void provisionManagedUser_shouldCreateClubMemberForStudentWhenStudentProfileProvided() {
        AdminUserProvisionDTO dto = new AdminUserProvisionDTO();
        dto.setTenantId(2L);
        dto.setUsername("student_sync_create");
        dto.setPassword("Passw0rd!");
        dto.setType(UserType.STUDENT.getValue());
        dto.setRoleId(2L);
        dto.setCellPhone("13800138020");
        dto.setName("Student Sync");
        dto.setEmail("student-sync@example.com");
        dto.setStatus(1);
        setProperty(dto, "studentId", "2026001001");
        setProperty(dto, "major", "Software Engineering");
        setProperty(dto, "grade", "2026");
        setProperty(dto, "department", "宣传部");

        when(authClient.provisionAdminUser(any(AdminUserProvisionDTO.class))).thenReturn(111L);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(clubMemberMapper.selectOne(any())).thenReturn(null);
        when(clubMemberMapper.selectAnyByTenantAndStudentId(2L, "2026001001")).thenReturn(null);
        doAnswer(invocation -> {
            User entity = invocation.getArgument(0);
            entity.setId(211L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        userService.provisionManagedUser(dto);

        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberMapper).insert(memberCaptor.capture());
        assertEquals(2L, memberCaptor.getValue().getTenantId());
        assertEquals("2026001001", memberCaptor.getValue().getStudentId());
        assertEquals("Software Engineering", memberCaptor.getValue().getMajor());
        assertEquals("2026", memberCaptor.getValue().getGrade());
        assertEquals("宣传部", memberCaptor.getValue().getDepartment());
    }

    @Test
    void provisionManagedUser_shouldRejectStudentWithoutStudentId() {
        AdminUserProvisionDTO dto = new AdminUserProvisionDTO();
        dto.setTenantId(2L);
        dto.setUsername("student_without_id");
        dto.setPassword("Passw0rd!");
        dto.setType(UserType.STUDENT.getValue());
        dto.setRoleId(2L);
        dto.setCellPhone("13800138021");
        dto.setName("Student Without Id");
        dto.setEmail("student-without-id@example.com");
        dto.setStatus(1);
        setProperty(dto, "studentId", "   ");

        assertThrows(BadRequestException.class, () -> userService.provisionManagedUser(dto));

        verifyNoInteractions(authClient);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void provisionManagedUser_shouldRejectStudentWithoutEmailBeforeCreatingAuthAccount() {
        AdminUserProvisionDTO dto = new AdminUserProvisionDTO();
        dto.setTenantId(2L);
        dto.setUsername("student_without_email");
        dto.setPassword("Passw0rd!");
        dto.setType(UserType.STUDENT.getValue());
        dto.setRoleId(2L);
        dto.setCellPhone("13800138024");
        dto.setName("Student Without Email");
        dto.setEmail("   ");
        dto.setStatus(1);
        setProperty(dto, "studentId", "2026001024");

        assertThrows(BadRequestException.class, () -> userService.provisionManagedUser(dto));

        verifyNoInteractions(authClient);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void importManagedUsers_shouldRejectStudentWithoutStudentId() {
        AdminUserProvisionDTO validAdmin = new AdminUserProvisionDTO();
        validAdmin.setTenantId(2L);
        validAdmin.setUsername("managed_admin");
        validAdmin.setPassword("Passw0rd!");
        validAdmin.setType(UserType.STAFF.getValue());
        validAdmin.setRoleId(7L);
        validAdmin.setCellPhone("13800138022");
        validAdmin.setName("Managed Admin");
        validAdmin.setEmail("managed-admin@example.com");
        validAdmin.setStatus(1);

        AdminUserProvisionDTO invalidStudent = new AdminUserProvisionDTO();
        invalidStudent.setTenantId(2L);
        invalidStudent.setUsername("managed_student_without_id");
        invalidStudent.setPassword("Passw0rd!");
        invalidStudent.setType(UserType.STUDENT.getValue());
        invalidStudent.setRoleId(2L);
        invalidStudent.setCellPhone("13800138023");
        invalidStudent.setName("Managed Student Without Id");
        invalidStudent.setEmail("managed-student-without-id@example.com");
        invalidStudent.setStatus(1);
        setProperty(invalidStudent, "studentId", "");

        assertThrows(BadRequestException.class, () -> userService.importManagedUsers(java.util.List.of(validAdmin, invalidStudent)));

        verifyNoInteractions(authClient);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void updateManagedUserStatus_shouldSyncAuthAndLocalStatus() {
        User existing = new User();
        existing.setId(202L);
        existing.setAuthUserId(102L);
        when(userMapper.selectById(202L)).thenReturn(existing);

        userService.updateManagedUserStatus(202L, 0);

        verify(authClient).updateAdminUserStatus(102L, 0);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(202L, userCaptor.getValue().getId());
        assertEquals(UserStatus.FROZEN, userCaptor.getValue().getStatus());
    }

    @Test
    void updateManagedUser_shouldKeepExistingUsernameWhenUsernameNotProvided() {
        User existing = new User();
        existing.setId(205L);
        existing.setAuthUserId(105L);
        existing.setTenantId(1L);
        existing.setUsername("existing_username");
        existing.setType(com.tianji.common.enums.UserType.STUDENT);
        existing.setStudentId("2026001998");
        existing.setEmail("existing_username@example.com");
        existing.setStatus(UserStatus.NORMAL);
        when(userMapper.selectById(205L)).thenReturn(existing);

        AdminUserProvisionDTO updateDTO = new AdminUserProvisionDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setCellPhone("13800138005");

        userService.updateManagedUser(205L, updateDTO);

        ArgumentCaptor<AdminUserProvisionDTO> authCaptor = ArgumentCaptor.forClass(AdminUserProvisionDTO.class);
        verify(authClient).updateAdminUser(org.mockito.ArgumentMatchers.eq(105L), authCaptor.capture());
        assertEquals("existing_username", authCaptor.getValue().getUsername());
    }

    @Test
    void resetManagedUserPassword_shouldSyncAuthResetAndUpdateLocalPassword() {
        User existing = new User();
        existing.setId(203L);
        existing.setAuthUserId(103L);
        when(userMapper.selectById(203L)).thenReturn(existing);
        when(passwordEncoder.encode(DEFAULT_PASSWORD)).thenReturn("encoded-default-password");

        userService.resetManagedUserPassword(203L, DEFAULT_PASSWORD);

        verify(authClient).resetAdminUserPassword(103L, DEFAULT_PASSWORD);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(203L, userCaptor.getValue().getId());
        assertEquals("encoded-default-password", userCaptor.getValue().getPassword());
    }

    @Test
    void deleteManagedUser_shouldDeleteAuthAccountAndLocalProfile() {
        User existing = new User();
        existing.setId(204L);
        existing.setAuthUserId(104L);
        when(userMapper.selectById(204L)).thenReturn(existing);

        userService.deleteManagedUser(204L);

        verify(authClient).deleteAdminUser(104L);
        verify(userMapper).deleteById(204L);
        verify(detailService, never()).save(any());
    }

    @Test
    void deleteManagedUser_shouldSoftDeleteMatchingClubMemberForStudent() {
        User existing = new User();
        existing.setId(214L);
        existing.setAuthUserId(114L);
        existing.setTenantId(2L);
        existing.setType(UserType.STUDENT);
        existing.setStudentId("2026001999");
        when(userMapper.selectById(214L)).thenReturn(existing);

        ClubMember member = new ClubMember();
        member.setId(314L);
        member.setTenantId(2L);
        member.setStudentId("2026001999");
        when(clubMemberMapper.selectOne(any())).thenReturn(member);

        userService.deleteManagedUser(214L);

        verify(authClient).deleteAdminUser(114L);
        verify(clubMemberMapper).deleteById(314L);
        verify(userMapper).deleteById(214L);
    }

    private void setProperty(AdminUserProvisionDTO dto, String property, Object value) {
        new BeanWrapperImpl(dto).setPropertyValue(property, value);
    }

}
