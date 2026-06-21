package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.vo.GrowthRetentionCenterVO;
import com.tianji.data.service.GrowthRetentionCenterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data/growth-retention-center")
@Api(tags = "租户增长与留存看板")
public class GrowthRetentionCenterController {

    @Autowired
    private GrowthRetentionCenterService growthRetentionCenterService;

    @GetMapping
    @ApiOperation("获取租户增长与留存看板数据")
    public R<GrowthRetentionCenterVO> getGrowthRetentionCenter(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            @RequestParam(value = "riskLevel", required = false) String riskLevel) {
        return R.ok(growthRetentionCenterService.getGrowthRetentionCenter(tenantId, timeRange, riskLevel));
    }
}
