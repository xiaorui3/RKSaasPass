package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminTaskOverviewVO {
    private String sourceUrl;
    private List<AdminTaskVO> tasks;
    private List<AdminTaskLogVO> logs;
}
