package com.tianji.auth.common.constants;

import java.time.Duration;

public class JwtConstants {
    public static final String PAYLOAD_USER_KEY = "user";
    public static final String PAYLOAD_JTI_KEY = "jti";

    public static final String JWT_REDIS_KEY_PREFIX = "jwt:uid:";
    // token过期时间，设置为1小时，refresh token为7天
    public static final String SESSION_REDIS_KEY_PREFIX = "rk:auth:token:";
    public static final Duration JWT_TOKEN_TTL = Duration.ofDays(1);
    public static final Duration JWT_MOBILE_TOKEN_TTL = Duration.ofDays(15);
    public static final Duration JWT_REFRESH_TTL = Duration.ofDays(1);
    public static final Duration JWT_REMEMBER_ME_TTL = Duration.ofDays(1);

    public static final String JWT_ALGORITHM = "rs256";
    public static final String AUTHORIZATION_HEADER = "authorization";
    public static final String REFRESH_HEADER = "refresh";
    public static final String ADMIN_REFRESH_HEADER = "admin-refresh";
    public static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    public static final String CLIENT_TYPE_WEB = "web";
    public static final String CLIENT_TYPE_MOBILE = "mobile";

    public static final String USER_HEADER = "user-info";

    /* 权限缓存 KEY  begin */
    public static final String AUTH_PRIVILEGE_KEY = "auth:privileges";
    public static final String AUTH_PRIVILEGE_VERSION_KEY = "version";
    public static final String LOCK_AUTH_PRIVILEGE_KEY = "lock:auth:privileges";
    /* 权限缓存 KEY  end */

}
