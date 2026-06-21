package com.tianji.message.service.impl;

import com.tianji.message.service.IEmailService;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 邮件发送服务实现类
 * 基于master分支的RkController邮件功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:z13039811650@163.com}")
    private String fromEmail;

    @Value("${spring.mail.from-name:软件项目开发社团}")
    private String fromName;

    @Override
    public boolean sendTextEmail(String to, String subject, String content) throws UnsupportedEncodingException, MessagingException {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFromAddress());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("✅ 文本邮件发送成功 - 收件人: {}, 主题: {}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("文本邮件发送失败 - 收件人: {}, 主题: {}", to, subject, e);
            throw e;
        }
    }

    @Override
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) throws UnsupportedEncodingException, MessagingException {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(getFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✅ HTML邮件发送成功 - 收件人: {}, 主题: {}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("HTML邮件发送失败 - 收件人: {}, 主题: {}", to, subject, e);
            throw e;
        }
    }

    @Override
    public boolean sendEmailWithAttachment(String to, String subject, String content, String attachmentPath, String attachmentName) throws UnsupportedEncodingException, MessagingException {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(getFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            // 添加附件
            if (attachmentPath != null && !attachmentPath.trim().isEmpty()) {
                FileSystemResource file = new FileSystemResource(new File(attachmentPath));
                if (file.exists()) {
                    helper.addAttachment(attachmentName != null ? attachmentName : file.getFilename(), file);
                }
            }

            mailSender.send(mimeMessage);
            log.info("✅ 带附件邮件发送成功 - 收件人: {}, 主题: {}, 附件: {}", to, subject, attachmentName);
            return true;
        } catch (Exception e) {
            log.error("带附件邮件发送失败 - 收件人: {}, 主题: {}", to, subject, e);
            throw e;
        }
    }

    @Override
    public Map<String, Object> sendBulkEmails(List<String> recipients, String subject, String content) throws UnsupportedEncodingException, MessagingException {
        Map<String, Object> result = new HashMap<>();
        List<String> successRecipients = new ArrayList<>();
        List<Map<String, String>> failedRecipients = new ArrayList<>();

        for (String recipient : recipients) {
            try {
                boolean sent = sendTextEmail(recipient, subject, content);
                if (sent) {
                    successRecipients.add(recipient);
                } else {
                    failedRecipients.add(Map.of("email", recipient, "error", "发送失败"));
                }
            } catch (Exception e) {
                failedRecipients.add(Map.of("email", recipient, "error", e.getMessage()));
                log.warn("批量邮件发送失败 - 收件人: {}", recipient, e);
            }
        }

        result.put("total", recipients.size());
        result.put("successCount", successRecipients.size());
        result.put("failedCount", failedRecipients.size());
        result.put("successRecipients", successRecipients);
        result.put("failedRecipients", failedRecipients);

        log.info("批量邮件发送完成 - 总数: {}, 成功: {}, 失败: {}",
                recipients.size(), successRecipients.size(), failedRecipients.size());

        return result;
    }

    @Override
    public boolean sendApplicationNotice(String adminEmail, Map<String, Object> applicantInfo) throws UnsupportedEncodingException, MessagingException {
        String subject = "社团成员新申请通知";
        String htmlContent = generateApplicationNoticeHtml(applicantInfo);
        return sendHtmlEmail(adminEmail, subject, htmlContent);
    }

    @Override
    public boolean sendReviewResultEmail(String applicantEmail, String result, String reason) throws UnsupportedEncodingException, MessagingException {
        String subject = "社团申请审核结果通知";

        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html lang='zh-CN'>")
                .append("<head>")
                .append("   <meta charset='UTF-8'>")
                .append("   <meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("   <title>审核结果通知</title>")
                .append("</head>")
                .append("<body style='background: #f5f7fa; font-family: \"Microsoft YaHei\", Arial, sans-serif; color: #333; padding: 20px; line-height: 1.6;'>")
                .append("   <div style='max-width: 720px; margin: 0 auto; background: #ffffff; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>")
                .append("       <div style='border-bottom: 3px solid #2c5282; padding-bottom: 15px; margin-bottom: 30px;'>")
                .append("           <h1 style='color: #2c5282; text-align: center; margin: 0; font-size: 24px;'>审核结果通知</h1>")
                .append("       </div>")
                .append("       <div style='padding: 30px; border-radius: 6px; margin-bottom: 30px; background-color: #f8fafc;'>");

        if ("通过".equals(result)) {
            htmlContent.append("           <p style='color: #22543d; font-size: 16px; font-weight: bold;'>恭喜您！您的社团申请已通过审核。</p>");
        } else if ("未通过".equals(result)) {
            htmlContent.append("           <p style='color: #c53030; font-size: 16px; font-weight: bold;'>很遗憾，您的社团申请未通过审核。</p>");
        } else {
            htmlContent.append("           <p style='color: #4a5568; font-size: 16px;'>您的社团申请审核结果为：").append(escapeHtml(result)).append("</p>");
        }

        if (reason != null && !reason.trim().isEmpty()) {
            htmlContent.append("           <p style='color: #4a5568; font-size: 16px; margin-top: 20px;'>审核说明：").append(escapeHtml(reason)).append("</p>");
        }

        htmlContent.append("       </div>")
                .append("       <div style='text-align: center; color: #718096; padding-top: 20px; border-top: 1px solid #e2e8f0;'>")
                .append("           <p style='margin: 5px 0;'>软件项目开发社团</p>")
                .append("           <p style='margin: 5px 0;'>").append(new SimpleDateFormat("yyyy年MM月dd日").format(new Date())).append("</p>")
                .append("       </div>")
                .append("   </div>")
                .append("</body>")
                .append("</html>");

        return sendHtmlEmail(applicantEmail, subject, htmlContent.toString());
    }

    @Override
    public String generateApplicationNoticeHtml(Map<String, Object> applicantInfo) {
        StringBuilder htmlContent = new StringBuilder();

        htmlContent.append("<html lang='zh-CN'>")
                .append("<head>")
                .append("   <meta charset='UTF-8'>")
                .append("   <meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("   <title>社团成员新申请通知</title>")
                .append("</head>")
                .append("<body style='background: #f5f7fa; font-family: \"Microsoft YaHei\", Arial, sans-serif; color: #333; padding: 20px; line-height: 1.6;'>")
                .append("   <div style='max-width: 720px; margin: 0 auto; background: #ffffff; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>")
                .append("       <div style='border-bottom: 3px solid #2c5282; padding-bottom: 15px; margin-bottom: 30px;'>")
                .append("           <h1 style='color: #2c5282; text-align: center; margin: 0; font-size: 24px;'>新成员申请通知</h1>")
                .append("       </div>")
                .append("       <div style='border: 1px solid #e2e8f0; padding: 30px; border-radius: 6px; margin-bottom: 30px; background-color: #f8fafc;'>")
                .append("           <p style='color: #4a5568; font-size: 16px; margin-top: 0;'>尊敬的负责人，您好：</p>")
                .append("           <p style='color: #4a5568; font-size: 16px;'>有新成员申请加入社团，申请人信息如下：</p>")
                .append("           <table style='width: 100%; border-collapse: collapse; margin-top: 20px;'>");

        // 定义字段映射
        Map<String, String> fieldMapping = new LinkedHashMap<>();
        fieldMapping.put("name", "姓名");
        fieldMapping.put("studentId", "学号");
        fieldMapping.put("major", "专业");
        fieldMapping.put("phone", "电话");
        fieldMapping.put("email", "邮箱");
        fieldMapping.put("interest", "兴趣");
        fieldMapping.put("experience", "经验");

        boolean alternate = false;
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            String key = entry.getKey();
            String label = entry.getValue();
            Object value = applicantInfo.get(key);

            if (value != null) {
                String bgColor = alternate ? "#edf2f7" : "#ffffff";
                htmlContent.append("               <tr style='border-bottom: 1px solid #e2e8f0;'>")
                        .append("                   <td style='padding: 12px 10px; width: 25%; font-weight: bold; color: #2d3748; background-color: ")
                        .append(bgColor).append(";'>").append(label).append("：</td>")
                        .append("                   <td style='padding: 12px 10px; color: #4a5568; background-color: ")
                        .append(bgColor).append(";'>");

                if ("experience".equals(key)) {
                    htmlContent.append("<div style='white-space: pre-line;'>").append(escapeHtml(value.toString())).append("</div>");
                } else {
                    htmlContent.append(escapeHtml(value.toString()));
                }

                htmlContent.append("</td>")
                        .append("               </tr>");
                alternate = !alternate;
            }
        }

        htmlContent.append("           </table>")
                .append("       </div>")
                .append("       <div style='text-align: center; color: #718096; padding-top: 20px; border-top: 1px solid #e2e8f0;'>")
                .append("           <p style='margin: 5px 0;'>软件项目开发社团人力资源部</p>")
                .append("           <p style='margin: 5px 0;'>").append(new SimpleDateFormat("yyyy年MM月dd日").format(new Date())).append("</p>")
                .append("       </div>")
                .append("   </div>")
                .append("</body>")
                .append("</html>");

        return htmlContent.toString();
    }

    private String getFromAddress() throws UnsupportedEncodingException {
        String encodedFromName = "=?UTF-8?B?" +
                java.util.Base64.getEncoder().encodeToString(fromName.getBytes(StandardCharsets.UTF_8)) +
                "?=";
        return encodedFromName + " <" + fromEmail + ">";
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}