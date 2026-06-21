package com.tianji.common.constants;

public interface MqConstants {
    interface Exchange {
        String COURSE_EXCHANGE = "course.topic";
        String ORDER_EXCHANGE = "order.topic";
        String LEARNING_EXCHANGE = "learning.topic";
        String SMS_EXCHANGE = "sms.direct";
        String EMAIL_EXCHANGE = "email.direct";
        String ERROR_EXCHANGE = "error.topic";
        String PAY_EXCHANGE = "pay.topic";
        String TRADE_DELAY_EXCHANGE = "trade.delay.topic";
        String LIKE_RECORD_EXCHANGE = "like.record.topic";
    }

    interface Queue {
        String ERROR_QUEUE_TEMPLATE = "error.{}.queue";
    }

    interface Key {
        String COURSE_NEW_KEY = "course.new";
        String COURSE_UP_KEY = "course.up";
        String COURSE_DOWN_KEY = "course.down";
        String COURSE_EXPIRE_KEY = "course.expire";
        String COURSE_DELETE_KEY = "course.delete";

        String ORDER_PAY_KEY = "order.pay";
        String ORDER_REFUND_KEY = "order.refund";

        String WRITE_REPLY = "reply.new";
        String SIGN_IN = "sign.in";
        String LEARN_SECTION = "section.learned";
        String WRITE_NOTE = "note.new";
        String NOTE_GATHERED = "note.gathered";

        String LIKED_TIMES_KEY_TEMPLATE = "{}.times.changed";
        String QA_LIKED_TIMES_KEY = "QA.times.changed";
        String NOTE_LIKED_TIMES_KEY = "NOTE.times.changed";

        String SMS_MESSAGE = "sms.message";
        String EMAIL_MESSAGE = "email.send";

        String ERROR_KEY_PREFIX = "error.";
        String DEFAULT_ERROR_KEY = "error.#";

        String PAY_SUCCESS = "pay.success";
        String REFUND_CHANGE = "refund.status.change";
        String ORDER_DELAY_KEY = "delay.order.query";
    }
}
