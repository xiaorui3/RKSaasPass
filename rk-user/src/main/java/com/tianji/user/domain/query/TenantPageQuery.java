package com.tianji.user.domain.query;

import lombok.Data;

/**
 * 租户分页查询条件
 */
@Data
public class TenantPageQuery {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 租户名称（模糊查询）
     */
    private String tenantName;

    /**
     * 租户编码（精确查询）
     */
    private String tenantCode;

    /**
     * 状态：1-正常 0-禁用
     */
    private Integer status;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;
}
