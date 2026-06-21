package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.vo.SaasVisualScreenVO;
import com.tianji.data.service.SaasVisualScreenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data/saas-visual-screen")
@Api(tags = "SaaS业务可视化大屏")
public class SaasVisualScreenController {

    @Autowired
    private SaasVisualScreenService saasVisualScreenService;

    @GetMapping
    @ApiOperation("获取SaaS业务可视化大屏聚合数据")
    public R<SaasVisualScreenVO> overview(@RequestParam(value = "tenantId", required = false) Long tenantId) {
        return R.ok(saasVisualScreenService.getOverview(tenantId));
    }
}
