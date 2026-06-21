package com.tianji.activity.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("rk_activity_vote")
public class ActivityVote extends BaseEntity {

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_CLOSED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    private String title;

    private String description;

    private Integer status;

    @TableField("target_user_ids")
    private String targetUserIds;

    @TableField("notify_sent")
    private Integer notifySent;

    @TableField("closed_time")
    private LocalDateTime closedTime;
}
