package com.tianji.user.controller;

import com.tianji.api.dto.user.LoginAuditRecordDTO;
import com.tianji.user.service.ISysLogininforService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/internal")
@RequiredArgsConstructor
public class AuditInternalController {

    private final ISysLogininforService sysLogininforService;

    @PostMapping("/logininfor")
    public Boolean recordLoginAudit(@RequestBody LoginAuditRecordDTO dto) {
        sysLogininforService.recordLogininfor(dto);
        return true;
    }
}
