package com.tianji.user.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员信息视图对象
 */
@Data
public class AdminVO {

    /**
     * 管理员ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 角色
     */
    private List<String> roles;

    /**
     * 权限
     */
    private List<String> permissions;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * Token
     */
    private String token;

    /**
     * Token过期时间
     */
    private Long tokenExpireTime;
}
