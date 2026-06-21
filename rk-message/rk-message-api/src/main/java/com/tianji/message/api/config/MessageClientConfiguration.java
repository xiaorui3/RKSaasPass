package com.tianji.message.api.config;


import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.TenantContext;
import com.tianji.message.api.client.AsyncEmailClient;
import com.tianji.message.api.client.AsyncSmsClient;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tianji.common.constants.Constant.*;

@Configuration
@EnableFeignClients(basePackages = "com.tianji.message.api.client")
public class MessageClientConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestInterceptor requestIdInterceptor(){
        return template -> {
            template.header(REQUEST_ID_HEADER, MDC.get(REQUEST_ID_HEADER));
            template.header(REQUEST_FROM_HEADER, FEIGN_ORIGIN_NAME);
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                template.header("X-Tenant-Id", String.valueOf(tenantId));
            }
            if (Boolean.TRUE.equals(TenantContext.isSuperAdmin())) {
                template.header("X-Super-Admin", "true");
            }
        };
    }

    @Bean
    public AsyncSmsClient smsClient(RabbitMqHelper mqHelper){
        return new AsyncSmsClient(mqHelper);
    }

    @Bean
    public AsyncEmailClient emailClient(RabbitMqHelper mqHelper){
        return new AsyncEmailClient(mqHelper);
    }
}
