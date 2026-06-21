package com.tianji.common.utils;

/**
 * 租户上下文管理类
 * 用于在请求处理过程中存储和获取当前租户ID
 * 使用ThreadLocal实现线程隔离
 *
 * @author RK-Web Team
 * @since 1.0.0
 */
public class TenantContext {

    /**
     * 线程本地变量，存储当前租户ID
     */
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    /**
     * 线程本地变量，存储是否为超级管理员
     * 超级管理员可以跨租户访问数据
     */
    private static final ThreadLocal<Boolean> SUPER_ADMIN = new ThreadLocal<>();

    /**
     * 设置当前租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，如果未设置则返回null
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 设置是否为超级管理员
     *
     * @param isSuperAdmin 是否为超级管理员
     */
    public static void setSuperAdmin(Boolean isSuperAdmin) {
        SUPER_ADMIN.set(isSuperAdmin);
    }

    /**
     * 判断是否为超级管理员
     *
     * @return 是否为超级管理员
     */
    public static Boolean isSuperAdmin() {
        return Boolean.TRUE.equals(SUPER_ADMIN.get());
    }

    /**
     * 移除当前租户ID
     * 通常在请求处理完成后调用，避免内存泄漏
     */
    public static void removeTenantId() {
        TENANT_ID.remove();
    }

    /**
     * 清理所有上下文信息
     */
    public static void clear() {
        TENANT_ID.remove();
        SUPER_ADMIN.remove();
    }
}