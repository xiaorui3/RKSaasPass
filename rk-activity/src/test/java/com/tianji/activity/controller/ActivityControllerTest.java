package com.tianji.activity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.activity.domain.dto.ActivityQueryDTO;
import com.tianji.activity.domain.po.Activity;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 活动模块接口测试类
 * 覆盖CRUD、报名、签到、统计等核心功能
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdActivityId;

    // ==================== 测试数据构建 ====================

    private Activity createValidActivity() {
        Activity activity = new Activity();
        activity.setActivityName("测试活动_" + System.currentTimeMillis());
        activity.setContent("这是一个测试活动的描述");
        activity.setLocation("测试地点");
        activity.setStartTime(LocalDateTime.now().plusDays(1));
        activity.setEndTime(LocalDateTime.now().plusDays(2));
        activity.setRegistrationEndTime(LocalDateTime.now().plusHours(12));
        activity.setMaxParticipants(50);
        activity.setCurrentParticipants(0);
        activity.setActivityStatus(1); // 报名中
        activity.setActivityType(1); // 线下活动
        activity.setIsHot(0);
        activity.setViewCount(0);
        return activity;
    }

    // ==================== 查询接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("获取所有活动列表-成功")
    void testGetAllActivities_Success() throws Exception {
        mockMvc.perform(get("/api/activity/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取所有活动列表测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("分页获取活动列表-成功")
    void testGetActivityPage_Success() throws Exception {
        mockMvc.perform(get("/api/activity")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 分页获取活动列表测试通过");
    }

    @Test
    @Order(3)
    @DisplayName("分页获取活动-带状态筛选")
    void testGetActivityPage_WithStatus() throws Exception {
        mockMvc.perform(get("/api/activity")
                .param("page", "1")
                .param("size", "10")
                .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 带状态筛选分页测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("分页获取活动-带类型筛选")
    void testGetActivityPage_WithType() throws Exception {
        mockMvc.perform(get("/api/activity")
                .param("page", "1")
                .param("size", "10")
                .param("type", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 带类型筛选分页测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("根据状态获取活动-成功")
    void testGetActivitiesByStatus_Success() throws Exception {
        mockMvc.perform(get("/api/activity/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 根据状态获取活动测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("获取热门活动-成功")
    void testGetHotActivities_Success() throws Exception {
        mockMvc.perform(get("/api/activity/hot")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取热门活动测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("获取推荐活动-成功")
    void testGetTopActivities_Success() throws Exception {
        mockMvc.perform(get("/api/activity/top")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取推荐活动测试通过");
    }

    @Test
    @Order(8)
    @DisplayName("搜索活动-成功")
    void testSearchActivities_Success() throws Exception {
        mockMvc.perform(get("/api/activity/search")
                .param("keyword", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 搜索活动测试通过");
    }

    @Test
    @Order(9)
    @DisplayName("获取活动统计-成功")
    void testGetActivityStatistics_Success() throws Exception {
        mockMvc.perform(get("/api/activity/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取活动统计测试通过");
    }

    // ==================== 详情接口测试 ====================

    @Test
    @Order(10)
    @DisplayName("获取活动详情-成功")
    void testGetActivityDetail_Success() throws Exception {
        mockMvc.perform(get("/api/activity/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取活动详情测试通过");
    }

    @Test
    @Order(11)
    @DisplayName("获取活动详情-不存在的ID")
    void testGetActivityDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/activity/999999"))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 不存在的活动详情验证通过");
    }

    @Test
    @Order(12)
    @DisplayName("获取活动详情VO-成功")
    void testGetActivityDetailVO_Success() throws Exception {
        mockMvc.perform(get("/api/activity/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取活动详情VO测试通过");
    }

    // ==================== 创建活动测试 ====================

    @Test
    @Order(20)
    @DisplayName("创建活动-成功")
    void testCreateActivity_Success() throws Exception {
        Activity activity = createValidActivity();

        MvcResult result = mockMvc.perform(post("/api/activity/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        System.out.println("[SUCCESS] 创建活动测试通过: " + result.getResponse().getContentAsString());
    }

    @Test
    @Order(21)
    @DisplayName("创建活动-名称为空")
    void testCreateActivity_EmptyName() throws Exception {
        Activity activity = createValidActivity();
        activity.setActivityName("");

        mockMvc.perform(post("/api/activity/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 活动名称为空验证通过");
    }

    @Test
    @Order(22)
    @DisplayName("创建活动-时间为空")
    void testCreateActivity_EmptyTime() throws Exception {
        Activity activity = createValidActivity();
        activity.setStartTime(null);
        activity.setEndTime(null);

        mockMvc.perform(post("/api/activity/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 时间为空测试通过");
    }

    @Test
    @Order(23)
    @DisplayName("创建活动-结束时间早于开始时间")
    void testCreateActivity_InvalidTimeRange() throws Exception {
        Activity activity = createValidActivity();
        activity.setStartTime(LocalDateTime.now().plusDays(2));
        activity.setEndTime(LocalDateTime.now().plusDays(1)); // 结束时间早于开始时间

        mockMvc.perform(post("/api/activity/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 无效时间范围测试通过");
    }

    // ==================== 更新活动测试 ====================

    @Test
    @Order(30)
    @DisplayName("更新活动-成功")
    void testUpdateActivity_Success() throws Exception {
        Activity activity = createValidActivity();
        activity.setId(1L);

        mockMvc.perform(put("/api/activity/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 更新活动测试通过");
    }

    @Test
    @Order(31)
    @DisplayName("更新活动-ID为空")
    void testUpdateActivity_EmptyId() throws Exception {
        Activity activity = createValidActivity();
        activity.setId(null);

        mockMvc.perform(put("/api/activity/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 更新活动ID为空验证通过");
    }

    // ==================== 删除活动测试 ====================

    @Test
    @Order(40)
    @DisplayName("删除活动-成功")
    void testDeleteActivity_Success() throws Exception {
        mockMvc.perform(delete("/api/activity/delete/999999"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 删除活动测试通过");
    }

    // ==================== 报名接口测试 ====================

    @Test
    @Order(50)
    @DisplayName("活动报名-成功")
    void testRegisterActivity_Success() throws Exception {
        mockMvc.perform(post("/api/activity/1/register")
                .param("remark", "测试报名备注"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 活动报名测试通过");
    }

    @Test
    @Order(51)
    @DisplayName("活动报名-重复报名")
    void testRegisterActivity_Duplicate() throws Exception {
        // 第一次报名
        mockMvc.perform(post("/api/activity/1/register")
                .param("remark", "第一次报名"))
                .andReturn();

        // 重复报名
        mockMvc.perform(post("/api/activity/1/register")
                .param("remark", "重复报名"))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 重复报名验证通过");
    }

    @Test
    @Order(52)
    @DisplayName("取消报名-成功")
    void testCancelRegistration_Success() throws Exception {
        mockMvc.perform(post("/api/activity/1/cancel")
                .param("reason", "测试取消原因"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 取消报名测试通过");
    }

    @Test
    @Order(53)
    @DisplayName("检查是否已报名-成功")
    void testCheckRegistered_Success() throws Exception {
        mockMvc.perform(get("/api/activity/1/registered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 检查报名状态测试通过");
    }

    @Test
    @Order(54)
    @DisplayName("获取报名列表-成功")
    void testGetRegistrationList_Success() throws Exception {
        mockMvc.perform(get("/api/activity/1/registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取报名列表测试通过");
    }

    // ==================== 签到接口测试 ====================

    @Test
    @Order(60)
    @DisplayName("活动签到-成功")
    void testCheckIn_Success() throws Exception {
        mockMvc.perform(post("/api/activity/1/checkin"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 活动签到测试通过");
    }

    @Test
    @Order(61)
    @DisplayName("活动签到-未报名")
    void testCheckIn_NotRegistered() throws Exception {
        mockMvc.perform(post("/api/activity/999999/checkin"))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 未报名签到验证通过");
    }

    // ==================== 状态更新测试 ====================

    @Test
    @Order(70)
    @DisplayName("更新活动状态-成功")
    void testUpdateActivityStatus_Success() throws Exception {
        mockMvc.perform(post("/api/activity/update-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 更新活动状态测试通过");
    }

    // ==================== VO分页测试 ====================

    @Test
    @Order(80)
    @DisplayName("分页获取活动列表VO-成功")
    void testGetActivityPageVO_Success() throws Exception {
        ActivityQueryDTO queryDTO = new ActivityQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setSize(10);
        queryDTO.setStatus(1);

        mockMvc.perform(post("/api/activity/page")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queryDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 分页获取活动VO测试通过");
    }

    // ==================== 边界测试 ====================

    @Test
    @Order(90)
    @DisplayName("搜索活动-特殊字符")
    void testSearch_SpecialCharacters() throws Exception {
        mockMvc.perform(get("/api/activity/search")
                .param("keyword", "<script>alert('xss')</script>"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 特殊字符搜索测试通过");
    }

    @Test
    @Order(91)
    @DisplayName("获取热门活动-超大limit")
    void testGetHotActivities_LargeLimit() throws Exception {
        mockMvc.perform(get("/api/activity/hot")
                .param("limit", "10000"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 超大limit测试通过");
    }
}
