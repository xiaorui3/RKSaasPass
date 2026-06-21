package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 志愿服务记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("volunteer_record")
public class VolunteerRecord extends BaseEntity {

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
     * 关联活动ID
     */
    private Long activityId;

    /**
     * 服务时长（小时）
     */
    private BigDecimal serviceHours;

    /**
     * 服务日期
     */
    private LocalDate serviceDate;

    /**
     * 服务标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 证明材料图片URL
     */
    private String certImageUrl;

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
