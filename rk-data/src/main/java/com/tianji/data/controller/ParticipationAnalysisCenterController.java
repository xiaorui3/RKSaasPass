package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.vo.ParticipationAnalysisCenterVO;
import com.tianji.data.service.ParticipationAnalysisCenterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data/participation-analysis-center")
@Api(tags = "参与度分析中心")
public class ParticipationAnalysisCenterController {

    @Autowired
    private ParticipationAnalysisCenterService participationAnalysisCenterService;

    @GetMapping
    @ApiOperation("获取参与度分析中心聚合数据")
    public R<ParticipationAnalysisCenterVO> getParticipationAnalysisCenter(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "dimension", required = false) String dimension,
            @RequestParam(value = "timeRange", required = false) String timeRange) {
        return R.ok(participationAnalysisCenterService.getParticipationAnalysisCenter(tenantId, dimension, timeRange));
    }
}
