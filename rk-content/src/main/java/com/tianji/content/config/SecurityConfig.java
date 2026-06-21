package com.tianji.content.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置
 * P0修复: 启用方法级安全，支持@PreAuthorize注解
 * 用于新闻管理、作品管理等模块的权限控制
 *
 * P1修复: 作品管理权限控制
 * - 添加PermissionFilter将用户权限设置到SecurityContext
 * - 使@PreAuthorize注解能够正常验证权限
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（微服务不需要）
            .csrf().disable()
            // 禁用 Session（使用 JWT）
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 允许所有请求，权限由 @PreAuthorize 注解控制
            .authorizeRequests()
                .anyRequest().permitAll()
            .and()
            // 添加自定义权限过滤器
            .addFilterBefore(permissionFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
