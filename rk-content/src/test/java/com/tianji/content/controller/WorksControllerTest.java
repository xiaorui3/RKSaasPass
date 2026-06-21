package com.tianji.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.content.domain.dto.WorksQueryDTO;
import com.tianji.content.domain.dto.WorksSaveDTO;
import com.tianji.content.domain.dto.WorksUpdateDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 作品模块接口测试类
 * 覆盖CRUD、点赞、统计等核心功能
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 测试数据构建 ====================

    private WorksSaveDTO createValidWorksSaveDTO() {
        WorksSaveDTO dto = new WorksSaveDTO();
        dto.setTitle("测试作品_" + System.currentTimeMillis());
        dto.setDescription("这是一个测试作品的描述");
        dto.setContent("作品详细内容");
        dto.setCategory("软件项目");
        dto.setCoverImage("/uploads/works/cover.jpg");
        dto.setDemoVideo("https://example.com/video.mp4");
        dto.setProjectLinks("https://github.com/example/project");
        dto.setTechnologies("Java, Spring Boot, Vue");
        dto.setAuthors("作者1, 作者2");
        dto.setIsFeatured(false);
        dto.setDisplayOrder(0);
        dto.setManagerReviewerId(7001L);
        dto.setTeacherReviewerId(8001L);
        return dto;
    }

    private WorksUpdateDTO createValidWorksUpdateDTO() {
        WorksUpdateDTO dto = new WorksUpdateDTO();
        dto.setId(1L);
        dto.setTitle("更新后的作品标题_" + System.currentTimeMillis());
        dto.setDescription("更新后的作品描述");
        dto.setCategory("硬件项目");
        return dto;
    }

    // ==================== 查询接口测试 ====================

    @Test
    @Order(1)
    @DisplayName("分页查询作品列表-成功")
    void testGetWorksPage_Success() throws Exception {
        mockMvc.perform(get("/api/works/page")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 分页查询作品列表测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("获取所有作品-成功")
    void testGetAllWorks_Success() throws Exception {
        mockMvc.perform(get("/api/works/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取所有作品测试通过");
    }

    @Test
    @Order(3)
    @DisplayName("按分类获取作品-成功")
    void testGetWorksByCategory_Success() throws Exception {
        mockMvc.perform(get("/api/works/category/软件项目"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 按分类获取作品测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("获取精选作品-成功")
    void testGetFeaturedWorks_Success() throws Exception {
        mockMvc.perform(get("/api/works/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取精选作品测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("搜索作品-成功")
    void testSearchWorks_Success() throws Exception {
        mockMvc.perform(get("/api/works/search")
                .param("title", "测试")
                .param("category", "软件项目"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 搜索作品测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("获取热门作品-成功")
    void testGetPopularWorks_Success() throws Exception {
        mockMvc.perform(get("/api/works/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取热门作品测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("获取最新作品-成功")
    void testGetLatestWorks_Success() throws Exception {
        mockMvc.perform(get("/api/works/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取最新作品测试通过");
    }

    @Test
    @Order(8)
    @DisplayName("获取作品统计-成功")
    void testGetWorkStatistics_Success() throws Exception {
        mockMvc.perform(get("/api/works/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取作品统计测试通过");
    }

    // ==================== 详情接口测试 ====================

    @Test
    @Order(10)
    @DisplayName("获取作品详情-成功")
    void testGetWorkDetail_Success() throws Exception {
        mockMvc.perform(get("/api/works/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 获取作品详情测试通过");
    }

    @Test
    @Order(11)
    @DisplayName("获取作品详情-不存在的ID")
    void testGetWorkDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/works/999999"))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 不存在的作品详情验证通过");
    }

    // ==================== 新增作品测试 ====================

    @Test
    @Order(20)
    @DisplayName("新增作品-成功")
    void testAddWork_Success() throws Exception {
        WorksSaveDTO dto = createValidWorksSaveDTO();

        MvcResult result = mockMvc.perform(post("/api/works")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        System.out.println("[SUCCESS] 新增作品测试通过: " + result.getResponse().getContentAsString());
    }

    @Test
    @Order(21)
    @DisplayName("新增作品-标题为空")
    void testAddWork_EmptyTitle() throws Exception {
        WorksSaveDTO dto = createValidWorksSaveDTO();
        dto.setTitle("");

        mockMvc.perform(post("/api/works")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 标题为空验证通过");
    }

    @Test
    @Order(22)
    @DisplayName("新增作品-描述为空")
    void testAddWork_EmptyDescription() throws Exception {
        WorksSaveDTO dto = createValidWorksSaveDTO();
        dto.setDescription("");

        mockMvc.perform(post("/api/works")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 描述为空验证通过");
    }

    // ==================== 更新作品测试 ====================

    @Test
    @Order(30)
    @DisplayName("更新作品-成功")
    void testUpdateWork_Success() throws Exception {
        WorksUpdateDTO dto = createValidWorksUpdateDTO();

        mockMvc.perform(put("/api/works")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 更新作品测试通过");
    }

    @Test
    @Order(31)
    @DisplayName("更新作品-ID为空")
    void testUpdateWork_EmptyId() throws Exception {
        WorksUpdateDTO dto = createValidWorksUpdateDTO();
        dto.setId(null);

        mockMvc.perform(put("/api/works")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] ID为空验证通过");
    }

    @Test
    @Order(32)
    @DisplayName("更新作品-不存在的ID")
    void testUpdateWork_NotFound() throws Exception {
        WorksUpdateDTO dto = createValidWorksUpdateDTO();
        dto.setId(999999L);

        mockMvc.perform(put("/api/works")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 不存在的作品更新验证通过");
    }

    // ==================== 删除作品测试 ====================

    @Test
    @Order(40)
    @DisplayName("删除作品-成功")
    void testDeleteWork_Success() throws Exception {
        mockMvc.perform(delete("/api/works/999999"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 删除作品测试通过");
    }

    @Test
    @Order(41)
    @DisplayName("批量删除作品-成功")
    void testDeleteWorksBatch_Success() throws Exception {
        mockMvc.perform(delete("/api/works/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList(1L, 2L, 3L))))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 批量删除作品测试通过");
    }

    @Test
    @Order(42)
    @DisplayName("批量删除作品-空列表")
    void testDeleteWorksBatch_EmptyList() throws Exception {
        mockMvc.perform(delete("/api/works/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList())))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 空列表批量删除验证通过");
    }

    // ==================== 点赞测试 ====================

    @Test
    @Order(50)
    @DisplayName("点赞作品-成功")
    void testLikeWork_Success() throws Exception {
        mockMvc.perform(post("/api/works/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 点赞作品测试通过");
    }

    @Test
    @Order(51)
    @DisplayName("点赞作品-不存在的ID")
    void testLikeWork_NotFound() throws Exception {
        mockMvc.perform(post("/api/works/999999/like"))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 不存在的作品点赞验证通过");
    }

    @Test
    @Order(52)
    @DisplayName("取消点赞-成功")
    void testUnlikeWork_Success() throws Exception {
        mockMvc.perform(post("/api/works/1/unlike"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 取消点赞测试通过");
    }

    @Test
    @Order(53)
    @DisplayName("取消点赞-不存在的ID")
    void testUnlikeWork_NotFound() throws Exception {
        mockMvc.perform(post("/api/works/999999/unlike"))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 不存在的作品取消点赞验证通过");
    }

    // ==================== 边界测试 ====================

    @Test
    @Order(60)
    @DisplayName("分页查询-超大页码")
    void testGetWorksPage_LargePageNumber() throws Exception {
        mockMvc.perform(get("/api/works/page")
                .param("page", "999999")
                .param("size", "10"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 超大页码测试通过");
    }

    @Test
    @Order(61)
    @DisplayName("分页查询-超大页面大小")
    void testGetWorksPage_LargePageSize() throws Exception {
        mockMvc.perform(get("/api/works/page")
                .param("page", "1")
                .param("size", "1000"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 超大页面大小测试通过");
    }

    @Test
    @Order(62)
    @DisplayName("搜索作品-特殊字符")
    void testSearchWorks_SpecialCharacters() throws Exception {
        mockMvc.perform(get("/api/works/search")
                .param("title", "<script>alert('xss')</script>"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 特殊字符搜索测试通过");
    }

    @Test
    @Order(63)
    @DisplayName("按分类获取作品-空分类")
    void testGetWorksByCategory_EmptyCategory() throws Exception {
        mockMvc.perform(get("/api/works/category/"))
                .andExpect(status().isNotFound()); // 路径不匹配
        
        System.out.println("[SUCCESS] 空分类测试通过");
    }
}
