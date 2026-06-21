package com.tianji.activity.controller;

import com.tianji.activity.domain.po.Competition;
import com.tianji.activity.domain.po.CompetitionParticipant;
import com.tianji.activity.mapper.CompetitionParticipantMapper;
import com.tianji.activity.service.ICompetitionService;
import com.tianji.api.client.user.UserClient;
import com.tianji.common.domain.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitionControllerCreditGrantTest {

    @Mock
    private ICompetitionService competitionService;

    @Mock
    private CompetitionParticipantMapper participantMapper;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private CompetitionController controller;

    @Test
    void grantCompetitionCredits_shouldMapAwardPointsBeforeCallingUserService() {
        Competition competition = new Competition();
        competition.setId(7L);
        competition.setTitle("程序设计竞赛");
        competition.setParticipationPoints(1);
        competition.setFirstPrizePoints(10);
        competition.setSecondPrizePoints(6);
        competition.setThirdPrizePoints(4);
        competition.setExcellentPrizePoints(2);
        competition.setStatus("COMPLETED");
        competition.setCompetitionEnd(LocalDateTime.now().minusHours(1));

        CompetitionParticipant firstPrize = new CompetitionParticipant();
        firstPrize.setCompetitionId(7L);
        firstPrize.setUserId(11L);
        firstPrize.setAward(1);
        firstPrize.setStatus(CompetitionParticipant.STATUS_REGISTERED);

        CompetitionParticipant participation = new CompetitionParticipant();
        participation.setCompetitionId(7L);
        participation.setUserId(12L);
        participation.setAward(null);
        participation.setStatus(CompetitionParticipant.STATUS_REGISTERED);

        when(competitionService.getCompetitionById(7L)).thenReturn(competition);
        when(participantMapper.findCreditCandidatesByCompetitionId(7L)).thenReturn(List.of(firstPrize, participation));
        when(userClient.grantCredits(anyList())).thenReturn(2);

        R<String> response = controller.grantCompetitionCredits(7L);

        assertEquals(200, response.getCode());
        verify(userClient).grantCredits(argThat(grants ->
                grants.size() == 2
                        && Long.valueOf(11L).equals(grants.get(0).getUserId())
                        && BigDecimal.valueOf(10).compareTo(grants.get(0).getCreditScore()) == 0
                        && Long.valueOf(12L).equals(grants.get(1).getUserId())
                        && BigDecimal.valueOf(1).compareTo(grants.get(1).getCreditScore()) == 0
        ));
    }

    @Test
    void grantCompetitionCredits_shouldRejectWhenCompetitionNotCompleted() {
        Competition competition = new Competition();
        competition.setId(7L);
        competition.setTitle("程序设计竞赛");
        competition.setParticipationPoints(1);
        competition.setStatus("PUBLISHED");
        competition.setCompetitionEnd(LocalDateTime.now().plusDays(1));

        when(competitionService.getCompetitionById(7L)).thenReturn(competition);

        R<String> response = controller.grantCompetitionCredits(7L);

        assertEquals(0, response.getCode());
        assertEquals("比赛结束后才能发放学分", response.getMsg());
        verify(userClient, never()).grantCredits(anyList());
    }
}
