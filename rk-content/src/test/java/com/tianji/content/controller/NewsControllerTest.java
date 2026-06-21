package com.tianji.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.content.domain.dto.NewsQueryDTO;
import com.tianji.content.domain.dto.NewsSaveDTO;
import com.tianji.content.domain.dto.NewsUpdateDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 新闻模块接口测试类
 * 覆盖CRUD、搜索、分类、统计等核心功能
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdNewsId;

    // ==================== 测试数据构建 ====================

    private NewsSaveDTO createValidNewsSaveDTO() {
        NewsSaveDTO dto = new NewsSaveDTO();
        dto.setTitle("测试新闻标题_" + System.currentTimeMillis());
        dto.setContent("这是测试新闻的内容，包含足够的字数用于测试。");
        dto.setSummary("测试新闻摘要");
        dto.setCategory("社团动态");
        dto.setAuthor("测试作者");
        dto.setCoverImage("/uploads/test.jpg");
        dto.setIsPublished(1);
        dto.setIsFeatured(0);
        return dto;
    }

    private NewsUpdateDTO createValidNewsUpdateDTO() {
        NewsUpdateDTO dto = new NewsUpdateDTO();
        dto.setTitle("更新后的新闻标题_" + System.currentTimeMillis());
        dto.setContent("更新后的新闻内容");
        dto.setSummary("更新后的摘要");
        dto.setCategory("通知公告");
        return dto;
    }

    // ==================== 查询接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("获取最新新闻-成功")
    void testGetLatestNews_Success() throws Exception {
        mockMvc.perform(get("/api/news/latest")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取最新新闻测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("获取置顶新闻-成功")
    void testGetTopNews_Success() throws Exception {
        mockMvc.perform(get("/api/news/top")
                .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取置顶新闻测试通过");
    }

    @Test
    @Order(3)
    @DisplayName("按分类获取新闻-成功")
    void testGetNewsByCategory_Success() throws Exception {
        mockMvc.perform(get("/api/news/category/社团动态")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 按分类获取新闻测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("搜索新闻-成功")
    void testSearchNews_Success() throws Exception {
        mockMvc.perform(get("/api/news/search")
                .param("keyword", "测试")
                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 搜索新闻测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("搜索新闻-空关键词")
    void testSearchNews_EmptyKeyword() throws Exception {
        mockMvc.perform(get("/api/news/search")
                .param("keyword", "")
                .param("limit", "20"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 空关键词搜索测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("获取新闻列表-成功")
    void testGetNewsList_Success() throws Exception {
        mockMvc.perform(get("/api/news/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取新闻列表测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("分页获取新闻-成功")
    void testGetNewsWithPagination_Success() throws Exception {
        mockMvc.perform(get("/api/news")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1));
        
        System.out.println("[SUCCESS] 分页获取新闻测试通过");
    }

    @Test
    @Order(8)
    @DisplayName("分页获取新闻-带筛选")
    void testGetNewsWithPagination_WithFilters() throws Exception {
        mockMvc.perform(get("/api/news")
                .param("page", "1")
                .param("size", "10")
                .param("category", "社团动态")
                .param("keyword", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 带筛选分页获取新闻测试通过");
    }

    @Test
    @Order(9)
    @DisplayName("获取新闻统计-成功")
    void testGetNewsStatistics_Success() throws Exception {
        mockMvc.perform(get("/api/news/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取新闻统计测试通过");
    }

    // ==================== 新增测试 ====================

    @Test
    @Order(20)
    @DisplayName("新增新闻-成功")
    void testAddNews_Success() throws Exception {
        NewsSaveDTO dto = createValidNewsSaveDTO();

        MvcResult result = mockMvc.perform(post("/api/news/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        System.out.println("[SUCCESS] 新增新闻测试通过: " + result.getResponse().getContentAsString());
    }

    @Test
    @Order(21)
    @DisplayName("新增新闻-标题为空")
    void testAddNews_EmptyTitle() throws Exception {
        NewsSaveDTO dto = createValidNewsSaveDTO();
        dto.setTitle("");

        mockMvc.perform(post("/api/news/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 标题为空验证通过");
    }

    @Test
    @Order(22)
    @DisplayName("新增新闻-内容为空")
    void testAddNews_EmptyContent() throws Exception {
        NewsSaveDTO dto = createValidNewsSaveDTO();
        dto.setContent("");

        mockMvc.perform(post("/api/news/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 内容为空验证通过");
    }

    @Test
    @Order(23)
    @DisplayName("新增新闻-分类为空")
    void testAddNews_EmptyCategory() throws Exception {
        NewsSaveDTO dto = createValidNewsSaveDTO();
        dto.setCategory("");

        mockMvc.perform(post("/api/news/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 分类为空验证通过");
    }

    // ==================== 更新测试 ====================

    @Test
    @Order(30)
    @DisplayName("更新新闻-成功")
    void testUpdateNews_Success() throws Exception {
        // 先创建一条新闻
        NewsSaveDTO saveDTO = createValidNewsSaveDTO();
        MvcResult createResult = mockMvc.perform(post("/api/news/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saveDTO)))
                .andReturn();

        // 更新新闻
        NewsUpdateDTO updateDTO = createValidNewsUpdateDTO();
        updateDTO.setId(1L); // 假设ID为1

        mockMvc.perform(put("/api/news/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 更新新闻测试通过");
    }

    @Test
    @Order(31)
    @DisplayName("更新新闻-ID为空")
    void testUpdateNews_EmptyId() throws Exception {
        NewsUpdateDTO dto = new NewsUpdateDTO();
        dto.setTitle("测试标题");
        dto.setContent("测试内容");

        mockMvc.perform(put("/api/news/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] ID为空验证通过");
    }

    // ==================== 删除测试 ====================

    @Test
    @Order(40)
    @DisplayName("删除新闻-成功")
    void testDeleteNews_Success() throws Exception {
        mockMvc.perform(delete("/api/news/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 删除新闻测试通过");
    }

    @Test
    @Order(41)
    @DisplayName("删除新闻-不存在的ID")
    void testDeleteNews_NotFound() throws Exception {
        mockMvc.perform(delete("/api/news/delete/999999"))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 删除不存在新闻验证通过");
    }

    @Test
    @Order(42)
    @DisplayName("批量删除新闻-成功")
    void testDeleteNewsByIds_Success() throws Exception {
        mockMvc.perform(delete("/api/news/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList(1L, 2L, 3L))))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 批量删除新闻测试通过");
    }

    @Test
    @Order(43)
    @DisplayName("批量删除新闻-空列表")
    void testDeleteNewsByIds_EmptyList() throws Exception {
        mockMvc.perform(delete("/api/news/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList())))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 空列表批量删除测试通过");
    }

    // ==================== 详情测试 ====================

    @Test
    @Order(50)
    @DisplayName("获取新闻详情-成功")
    void testGetNewsDetail_Success() throws Exception {
        mockMvc.perform(get("/api/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取新闻详情测试通过");
    }

    @Test
    @Order(51)
    @DisplayName("获取新闻详情-不存在的ID")
    void testGetNewsDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/news/999999"))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 获取不存在新闻详情验证通过");
    }

    // ==================== 边界测试 ====================

    @Test
    @Order(60)
    @DisplayName("分页-超大页码")
    void testPagination_LargePageNumber() throws Exception {
        mockMvc.perform(get("/api/news")
                .param("page", "999999")
                .param("size", "10"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 超大页码测试通过");
    }

    @Test
    @Order(61)
    @DisplayName("分页-超大页面大小")
    void testPagination_LargePageSize() throws Exception {
        mockMvc.perform(get("/api/news")
                .param("page", "1")
                .param("size", "1000"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 超大页面大小测试通过");
    }

    @Test
    @Order(62)
    @DisplayName("搜索-特殊字符")
    void testSearch_SpecialCharacters() throws Exception {
        mockMvc.perform(get("/api/news/search")
                .param("keyword", "<script>alert('xss')</script>"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 特殊字符搜索测试通过");
    }
}
