package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.vo.OpenSourceHealthVO;
import com.tianji.data.service.OpenSourceHealthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data/open-source-health")
@Api(tags = "开源系统自检")
public class OpenSourceHealthController {

    @Autowired
    private OpenSourceHealthService openSourceHealthService;

    @GetMapping
    @ApiOperation("获取开源部署业务层系统自检结果")
    public R<OpenSourceHealthVO> check(@RequestParam(value = "tenantId", required = false) Long tenantId) {
        return R.ok(openSourceHealthService.check(tenantId));
    }
}
