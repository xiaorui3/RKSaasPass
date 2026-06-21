package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户学分汇总实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_credit_summary")
public class UserCreditSummary extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总学分
     */
    private BigDecimal totalCredits;

    /**
     * 总学时
     */
    private BigDecimal totalHours;

    /**
     * 志愿服务时长
     */
    private BigDecimal volunteerHours;

    /**
     * 活动参与次数
     */
    private Integer activityCount;

    /**
     * 竞赛获奖次数
     */
    private Integer competitionAwards;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;
}
