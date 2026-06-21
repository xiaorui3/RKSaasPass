package com.tianji.user.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户视图对象
 */
@Data
public class TenantVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * Logo URL
     */
    private String logoUrl;

    /**
     * Display order
     */
    private Integer displayOrder;

    /**
     * 描述
     */
    private String description;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 状态：1-正常 0-禁用
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否已过期
     */
    private Boolean expired;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 初始化状态
     */
    private Integer initStatus;

    /**
     * 成员数量
     */
    private Integer memberCount;
}
