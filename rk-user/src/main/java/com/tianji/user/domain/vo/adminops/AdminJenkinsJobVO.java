package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminJenkinsJobVO {
    private String name;
    private String url;
    private String color;
    private String status;
    private Integer lastBuildNumber;
    private String lastBuildUrl;
    private Integer lastCompletedBuildNumber;
}
