package com.tianji.message.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系我们消息实体类（支持多租户）
 * 对应数据库表：rk_message.contact_us_messages
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@TableName("contact_us_messages")
public class ContactUsMessage {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（企业级多租户标准）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 联系人姓名
     */
    private String name;

    /**
     * 联系人邮箱
     */
    private String email;

    @TableField("target_email")
    private String targetEmail;

    /**
     * 联系人电话
     */
    private String phone;

    /**
     * 消息主题
     */
    private String subject;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 状态：pending-待处理, processing-处理中, replied-已回复, closed-已关闭
     */
    private String status;

    /**
     * 管理员回复内容
     */
    private String response;

    /**
     * 回复时间
     */
    private LocalDateTime responseTime;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

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
     * 删除标记：0-未删除 1-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    // ==================== 状态常量 ====================

    /**
     * 状态：待处理
     */
    public static final String STATUS_PENDING = "pending";

    /**
     * 状态：处理中
     */
    public static final String STATUS_PROCESSING = "processing";

    /**
     * 状态：已回复
     */
    public static final String STATUS_REPLIED = "replied";

    /**
     * 状态：已关闭
     */
    public static final String STATUS_CLOSED = "closed";
}
