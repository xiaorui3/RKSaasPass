package com.tianji.message.controller;

import com.tianji.common.domain.R;
import com.tianji.message.service.IEmailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * 邮件发送控制器
 * 基于master分支的RkController邮件功能
 */
@Slf4j
@Api(tags = "邮件发送接口")
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final IEmailService emailService;

    /**
     * 发送文本邮件
     */
    @ApiOperation("发送文本邮件")
    @PostMapping("/send-text")
    public R<String> sendTextEmail(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            String subject = request.get("subject");
            String content = request.get("content");

            if (to == null || to.trim().isEmpty()) {
                return R.error("收件人邮箱不能为空");
            }
            if (subject == null || subject.trim().isEmpty()) {
                return R.error("邮件主题不能为空");
            }
            if (content == null || content.trim().isEmpty()) {
                content = "无内容";
            }

            boolean success = emailService.sendTextEmail(to, subject, content);
            if (success) {
                log.info("✅ 文本邮件发送成功 - 收件人: {}, 主题: {}", to, subject);
                return R.ok("邮件发送成功");
            } else {
                return R.error("邮件发送失败");
            }
        } catch (Exception e) {
            log.error("文本邮件发送失败", e);
            return R.error("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送HTML邮件
     */
    @ApiOperation("发送HTML邮件")
    @PostMapping("/send-html")
    public R<String> sendHtmlEmail(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            String subject = request.get("subject");
            String htmlContent = request.get("htmlContent");

            if (to == null || to.trim().isEmpty()) {
                return R.error("收件人邮箱不能为空");
            }
            if (subject == null || subject.trim().isEmpty()) {
                return R.error("邮件主题不能为空");
            }
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                return R.error("邮件内容不能为空");
            }

            boolean success = emailService.sendHtmlEmail(to, subject, htmlContent);
            if (success) {
                log.info("✅ HTML邮件发送成功 - 收件人: {}, 主题: {}", to, subject);
                return R.ok("邮件发送成功");
            } else {
                return R.error("邮件发送失败");
            }
        } catch (Exception e) {
            log.error("HTML邮件发送失败", e);
            return R.error("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送申请通知邮件
     */
    @ApiOperation("发送申请通知邮件")
    @PostMapping("/send-application-notice")
    public R<String> sendApplicationNotice(@RequestBody Map<String, Object> request) {
        try {
            String adminEmail = (String) request.get("adminEmail");
            @SuppressWarnings("unchecked")
            Map<String, Object> applicantInfo = (Map<String, Object>) request.get("applicantInfo");

            if (adminEmail == null || adminEmail.trim().isEmpty()) {
                return R.error("管理员邮箱不能为空");
            }
            if (applicantInfo == null || applicantInfo.isEmpty()) {
                return R.error("申请信息不能为空");
            }

            boolean success = emailService.sendApplicationNotice(adminEmail, applicantInfo);
            if (success) {
                log.info("✅ 申请通知邮件发送成功 - 管理员: {}", adminEmail);
                return R.ok("申请通知邮件发送成功");
            } else {
                return R.error("申请通知邮件发送失败");
            }
        } catch (Exception e) {
            log.error("申请通知邮件发送失败", e);
            return R.error("申请通知邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送审核结果邮件
     */
    @ApiOperation("发送审核结果邮件")
    @PostMapping("/send-review-result")
    public R<String> sendReviewResultEmail(@RequestBody Map<String, String> request) {
        try {
            String applicantEmail = request.get("applicantEmail");
            String result = request.get("result");
            String reason = request.get("reason");

            if (applicantEmail == null || applicantEmail.trim().isEmpty()) {
                return R.error("申请者邮箱不能为空");
            }
            if (result == null || result.trim().isEmpty()) {
                return R.error("审核结果不能为空");
            }

            boolean success = emailService.sendReviewResultEmail(applicantEmail, result, reason);
            if (success) {
                log.info("✅ 审核结果邮件发送成功 - 申请者: {}, 结果: {}", applicantEmail, result);
                return R.ok("审核结果邮件发送成功");
            } else {
                return R.error("审核结果邮件发送失败");
            }
        } catch (Exception e) {
            log.error("审核结果邮件发送失败", e);
            return R.error("审核结果邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 批量发送邮件
     */
    @ApiOperation("批量发送邮件")
    @PostMapping("/send-bulk")
    public R<Map<String, Object>> sendBulkEmails(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> recipients = (List<String>) request.get("recipients");
            String subject = (String) request.get("subject");
            String content = (String) request.get("content");

            if (recipients == null || recipients.isEmpty()) {
                return R.error("收件人列表不能为空");
            }
            if (subject == null || subject.trim().isEmpty()) {
                return R.error("邮件主题不能为空");
            }
            if (content == null || content.trim().isEmpty()) {
                content = "无内容";
            }

            Map<String, Object> result = emailService.sendBulkEmails(recipients, subject, content);
            log.info("✅ 批量邮件发送完成 - 总数: {}, 成功: {}, 失败: {}",
                    result.get("total"), result.get("successCount"), result.get("failedCount"));

            return R.ok(result);
        } catch (Exception e) {
            log.error("批量邮件发送失败", e);
            return R.error("批量邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送带附件的邮件
     */
    @ApiOperation("发送带附件的邮件")
    @PostMapping("/send-with-attachment")
    public R<String> sendEmailWithAttachment(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            String subject = request.get("subject");
            String content = request.get("content");
            String attachmentPath = request.get("attachmentPath");
            String attachmentName = request.get("attachmentName");

            if (to == null || to.trim().isEmpty()) {
                return R.error("收件人邮箱不能为空");
            }
            if (subject == null || subject.trim().isEmpty()) {
                return R.error("邮件主题不能为空");
            }

            boolean success = emailService.sendEmailWithAttachment(to, subject, content, attachmentPath, attachmentName);
            if (success) {
                log.info("✅ 带附件邮件发送成功 - 收件人: {}, 主题: {}, 附件: {}", to, subject, attachmentName);
                return R.ok("带附件邮件发送成功");
            } else {
                return R.error("带附件邮件发送失败");
            }
        } catch (Exception e) {
            log.error("带附件邮件发送失败", e);
            return R.error("带附件邮件发送失败: " + e.getMessage());
        }
    }
}