package com.tianji.activity.domain.converter;

import com.tianji.activity.domain.po.Competition;
import com.tianji.activity.domain.vo.CompetitionListVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompetitionConverterTest {

    @Test
    void toListVO_shouldExposeTenantIdForMobileTenantFiltering() {
        Competition competition = new Competition();
        competition.setId(12L);
        competition.setTenantId(30L);
        competition.setTitle("tenant scoped competition");

        CompetitionListVO vo = CompetitionConverter.toListVO(competition);

        assertEquals(30L, vo.getTenantId());
    }
}
