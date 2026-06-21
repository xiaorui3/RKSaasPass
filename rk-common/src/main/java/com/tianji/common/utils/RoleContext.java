package com.tianji.common.utils;

public class RoleContext {
    private static final ThreadLocal<Long> ROLE_ID = new ThreadLocal<>();

    public static void setRoleId(Long roleId) {
        ROLE_ID.set(roleId);
    }

    public static Long getRoleId() {
        return ROLE_ID.get();
    }

    public static void clear() {
        ROLE_ID.remove();
    }
}
