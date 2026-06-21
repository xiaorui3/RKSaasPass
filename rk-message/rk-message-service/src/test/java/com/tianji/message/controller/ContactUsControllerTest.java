package com.tianji.message.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.message.domain.dto.ContactPublicShieldDTO;
import com.tianji.message.domain.po.ContactUsMessage;
import com.tianji.message.service.IContactUsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContactUsControllerTest {

    @Mock
    private IContactUsService contactUsService;

    @InjectMocks
    private ContactUsController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("json contact submit should accept blank email")
    void testSubmitContactUs_EmptyEmail() throws Exception {
        ContactUsMessage message = new ContactUsMessage();
        message.setName("tester");
        message.setEmail("");
        message.setPhone("13800138000");
        message.setSubject("合作咨询");
        message.setMessage("这里是一条足够长的测试留言内容。");
        message.setStatus(ContactUsMessage.STATUS_PENDING);

        when(contactUsService.submitMessage(any(ContactUsMessage.class))).thenReturn(true);

        mockMvc.perform(post("/api/contact/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("form contact submit should allow missing email")
    void testSubmitContactUsForm_EmailOptional() throws Exception {
        when(contactUsService.submitMessage(any(ContactUsMessage.class))).thenReturn(true);

        mockMvc.perform(post("/api/contact/submit-form")
                        .param("name", "tester")
                        .param("subject", "活动咨询")
                        .param("message", "这里是一条足够长的表单留言内容。"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("public contact shield endpoint should issue one-time submit metadata")
    void testIssuePublicShield() throws Exception {
        ContactPublicShieldDTO shield = new ContactPublicShieldDTO();
        shield.setToken("shield-token");
        shield.setIssuedAt(1715000000000L);
        shield.setMinSubmitDelayMs(1200L);
        shield.setExpiresInSeconds(900L);

        when(contactUsService.issuePublicShield(any(), any(), any())).thenReturn(shield);

        mockMvc.perform(get("/api/contact/public/shield"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.issuedAt").value(1715000000000L))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.minSubmitDelayMs").isNumber())
                .andExpect(jsonPath("$.data.expiresInSeconds").isNumber());
    }

    @Test
    @DisplayName("public contact submit should accept shield token payload")
    void testSubmitPublicContactMessage() throws Exception {
        String payload = "{"
                + "\"name\":\"visitor\","
                + "\"email\":\"visitor@example.com\","
                + "\"phone\":\"13800138000\","
                + "\"subject\":\"合作咨询\","
                + "\"message\":\"这里是一条足够长的公开联系留言，用来验证前台防刷提交链路。\","
                + "\"shieldToken\":\"shield-token\","
                + "\"issuedAt\":1715000000000,"
                + "\"honeypot\":\"\""
                + "}";

        when(contactUsService.submitPublicMessage(any(ContactUsMessage.class), any(), any(), any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/contact/public/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
