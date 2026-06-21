package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.vo.TenantOperationsCenterVO;
import com.tianji.data.service.TenantOperationsCenterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data/tenant-operations-center")
@Api(tags = "Tenant operations center")
public class TenantOperationsCenterController {

    @Autowired
    private TenantOperationsCenterService tenantOperationsCenterService;

    @GetMapping
    @ApiOperation("Get tenant operations center overview")
    public R<TenantOperationsCenterVO> overview(@RequestParam(value = "tenantId", required = false) Long tenantId) {
        return R.ok(tenantOperationsCenterService.getOverview(tenantId));
    }
}
