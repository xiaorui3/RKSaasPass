package com.tianji.user.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置VO
 *
 * @author RK-Web
 * @since 2026-03-29
 */
@Data
@ApiModel(description = "系统配置视图对象")
public class SystemConfigVO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("配置键")
    private String configKey;

    @ApiModelProperty("配置值")
    private String configValue;

    @ApiModelProperty("配置描述")
    private String description;

    @ApiModelProperty("租户ID")
    private Long tenantId;

    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Boolean isEnabled;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
