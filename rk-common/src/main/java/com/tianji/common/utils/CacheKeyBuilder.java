package com.tianji.common.utils;

/**
 * 缓存Key构建工具类
 * 用于构建带租户隔离的Redis缓存Key
 *
 * Key格式：模块:租户ID:业务:标识
 * 示例：news:1:list:page1
 *
 * @author RK-Web Team
 * @since 1.0.0
 */
public class CacheKeyBuilder {

    private static final String SEPARATOR = ":";

    /**
     * 构建缓存Key（自动获取当前租户ID）
     *
     * @param module 模块名称（如：news、works、competition）
     * @param business 业务类型（如：list、detail、count）
     * @param identifier 唯一标识
     * @return 完整的缓存Key
     */
    public static String build(String module, String business, String identifier) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = 0L; // 默认租户
        }
        return build(module, tenantId, business, identifier);
    }

    /**
     * 构建缓存Key（指定租户ID）
     *
     * @param module 模块名称
     * @param tenantId 租户ID
     * @param business 业务类型
     * @param identifier 唯一标识
     * @return 完整的缓存Key
     */
    public static String build(String module, Long tenantId, String business, String identifier) {
        StringBuilder sb = new StringBuilder();
        sb.append(module).append(SEPARATOR);
        sb.append(tenantId).append(SEPARATOR);
        sb.append(business).append(SEPARATOR);
        sb.append(identifier);
        return sb.toString();
    }

    /**
     * 构建列表缓存Key
     *
     * @param module 模块名称
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 列表缓存Key
     */
    public static String buildListKey(String module, int pageNum, int pageSize) {
        return build(module, "list", "page" + pageNum + "_size" + pageSize);
    }

    /**
     * 构建详情缓存Key
     *
     * @param module 模块名称
     * @param id 记录ID
     * @return 详情缓存Key
     */
    public static String buildDetailKey(String module, Long id) {
        return build(module, "detail", String.valueOf(id));
    }

    /**
     * 构建计数缓存Key
     *
     * @param module 模块名称
     * @param type 计数类型（如：view、like）
     * @param id 记录ID
     * @return 计数缓存Key
     */
    public static String buildCountKey(String module, String type, Long id) {
        return build(module, type + "_count", String.valueOf(id));
    }

    /**
     * 构建用户相关缓存Key
     *
     * @param module 模块名称
     * @param userId 用户ID
     * @param business 业务类型
     * @return 用户相关缓存Key
     */
    public static String buildUserKey(String module, Long userId, String business) {
        return build(module, "user_" + userId, business);
    }

    // ==================== 预定义模块常量 ====================

    /**
     * 新闻模块
     */
    public static final String MODULE_NEWS = "news";

    /**
     * 作品模块
     */
    public static final String MODULE_WORKS = "works";

    /**
     * 比赛模块
     */
    public static final String MODULE_COMPETITION = "competition";

    /**
     * 活动模块
     */
    public static final String MODULE_ACTIVITY = "activity";

    /**
     * 校友模块
     */
    public static final String MODULE_ALUMNI = "alumni";

    /**
     * 用户模块
     */
    public static final String MODULE_USER = "user";

    /**
     * 租户模块
     */
    public static final String MODULE_TENANT = "tenant";
}
