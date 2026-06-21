package com.tianji.activity.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 比赛报名DTO
 * 用于用户报名参加比赛
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("比赛报名请求")
public class CompetitionRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    @ApiModelProperty(value = "姓名", required = true)
    private String name;

    /**
     * 学号
     */
    @NotBlank(message = "学号不能为空")
    @ApiModelProperty(value = "学号", required = true)
    private String studentId;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @ApiModelProperty(value = "邮箱", required = true)
    private String email;

    /**
     * 手机号
     */
    @ApiModelProperty("手机号")
    private String phone;

    /**
     * 专业
     */
    @ApiModelProperty("专业")
    private String major;

    /**
     * 年级
     */
    @ApiModelProperty("年级")
    private String grade;

    /**
     * 队伍名称（团队赛时必填）
     */
    @ApiModelProperty("队伍名称（团队赛时必填）")
    private String teamName;

    /**
     * 队伍人数（团队赛时必填）
     */
    @ApiModelProperty("队伍人数（团队赛时必填）")
    private Integer teamSize;

    /**
     * 队员信息（团队赛时填写，JSON格式）
     */
    @ApiModelProperty("队员信息（团队赛时填写，JSON格式，包含姓名、学号等）")
    private String teamMembers;

    /**
     * 相关经验
     */
    @ApiModelProperty("相关经验或项目经历")
    private String experience;

    /**
     * 备注
     */
    @ApiModelProperty("备注说明")
    private String remark;
}
