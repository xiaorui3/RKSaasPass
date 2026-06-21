package com.tianji.activity.controller;

import com.tianji.activity.domain.po.Competition;
import com.tianji.activity.mapper.CompetitionParticipantMapper;
import com.tianji.activity.service.ICompetitionService;
import com.tianji.api.client.user.UserClient;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.validation.Validation;
import javax.validation.Validator;
import java.time.LocalDateTime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompetitionControllerRegistrationGuardTest {

    @Mock
    private ICompetitionService competitionService;

    @Mock
    private CompetitionParticipantMapper participantMapper;

    @Mock
    private UserClient userClient;

    private MockMvc mockMvc;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        CompetitionController controller = new CompetitionController(
                competitionService,
                participantMapper,
                userClient,
                validator
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        UserContext.setUser(1L);
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
        TenantContext.clear();
    }

    @Test
    void register_shouldReturnRegistrationStartReasonBeforeReadingRequiredBody() throws Exception {
        Competition competition = new Competition();
        competition.setId(348L);
        competition.setTenantId(1L);
        competition.setTitle("未开始比赛");
        competition.setIsPublished(true);
        competition.setStatus("PUBLISHED");
        competition.setRegistrationStart(LocalDateTime.now().plusDays(1));
        competition.setRegistrationEnd(LocalDateTime.now().plusDays(2));
        competition.setCompetitionStart(LocalDateTime.now().plusDays(3));
        competition.setCompetitionEnd(LocalDateTime.now().plusDays(4));
        competition.setMaxParticipants(50);
        competition.setRegistrationCount(0);

        when(competitionService.getCompetitionById(348L)).thenReturn(competition);

        mockMvc.perform(post("/api/competition/348/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("报名尚未开始"));

        verify(participantMapper, never()).findByCompetitionAndUser(348L, 1L);
    }
}
