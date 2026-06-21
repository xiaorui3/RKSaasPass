package com.tianji.message.handler;

import com.rabbitmq.client.Channel;
import com.tianji.common.constants.MqConstants;
import com.tianji.message.domain.dto.EmailInfoDTO;
import com.tianji.message.service.EmailSendResultCallbackClient;
import com.tianji.message.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailMessageHandler {

    private final IEmailService emailService;
    private final EmailSendResultCallbackClient callbackClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "email.send.queue", durable = "true"),
            exchange = @Exchange(MqConstants.Exchange.EMAIL_EXCHANGE),
            key = MqConstants.Key.EMAIL_MESSAGE
    ))
    public void listenEmailMessage(EmailInfoDTO emailInfoDTO, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        if (emailInfoDTO == null || emailInfoDTO.getEmails() == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        boolean html = Boolean.TRUE.equals(emailInfoDTO.getHtml());
        int index = 0;
        for (String email : emailInfoDTO.getEmails()) {
            try {
                if (html) {
                    emailService.sendHtmlEmail(email, emailInfoDTO.getSubject(), emailInfoDTO.getContent());
                } else {
                    emailService.sendTextEmail(email, emailInfoDTO.getSubject(), emailInfoDTO.getContent());
                }
                callbackSuccess(emailInfoDTO, index);
            } catch (Exception e) {
                log.error("邮件异步发送失败, email={}, subject={}", email, emailInfoDTO.getSubject(), e);
                callbackFailure(emailInfoDTO, index, e);
            }
            index++;
        }
        channel.basicAck(deliveryTag, false);
    }

    private void callbackSuccess(EmailInfoDTO emailInfoDTO, int index) {
        if (emailInfoDTO.getTaskId() == null || emailInfoDTO.getRecipientIds() == null || index >= emailInfoDTO.getRecipientIds().size()) {
            return;
        }
        try {
            callbackClient.markRecipientSuccess(emailInfoDTO.getTaskId(), emailInfoDTO.getRecipientIds().get(index));
        } catch (Exception e) {
            log.error("邮件发送成功后回写状态失败, taskId={}, recipientId={}",
                    emailInfoDTO.getTaskId(), emailInfoDTO.getRecipientIds().get(index), e);
        }
    }

    private void callbackFailure(EmailInfoDTO emailInfoDTO, int index, Exception sendException) {
        if (emailInfoDTO.getTaskId() == null || emailInfoDTO.getRecipientIds() == null || index >= emailInfoDTO.getRecipientIds().size()) {
            return;
        }
        Long recipientId = emailInfoDTO.getRecipientIds().get(index);
        try {
            callbackClient.markRecipientFailure(emailInfoDTO.getTaskId(), recipientId, sendException.getMessage());
        } catch (Exception e) {
            log.error("邮件发送失败后回写状态失败, taskId={}, recipientId={}", emailInfoDTO.getTaskId(), recipientId, e);
        }
    }
}
