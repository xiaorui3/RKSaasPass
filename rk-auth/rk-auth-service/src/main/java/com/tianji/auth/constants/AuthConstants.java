package com.tianji.auth.constants;

public abstract class AuthConstants {
    /*管理员的角色ID*/
    public static final Long ADMIN_ROLE_ID = 1L;

    /*刷新token过期时间（秒）*/
    public static final Long REFRESH_TOKEN_EXPIRE_TIME = 2592000L; // 30天
}
