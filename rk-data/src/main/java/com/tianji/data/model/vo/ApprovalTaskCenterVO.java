package com.tianji.data.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class ApprovalTaskCenterVO {
    private Long tenantId;
    private String tenantScope;
    private LocalDateTime updatedAt;
    private CacheInfo cache = new CacheInfo();
    private Filters filters = new Filters();
    private Summary summary = new Summary();
    private List<TaskItem> tasks = new ArrayList<>();
    private List<SourceInfo> sources = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class CacheInfo {
        private boolean hit;
        private String mode = "computed";
        private String key;
        private Integer ttlSeconds;
        private LocalDateTime cachedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class Filters {
        private String taskType = "ALL";
        private String slaStatus = "ALL";
        private String status = "ALL";
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private long totalPendingCount;
        private long overdueCount;
        private long dueSoonCount;
        private long normalCount;
        private long admissionPendingCount;
        private long joinPendingCount;
        private long activityPendingCount;
        private long competitionPendingCount;
        private long contentPendingCount;
        private long financePendingCount;
        private long noticePendingCount;
    }

    @Data
    @Accessors(chain = true)
    public static class TaskItem {
        private String taskType;
        private String taskTypeLabel;
        private String title;
        private String status;
        private String slaStatus;
        private String riskLevel;
        private String source;
        private String sourceId;
        private String sourcePath;
        private String submitterName;
        private String currentNode;
        private String createdAt;
        private String updatedAt;
        private long waitingHours;
    }

    @Data
    @Accessors(chain = true)
    public static class SourceInfo {
        private String metric;
        private String tableName;
        private String status;
        private LocalDateTime updatedAt;
    }
}
