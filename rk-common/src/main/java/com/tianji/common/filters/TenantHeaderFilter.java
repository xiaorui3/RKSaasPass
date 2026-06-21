package com.tianji.common.filters;

import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.RoleContext;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 租户ID过滤器
 * 从请求头中读取X-Tenant-Id并设置到TenantContext
 * 同时处理超级管理员标识X-Super-Admin
 * 
 * 默认租户ID为1（软开社团），用于公开访问的页面
 *
 * @author RK-Web Team
 * @since 1.0.0
 */
public class TenantHeaderFilter implements Filter {
    
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String ROLE_HEADER = "X-Role-Id";
    private static final String SUPER_ADMIN_HEADER = "X-Super-Admin";
    
    /**
     * 默认租户ID - 软开社团
     * 公开访问时使用此租户ID
     */
    private static final Long DEFAULT_TENANT_ID = 1L;
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 1.获取request
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        
        // 2.获取请求头中的租户ID
        String tenantIdStr = request.getHeader(TENANT_HEADER);
        String roleIdStr = request.getHeader(ROLE_HEADER);
        
        // 3.获取超级管理员标识
        String superAdminStr = request.getHeader(SUPER_ADMIN_HEADER);
        boolean isSuperAdmin = "true".equalsIgnoreCase(superAdminStr);
        
        try {
            // 4.设置超级管理员标识
            if (isSuperAdmin) {
                TenantContext.setSuperAdmin(true);
            }
            
            Long tenantId = DEFAULT_TENANT_ID; // 默认使用软开社团
            
            // 5.如果请求头中有租户ID，使用请求头的值
            if (tenantIdStr != null && !tenantIdStr.isEmpty()) {
                try {
                    tenantId = Long.parseLong(tenantIdStr);
                } catch (NumberFormatException e) {
                    // 忽略无效的租户ID，使用默认值
                }
            }
            
            // 6.设置到TenantContext
            TenantContext.setTenantId(tenantId);
            if (roleIdStr != null && !roleIdStr.isEmpty()) {
                try {
                    Long roleId = Long.parseLong(roleIdStr);
                    if (roleId != null && roleId > 0) {
                        RoleContext.setRoleId(roleId);
                    } else {
                        RoleContext.clear();
                    }
                } catch (NumberFormatException e) {
                    RoleContext.clear();
                }
            }
            // 同时设置到MDC方便日志追踪
            MDC.put("tenantId", String.valueOf(tenantId));
            
            filterChain.doFilter(request, servletResponse);
        } finally {
            // 7.清理
            TenantContext.clear();
            RoleContext.clear();
            MDC.remove("tenantId");
        }
    }
}
