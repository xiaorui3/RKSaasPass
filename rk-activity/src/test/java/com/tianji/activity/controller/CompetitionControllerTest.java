package com.tianji.activity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.activity.domain.dto.CompetitionQueryDTO;
import com.tianji.activity.domain.dto.CompetitionRegisterDTO;
import com.tianji.activity.domain.dto.CompetitionSaveDTO;
import com.tianji.activity.domain.dto.CompetitionUpdateDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 比赛模块接口测试类
 * 覆盖CRUD、报名、状态管理等核心功能
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CompetitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 测试数据构建 ====================

    private CompetitionSaveDTO createValidCompetitionSaveDTO() {
        CompetitionSaveDTO dto = new CompetitionSaveDTO();
        dto.setTitle("测试比赛_" + System.currentTimeMillis());
        dto.setDescription("这是一个测试比赛的描述");
        dto.setCompetitionType("编程竞赛");
        dto.setRegistrationStart(LocalDateTime.now());
        dto.setRegistrationEnd(LocalDateTime.now().plusDays(7));
        dto.setCompetitionStart(LocalDateTime.now().plusDays(10));
        dto.setCompetitionEnd(LocalDateTime.now().plusDays(11));
        dto.setMaxParticipants(100);
        dto.setRulesFile("/files/rules.pdf");
        dto.setAwards("奖项设置说明");
        dto.setIsPublished(true);
        dto.setIsFeatured(false);
        dto.setManagerReviewerId(7001L);
        dto.setTeacherReviewerId(8001L);
        return dto;
    }

    private CompetitionUpdateDTO createValidCompetitionUpdateDTO() {
        CompetitionUpdateDTO dto = new CompetitionUpdateDTO();
        dto.setTitle("更新后的比赛标题_" + System.currentTimeMillis());
        dto.setDescription("更新后的比赛描述");
        dto.setMaxParticipants(200);
        return dto;
    }

    private CompetitionRegisterDTO createValidRegisterDTO() {
        CompetitionRegisterDTO dto = new CompetitionRegisterDTO();
        dto.setName("测试用户");
        dto.setStudentId("2021001");
        dto.setTeamName("测试团队");
        dto.setTeamMembers("成员1,成员2,成员3");
        dto.setEmail("contact@test.com");
        dto.setPhone("13800138000");
        dto.setRemark("报名备注");
        return dto;
    }

    // ==================== 列表查询测试 ====================

    @Test
    @Order(1)
    @DisplayName("分页查询比赛列表-成功")
    void testPageCompetitions_Success() throws Exception {
        mockMvc.perform(get("/api/competition/page")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 分页查询比赛列表测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("获取所有比赛-成功")
    void testGetAllCompetitions_Success() throws Exception {
        mockMvc.perform(get("/api/competition/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取所有比赛测试通过");
    }

    @Test
    @Order(3)
    @DisplayName("获取已发布比赛-成功")
    void testGetPublishedCompetitions_Success() throws Exception {
        mockMvc.perform(get("/api/competition/published")
                .param("sortBy", "create_time")
                .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取已发布比赛测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("获取推荐比赛-成功")
    void testGetFeaturedCompetitions_Success() throws Exception {
        mockMvc.perform(get("/api/competition/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取推荐比赛测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("根据状态获取比赛-成功")
    void testGetCompetitionsByStatus_Success() throws Exception {
        mockMvc.perform(get("/api/competition/status/REGISTRATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 根据状态获取比赛测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("获取简单比赛列表-成功")
    void testGetSimpleList_Success() throws Exception {
        mockMvc.perform(get("/api/competition/simple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取简单比赛列表测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("搜索比赛-成功")
    void testSearchCompetitions_Success() throws Exception {
        mockMvc.perform(get("/api/competition/search")
                .param("keyword", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 搜索比赛测试通过");
    }

    // ==================== 详情查询测试 ====================

    @Test
    @Order(10)
    @DisplayName("获取比赛详情-成功")
    void testGetCompetitionDetail_Success() throws Exception {
        mockMvc.perform(get("/api/competition/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取比赛详情测试通过");
    }

    @Test
    @Order(11)
    @DisplayName("获取比赛详情-不存在的ID")
    void testGetCompetitionDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/competition/999999"))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 不存在的比赛详情验证通过");
    }

    // ==================== 创建比赛测试 ====================

    @Test
    @Order(20)
    @DisplayName("创建比赛-成功")
    void testCreateCompetition_Success() throws Exception {
        CompetitionSaveDTO dto = createValidCompetitionSaveDTO();

        MvcResult result = mockMvc.perform(post("/api/competition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        System.out.println("[SUCCESS] 创建比赛测试通过: " + result.getResponse().getContentAsString());
    }

    @Test
    @Order(21)
    @DisplayName("创建比赛-标题为空")
    void testCreateCompetition_EmptyTitle() throws Exception {
        CompetitionSaveDTO dto = createValidCompetitionSaveDTO();
        dto.setTitle("");

        mockMvc.perform(post("/api/competition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 标题为空验证通过");
    }

    @Test
    @Order(22)
    @DisplayName("创建比赛-时间为空")
    void testCreateCompetition_EmptyTime() throws Exception {
        CompetitionSaveDTO dto = createValidCompetitionSaveDTO();
        dto.setRegistrationStart(null);
        dto.setRegistrationEnd(null);

        mockMvc.perform(post("/api/competition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 时间为空验证通过");
    }

    // ==================== 更新比赛测试 ====================

    @Test
    @Order(30)
    @DisplayName("更新比赛-成功")
    void testUpdateCompetition_Success() throws Exception {
        CompetitionUpdateDTO dto = createValidCompetitionUpdateDTO();

        mockMvc.perform(put("/api/competition/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 更新比赛测试通过");
    }

    @Test
    @Order(31)
    @DisplayName("更新比赛-不存在的ID")
    void testUpdateCompetition_NotFound() throws Exception {
        CompetitionUpdateDTO dto = createValidCompetitionUpdateDTO();

        mockMvc.perform(put("/api/competition/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 更新不存在的比赛验证通过");
    }

    // ==================== 删除比赛测试 ====================

    @Test
    @Order(40)
    @DisplayName("删除比赛-成功")
    void testDeleteCompetition_Success() throws Exception {
        mockMvc.perform(delete("/api/competition/999999"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 删除比赛测试通过");
    }

    // ==================== 发布/取消比赛测试 ====================

    @Test
    @Order(50)
    @DisplayName("发布比赛-成功")
    void testPublishCompetition_Success() throws Exception {
        mockMvc.perform(post("/api/competition/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 发布比赛测试通过");
    }

    @Test
    @Order(51)
    @DisplayName("取消比赛-成功")
    void testCancelCompetition_Success() throws Exception {
        mockMvc.perform(post("/api/competition/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 取消比赛测试通过");
    }

    // ==================== 报名测试 ====================

    @Test
    @Order(60)
    @DisplayName("比赛报名-成功")
    void testRegisterCompetition_Success() throws Exception {
        CompetitionRegisterDTO dto = createValidRegisterDTO();

        mockMvc.perform(post("/api/competition/1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 比赛报名测试通过");
    }

    @Test
    @Order(61)
    @DisplayName("比赛报名-重复报名")
    void testRegisterCompetition_Duplicate() throws Exception {
        CompetitionRegisterDTO dto = createValidRegisterDTO();

        // 第一次报名
        mockMvc.perform(post("/api/competition/1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        // 重复报名
        mockMvc.perform(post("/api/competition/1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 重复报名验证通过");
    }

    @Test
    @Order(62)
    @DisplayName("取消报名-成功")
    void testCancelRegistration_Success() throws Exception {
        mockMvc.perform(post("/api/competition/1/register/cancel"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 取消报名测试通过");
    }

    @Test
    @Order(63)
    @DisplayName("获取报名列表-成功")
    void testGetRegistrations_Success() throws Exception {
        mockMvc.perform(get("/api/competition/1/registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取报名列表测试通过");
    }

    @Test
    @Order(64)
    @DisplayName("检查是否已报名-成功")
    void testCheckRegistered_Success() throws Exception {
        mockMvc.perform(get("/api/competition/1/registered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 检查报名状态测试通过");
    }

    @Test
    @Order(65)
    @DisplayName("获取我的报名详情-成功")
    void testGetMyRegistration_Success() throws Exception {
        mockMvc.perform(get("/api/competition/1/register/my"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 获取我的报名详情测试通过");
    }

    // ==================== 边界测试 ====================

    @Test
    @Order(70)
    @DisplayName("分页查询-超大页码")
    void testPageCompetitions_LargePageNumber() throws Exception {
        mockMvc.perform(get("/api/competition/page")
                .param("page", "999999")
                .param("size", "10"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 超大页码测试通过");
    }

    @Test
    @Order(71)
    @DisplayName("搜索比赛-空关键词")
    void testSearchCompetitions_EmptyKeyword() throws Exception {
        mockMvc.perform(get("/api/competition/search")
                .param("keyword", ""))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 空关键词搜索测试通过");
    }

    @Test
    @Order(72)
    @DisplayName("搜索比赛-特殊字符")
    void testSearchCompetitions_SpecialCharacters() throws Exception {
        mockMvc.perform(get("/api/competition/search")
                .param("keyword", "<script>alert('xss')</script>"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 特殊字符搜索测试通过");
    }
}
