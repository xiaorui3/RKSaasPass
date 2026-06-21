package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.dto.DataQualityRepairRequestDTO;
import com.tianji.data.model.vo.DataQualityCenterVO;
import com.tianji.data.service.DataQualityCenterService;
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
@RequestMapping("/data/data-quality-center")
@Api(tags = "数据质量与对账中心")
public class DataQualityCenterController {

    @Autowired
    private DataQualityCenterService dataQualityCenterService;

    @GetMapping
    @ApiOperation("获取数据质量与对账中心诊断数据")
    public R<DataQualityCenterVO> getDataQualityCenter(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "issueType", required = false) String issueType,
            @RequestParam(value = "riskLevel", required = false) String riskLevel) {
        return R.ok(dataQualityCenterService.getDataQualityCenter(tenantId, issueType, riskLevel));
    }

    @PostMapping("/repair")
    @ApiOperation("登记并执行可审计的数据质量修复动作")
    public R<DataQualityCenterVO.RepairLog> repair(@RequestBody DataQualityRepairRequestDTO request) {
        return R.ok(dataQualityCenterService.repair(request));
    }
}
