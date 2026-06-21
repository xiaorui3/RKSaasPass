package com.tianji.user.service.credit;

import com.tianji.api.dto.user.CreditGrantDTO;
import com.tianji.user.domain.po.CreditType;
import com.tianji.user.domain.po.UserCreditRecord;
import com.tianji.user.domain.po.UserCreditSummary;
import com.tianji.user.domain.po.VolunteerRecord;
import com.tianji.user.domain.vo.CreditTypeBreakdownVO;
import com.tianji.user.mapper.UserCreditRecordMapper;
import com.tianji.user.mapper.UserCreditSummaryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCreditLedgerServiceTest {

    @Mock
    private UserCreditRecordMapper userCreditRecordMapper;

    @Mock
    private UserCreditSummaryMapper userCreditSummaryMapper;

    @InjectMocks
    private UserCreditLedgerService service;

    @Test
    void grantCredits_shouldInsertApprovedRecordAndSummary() {
        CreditGrantDTO grant = new CreditGrantDTO();
        grant.setUserId(8L);
        grant.setTenantId(1L);
        grant.setSourceType("activity");
        grant.setSourceId(12L);
        grant.setCreditTypeCode("activity");
        grant.setCreditHours(BigDecimal.ZERO);
        grant.setCreditScore(new BigDecimal("2.5"));
        grant.setDescription("活动学分");

        UserCreditRecord approvedRecord = new UserCreditRecord();
        approvedRecord.setUserId(8L);
        approvedRecord.setTenantId(1L);
        approvedRecord.setSourceType("activity");
        approvedRecord.setSourceId(12L);
        approvedRecord.setCreditTypeCode("activity");
        approvedRecord.setCreditHours(BigDecimal.ZERO);
        approvedRecord.setCreditScore(new BigDecimal("2.5"));
        approvedRecord.setStatus(1);

        when(userCreditRecordMapper.selectList(any()))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(approvedRecord));
        when(userCreditSummaryMapper.selectOne(any())).thenReturn(null);

        Integer processed = service.grantCredits(List.of(grant), 99L);

        assertEquals(1, processed);

        ArgumentCaptor<UserCreditRecord> recordCaptor = ArgumentCaptor.forClass(UserCreditRecord.class);
        verify(userCreditRecordMapper).insert(recordCaptor.capture());
        UserCreditRecord inserted = recordCaptor.getValue();
        assertEquals(8L, inserted.getUserId());
        assertEquals(1L, inserted.getTenantId());
        assertEquals("activity", inserted.getSourceType());
        assertEquals(12L, inserted.getSourceId());
        assertEquals("activity", inserted.getCreditTypeCode());
        assertEquals(0, new BigDecimal("2.5").compareTo(inserted.getCreditScore()));
        assertEquals(1, inserted.getStatus());
        assertEquals(99L, inserted.getVerifierId());

        ArgumentCaptor<UserCreditSummary> summaryCaptor = ArgumentCaptor.forClass(UserCreditSummary.class);
        verify(userCreditSummaryMapper).insert(summaryCaptor.capture());
        UserCreditSummary insertedSummary = summaryCaptor.getValue();
        assertEquals(8L, insertedSummary.getUserId());
        assertEquals(1L, insertedSummary.getTenantId());
        assertEquals(0, new BigDecimal("2.5").compareTo(insertedSummary.getTotalCredits()));
        assertEquals(0, BigDecimal.ZERO.compareTo(insertedSummary.getTotalHours()));
        assertEquals(1, insertedSummary.getActivityCount());
    }

    @Test
    void syncVolunteerApproval_shouldCreateVolunteerCreditLedger() {
        VolunteerRecord volunteerRecord = new VolunteerRecord();
        volunteerRecord.setId(15L);
        volunteerRecord.setUserId(9L);
        volunteerRecord.setTenantId(2L);
        volunteerRecord.setTitle("志愿清洁");
        volunteerRecord.setServiceHours(new BigDecimal("4.5"));

        UserCreditRecord approvedRecord = new UserCreditRecord();
        approvedRecord.setUserId(9L);
        approvedRecord.setTenantId(2L);
        approvedRecord.setSourceType("volunteer");
        approvedRecord.setSourceId(15L);
        approvedRecord.setCreditTypeCode("volunteer");
        approvedRecord.setCreditHours(new BigDecimal("4.5"));
        approvedRecord.setCreditScore(new BigDecimal("4.5"));
        approvedRecord.setStatus(1);

        when(userCreditRecordMapper.selectList(any()))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(approvedRecord));
        when(userCreditSummaryMapper.selectOne(any())).thenReturn(null);

        service.syncVolunteerApproval(volunteerRecord, 100L);

        ArgumentCaptor<UserCreditRecord> recordCaptor = ArgumentCaptor.forClass(UserCreditRecord.class);
        verify(userCreditRecordMapper).insert(recordCaptor.capture());
        UserCreditRecord inserted = recordCaptor.getValue();
        assertEquals("volunteer", inserted.getSourceType());
        assertEquals(15L, inserted.getSourceId());
        assertEquals(0, new BigDecimal("4.5").compareTo(inserted.getCreditHours()));
        assertEquals(0, new BigDecimal("4.5").compareTo(inserted.getCreditScore()));
        assertEquals(100L, inserted.getVerifierId());
    }

    @Test
    void listBreakdown_shouldAggregateApprovedRecordsByType() {
        CreditType activity = new CreditType();
        activity.setId(1L);
        activity.setCode("activity");
        activity.setName("活动学分");
        activity.setMaxCredit(new BigDecimal("10"));

        UserCreditRecord record = new UserCreditRecord();
        record.setCreditTypeCode("activity");
        record.setCreditHours(new BigDecimal("2.0"));
        record.setCreditScore(new BigDecimal("3.0"));
        record.setStatus(1);

        when(userCreditRecordMapper.selectList(any())).thenReturn(List.of(record));

        List<CreditTypeBreakdownVO> result = service.listBreakdown(8L, 1L, List.of(activity));

        assertEquals(1, result.size());
        CreditTypeBreakdownVO first = result.get(0);
        assertEquals("activity", first.getCode());
        assertEquals("活动学分", first.getName());
        assertEquals(0, new BigDecimal("3.0").compareTo(first.getTotalCredits()));
        assertEquals(0, new BigDecimal("2.0").compareTo(first.getTotalHours()));
        assertEquals(1, first.getRecordCount());
        assertNotNull(first.getMaxCredit());
    }
}
