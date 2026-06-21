package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminTaskLogVO {
    private Long id;
    private Long taskId;
    private String taskName;
    private String executeTime;
    private Long duration;
    private String status;
    private String message;
}
