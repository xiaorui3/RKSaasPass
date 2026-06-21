package com.tianji.data.service;

import com.tianji.data.model.vo.FrontendCacheWarmupSummaryVO;

public interface FrontendCacheWarmupService {

    FrontendCacheWarmupSummaryVO checkAndWarmup();

    FrontendCacheWarmupSummaryVO manualWarmup();

    FrontendCacheWarmupSummaryVO latestSummary();
}
