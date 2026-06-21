package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内推码实体类
 * 支持多租户的内推码管理功能
 */
@Data
@Accessors(chain = true)
@TableName("referral_code")
public class ReferralCode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（多租户隔离）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 内推码（唯一）
     */
    private String code;

    /**
     * 生成者用户ID
     */
    @TableField("generator_id")
    private Long generatorId;

    /**
     * 最大使用次数
     */
    @TableField("max_uses")
    private Integer maxUses;

    /**
     * 已使用次数
     */
    @TableField("used_count")
    private Integer usedCount;

    /**
     * 过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * 状态: 1-有效 0-失效
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标记: 0-未删除 1-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
