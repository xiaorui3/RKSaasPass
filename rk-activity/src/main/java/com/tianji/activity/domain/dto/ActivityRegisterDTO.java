package com.tianji.activity.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 活动报名DTO
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("活动报名请求")
public class ActivityRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 备注
     */
    @ApiModelProperty("报名备注")
    private String remark;

    /**
     * 联系电话
     */
    @ApiModelProperty("联系电话")
    private String phone;

    /**
     * 额外信息（JSON格式）
     */
    @ApiModelProperty("额外信息")
    private String extraInfo;
}
