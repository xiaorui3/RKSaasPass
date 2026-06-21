package com.tianji.common.constants;

/**
 * 企业级错误码和错误信息定义
 * 
 * 错误码规范：
 * - 200: 成功
 * - 400-499: 客户端错误
 * - 500-599: 服务端错误
 * - 1000-1999: 业务错误
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
public interface ErrorInfo {

    interface Msg {
        // ==================== 通用消息 ====================
        String OK = "操作成功";
        String OPERATE_FAILED = "操作失败";
        
        // ==================== 验证相关 ====================
        String INVALID_VERIFY_CODE = "验证码错误";
        String INVALID_TOKEN = "令牌无效或已过期";
        String UNAUTHORIZED = "未授权，请先登录";
        String FORBIDDEN = "权限不足，拒绝访问";

        // ==================== 服务器错误 ====================
        String SERVER_INTER_ERROR = "服务器内部错误";
        String SERVICE_UNAVAILABLE = "服务暂时不可用";

        // ==================== 数据库操作 ====================
        String DB_SAVE_EXCEPTION = "数据新增失败";
        String DB_DELETE_EXCEPTION = "数据删除失败";
        String DB_BATCH_DELETE_EXCEPTION = "数据批量删除失败";
        String DB_UPDATE_EXCEPTION = "数据更新失败";
        String DB_QUERY_EXCEPTION = "数据查询失败";
        String DB_SORT_FIELD_NOT_FOUND = "排序字段不存在";
        String DATA_NOT_FOUND = "数据不存在";

        // ==================== 请求错误 ====================
        String REQUEST_PARAM_ILLEGAL = "请求参数不合法";
        String REQUEST_PARAM_MISSING = "请求参数缺失";
        String REQUEST_OPERATE_FREQUENTLY = "操作频繁,请稍后重试";
        String REQUEST_TIME_OUT = "请求超时";
        String RESOURCE_NOT_FOUND = "请求的资源不存在";

        // ==================== 用户相关 ====================
        String USER_NOT_EXISTS = "用户信息不存在";
        String INVALID_USER_TYPE = "无效的用户类型";
        String USER_DISABLED = "账号已被禁用";
        String PASSWORD_ERROR = "密码错误";
        String ACCOUNT_LOCKED = "账号已被锁定";

        // ==================== 租户相关 ====================
        String TENANT_NOT_EXISTS = "租户不存在";
        String TENANT_DISABLED = "租户已被禁用";
        String TENANT_EXPIRED = "租户已过期";

        // ==================== 业务错误 ====================
        String BUSINESS_ERROR = "业务处理失败";
        String DUPLICATE_DATA = "数据已存在，请勿重复操作";
    }

    interface Code {
        // ==================== 成功 ====================
        int SUCCESS = 200;

        // ==================== 客户端错误 4xx ====================
        int BAD_REQUEST = 400;                    // 请求参数错误
        int UNAUTHORIZED = 401;                   // 未授权
        int FORBIDDEN = 403;                      // 权限不足
        int NOT_FOUND = 404;                      // 资源不存在
        int METHOD_NOT_ALLOWED = 405;             // 方法不允许
        int REQUEST_TIMEOUT = 408;                // 请求超时
        int CONFLICT = 409;                       // 资源冲突
        int TOO_MANY_REQUESTS = 429;              // 请求过于频繁

        // ==================== 服务端错误 5xx ====================
        int INTERNAL_SERVER_ERROR = 500;          // 服务器内部错误
        int SERVICE_UNAVAILABLE = 503;            // 服务不可用
        int GATEWAY_TIMEOUT = 504;                // 网关超时

        // ==================== 业务错误 1xxx ====================
        int FAILED = 0;                           // 通用失败
        int BUSINESS_ERROR_CODE = 1000;           // 业务错误基码
        
        // 用户相关 10xx
        int USER_NOT_FOUND = 1001;                // 用户不存在
        int PASSWORD_INCORRECT = 1002;            // 密码错误
        int ACCOUNT_DISABLED = 1003;              // 账号已禁用
        int ACCOUNT_LOCKED_CODE = 1004;           // 账号已锁定
        int TOKEN_EXPIRED = 1005;                 // Token过期
        int TOKEN_INVALID = 1006;                 // Token无效

        // 租户相关 11xx
        int TENANT_NOT_FOUND = 1101;              // 租户不存在
        int TENANT_DISABLED_CODE = 1102;          // 租户已禁用
        int TENANT_EXPIRED_CODE = 1103;           // 租户已过期

        // 数据操作 12xx
        int DATA_SAVE_FAILED = 1201;              // 数据保存失败
        int DATA_UPDATE_FAILED = 1202;            // 数据更新失败
        int DATA_DELETE_FAILED = 1203;            // 数据删除失败
        int DATA_QUERY_FAILED = 1204;             // 数据查询失败
        int DATA_DUPLICATE = 1205;                // 数据重复

        // 新闻相关 20xx
        int NEWS_NOT_FOUND = 2001;                // 新闻不存在

        // 活动相关 30xx
        int ACTIVITY_NOT_FOUND = 3001;            // 活动不存在
        int ACTIVITY_REGISTRATION_CLOSED = 3002;  // 活动报名已截止
        int ACTIVITY_FULL = 3003;                 // 活动名额已满

        // 比赛相关 40xx
        int COMPETITION_NOT_FOUND = 4001;         // 比赛不存在
        int COMPETITION_REGISTRATION_CLOSED = 4002; // 比赛报名已截止

        // 文件相关 50xx
        int FILE_UPLOAD_FAILED = 5001;            // 文件上传失败
        int FILE_NOT_FOUND = 5002;                // 文件不存在
        int FILE_TYPE_NOT_ALLOWED = 5003;         // 文件类型不允许
    }
}
