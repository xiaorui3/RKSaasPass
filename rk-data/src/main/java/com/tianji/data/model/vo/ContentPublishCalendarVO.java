package com.tianji.data.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class ContentPublishCalendarVO {
    private Long tenantId;
    private String tenantScope;
    private LocalDateTime updatedAt;
    private CacheInfo cache = new CacheInfo();
    private Filters filters = new Filters();
    private Summary summary = new Summary();
    private List<SummaryCard> summaryCards = new ArrayList<>();
    private List<CalendarItem> calendarRows = new ArrayList<>();
    private List<TimelineItem> timelineRows = new ArrayList<>();
    private List<RiskItem> riskRows = new ArrayList<>();
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
        private String timeRange = "30D";
        private String contentType = "ALL";
        private String status = "ALL";
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private long totalCount;
        private long publishedCount;
        private long pendingCount;
        private long scheduledCount;
        private long overdueCount;
        private long rejectedCount;
        private long newsCount;
        private long noticeCount;
        private long worksCount;
        private long activityCount;
        private long competitionCount;
    }

    @Data
    @Accessors(chain = true)
    public static class SummaryCard {
        private String metric;
        private String label;
        private long value;
        private String hint;
        private String tone;
    }

    @Data
    @Accessors(chain = true)
    public static class CalendarItem {
        private LocalDate date;
        private long totalCount;
        private long publishedCount;
        private long pendingCount;
        private long scheduledCount;
        private long overdueCount;
        private long rejectedCount;
        private long newsCount;
        private long noticeCount;
        private long worksCount;
        private long activityCount;
        private long competitionCount;
    }

    @Data
    @Accessors(chain = true)
    public static class TimelineItem {
        private Long id;
        private String title;
        private String contentType;
        private String contentTypeName;
        private String status;
        private String statusName;
        private LocalDateTime publishAt;
        private String source;
        private String targetPath;
    }

    @Data
    @Accessors(chain = true)
    public static class RiskItem {
        private String riskType;
        private String title;
        private String riskLevel;
        private String status;
        private String suggestion;
        private LocalDateTime detectedAt;
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
