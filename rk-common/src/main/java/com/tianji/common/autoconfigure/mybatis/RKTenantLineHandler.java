package com.tianji.common.autoconfigure.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class RKTenantLineHandler implements TenantLineHandler {

    private static final List<String> IGNORE_TABLES = Arrays.asList(
            "rk_tenant",
            "client",
            "oauth_client",
            "menu",
            "privilege",
            "role_permission",
            "user_role",
            "account_role",
            "role_menu",
            "role_privilege",
            "code",
            "sys_dept",
            "sys_user",
            "sys_role",
            "sys_menu",
            "sys_post",
            "sys_dict_type",
            "sys_dict_data",
            "sys_config",
            "sys_oper_log",
            "sys_logininfor",
            "sys_notice",
            "sys_user_role",
            "sys_role_menu",
            "sys_role_dept",
            "sys_user_post",
            "login_record",
            "email_send_task",
            "email_send_recipient"
    );

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            if (TenantContext.isSuperAdmin()) {
                log.debug("RKTenantLineHandler.getTenantId() - super admin mode, using default tenantId=1");
                return new LongValue(1L);
            }
            log.error("RKTenantLineHandler.getTenantId() - tenantId missing");
            throw new BadRequestException(403, "租户信息缺失，无权访问");
        }
        log.debug("RKTenantLineHandler.getTenantId() - tenantId={}", tenantId);
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (TenantContext.isSuperAdmin()) {
            log.debug("RKTenantLineHandler.ignoreTable() - super admin skips table {}", tableName);
            return true;
        }
        return IGNORE_TABLES.stream().anyMatch(tableName::equalsIgnoreCase);
    }
}
