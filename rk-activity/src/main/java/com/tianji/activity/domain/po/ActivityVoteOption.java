package com.tianji.activity.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("rk_activity_vote_option")
public class ActivityVoteOption extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("vote_id")
    private Long voteId;

    @TableField("option_label")
    private String optionLabel;

    @TableField("vote_count")
    private Integer voteCount;

    @TableField("sort_order")
    private Integer sortOrder;
}
