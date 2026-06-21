package com.tianji.common.domain;

import com.tianji.common.constants.ErrorInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一响应结果R类单元测试
 * 测试R类的各种静态工厂方法和工具方法
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
class RTest {

    // ==================== 成功响应测试 ====================

    @Test
    @DisplayName("成功响应-无数据")
    void testOk_NoData() {
        R<Void> result = R.ok();
        
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getState());
        assertNull(result.getData());
        assertNotNull(result.getTimestamp());
        
        System.out.println("[SUCCESS] 成功响应(无数据)测试通过");
    }

    @Test
    @DisplayName("成功响应-带数据")
    void testOk_WithData() {
        String testData = "测试数据";
        R<String> result = R.ok(testData);
        
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getState());
        assertEquals(testData, result.getData());
        
        System.out.println("[SUCCESS] 成功响应(带数据)测试通过");
    }

    @Test
    @DisplayName("成功响应-带消息和数据")
    void testOk_WithMessageAndData() {
        String message = "操作成功";
        Integer data = 100;
        R<Integer> result = R.ok(message, data);
        
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getState());
        assertEquals(message, result.getMsg());
        assertEquals(data, result.getData());
        
        System.out.println("[SUCCESS] 成功响应(带消息和数据)测试通过");
    }

    // ==================== 失败响应测试 ====================

    @Test
    @DisplayName("失败响应-默认错误码")
    void testError_DefaultCode() {
        String errorMsg = "操作失败";
        R<Void> result = R.error(errorMsg);
        
        assertNotNull(result);
        assertEquals(ErrorInfo.Code.FAILED, result.getCode()); // 默认失败码为0
        assertEquals("error", result.getState());
        assertEquals(errorMsg, result.getMsg());
        assertNull(result.getData());
        
        System.out.println("[SUCCESS] 失败响应(默认错误码)测试通过");
    }

    @Test
    @DisplayName("失败响应-自定义错误码")
    void testError_CustomCode() {
        Integer customCode = 400;
        String errorMsg = "参数错误";
        R<Void> result = R.error(customCode, errorMsg);
        
        assertNotNull(result);
        assertEquals(customCode, result.getCode());
        assertEquals("error", result.getState());
        assertEquals(errorMsg, result.getMsg());
        
        System.out.println("[SUCCESS] 失败响应(自定义错误码)测试通过");
    }

    @Test
    @DisplayName("失败响应-完整参数")
    void testError_FullParams() {
        Integer code = 403;
        String msg = "权限不足";
        String state = "warning";
        R<Void> result = R.error(code, msg, state);
        
        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals(msg, result.getMsg());
        assertEquals(state, result.getState());
        
        System.out.println("[SUCCESS] 失败响应(完整参数)测试通过");
    }

    // ==================== 警告响应测试 ====================

    @Test
    @DisplayName("警告响应-无数据")
    void testWarning_NoData() {
        String warningMsg = "请注意";
        R<Void> result = R.warning(warningMsg);
        
        assertNotNull(result);
        assertEquals("warning", result.getState());
        assertEquals(warningMsg, result.getMsg());
        
        System.out.println("[SUCCESS] 警告响应(无数据)测试通过");
    }

    @Test
    @DisplayName("警告响应-带数据")
    void testWarning_WithData() {
        String warningMsg = "数据可能不完整";
        Object data = new Object();
        R<Object> result = R.warning(warningMsg, data);
        
        assertNotNull(result);
        assertEquals("warning", result.getState());
        assertEquals(warningMsg, result.getMsg());
        assertEquals(data, result.getData());
        
        System.out.println("[SUCCESS] 警告响应(带数据)测试通过");
    }

    // ==================== 链式调用测试 ====================

    @Test
    @DisplayName("链式调用-requestId")
    void testChain_RequestId() {
        String requestId = "req-123456";
        R<String> result = R.ok("测试").requestId(requestId);
        
        assertEquals(requestId, result.getRequestId());
        
        System.out.println("[SUCCESS] 链式调用requestId测试通过");
    }

    @Test
    @DisplayName("链式调用-msg")
    void testChain_Msg() {
        String customMsg = "自定义消息";
        R<String> result = R.ok("测试").msg(customMsg);
        
        assertEquals(customMsg, result.getMsg());
        
        System.out.println("[SUCCESS] 链式调用msg测试通过");
    }

    @Test
    @DisplayName("链式调用-data")
    void testChain_Data() {
        String newData = "新数据";
        R<String> result = R.<String>ok().data(newData);
        
        assertEquals(newData, result.getData());
        
        System.out.println("[SUCCESS] 链式调用data测试通过");
    }

    @Test
    @DisplayName("链式调用-组合")
    void testChain_Combined() {
        String requestId = "req-789";
        String msg = "组合测试";
        String data = "测试数据";
        
        R<String> result = R.<String>ok()
                .requestId(requestId)
                .msg(msg)
                .data(data);
        
        assertEquals(requestId, result.getRequestId());
        assertEquals(msg, result.getMsg());
        assertEquals(data, result.getData());
        
        System.out.println("[SUCCESS] 链式调用组合测试通过");
    }

    // ==================== 工具方法测试 ====================

    @Test
    @DisplayName("判断成功-success")
    void testSuccess_Success() {
        R<String> result = R.ok("测试");
        assertTrue(result.success());
        assertFalse(result.failed());
        
        System.out.println("[SUCCESS] 判断成功(success)测试通过");
    }

    @Test
    @DisplayName("判断成功-error")
    void testSuccess_Error() {
        R<Void> result = R.error("错误");
        assertFalse(result.success());
        assertTrue(result.failed());
        
        System.out.println("[SUCCESS] 判断成功(error)测试通过");
    }

    @Test
    @DisplayName("判断成功-warning")
    void testSuccess_Warning() {
        R<Void> result = R.warning("警告");
        assertFalse(result.success());
        assertTrue(result.failed());
        
        System.out.println("[SUCCESS] 判断成功(warning)测试通过");
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("空数据测试")
    void testNullData() {
        R<String> result = R.ok(null);
        
        assertEquals(200, result.getCode());
        assertNull(result.getData());
        
        System.out.println("[SUCCESS] 空数据测试通过");
    }

    @Test
    @DisplayName("复杂对象数据测试")
    void testComplexObjectData() {
        TestData data = new TestData(1L, "测试名称", "测试描述");
        R<TestData> result = R.ok(data);
        
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().id);
        assertEquals("测试名称", result.getData().name);
        
        System.out.println("[SUCCESS] 复杂对象数据测试通过");
    }

    // 测试用的简单类
    static class TestData {
        Long id;
        String name;
        String description;

        TestData(Long id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
    }
}
