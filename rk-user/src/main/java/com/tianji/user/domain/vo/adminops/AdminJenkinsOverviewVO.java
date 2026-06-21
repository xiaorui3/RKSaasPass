package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminJenkinsOverviewVO {
    private String sourceUrl;
    private List<AdminJenkinsJobVO> jobs;
}
