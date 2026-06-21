package com.tianji.common.autoconfigure.mybatis;


import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({MybatisPlusInterceptor.class, BaseMapper.class})
public class MybatisConfig {

    /**
     * @deprecated 存在任务更新数据导致updater写入0或null的问题，暂时废弃
     * @see MyBatisAutoFillInterceptor 通过自定义拦截器来实现自动注入creater和updater
     */
    // @Bean
    // @ConditionalOnMissingBean
    public BaseMetaObjectHandler baseMetaObjectHandler(){
        return new BaseMetaObjectHandler();
    }

    /**
     * 租户行处理器Bean（企业级ERP多租户标准）
     */
    @Bean
    @ConditionalOnMissingBean
    public RKTenantLineHandler tenantLineHandler() {
        return new RKTenantLineHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 租户隔离插件（企业级ERP多租户标准）- 必须在分页插件之前执行
        com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor tenantLineInnerInterceptor =
            new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(tenantLineHandler());
        interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        // 分页插件 - 在租户过滤之后执行
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(200L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        // 自动填充插件
        interceptor.addInnerInterceptor(new MyBatisAutoFillInterceptor());
        return interceptor;
    }
}
