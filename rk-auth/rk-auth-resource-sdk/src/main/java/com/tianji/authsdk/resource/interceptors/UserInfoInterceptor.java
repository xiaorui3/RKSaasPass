package com.tianji.authsdk.resource.interceptors;

import com.tianji.auth.common.constants.JwtConstants;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class UserInfoInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.尝试获取头信息中的用户信息
        String authorization = request.getHeader(JwtConstants.USER_HEADER);
        // 2.判断是否为空
        if (authorization != null) {
            try {
                Long userId = Long.valueOf(authorization);
                UserContext.setUser(userId);
            } catch (NumberFormatException e) {
                log.error("用户身份信息格式不正确，{}, 原因：{}", authorization, e.getMessage());
            }
        }

        // 3. 获取租户ID（企业级多租户标准）
        String tenantIdStr = request.getHeader(TENANT_HEADER);
        if (tenantIdStr != null) {
            try {
                Long tenantId = Long.valueOf(tenantIdStr);
                TenantContext.setTenantId(tenantId);
            } catch (NumberFormatException e) {
                log.error("租户ID格式不正确，{}, 原因：{}", tenantIdStr, e.getMessage());
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理用户信息和租户信息
        UserContext.removeUser();
        TenantContext.removeTenantId();
    }
}
