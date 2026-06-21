package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import io.lettuce.core.dynamic.annotation.Key;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 赵锐
 * @version 1.0
 * @description: TODO
 * @date 2026/2/9 12:40
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LessonChangeListener {

    //@Autowired
    private final ILearningLessonService iLearningLessonService;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE,type = "topic"),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void lessonListener(OrderBasicDTO order){

        if (order == null || order.getUserId() == null || CollUtils.isEmpty(order.getCourseIds())){
            log.error("订单支付，异常消息，信息未空");
            return;
        }
        log.info("订单支付成功，开始处理课程信息");
        iLearningLessonService.createLesson(order.getUserId(), order.getCourseIds());

    }
}
