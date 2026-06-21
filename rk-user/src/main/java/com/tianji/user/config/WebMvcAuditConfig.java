package com.tianji.user.config;

import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.ISysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebMvcAuditConfig implements WebMvcConfigurer {

    private static final long LINUX_SSH_STREAMING_TIMEOUT_MILLIS = Duration.ofHours(2).toMillis();

    private final ISysOperLogService sysOperLogService;
    private final UserMapper userMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestAuditInterceptor(sysOperLogService, userMapper))
                .addPathPatterns("/**");
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(LINUX_SSH_STREAMING_TIMEOUT_MILLIS);
    }
}
