package com.tianji.common.utils;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 租户上下文工具类单元测试
 * 测试多租户上下文的设置和获取
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
class TenantContextTest {

    @BeforeEach
    void setUp() {
        // 每个测试前清除租户上下文
        TenantContext.removeTenantId();
    }

    @AfterEach
    void tearDown() {
        // 每个测试后清除租户上下文
        TenantContext.removeTenantId();
    }

    // ==================== 基本操作测试 ====================

    @Test
    @DisplayName("设置租户ID-成功")
    void testSetTenantId_Success() {
        Long tenantId = 1L;
        TenantContext.setTenantId(tenantId);
        
        assertEquals(tenantId, TenantContext.getTenantId());
        
        System.out.println("[SUCCESS] 设置租户ID测试通过");
    }

    @Test
    @DisplayName("获取租户ID-未设置时返回null")
    void testGetTenantId_NotSet() {
        Long tenantId = TenantContext.getTenantId();
        
        assertNull(tenantId);
        
        System.out.println("[SUCCESS] 未设置时获取租户ID测试通过");
    }

    @Test
    @DisplayName("移除租户ID-成功")
    void testRemoveTenantId_Success() {
        TenantContext.setTenantId(1L);
        assertNotNull(TenantContext.getTenantId());
        
        TenantContext.removeTenantId();
        assertNull(TenantContext.getTenantId());
        
        System.out.println("[SUCCESS] 移除租户ID测试通过");
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("设置null租户ID")
    void testSetTenantId_Null() {
        TenantContext.setTenantId(null);
        
        assertNull(TenantContext.getTenantId());
        
        System.out.println("[SUCCESS] 设置null租户ID测试通过");
    }

    @Test
    @DisplayName("设置多个租户ID-覆盖")
    void testSetTenantId_Override() {
        TenantContext.setTenantId(1L);
        assertEquals(1L, TenantContext.getTenantId());
        
        TenantContext.setTenantId(2L);
        assertEquals(2L, TenantContext.getTenantId());
        
        System.out.println("[SUCCESS] 覆盖租户ID测试通过");
    }

    @Test
    @DisplayName("设置大数值租户ID")
    void testSetTenantId_LargeValue() {
        Long largeTenantId = Long.MAX_VALUE;
        TenantContext.setTenantId(largeTenantId);
        
        assertEquals(largeTenantId, TenantContext.getTenantId());
        
        System.out.println("[SUCCESS] 大数值租户ID测试通过");
    }

    @Test
    @DisplayName("设置负数租户ID")
    void testSetTenantId_Negative() {
        Long negativeTenantId = -1L;
        TenantContext.setTenantId(negativeTenantId);
        
        assertEquals(negativeTenantId, TenantContext.getTenantId());
        
        System.out.println("[SUCCESS] 负数租户ID测试通过");
    }

    // ==================== 并发测试 ====================

    @Test
    @DisplayName("并发设置租户ID")
    void testSetTenantId_Concurrent() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final Long tenantId = (long) (i + 1);
            threads[i] = new Thread(() -> {
                TenantContext.setTenantId(tenantId);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                assertEquals(tenantId, TenantContext.getTenantId());
                TenantContext.removeTenantId();
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("[SUCCESS] 并发设置租户ID测试通过");
    }
}
