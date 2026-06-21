package com.tianji.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.api.client.user.UserClient;
import com.tianji.auth.domain.dto.LoginDTO;
import com.tianji.auth.domain.dto.RegisterDTO;
import com.tianji.auth.service.IAuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证接口测试类
 * 覆盖登录、注册、登出、刷新Token等核心功能
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IAuthService authService;

    @MockBean
    private UserClient userClient;

    private static String accessToken;
    private static String refreshToken;

    // ==================== 测试数据 ====================

    private LoginDTO createValidLoginDTO() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("Test@123456");
        dto.setOrganizationId("1");
        return dto;
    }

    private RegisterDTO createValidRegisterDTO() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newuser_" + System.currentTimeMillis());
        dto.setPassword("Test@123456");
        dto.setPhone("13800138000");
        dto.setEmail("test@example.com");
        dto.setOrganizationId("1");
        dto.setEmailCode("123456");
        return dto;
    }

    @BeforeEach
    void stubUserClient() {
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);
        when(userClient.syncUserOnRegister(any())).thenReturn(1L);
    }

    // ==================== 登录测试 ====================

    @Test
    @Order(1)
    @DisplayName("登录-成功用例")
    void testLogin_Success() throws Exception {
        LoginDTO dto = createValidLoginDTO();

        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
        System.out.println("[SUCCESS] 登录接口响应: " + response);
    }

    @Test
    @Order(2)
    @DisplayName("登录-用户名为空")
    void testLogin_EmptyUsername() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("");
        dto.setPassword("Test@123456");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 用户名为空验证通过");
    }

    @Test
    @Order(3)
    @DisplayName("登录-密码为空")
    void testLogin_EmptyPassword() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 密码为空验证通过");
    }

    @Test
    @Order(4)
    @DisplayName("登录-错误密码")
    void testLogin_WrongPassword() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 错误密码验证通过");
    }

    @Test
    @Order(5)
    @DisplayName("登录-不存在的用户")
    void testLogin_UserNotFound() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("nonexistentuser_" + System.currentTimeMillis());
        dto.setPassword("Test@123456");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 不存在用户验证通过");
    }

    // ==================== 注册测试 ====================

    @Test
    @Order(10)
    @DisplayName("注册-成功用例")
    void testRegister_Success() throws Exception {
        RegisterDTO dto = createValidRegisterDTO();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 注册接口测试通过");
    }

    @Test
    @Order(11)
    @DisplayName("注册-用户名为空")
    void testRegister_EmptyUsername() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("");
        dto.setPassword("Test@123456");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 注册用户名为空验证通过");
    }

    @Test
    @Order(12)
    @DisplayName("注册-密码为空")
    void testRegister_EmptyPassword() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 注册密码为空验证通过");
    }

    @Test
    @Order(13)
    @DisplayName("注册-重复用户名")
    void testRegister_DuplicateUsername() throws Exception {
        // 第一次注册
        RegisterDTO dto = createValidRegisterDTO();
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        // 第二次使用相同用户名注册
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 重复用户名验证通过");
    }

    // ==================== 登出测试 ====================

    @Test
    @Order(20)
    @DisplayName("登出-成功用例")
    void testLogout_Success() throws Exception {
        // 先登录获取token
        LoginDTO loginDTO = createValidLoginDTO();
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andReturn();
        
        // 使用token登出
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 登出接口测试通过");
    }

    @Test
    @Order(21)
    @DisplayName("登出-无Token")
    void testLogout_NoToken() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isBadRequest());
        
        System.out.println("[SUCCESS] 无Token登出验证通过");
    }

    // ==================== 刷新Token测试 ====================

    @Test
    @Order(30)
    @DisplayName("刷新Token-成功用例")
    void testRefreshToken_Success() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"valid-refresh-token\""))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 刷新Token接口测试通过");
    }

    @Test
    @Order(31)
    @DisplayName("刷新Token-无效Token")
    void testRefreshToken_InvalidToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"invalid-refresh-token\""))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 无效Token刷新验证通过");
    }

    // ==================== 多租户测试 ====================

    @Test
    @Order(40)
    @DisplayName("多租户登录-指定租户")
    void testLogin_WithTenant() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("Test@123456");
        dto.setOrganizationId("2"); // 指定租户ID

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 多租户登录测试通过");
    }

    @Test
    @Order(41)
    @DisplayName("多租户登录-默认租户")
    void testLogin_DefaultTenant() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("Test@123456");
        // 不指定租户ID，使用默认租户

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 默认租户登录测试通过");
    }
}
