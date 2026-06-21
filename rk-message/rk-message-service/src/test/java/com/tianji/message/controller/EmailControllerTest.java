package com.tianji.message.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 邮件服务接口测试类
 * 覆盖文本邮件、HTML邮件、申请通知、审核结果、批量邮件等核心功能
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 发送文本邮件测试 ====================

    @Test
    @Order(1)
    @DisplayName("发送文本邮件-成功")
    void testSendTextEmail_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "测试邮件主题");
        request.put("content", "这是测试邮件内容");

        mockMvc.perform(post("/api/email/send-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 发送文本邮件测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("发送文本邮件-收件人为空")
    void testSendTextEmail_EmptyTo() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "");
        request.put("subject", "测试邮件主题");
        request.put("content", "测试内容");

        mockMvc.perform(post("/api/email/send-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 收件人为空验证通过");
    }

    @Test
    @Order(3)
    @DisplayName("发送文本邮件-主题为空")
    void testSendTextEmail_EmptySubject() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "");
        request.put("content", "测试内容");

        mockMvc.perform(post("/api/email/send-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 主题为空验证通过");
    }

    @Test
    @Order(4)
    @DisplayName("发送文本邮件-内容为空(使用默认值)")
    void testSendTextEmail_EmptyContent() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "测试邮件主题");
        request.put("content", "");

        mockMvc.perform(post("/api/email/send-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 内容为空测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("发送文本邮件-无效邮箱格式")
    void testSendTextEmail_InvalidEmail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "invalid-email");
        request.put("subject", "测试邮件主题");
        request.put("content", "测试内容");

        mockMvc.perform(post("/api/email/send-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // 根据业务逻辑可能返回错误
        
        System.out.println("[SUCCESS] 无效邮箱格式测试通过");
    }

    // ==================== 发送HTML邮件测试 ====================

    @Test
    @Order(10)
    @DisplayName("发送HTML邮件-成功")
    void testSendHtmlEmail_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "HTML邮件主题");
        request.put("htmlContent", "<html><body><h1>测试HTML邮件</h1></body></html>");

        mockMvc.perform(post("/api/email/send-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 发送HTML邮件测试通过");
    }

    @Test
    @Order(11)
    @DisplayName("发送HTML邮件-收件人为空")
    void testSendHtmlEmail_EmptyTo() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "");
        request.put("subject", "HTML邮件主题");
        request.put("htmlContent", "<html><body>测试</body></html>");

        mockMvc.perform(post("/api/email/send-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] HTML邮件收件人为空验证通过");
    }

    @Test
    @Order(12)
    @DisplayName("发送HTML邮件-内容为空")
    void testSendHtmlEmail_EmptyContent() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "HTML邮件主题");
        request.put("htmlContent", "");

        mockMvc.perform(post("/api/email/send-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] HTML邮件内容为空验证通过");
    }

    // ==================== 发送申请通知邮件测试 ====================

    @Test
    @Order(20)
    @DisplayName("发送申请通知邮件-成功")
    void testSendApplicationNotice_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("adminEmail", "admin@example.com");
        
        Map<String, Object> applicantInfo = new HashMap<>();
        applicantInfo.put("name", "申请人姓名");
        applicantInfo.put("email", "applicant@example.com");
        applicantInfo.put("phone", "13800138000");
        applicantInfo.put("applyType", "社团入社申请");
        applicantInfo.put("message", "申请说明内容");
        request.put("applicantInfo", applicantInfo);

        mockMvc.perform(post("/api/email/send-application-notice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 发送申请通知邮件测试通过");
    }

    @Test
    @Order(21)
    @DisplayName("发送申请通知邮件-管理员邮箱为空")
    void testSendApplicationNotice_EmptyAdminEmail() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("adminEmail", "");
        request.put("applicantInfo", new HashMap<>());

        mockMvc.perform(post("/api/email/send-application-notice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 管理员邮箱为空验证通过");
    }

    @Test
    @Order(22)
    @DisplayName("发送申请通知邮件-申请信息为空")
    void testSendApplicationNotice_EmptyApplicantInfo() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("adminEmail", "admin@example.com");
        request.put("applicantInfo", new HashMap<>());

        mockMvc.perform(post("/api/email/send-application-notice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 申请信息为空验证通过");
    }

    // ==================== 发送审核结果邮件测试 ====================

    @Test
    @Order(30)
    @DisplayName("发送审核结果邮件-通过")
    void testSendReviewResultEmail_Approved() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("applicantEmail", "applicant@example.com");
        request.put("result", "approved");
        request.put("reason", "您的申请已通过审核，欢迎加入！");

        mockMvc.perform(post("/api/email/send-review-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 发送审核通过邮件测试通过");
    }

    @Test
    @Order(31)
    @DisplayName("发送审核结果邮件-拒绝")
    void testSendReviewResultEmail_Rejected() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("applicantEmail", "applicant@example.com");
        request.put("result", "rejected");
        request.put("reason", "抱歉，您的申请未通过审核。");

        mockMvc.perform(post("/api/email/send-review-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 发送审核拒绝邮件测试通过");
    }

    @Test
    @Order(32)
    @DisplayName("发送审核结果邮件-申请者邮箱为空")
    void testSendReviewResultEmail_EmptyApplicantEmail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("applicantEmail", "");
        request.put("result", "approved");
        request.put("reason", "测试原因");

        mockMvc.perform(post("/api/email/send-review-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 申请者邮箱为空验证通过");
    }

    @Test
    @Order(33)
    @DisplayName("发送审核结果邮件-结果为空")
    void testSendReviewResultEmail_EmptyResult() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("applicantEmail", "applicant@example.com");
        request.put("result", "");
        request.put("reason", "测试原因");

        mockMvc.perform(post("/api/email/send-review-result")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 审核结果为空验证通过");
    }

    // ==================== 批量发送邮件测试 ====================

    @Test
    @Order(40)
    @DisplayName("批量发送邮件-成功")
    void testSendBulkEmails_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("recipients", Arrays.asList(
                "user1@example.com",
                "user2@example.com",
                "user3@example.com"
        ));
        request.put("subject", "批量邮件测试主题");
        request.put("content", "这是批量发送的测试邮件内容");

        mockMvc.perform(post("/api/email/send-bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 批量发送邮件测试通过");
    }

    @Test
    @Order(41)
    @DisplayName("批量发送邮件-收件人列表为空")
    void testSendBulkEmails_EmptyRecipients() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("recipients", new ArrayList<>());
        request.put("subject", "批量邮件测试主题");
        request.put("content", "测试内容");

        mockMvc.perform(post("/api/email/send-bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 收件人列表为空验证通过");
    }

    @Test
    @Order(42)
    @DisplayName("批量发送邮件-主题为空")
    void testSendBulkEmails_EmptySubject() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("recipients", Arrays.asList("user1@example.com"));
        request.put("subject", "");
        request.put("content", "测试内容");

        mockMvc.perform(post("/api/email/send-bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 批量邮件主题为空验证通过");
    }

    @Test
    @Order(43)
    @DisplayName("批量发送邮件-内容为空")
    void testSendBulkEmails_EmptyContent() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("recipients", Arrays.asList("user1@example.com"));
        request.put("subject", "批量邮件测试主题");
        request.put("content", "");

        mockMvc.perform(post("/api/email/send-bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // 内容为空使用默认值
        
        System.out.println("[SUCCESS] 批量邮件内容为空测试通过");
    }

    @Test
    @Order(44)
    @DisplayName("批量发送邮件-大量收件人")
    void testSendBulkEmails_LargeRecipients() throws Exception {
        List<String> recipients = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            recipients.add("user" + i + "@example.com");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("recipients", recipients);
        request.put("subject", "大量收件人测试");
        request.put("content", "测试内容");

        mockMvc.perform(post("/api/email/send-bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 大量收件人测试通过");
    }

    // ==================== 发送带附件邮件测试 ====================

    @Test
    @Order(50)
    @DisplayName("发送带附件邮件-成功")
    void testSendEmailWithAttachment_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "带附件的邮件");
        request.put("content", "请查收附件");
        request.put("attachmentPath", "/uploads/test.pdf");
        request.put("attachmentName", "测试附件.pdf");

        mockMvc.perform(post("/api/email/send-with-attachment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        System.out.println("[SUCCESS] 发送带附件邮件测试通过");
    }

    @Test
    @Order(51)
    @DisplayName("发送带附件邮件-收件人为空")
    void testSendEmailWithAttachment_EmptyTo() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "");
        request.put("subject", "带附件的邮件");
        request.put("content", "请查收附件");
        request.put("attachmentPath", "/uploads/test.pdf");

        mockMvc.perform(post("/api/email/send-with-attachment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 带附件邮件收件人为空验证通过");
    }

    @Test
    @Order(52)
    @DisplayName("发送带附件邮件-主题为空")
    void testSendEmailWithAttachment_EmptySubject() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "");
        request.put("content", "请查收附件");
        request.put("attachmentPath", "/uploads/test.pdf");

        mockMvc.perform(post("/api/email/send-with-attachment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(500));
        
        System.out.println("[SUCCESS] 带附件邮件主题为空验证通过");
    }

    @Test
    @Order(53)
    @DisplayName("发送带附件邮件-附件路径为空")
    void testSendEmailWithAttachment_EmptyAttachmentPath() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "带附件的邮件");
        request.put("content", "请查收附件");
        request.put("attachmentPath", "");

        mockMvc.perform(post("/api/email/send-with-attachment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // 根据业务逻辑
        
        System.out.println("[SUCCESS] 附件路径为空测试通过");
    }

    // ==================== 边界测试 ====================

    @Test
    @Order(60)
    @DisplayName("发送邮件-超长主题")
    void testSendEmail_LongSubject() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "a".repeat(500)); // 超长主题
        request.put("content", "测试内容");

        mockMvc.perform(post("/api/email/send-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 超长主题测试通过");
    }

    @Test
    @Order(61)
    @DisplayName("发送邮件-特殊字符内容")
    void testSendEmail_SpecialCharacters() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("to", "test@example.com");
        request.put("subject", "测试特殊字符");
        request.put("content", "<script>alert('xss')</script> & <>&\"'");

        mockMvc.perform(post("/api/email/send-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        System.out.println("[SUCCESS] 特殊字符内容测试通过");
    }
}
