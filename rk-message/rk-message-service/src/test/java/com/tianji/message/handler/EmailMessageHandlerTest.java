package com.tianji.message.handler;

import com.rabbitmq.client.Channel;
import com.tianji.message.domain.dto.EmailInfoDTO;
import com.tianji.message.service.EmailSendResultCallbackClient;
import com.tianji.message.service.IEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailMessageHandlerTest {

    @Mock
    private IEmailService emailService;

    @Mock
    private EmailSendResultCallbackClient callbackClient;

    @Mock
    private Channel channel;

    @Test
    void listenEmailMessage_shouldAckWhenSendSucceeds() throws Exception {
        EmailMessageHandler handler = new EmailMessageHandler(emailService, callbackClient);
        EmailInfoDTO dto = new EmailInfoDTO();
        dto.setSubject("subject");
        dto.setContent("content");
        dto.setHtml(true);
        dto.setEmails(List.of("ok@example.com"));
        dto.setTaskId(10L);
        dto.setRecipientIds(List.of(100L));

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(11L);
        Message message = new Message(new byte[0], properties);

        handler.listenEmailMessage(dto, channel, message);

        verify(emailService).sendHtmlEmail("ok@example.com", "subject", "content");
        verify(callbackClient).markRecipientSuccess(10L, 100L);
        verify(channel).basicAck(11L, false);
    }

    @Test
    void listenEmailMessage_shouldAckAndCallbackFailureWhenSendFails() throws Exception {
        EmailMessageHandler handler = new EmailMessageHandler(emailService, callbackClient);
        EmailInfoDTO dto = new EmailInfoDTO();
        dto.setSubject("subject");
        dto.setContent("content");
        dto.setHtml(false);
        dto.setEmails(List.of("fail@example.com"));
        dto.setTaskId(11L);
        dto.setRecipientIds(List.of(200L));

        doThrow(new RuntimeException("boom")).when(emailService).sendTextEmail("fail@example.com", "subject", "content");

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(12L);
        Message message = new Message(new byte[0], properties);

        handler.listenEmailMessage(dto, channel, message);

        verify(callbackClient).markRecipientFailure(eq(11L), eq(200L), contains("boom"));
        verify(channel).basicAck(12L, false);
    }
}
