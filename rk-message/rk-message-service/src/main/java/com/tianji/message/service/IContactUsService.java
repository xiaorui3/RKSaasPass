package com.tianji.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.message.domain.dto.ContactPublicShieldDTO;
import com.tianji.message.domain.po.ContactUsMessage;

import java.util.List;
import java.util.Map;

/**
 * 联系我们服务接口
 * 基于master分支的ContactUsController功能
 */
public interface IContactUsService extends IService<ContactUsMessage> {

    // 提交联系消息
    boolean submitMessage(ContactUsMessage message);

    // 签发前台公开提交防刷令牌
    ContactPublicShieldDTO issuePublicShield(Long tenantId, String ipAddress, String userAgent);

    // 提交前台公开联系消息
    boolean submitPublicMessage(ContactUsMessage message, String shieldToken, Long issuedAt, String honeypot);

    boolean submitPublicMessage(ContactUsMessage message, String shieldToken, Long issuedAt, String honeypot, String verificationCode);

    // 获取所有消息
    List<ContactUsMessage> getAllMessages();

    // 根据状态获取消息
    List<ContactUsMessage> getMessagesByStatus(String status);

    // 获取消息详情
    ContactUsMessage getMessageById(Long id);

    // 回复消息
    boolean replyToMessage(Long id, String response);

    // 更新消息状态
    boolean updateMessageStatus(Long id, String status);

    // 删除消息
    boolean deleteMessage(Long id);

    // 获取统计数据
    Map<String, Object> getStatistics();
}
