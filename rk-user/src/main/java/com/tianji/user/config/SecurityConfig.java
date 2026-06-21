package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置
 * P0-1修复: 启用方法级安全，支持@PreAuthorize注解
 * P0-rk-user: 禁用默认Security filter chain,使用MVC拦截器进行认证
 * 2026-03-27修复: 添加PermissionFilter支持权限检查
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final PermissionFilter permissionFilter;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 禁用CSRF和默认的HTTP Basic认证
        // 允许所有请求,让MVC拦截器处理认证
        http
            .csrf().disable()
            .authorizeRequests()
            .anyRequest().permitAll()
            .and()
            .httpBasic().disable()
            .formLogin().disable()
            // 不禁用匿名认证，这样公开接口可以正常访问
            // .anonymous().disable()
            // 添加权限过滤器
            .addFilterBefore(permissionFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
