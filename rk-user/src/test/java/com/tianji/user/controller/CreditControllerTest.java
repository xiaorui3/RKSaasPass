package com.tianji.user.controller;

import com.tianji.api.dto.user.CreditGrantDTO;
import com.tianji.common.domain.R;
import com.tianji.user.domain.po.CreditType;
import com.tianji.user.domain.vo.CreditTypeBreakdownVO;
import com.tianji.user.mapper.CreditTypeMapper;
import com.tianji.user.mapper.UserCreditRecordMapper;
import com.tianji.user.service.credit.UserCreditLedgerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditControllerTest {

    @Mock
    private CreditTypeMapper creditTypeMapper;

    @Mock
    private UserCreditRecordMapper userCreditRecordMapper;

    @Mock
    private UserCreditLedgerService creditLedgerService;

    @InjectMocks
    private CreditController controller;

    @Test
    void listCreditTypes_shouldReturnDefaultsWhenQueryFails() {
        when(creditTypeMapper.selectList(any())).thenThrow(new RuntimeException("credit_type missing"));

        R<List<CreditType>> response = controller.listCreditTypes();

        assertEquals(200, response.getCode());
        assertFalse(response.getData().isEmpty());
        assertEquals("activity", response.getData().get(0).getCode());
    }

    @Test
    void grantCreditsInternal_shouldDelegateToLedgerService() {
        CreditGrantDTO grant = new CreditGrantDTO();
        grant.setUserId(8L);
        grant.setTenantId(1L);
        grant.setSourceType("activity");
        grant.setSourceId(12L);
        grant.setCreditTypeCode("activity");
        grant.setCreditHours(BigDecimal.ZERO);
        grant.setCreditScore(new BigDecimal("2.5"));

        when(creditLedgerService.grantCredits(any(), eq(null))).thenReturn(1);

        Integer processed = controller.grantCreditsInternal(List.of(grant));

        assertEquals(1, processed);
        verify(creditLedgerService).grantCredits(any(), eq(null));
    }

    @Test
    void myBreakdown_shouldReturnDelegatedBreakdown() {
        CreditType configured = new CreditType();
        configured.setId(1L);
        configured.setCode("activity");
        configured.setName("活动学分");

        CreditTypeBreakdownVO breakdown = new CreditTypeBreakdownVO();
        breakdown.setCode("activity");
        breakdown.setName("活动学分");
        breakdown.setTotalCredits(new BigDecimal("3.0"));

        when(creditTypeMapper.selectList(any())).thenReturn(List.of(configured));
        when(creditLedgerService.listBreakdown(eq(null), eq(1L), any())).thenReturn(List.of(breakdown));

        R<List<CreditTypeBreakdownVO>> response = controller.myBreakdown();

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
        assertEquals("activity", response.getData().get(0).getCode());
    }
}
