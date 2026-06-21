package com.tianji.user.controller;

import com.tianji.user.domain.po.ClubAlumni;
import com.tianji.user.service.IClubAlumniService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClubAlumniControllerTest {

    @Mock
    private IClubAlumniService clubAlumniService;

    @InjectMocks
    private ClubAlumniController controller;

    @Test
    void showOverviewAlias_shouldReturnAlumniOverviewPayload() throws Exception {
        when(clubAlumniService.getShowAlumniOverview()).thenReturn(Map.of(
                "totalCount", 314,
                "rosterCount", 314,
                "alumniCount", 300L,
                "graduatedCount", 314L,
                "currentCount", 0L,
                "generationCount", 10
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/alumni/show-overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(314))
                .andExpect(jsonPath("$.data.rosterCount").value(314))
                .andExpect(jsonPath("$.data.alumniCount").value(300))
                .andExpect(jsonPath("$.data.graduatedCount").value(314))
                .andExpect(jsonPath("$.data.currentCount").value(0))
                .andExpect(jsonPath("$.data.generationCount").value(10));
    }

    @Test
    void showGroupedByGenerationAlias_shouldReturnGroupedGraduatedAlumni() throws Exception {
        ClubAlumni alumni = new ClubAlumni();
        alumni.setId(1L);
        alumni.setName("legacy alumni");
        alumni.setGenerationYear(2020);

        when(clubAlumniService.getShowAlumniGroupedByGeneration()).thenReturn(Map.of(
                2020, List.of(alumni)
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/alumni/show-grouped-by-generation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.2020[0].id").value(1))
                .andExpect(jsonPath("$.data.2020[0].name").value("legacy alumni"));
    }

    @Test
    void nonNumericIdPath_shouldReturnNotFound() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/alumni/not-a-number"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicDisplayEndpoints_shouldNotRequireBackOfficeAlumniAuthorities() throws Exception {
        Method groupedMethod = ClubAlumniController.class.getMethod("getPublicAlumniGroupedByGeneration");
        Method overviewMethod = ClubAlumniController.class.getMethod("getPublicAlumniOverview");

        assertNull(groupedMethod.getAnnotation(PreAuthorize.class));
        assertNull(overviewMethod.getAnnotation(PreAuthorize.class));
    }

    @Test
    void list_shouldPassExplicitTenantIdToServiceForMobileTenantIsolation() throws Exception {
        when(clubAlumniService.getAllAlumni(1L)).thenReturn(List.of());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/alumni/list")
                        .param("tenantId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(clubAlumniService).getAllAlumni(1L);
    }
}
