package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.dto.SecurityAuditNotifyRequestDTO;
import com.tianji.data.model.vo.SecurityAuditCenterVO;
import com.tianji.data.service.SecurityAuditCenterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data/security-audit-center")
@Api(tags = "安全审计中心")
public class SecurityAuditCenterController {

    @Autowired
    private SecurityAuditCenterService securityAuditCenterService;

    @GetMapping
    @ApiOperation("获取安全审计中心聚合数据")
    public R<SecurityAuditCenterVO> getSecurityAuditCenter(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "riskType", required = false) String riskType,
            @RequestParam(value = "riskLevel", required = false) String riskLevel,
            @RequestParam(value = "notifyStatus", required = false) String notifyStatus) {
        return R.ok(securityAuditCenterService.getSecurityAuditCenter(tenantId, riskType, riskLevel, notifyStatus));
    }

    @PostMapping("/notify")
    @ApiOperation("登记高风险安全审计通知")
    public R<SecurityAuditCenterVO.NotifyLog> notifyRisk(@RequestBody SecurityAuditNotifyRequestDTO request) {
        return R.ok(securityAuditCenterService.notifyRisk(request));
    }
}
