package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 学生课程表
 * </p>
 *
 * @author 小锐
 * @since 2026-02-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("learning_lesson")
public class LearningLesson implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学员id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 课程id
     */
    @TableField("course_id")
    private Long courseId;

    /**
     * 课程状态，0-未学习，1-学习中，2-已学完，3-已失效
     */
    @TableField("status")
    private LessonStatus status;

    /**
     * 每周学习频率，例如每周学习6小节，则频率为6
     */
    @TableField("week_freq")
    private Integer weekFreq;

    /**
     * 学习计划状态，0-没有计划，1-计划进行中
     */
    @TableField("plan_status")
    private PlanStatus planStatus;

    /**
     * 已学习小节数量
     */
    @TableField("learned_sections")
    private Integer learnedSections;

    /**
     * 最近一次学习的小节id
     */
    @TableField("latest_section_id")
    private Long latestSectionId;

    /**
     * 最近一次学习的时间
     */
    @TableField("latest_learn_time")
    private LocalDateTime latestLearnTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;


}
