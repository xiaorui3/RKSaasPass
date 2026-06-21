package com.tianji.user.service.impl;

import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.po.UserDetail;
import com.tianji.user.domain.vo.UserDetailVO;
import com.tianji.user.enums.UserStatus;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.ICodeService;
import com.tianji.user.service.IUserDetailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplMyInfoTest {

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
        UserContext.setUser(108L);
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void myInfo_shouldPreferAuthRoleNameWhenLocalUserTypeIsStudentButAuthRoleIsAdmin() {
        User currentUser = new User();
        currentUser.setId(8L);
        currentUser.setAuthUserId(108L);
        currentUser.setTenantId(1L);
        currentUser.setUsername("admin_a");
        currentUser.setRealName("admin_a");
        currentUser.setType(UserType.STUDENT);
        currentUser.setStatus(UserStatus.NORMAL);

        UserDetail detail = new UserDetail();
        detail.setId(8L);
        detail.setUserId(8L);
        detail.setUsername("admin_a");
        detail.setType(UserType.STUDENT.name());

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(1L);

        when(userMapper.selectOne(any())).thenReturn(currentUser);
        when(detailService.queryById(8L)).thenReturn(detail);
        when(authClient.queryAdminUserById(108L)).thenReturn(authUser);
        when(authClient.queryRoleById(1L)).thenReturn(new RoleDTO().setId(1L).setName("超级管理员"));

        UserDetailVO result = userService.myInfo();

        assertEquals("超级管理员", result.getRoleName());
    }
    @Test
    void myInfo_shouldNotQueryRoleWithNullIdWhenStaffDetailHasNoLegacyRoleId() {
        User currentUser = new User();
        currentUser.setId(30L);
        currentUser.setAuthUserId(1530L);
        currentUser.setTenantId(30L);
        currentUser.setUsername("manager_c");
        currentUser.setRealName("manager_c");
        currentUser.setType(UserType.STAFF);
        currentUser.setStatus(UserStatus.NORMAL);

        UserDetail detail = new UserDetail();
        detail.setId(30L);
        detail.setUserId(30L);
        detail.setUsername("manager_c");
        detail.setType(UserType.STAFF.name());

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(153L);

        when(userMapper.selectOne(any())).thenReturn(currentUser);
        when(detailService.queryById(30L)).thenReturn(detail);
        when(authClient.queryAdminUserById(1530L)).thenReturn(authUser);
        when(authClient.queryRoleById(153L)).thenReturn(null);

        UserDetailVO result = userService.myInfo();

        assertEquals("\u79df\u6237\u7ba1\u7406\u5458", result.getRoleName());
        verify(authClient, never()).queryRoleById(isNull());
    }
}
