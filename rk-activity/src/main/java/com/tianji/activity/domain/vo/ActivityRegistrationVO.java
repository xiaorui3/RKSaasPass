package com.tianji.activity.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动报名记录VO
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("活动报名记录")
public class ActivityRegistrationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报名ID
     */
    @ApiModelProperty("报名ID")
    private Long id;

    /**
     * 活动ID
     */
    @ApiModelProperty("活动ID")
    private Long activityId;

    /**
     * 活动名称
     */
    @ApiModelProperty("活动名称")
    private String activityName;

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     * 用户名
     */
    @ApiModelProperty("用户名")
    private String userName;

    /**
     * 用户头像
     */
    @ApiModelProperty("用户头像")
    private String userAvatar;

    @ApiModelProperty("学号")
    private String studentId;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("手机号")
    private String cellPhone;

    /**
     * 报名状态
     */
    @ApiModelProperty("报名状态：1-已报名, 2-已签到, 3-已取消, 4-已缺席")
    private Integer registrationStatus;

    /**
     * 报名状态名称
     */
    @ApiModelProperty("报名状态名称")
    private String registrationStatusName;

    /**
     * 报名时间
     */
    @ApiModelProperty("报名时间")
    private LocalDateTime registrationTime;

    /**
     * 签到时间
     */
    @ApiModelProperty("签到时间")
    private LocalDateTime checkInTime;

    /**
     * 取消时间
     */
    @ApiModelProperty("取消时间")
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    @ApiModelProperty("取消原因")
    private String cancelReason;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;

    /**
     * 获取报名状态名称
     */
    public String getRegistrationStatusName() {
        if (registrationStatus == null) return "未知";
        switch (registrationStatus) {
            case 1: return "已报名";
            case 2: return "已签到";
            case 3: return "已取消";
            case 4: return "已缺席";
            default: return "未知";
        }
    }
}
