package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.user.domain.vo.DemoDataCenterVO;
import com.tianji.user.service.IDemoDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "演示数据中心")
@RestController
@RequestMapping("/api/demo-data")
@RequiredArgsConstructor
public class DemoDataController {

    private final IDemoDataService demoDataService;

    @ApiOperation("获取演示数据状态")
    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('system:config:view', 'system:config:query', 'system:config:list')")
    public R<DemoDataCenterVO> status() {
        return R.ok(demoDataService.getStatus());
    }

    @ApiOperation("生成演示数据")
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<DemoDataCenterVO> generate() {
        return R.ok(demoDataService.generate());
    }

    @ApiOperation("清理演示数据")
    @PostMapping("/cleanup")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<DemoDataCenterVO> cleanup() {
        return R.ok(demoDataService.cleanup());
    }

    @ApiOperation("重置演示数据")
    @PostMapping("/reset")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<DemoDataCenterVO> reset() {
        return R.ok(demoDataService.reset());
    }
}
