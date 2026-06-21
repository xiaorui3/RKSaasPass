package com.tianji.user.controller;

import com.tianji.user.domain.vo.ScopedUserStatisticsVO;
import com.tianji.user.domain.vo.PeopleDomainReconcileResultVO;
import com.tianji.user.domain.vo.UserDetailVO;
import com.tianji.user.service.IUserDetailService;
import com.tianji.user.service.IUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private IUserService userService;

    @Mock
    private IUserDetailService detailService;

    @InjectMocks
    private UserController controller;

    @Test
    void infoEndpoint_shouldReturnCurrentUserProfile() throws Exception {
        UserDetailVO detail = new UserDetailVO();
        detail.setId(6L);
        detail.setName("admin_a");
        detail.setUsername("admin_a");

        when(userService.myInfo()).thenReturn(detail);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/users/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6L))
                .andExpect(jsonPath("$.name").value("admin_a"))
                .andExpect(jsonPath("$.username").value("admin_a"));
    }

    @Test
    void reconcilePeopleDomainEndpoint_shouldTriggerServiceRepair() throws Exception {
        PeopleDomainReconcileResultVO result = new PeopleDomainReconcileResultVO();
        result.setScannedAccountCount(3L);
        result.setCreatedMemberCount(2L);
        result.setCreatedUserCount(1L);
        result.setSummary("修复完成：新增成员台账 2，新增用户账号 1");

        when(userService.reconcilePeopleDomainForCurrentScope()).thenReturn(result);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/users/reconcile/people-domain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scannedAccountCount").value(3L))
                .andExpect(jsonPath("$.data.createdMemberCount").value(2L))
                .andExpect(jsonPath("$.data.createdUserCount").value(1L))
                .andExpect(jsonPath("$.data.summary").value("修复完成：新增成员台账 2，新增用户账号 1"));

        verify(userService).reconcilePeopleDomainForCurrentScope();
    }

    @Test
    void scopedStatisticsEndpoint_shouldReturnScopedStatisticsPayload() throws Exception {
        ScopedUserStatisticsVO statistics = new ScopedUserStatisticsVO();
        statistics.setAccountCount(61L);
        statistics.setMemberLinkedAccountCount(42L);

        when(userService.queryScopedUserStatistics(1L)).thenReturn(statistics);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/users/statistics/scoped").param("tenantId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountCount").value(61L))
                .andExpect(jsonPath("$.data.memberLinkedAccountCount").value(42L));
    }
}
