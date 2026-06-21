package com.tianji.common.autoconfigure.mybatis;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.tianji.common.utils.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * MyBatis Plus 租户拦截器
 * 自动在SQL查询中添加 tenant_id 条件，实现租户数据隔离
 *
 * 工作原理：
 * 1. 拦截所有SQL查询操作
 * 2. 从TenantContext获取当前租户ID
 * 3. 在WHERE子句中自动添加 tenant_id = ? 条件
 *
 * @author RK-Web Team
 * @since 1.0.0
 */
public class TenantLineInnerInterceptor implements InnerInterceptor {

    /**
     * 租户ID字段名称
     */
    private static final String TENANT_FIELD = "tenant_id";

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                           ResultHandler resultHandler, BoundSql boundSql) {
        // 获取当前租户ID
        Long tenantId = TenantContext.getTenantId();

        // 如果没有租户ID，不进行拦截（可能是系统管理员或无需租户隔离的操作）
        if (tenantId == null) {
            return;
        }

        // 获取原始SQL
        String originalSql = boundSql.getSql();

        // 检查SQL是否已经包含 tenant_id 条件
        if (originalSql.toLowerCase().contains(TENANT_FIELD)) {
            return;
        }

        // 在SQL中添加租户ID条件
        String modifiedSql = addTenantCondition(originalSql, tenantId);

        // 修改SQL（通过反射设置）
        try {
            java.lang.reflect.Field field = boundSql.getClass().getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, modifiedSql);
        } catch (Exception e) {
            throw new RuntimeException("添加租户条件失败", e);
        }
    }

    /**
     * 在SQL中添加租户条件
     *
     * @param sql 原始SQL
     * @param tenantId 租户ID
     * @return 添加租户条件后的SQL
     */
    private String addTenantCondition(String sql, Long tenantId) {
        // 简单实现：在WHERE子句中添加 tenant_id = ?
        // 注意：这是一个简化实现，生产环境建议使用JSqlParser等工具进行SQL解析和修改

        String sqlUpperCase = sql.toUpperCase();

        // 查找WHERE关键字的位置
        int whereIndex = sqlUpperCase.indexOf(" WHERE ");

        if (whereIndex == -1) {
            // 没有WHERE子句，添加一个
            return sql + " WHERE tenant_id = " + tenantId;
        } else {
            // 有WHERE子句，在WHERE后面添加租户条件
            int insertIndex = whereIndex + " WHERE ".length();
            return sql.substring(0, insertIndex) + "tenant_id = " + tenantId + " AND " + sql.substring(insertIndex);
        }
    }
}