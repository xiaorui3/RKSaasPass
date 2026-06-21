package com.tianji.api.config;

import com.tianji.api.client.activity.fallback.ActivityRecipientClientFallback;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FallbackConfigTest {

    @Test
    void shouldRegisterActivityRecipientClientFallbackBean() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(FallbackConfig.class)) {
            assertNotNull(context.getBean(ActivityRecipientClientFallback.class));
        }
    }
}
