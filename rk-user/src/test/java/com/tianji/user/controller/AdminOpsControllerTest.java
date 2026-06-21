package com.tianji.user.controller;

import com.tianji.user.domain.dto.adminops.AdminJenkinsBuildRequestDTO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsBuildStatusVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsBuildTriggerVO;
import com.tianji.user.domain.vo.adminops.AdminApiWorkbenchDocVO;
import com.tianji.user.domain.vo.adminops.AdminApiWorkbenchResourceVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsGitBranchesVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsGitSourceVO;
import com.tianji.user.domain.vo.adminops.AdminJenkinsOverviewVO;
import com.tianji.user.service.IAdminOpsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOpsControllerTest {

    @Mock
    private IAdminOpsService adminOpsService;

    @InjectMocks
    private AdminOpsController controller;

    @Test
    void apiWorkbenchResourcesEndpoint_shouldReturnMappedSwaggerResources() throws Exception {
        AdminApiWorkbenchResourceVO resource = new AdminApiWorkbenchResourceVO();
        resource.setName("rk-user");
        resource.setUrl("/users/v2/api-docs");
        when(adminOpsService.getApiWorkbenchResources()).thenReturn(List.of(resource));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/admin/ops/api-workbench/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("rk-user"))
                .andExpect(jsonPath("$.data[0].url").value("/users/v2/api-docs"));
    }

    @Test
    void apiWorkbenchDocEndpoint_shouldReturnRequestedDocPayload() throws Exception {
        AdminApiWorkbenchDocVO doc = new AdminApiWorkbenchDocVO();
        doc.setSourceUrl("/users/v2/api-docs");
        doc.setServiceName("澶╂満瀛﹀爞 - 鐢ㄦ埛涓績鎺ュ彛鏂囨。");
        doc.setBasePath("/");
        when(adminOpsService.getApiWorkbenchDoc("/users/v2/api-docs")).thenReturn(doc);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/admin/ops/api-workbench/docs").param("url", "/users/v2/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceUrl").value("/users/v2/api-docs"))
                .andExpect(jsonPath("$.data.serviceName").value("澶╂満瀛﹀爞 - 鐢ㄦ埛涓績鎺ュ彛鏂囨。"))
                .andExpect(jsonPath("$.data.basePath").value("/"));

        verify(adminOpsService).getApiWorkbenchDoc("/users/v2/api-docs");
    }

    @Test
    void jenkinsOverviewEndpoint_shouldExposeOverviewPayload() throws Exception {
        AdminJenkinsOverviewVO overview = new AdminJenkinsOverviewVO();
        overview.setSourceUrl("http://127.0.0.1:18080");
        when(adminOpsService.getJenkinsOverview()).thenReturn(overview);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/admin/ops/jenkins/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceUrl").value("http://127.0.0.1:18080"));
    }

    @Test
    void jenkinsGitSourcesEndpoint_shouldExposeSelectableRepositories() throws Exception {
        AdminJenkinsGitSourceVO local = new AdminJenkinsGitSourceVO();
        local.setSource("local");
        local.setLabel("鏈湴 Gogs");
        local.setDefaultSource(true);
        when(adminOpsService.getJenkinsGitSources()).thenReturn(List.of(local));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/admin/ops/jenkins/git-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].source").value("local"))
                .andExpect(jsonPath("$.data[0].label").value("鏈湴 Gogs"))
                .andExpect(jsonPath("$.data[0].defaultSource").value(true));

        verify(adminOpsService).getJenkinsGitSources();
    }

    @Test
    void jenkinsGitBranchesEndpoint_shouldExposeBranchesForSelectedSource() throws Exception {
        AdminJenkinsGitBranchesVO branches = new AdminJenkinsGitBranchesVO();
        branches.setSource("gitee");
        branches.setLabel("Gitee");
        branches.setBranches(List.of("cloud-master-new", "feature/demo"));
        when(adminOpsService.getJenkinsGitBranches("gitee")).thenReturn(branches);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/admin/ops/jenkins/git-branches").param("source", "gitee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("gitee"))
                .andExpect(jsonPath("$.data.label").value("Gitee"))
                .andExpect(jsonPath("$.data.branches[0]").value("cloud-master-new"));

        verify(adminOpsService).getJenkinsGitBranches("gitee");
    }

    @Test
    void triggerJenkinsJobEndpoint_shouldDelegateJobTrigger() throws Exception {
        AdminJenkinsBuildTriggerVO trigger = new AdminJenkinsBuildTriggerVO();
        trigger.setAccepted(true);
        trigger.setQueueId("321");
        when(adminOpsService.triggerJenkinsJob(org.mockito.ArgumentMatchers.eq("rk-web-cloud-master-new"), org.mockito.ArgumentMatchers.any(AdminJenkinsBuildRequestDTO.class)))
                .thenReturn(trigger);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/admin/ops/jenkins/jobs/rk-web-cloud-master-new/trigger")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"services\":\"rk-user\",\"imageMode\":\"local\",\"targetBranch\":\"cloud-master-new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.queueId").value("321"));

        verify(adminOpsService).triggerJenkinsJob(org.mockito.ArgumentMatchers.eq("rk-web-cloud-master-new"), org.mockito.ArgumentMatchers.any(AdminJenkinsBuildRequestDTO.class));
    }

    @Test
    void jenkinsBuildStatusEndpoint_shouldReturnBuildProgressAndLogs() throws Exception {
        AdminJenkinsBuildStatusVO status = new AdminJenkinsBuildStatusVO();
        status.setQueueId("321");
        status.setBuildNumber(15);
        status.setStatus("running");
        status.setLogTail("building...");
        when(adminOpsService.getJenkinsBuildStatus("rk-web-cloud-master-new", "321", null, 120)).thenReturn(status);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/admin/ops/jenkins/jobs/rk-web-cloud-master-new/build-status")
                        .param("queueId", "321")
                        .param("tailLines", "120"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.queueId").value("321"))
                .andExpect(jsonPath("$.data.buildNumber").value(15))
                .andExpect(jsonPath("$.data.logTail").value("building..."));

        verify(adminOpsService).getJenkinsBuildStatus("rk-web-cloud-master-new", "321", null, 120);
    }
}
