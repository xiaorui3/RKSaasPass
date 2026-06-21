package com.tianji.user.service.impl;

import com.tianji.user.domain.po.ReferralCode;
import com.tianji.user.domain.po.ReferralConversionRecord;
import com.tianji.user.mapper.ReferralCodeMapper;
import com.tianji.user.mapper.ReferralConversionRecordMapper;
import com.tianji.user.domain.vo.ReferralConversionOverviewVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralCodeServiceImplTest {

    @Mock
    private ReferralCodeMapper referralCodeMapper;

    @Mock
    private ReferralConversionRecordMapper referralConversionRecordMapper;

    @InjectMocks
    private ReferralCodeServiceImpl service;

    @Test
    void getReferralConversionOverview_shouldReturnSummaryAndRecords() {
        ReferralCode code = new ReferralCode();
        code.setId(1L);
        code.setCode("REF1001");
        code.setUsedCount(2);

        ReferralConversionRecord opened = new ReferralConversionRecord();
        opened.setReferralCodeId(1L);
        opened.setConversionStatus("OPENED");
        opened.setTargetEmail("a@example.com");
        opened.setConvertedTime(LocalDateTime.now());

        ReferralConversionRecord registerSuccess = new ReferralConversionRecord();
        registerSuccess.setReferralCodeId(1L);
        registerSuccess.setConversionStatus("REGISTER_SUCCESS");
        registerSuccess.setTargetEmail("b@example.com");
        registerSuccess.setConvertedTime(LocalDateTime.now());

        ReferralConversionRecord joinApproved = new ReferralConversionRecord();
        joinApproved.setReferralCodeId(1L);
        joinApproved.setConversionStatus("JOIN_APPROVED");
        joinApproved.setTargetEmail("c@example.com");
        joinApproved.setConvertedTime(LocalDateTime.now());

        when(referralCodeMapper.selectById(1L)).thenReturn(code);
        when(referralConversionRecordMapper.selectList(any())).thenReturn(List.of(opened, registerSuccess, joinApproved));

        ReferralConversionOverviewVO overview = service.getReferralConversionOverview(1L);

        assertNotNull(overview);
        assertEquals("REF1001", overview.getReferralCode());
        assertEquals(1, overview.getOpenedCount());
        assertEquals(1, overview.getRegisterSuccessCount());
        assertEquals(1, overview.getJoinApprovedCount());
        assertEquals(3, overview.getRecords().size());
    }

    @Test
    void markGenericRegisterSuccess_shouldCreateRecordAndIncrementUsedCount() {
        ReferralCode code = new ReferralCode();
        code.setId(2L);
        code.setTenantId(1L);
        code.setCode("REF2001");
        code.setUsedCount(0);
        code.setMaxUses(5);
        code.setStatus(1);

        when(referralCodeMapper.selectOne(any())).thenReturn(code);
        when(referralConversionRecordMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            ReferralConversionRecord record = invocation.getArgument(0);
            record.setId(20L);
            return 1;
        }).when(referralConversionRecordMapper).insert(any(ReferralConversionRecord.class));

        boolean result = service.markGenericRegisterSuccess(1L, "REF2001", "generic@example.com", 101L);

        assertTrue(result);
        assertEquals(1, code.getUsedCount());
    }

    @Test
    void markGenericJoinSubmittedAndApproved_shouldUpdateExistingRecordAndIncrementUsedCountOnce() {
        ReferralCode code = new ReferralCode();
        code.setId(3L);
        code.setTenantId(1L);
        code.setCode("REF3001");
        code.setUsedCount(0);
        code.setMaxUses(5);
        code.setStatus(1);

        ReferralConversionRecord submitted = new ReferralConversionRecord();
        submitted.setId(30L);
        submitted.setReferralCodeId(3L);
        submitted.setReferralCode("REF3001");
        submitted.setTargetEmail("join@example.com");
        submitted.setConversionType("JOIN");
        submitted.setConversionStatus("JOIN_SUBMITTED");
        submitted.setJoinRequestId(200L);

        when(referralCodeMapper.selectOne(any())).thenReturn(code);
        when(referralConversionRecordMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(submitted);
        doAnswer(invocation -> {
            ReferralConversionRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(30L);
            }
            return 1;
        }).when(referralConversionRecordMapper).insert(any(ReferralConversionRecord.class));

        boolean submittedResult = service.markGenericJoinSubmitted(1L, "REF3001", "join@example.com", 200L);
        boolean approvedResult = service.markGenericJoinApproved(1L, "REF3001", "join@example.com", 102L, 200L);

        assertTrue(submittedResult);
        assertTrue(approvedResult);
        assertEquals(1, code.getUsedCount());
        assertEquals("JOIN_APPROVED", submitted.getConversionStatus());
        assertEquals(102L, submitted.getAuthUserId());
    }
}
