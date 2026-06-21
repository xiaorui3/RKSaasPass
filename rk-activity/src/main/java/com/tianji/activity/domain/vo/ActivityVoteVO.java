package com.tianji.activity.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ActivityVoteVO {
    private Long id;
    private Long activityId;
    private String activityName;
    private String title;
    private String description;
    private Integer status;
    private String statusText;
    private Integer targetCount;
    private Integer totalVotes;
    private LocalDateTime createTime;
    private LocalDateTime closedTime;
    private List<Option> options = new ArrayList<>();

    @Data
    public static class Option {
        private Long id;
        private String label;
        private Integer count;
    }
}
