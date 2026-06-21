package com.tianji.exam.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试实体类
 */
@Data
@TableName("rk_exam")
public class Exam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String description;
    private String examType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Integer totalScore;
    private Integer passScore;
    /**
     * 租户ID（企业级多租户标准）
     */
    private Long tenantId;
    private String createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleteFlag;
}