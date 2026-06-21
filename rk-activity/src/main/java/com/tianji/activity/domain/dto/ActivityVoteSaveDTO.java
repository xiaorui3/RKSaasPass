package com.tianji.activity.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ActivityVoteSaveDTO {
    private Long activityId;
    private String title;
    private String description;
    private List<String> options;
    private List<Long> extraUserIds;
    private Boolean notifyUsers;
}
