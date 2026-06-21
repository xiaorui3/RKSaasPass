package com.tianji.common.autoconfigure.media;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;

@Configuration
@ConditionalOnClass({Filter.class})
@EnableConfigurationProperties(MediaPathProperties.class)
public class MediaPathAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MediaPathHelper mediaPathHelper(MediaPathProperties properties) {
        return new MediaPathHelper(properties);
    }

    @Bean
    public MediaPathRequestBodyAdvice mediaPathRequestBodyAdvice(MediaPathHelper helper) {
        return new MediaPathRequestBodyAdvice(helper);
    }

    @Bean
    public MediaPathResponseBodyAdvice mediaPathResponseBodyAdvice(MediaPathHelper helper) {
        return new MediaPathResponseBodyAdvice(helper);
    }
}
