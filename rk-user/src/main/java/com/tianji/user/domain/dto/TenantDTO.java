package com.tianji.user.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 租户数据传输对象
 */
@Data
public class TenantDTO {

    /**
     * 租户ID（更新时需要）
     */
    private Long id;

    /**
     * 租户编码
     */
    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    /**
     * 租户名称
     */
    @NotBlank(message = "租户名称不能为空")
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
     * 过期时间
     */
    private LocalDateTime expireTime;
}
