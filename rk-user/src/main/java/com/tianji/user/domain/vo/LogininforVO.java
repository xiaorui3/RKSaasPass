package com.tianji.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志VO
 *
 * @author RK-Web
 * @since 2026-03-29
 */
@Data
@ApiModel(description = "登录日志视图对象")
public class LogininforVO {

    @ApiModelProperty("访问ID")
    private Long infoId;

    @ApiModelProperty("登录账号")
    private String loginName;

    @ApiModelProperty("登录IP地址")
    private String ipaddr;

    @ApiModelProperty("登录地点")
    private String loginLocation;

    @ApiModelProperty("浏览器类型")
    private String browser;

    @ApiModelProperty("操作系统")
    private String os;

    @ApiModelProperty("登录状态（0成功 1失败）")
    private String status;

    @ApiModelProperty("提示消息")
    private String msg;

    @ApiModelProperty("访问时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;
}
