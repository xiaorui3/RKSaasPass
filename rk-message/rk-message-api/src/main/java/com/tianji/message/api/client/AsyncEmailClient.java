package com.tianji.message.api.client;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.message.domain.dto.EmailInfoDTO;

public class AsyncEmailClient {

    private final RabbitMqHelper mqHelper;

    public AsyncEmailClient(RabbitMqHelper mqHelper) {
        this.mqHelper = mqHelper;
    }

    public void sendMessage(EmailInfoDTO emailInfoDTO) {
        mqHelper.send(MqConstants.Exchange.EMAIL_EXCHANGE, MqConstants.Key.EMAIL_MESSAGE, emailInfoDTO);
    }
}
