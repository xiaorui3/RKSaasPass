package com.tianji.user.service.impl;

import com.tianji.api.dto.user.LoginAuditRecordDTO;
import com.tianji.user.domain.po.SysLogininfor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SysLogininforServiceImplTest {

    @Spy
    @InjectMocks
    private SysLogininforServiceImpl sysLogininforService;

    @Test
    void recordLogininfor_shouldTrimOversizedMessage() {
        LoginAuditRecordDTO dto = new LoginAuditRecordDTO();
        dto.setLoginName("member_a");
        dto.setIpaddr("127.0.0.1");
        dto.setBrowser("Playwright");
        dto.setOs("Windows");
        dto.setStatus("1");
        dto.setMsg("x".repeat(800));

        doReturn(true).when(sysLogininforService).save(any(SysLogininfor.class));

        sysLogininforService.recordLogininfor(dto);

        ArgumentCaptor<SysLogininfor> recordCaptor = ArgumentCaptor.forClass(SysLogininfor.class);
        verify(sysLogininforService).save(recordCaptor.capture());
        assertEquals(500, recordCaptor.getValue().getMsg().length());
        assertEquals("x".repeat(500), recordCaptor.getValue().getMsg());
    }
}
