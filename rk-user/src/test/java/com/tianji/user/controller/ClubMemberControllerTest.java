package com.tianji.user.controller;

import com.tianji.user.service.IClubMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClubMemberControllerTest {

    @Mock
    private IClubMemberService clubMemberService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClubMemberController(clubMemberService)).build();
    }

    @Test
    void applicationsAlias_shouldReturnApplicationListInsteadOfMatchingDetailRoute() throws Exception {
        when(clubMemberService.getAllApplications()).thenReturn(List.of());

        mockMvc.perform(get("/api/members/applications")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(clubMemberService).getAllApplications();
    }

    @Test
    void list_shouldPassExplicitTenantIdToServiceForMobileTenantIsolation() throws Exception {
        when(clubMemberService.getAllApplications(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/members/list")
                        .param("tenantId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(clubMemberService).getAllApplications(1L);
    }
}
