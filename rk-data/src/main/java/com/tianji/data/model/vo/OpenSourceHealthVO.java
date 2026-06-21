package com.tianji.data.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class OpenSourceHealthVO {
    private Long tenantId;
    private String tenantScope;
    private String overallStatus;
    private LocalDateTime checkedAt;
    private Summary summary = new Summary();
    private List<CheckItem> checks = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private int totalCount;
        private int passCount;
        private int warnCount;
        private int failCount;
        private int unknownCount;
    }

    @Data
    @Accessors(chain = true)
    public static class CheckItem {
        private String code;
        private String category;
        private String title;
        private String status;
        private String diagnostics;
        private String suggestion;
        private String source;
        private LocalDateTime checkedAt;
    }
}
