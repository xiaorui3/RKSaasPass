package com.tianji.common.domain;

import com.tianji.common.constants.Constant;
import com.tianji.common.constants.ErrorInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 企业级统一API响应结果类
 * 
 * 响应格式（参考用友企业级标准）：
 * {
 *   "code": 200,        // 业务状态码：200成功，其他失败
 *   "msg": "操作成功",   // 响应消息
 *   "state": "success", // 状态标识：success/error/warning
 *   "data": { ... }     // 业务数据
 * }
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel(description = "企业级统一API响应结果")
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态枚举
     */
    public static final String STATE_SUCCESS = "success";
    public static final String STATE_ERROR = "error";
    public static final String STATE_WARNING = "warning";

    @ApiModelProperty(value = "业务状态码：200-成功，其他-失败", example = "200")
    private Integer code;

    @ApiModelProperty(value = "响应消息", example = "操作成功")
    private String msg;

    @ApiModelProperty(value = "状态标识：success/error/warning", example = "success")
    private String state;

    @ApiModelProperty(value = "响应数据")
    private T data;

    @ApiModelProperty(value = "请求追踪ID", example = "1af123c11412e")
    private String requestId;

    @ApiModelProperty(value = "响应时间戳", example = "1707580800000")
    private Long timestamp;

    // ==================== 构造方法 ====================

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public R(Integer code, String msg, String state, T data) {
        this.code = code;
        this.msg = msg;
        this.state = state;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.requestId = MDC.get(Constant.REQUEST_ID_HEADER);
    }

    // ==================== 成功响应 ====================

    /**
     * 成功响应（无数据）
     * 支持泛型链式调用，例如：R.&lt;String&gt;ok().data("数据")
     */
    public static <T> R<T> ok() {
        return new R<>(ErrorInfo.Code.SUCCESS, ErrorInfo.Msg.OK, STATE_SUCCESS, null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> ok(T data) {
        return new R<>(ErrorInfo.Code.SUCCESS, ErrorInfo.Msg.OK, STATE_SUCCESS, data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> R<T> ok(String msg, T data) {
        return new R<>(ErrorInfo.Code.SUCCESS, msg, STATE_SUCCESS, data);
    }

    // ==================== 失败响应 ====================

    /**
     * 失败响应（默认错误码）
     */
    public static <T> R<T> error(String msg) {
        return new R<>(ErrorInfo.Code.FAILED, msg, STATE_ERROR, null);
    }

    /**
     * 失败响应（自定义错误码）
     */
    public static <T> R<T> error(Integer code, String msg) {
        return new R<>(code, msg, STATE_ERROR, null);
    }

    /**
     * 失败响应（完整参数）
     */
    public static <T> R<T> error(Integer code, String msg, String state) {
        return new R<>(code, msg, state, null);
    }

    // ==================== 警告响应 ====================

    /**
     * 警告响应
     */
    public static <T> R<T> warning(String msg) {
        return new R<>(ErrorInfo.Code.FAILED, msg, STATE_WARNING, null);
    }

    /**
     * 警告响应（带数据）
     */
    public static <T> R<T> warning(String msg, T data) {
        return new R<>(ErrorInfo.Code.FAILED, msg, STATE_WARNING, data);
    }

    // ==================== 链式调用 ====================

    /**
     * 设置请求ID
     */
    public R<T> requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * 设置消息
     */
    public R<T> msg(String msg) {
        this.msg = msg;
        return this;
    }

    /**
     * 设置数据
     */
    public R<T> data(T data) {
        this.data = data;
        return this;
    }

    // ==================== 工具方法 ====================

    /**
     * 判断是否成功
     */
    public boolean success() {
        return code != null && code == ErrorInfo.Code.SUCCESS;
    }

    /**
     * 判断是否失败
     */
    public boolean failed() {
        return !success();
    }
}
