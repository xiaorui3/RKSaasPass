package com.tianji.message.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收件箱表（支持多租户）
 * 基于企业级ERP多租户标准
 */
@Data
@Accessors(chain = true)
@TableName("rk_user_inbox")
public class UserInbox implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（企业级多租户标准）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 消息类型：1-系统通知 2-业务通知 3-消息
     */
    @TableField("message_type")
    private Integer messageType;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 是否已读：0-未读 1-已读
     */
    @TableField("is_read")
    private Integer isRead;

    /**
     * 阅读时间
     */
    @TableField("read_time")
    private LocalDateTime readTime;

    /**
     * 类型
     */
    @TableField(exist = false)
    private Integer type;

    /**
     * 推送时间
     */
    @TableField(exist = false)
    private LocalDateTime pushTime;

    /**
     * 过期时间
     */
    @TableField(exist = false)
    private LocalDateTime expireTime;

    /**
     * 发布者ID
     */
    @TableField(exist = false)
    private Long publisher;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}