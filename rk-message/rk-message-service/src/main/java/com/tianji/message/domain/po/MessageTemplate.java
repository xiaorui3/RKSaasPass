package com.tianji.message.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息模板表（支持多租户）
 * 基于企业级ERP多租户标准
 */
@Data
@Accessors(chain = true)
@TableName("rk_message_template")
public class MessageTemplate implements Serializable {

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
     * 模板编码
     */
    @TableField("template_code")
    private String templateCode;

    /**
     * 模板名称
     */
    @TableField("template_name")
    private String templateName;

    /**
     * 邮件主题
     */
    @TableField("email_subject")
    private String emailSubject;

    /**
     * 模板类型：1-短信 2-邮件 3-站内信
     */
    @TableField("template_type")
    private Integer templateType;

    /**
     * 模板内容
     */
    @TableField("template_content")
    private String templateContent;

    /**
     * 是否HTML
     */
    @TableField("html_mode")
    private Integer htmlMode;

    /**
     * 媒体配置JSON
     */
    @TableField("media_payload")
    private String mediaPayload;

    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;

    /**
     * 模板ID（非数据库字段）
     */
    @TableField(exist = false)
    private Long templateId;

    /**
     * 名称（非数据库字段）
     */
    @TableField(exist = false)
    private String name;

    /**
     * 内容（非数据库字段）
     */
    @TableField(exist = false)
    private String content;

    /**
     * 平台编码（非数据库字段）
     */
    @TableField(exist = false)
    private String platformCode;

    /**
     * 第三方模板编码（非数据库字段）
     */
    @TableField(exist = false)
    private String thirdTemplateCode;

    /**
     * 签名名称（非数据库字段）
     */
    @TableField(exist = false)
    private String signName;

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
     * 创建者ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long creator;

    /**
     * 更新者ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;

    /**
     * 删除标记：0-未删除 1-已删除（企业级多租户标准）
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
