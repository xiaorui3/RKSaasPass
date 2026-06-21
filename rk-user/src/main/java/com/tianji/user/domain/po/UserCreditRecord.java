package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户学分记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_credit_record")
public class UserCreditRecord extends BaseEntity {

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
     * 来源类型（如 activity, competition, volunteer 等）
     */
    private String sourceType;

    /**
     * 来源ID
     */
    private Long sourceId;

    /**
     * 学分类型编码
     */
    private String creditTypeCode;

    /**
     * 学时
     */
    private BigDecimal creditHours;

    /**
     * 学分分数
     */
    private BigDecimal creditScore;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：0-待审核，1-已通过，2-已拒绝
     */
    private Integer status;

    /**
     * 审核人ID
     */
    private Long verifierId;

    /**
     * 审核时间
     */
    private LocalDateTime verifyTime;

    /**
     * 拒绝原因
     */
    private String rejectReason;
}
