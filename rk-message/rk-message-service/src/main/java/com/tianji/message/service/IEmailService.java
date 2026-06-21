package com.tianji.message.service;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * 邮件发送服务接口
 * 基于master分支的RkController邮件功能
 */
public interface IEmailService {

    // 发送简单文本邮件
    boolean sendTextEmail(String to, String subject, String content) throws UnsupportedEncodingException, MessagingException;

    // 发送HTML邮件
    boolean sendHtmlEmail(String to, String subject, String htmlContent) throws UnsupportedEncodingException, MessagingException;

    // 发送带附件的邮件
    boolean sendEmailWithAttachment(String to, String subject, String content, String attachmentPath, String attachmentName) throws UnsupportedEncodingException, MessagingException;

    // 批量发送邮件
    Map<String, Object> sendBulkEmails(List<String> recipients, String subject, String content) throws UnsupportedEncodingException, MessagingException;

    // 发送申请通知邮件给管理员
    boolean sendApplicationNotice(String adminEmail, Map<String, Object> applicantInfo) throws UnsupportedEncodingException, MessagingException;

    // 发送审核结果邮件给申请者
    boolean sendReviewResultEmail(String applicantEmail, String result, String reason) throws UnsupportedEncodingException, MessagingException;

    // 生成HTML格式的申请通知内容
    String generateApplicationNoticeHtml(Map<String, Object> applicantInfo);
}