package com.tianji.data.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontendCacheWarmupSummaryVO {

    private boolean redisAvailable;
    private boolean warmed;
    private boolean degraded;
    private String reason;
    private LocalDateTime checkedAt;
    private long elapsedMs;
    private int tenantCount;
    private int keyCount;
    private int hitCount;
    private int missCount;
    private int failureCount;

    @Builder.Default
    private List<String> checkedKeys = new ArrayList<>();

    @Builder.Default
    private List<String> failedItems = new ArrayList<>();
}
