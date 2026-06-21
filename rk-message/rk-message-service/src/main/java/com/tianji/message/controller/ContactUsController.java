package com.tianji.message.controller;

import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.message.domain.dto.ContactPublicShieldDTO;
import com.tianji.message.domain.dto.ContactPublicSubmitDTO;
import com.tianji.message.domain.po.ContactUsMessage;
import com.tianji.message.service.IContactUsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "联系我们接口")
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactUsController {

    private final IContactUsService contactUsService;

    @ApiOperation("获取公开留言防刷令牌")
    @GetMapping("/public/shield")
    public R<ContactPublicShieldDTO> issuePublicShield(HttpServletRequest request) {
        ContactUsMessage message = new ContactUsMessage();
        populateRequestContext(message, request);
        ContactPublicShieldDTO shield = contactUsService.issuePublicShield(
                message.getTenantId(),
                message.getIpAddress(),
                message.getUserAgent()
        );
        return R.ok(shield);
    }

    @ApiOperation("提交公开留言")
    @PostMapping("/public/submit")
    public R<String> submitPublicContact(@RequestBody ContactPublicSubmitDTO form, HttpServletRequest request) {
        try {
            ContactUsMessage message = new ContactUsMessage();
            message.setName(form.getName());
            message.setEmail(form.getEmail());
            message.setTargetEmail(form.getTargetEmail());
            message.setPhone(form.getPhone());
            message.setSubject(form.getSubject());
            message.setMessage(form.getMessage());
            message.setStatus(ContactUsMessage.STATUS_PENDING);
            populateRequestContext(message, request);

            boolean success = contactUsService.submitPublicMessage(
                    message,
                    form.getShieldToken(),
                    form.getIssuedAt(),
                    form.getHoneypot(),
                    form.getVerificationCode()
            );
            if (success) {
                log.info("public contact message submitted, subject={}", message.getSubject());
                return R.ok("消息提交成功，我们会尽快回复您。");
            }
            return R.error("提交已失效、过快或过于频繁，请刷新页面后重试");
        } catch (Exception e) {
            log.error("submit public contact message failed", e);
            return R.error("消息提交失败：" + e.getMessage());
        }
    }

    @ApiOperation("提交联系我们消息")
    @PostMapping("/submit")
    public R<String> submitContactUs(@RequestBody ContactUsMessage message, HttpServletRequest request) {
        try {
            populateRequestContext(message, request);
            boolean success = contactUsService.submitMessage(message);
            if (success) {
                log.info("contact message submitted, subject={}", message.getSubject());
                return R.ok("消息提交成功，我们会尽快回复您。");
            }
            return R.error("提交过于频繁或系统未能处理本次留言，请稍后再试");
        } catch (Exception e) {
            log.error("submit contact message failed", e);
            return R.error("消息提交失败：" + e.getMessage());
        }
    }

    @ApiOperation("提交联系我们消息（表单）")
    @PostMapping("/submit-form")
    public R<String> submitContactUsForm(@RequestParam String name,
                                         @RequestParam(required = false) String email,
                                         @RequestParam(required = false) String phone,
                                         @RequestParam String subject,
                                         @RequestParam String message,
                                         HttpServletRequest request) {
        try {
            ContactUsMessage contactMessage = new ContactUsMessage();
            contactMessage.setName(name);
            contactMessage.setEmail(email);
            contactMessage.setPhone(phone);
            contactMessage.setSubject(subject);
            contactMessage.setMessage(message);
            contactMessage.setStatus(ContactUsMessage.STATUS_PENDING);
            populateRequestContext(contactMessage, request);

            boolean success = contactUsService.submitMessage(contactMessage);
            if (success) {
                log.info("contact form submitted, subject={}", subject);
                return R.ok("消息提交成功，我们会尽快回复您。");
            }
            return R.error("提交过于频繁或系统未能处理本次留言，请稍后再试");
        } catch (Exception e) {
            log.error("submit contact form failed", e);
            return R.error("消息提交失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取所有消息列表")
    @GetMapping("/list")
    public R<List<ContactUsMessage>> getAllMessages() {
        try {
            return R.ok(contactUsService.getAllMessages());
        } catch (Exception e) {
            log.error("list contact messages failed", e);
            return R.error("获取消息列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("根据状态获取消息")
    @GetMapping("/status/{status}")
    public R<List<ContactUsMessage>> getMessagesByStatus(@PathVariable String status) {
        try {
            return R.ok(contactUsService.getMessagesByStatus(status));
        } catch (Exception e) {
            log.error("list contact messages by status failed", e);
            return R.error("获取消息列表失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取消息详情")
    @GetMapping("/{id}")
    public R<ContactUsMessage> getMessageDetail(@PathVariable Long id) {
        try {
            ContactUsMessage message = contactUsService.getMessageById(id);
            if (message == null) {
                return R.error("消息不存在");
            }
            return R.ok(message);
        } catch (Exception e) {
            log.error("get contact detail failed", e);
            return R.error("获取消息详情失败：" + e.getMessage());
        }
    }

    @ApiOperation("回复消息")
    @PostMapping("/{id}/reply")
    public R<String> replyToMessage(@PathVariable Long id, @RequestParam String response) {
        try {
            boolean success = contactUsService.replyToMessage(id, response);
            return success ? R.ok("回复成功") : R.error("回复失败");
        } catch (Exception e) {
            log.error("reply contact message failed", e);
            return R.error("回复失败：" + e.getMessage());
        }
    }

    @ApiOperation("更新消息状态")
    @PutMapping("/{id}/status")
    public R<String> updateMessageStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            boolean success = contactUsService.updateMessageStatus(id, status);
            return success ? R.ok("状态更新成功") : R.error("状态更新失败");
        } catch (Exception e) {
            log.error("update contact message status failed", e);
            return R.error("状态更新失败：" + e.getMessage());
        }
    }

    @ApiOperation("删除消息")
    @DeleteMapping("/{id}")
    public R<String> deleteMessage(@PathVariable Long id) {
        try {
            boolean success = contactUsService.deleteMessage(id);
            return success ? R.ok("删除成功") : R.error("删除失败");
        } catch (Exception e) {
            log.error("delete contact message failed", e);
            return R.error("删除失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取统计信息")
    @GetMapping("/statistics")
    public R<Map<String, Object>> getStatistics() {
        try {
            return R.ok(contactUsService.getStatistics());
        } catch (Exception e) {
            log.error("get contact statistics failed", e);
            return R.error("获取统计信息失败：" + e.getMessage());
        }
    }

    private void populateRequestContext(ContactUsMessage message, HttpServletRequest request) {
        if (message == null || request == null) {
            return;
        }
        if (message.getTenantId() == null) {
            Long tenantId = TenantContext.getTenantId();
            message.setTenantId(tenantId == null ? 1L : tenantId);
        }
        if (message.getIpAddress() == null || message.getIpAddress().isBlank()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                message.setIpAddress(forwardedFor.split(",")[0].trim());
            } else {
                message.setIpAddress(request.getRemoteAddr());
            }
        }
        if (message.getUserAgent() == null || message.getUserAgent().isBlank()) {
            message.setUserAgent(request.getHeader("User-Agent"));
        }
    }
}
