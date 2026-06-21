package com.tianji.user.domain.dto;

import com.tianji.common.constants.RegexConstants;
import com.tianji.common.validate.annotations.EnumValid;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(description = "褰撳墠鐧诲綍鐢ㄦ埛淇敼璧勬枡琛ㄥ崟")
public class UserFormDTO {

    @ApiModelProperty(value = "鐢ㄦ埛鍚?, example = "member_a")
    private String username;

    @ApiModelProperty(value = "灞曠ず鍚嶇О", example = "寮犱笁")
    private String name;

    @ApiModelProperty(value = "鎵嬫満鍙凤紝鍙负绌?, example = "13800010004")
    @Pattern(regexp = "(^$)|(" + RegexConstants.PHONE_PATTERN + ")", message = "invalid cellphone")
    private String cellPhone;

    @ApiModelProperty(value = "閭")
    private String email;

    @ApiModelProperty(value = "QQ")
    private String qq;

    @ApiModelProperty(value = "鐪佷唤")
    private String province;

    @ApiModelProperty(value = "鍩庡競")
    private String city;

    @ApiModelProperty(value = "鍖哄幙")
    private String district;

    @ApiModelProperty(value = "鎬у埆锛?-鐢锋€э紝1-濂虫€?, example = "0")
    @EnumValid(enumeration = {0, 1}, message = "invalid gender")
    private Integer gender;

    @ApiModelProperty(value = "澶村儚URL")
    private String icon;

    @ApiModelProperty(value = "涓汉浠嬬粛")
    private String intro;

    @ApiModelProperty(value = "鍘熷瀵嗙爜锛堜慨鏀瑰瘑鐮佹椂蹇呭～锛?, example = "change-me")
    private String oldPassword;

    @ApiModelProperty(value = "鏂板瘑鐮侊紙淇敼瀵嗙爜鏃跺繀濉級", example = "change-me")
    private String password;
}
