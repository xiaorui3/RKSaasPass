package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.vo.FrontendCacheWarmupSummaryVO;
import com.tianji.data.service.FrontendCacheWarmupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Frontend cache warmup")
@RestController
@RequestMapping("/data/cache/frontend")
@RequiredArgsConstructor
public class FrontendCacheWarmupController {

    private final FrontendCacheWarmupService warmupService;

    @ApiOperation("Latest frontend cache warmup summary")
    @GetMapping("/summary")
    public R<FrontendCacheWarmupSummaryVO> latestSummary() {
        return R.ok(warmupService.latestSummary());
    }

    @ApiOperation("Manual frontend cache warmup")
    @PostMapping("/warmup")
    public R<FrontendCacheWarmupSummaryVO> manualWarmup() {
        return R.ok(warmupService.manualWarmup());
    }
}
