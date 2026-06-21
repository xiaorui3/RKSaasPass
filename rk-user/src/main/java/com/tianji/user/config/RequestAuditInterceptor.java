package com.tianji.user.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.common.utils.WebUtils;
import com.tianji.user.domain.po.User;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.ISysOperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
public class RequestAuditInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = RequestAuditInterceptor.class.getName() + ".startTime";
    private static final String OPERATOR_NAME_ATTR = RequestAuditInterceptor.class.getName() + ".operatorName";
    private static final String USER_HEADER = "user-info";

    private final ISysOperLogService sysOperLogService;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        if (!shouldSkip(request)) {
            Long authUserId = resolveCurrentUserId(request);
            if (authUserId != null) {
                request.setAttribute(OPERATOR_NAME_ATTR, safeResolveOperatorName(authUserId));
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (shouldSkip(request)) {
            return;
        }
        Long authUserId = resolveCurrentUserId(request);
        if (authUserId == null) {
            return;
        }

        long startTime = request.getAttribute(START_TIME_ATTR) instanceof Long
                ? (Long) request.getAttribute(START_TIME_ATTR)
                : System.currentTimeMillis();
        long costTime = Math.max(0L, System.currentTimeMillis() - startTime);

        try {
            sysOperLogService.recordOperation(
                    buildTitle(request),
                    handler instanceof HandlerMethod ? ((HandlerMethod) handler).getMethod().toGenericString() : handler.getClass().getName(),
                    request.getMethod(),
                    resolveBusinessType(request.getMethod()),
                    resolveOperatorName(request, authUserId),
                    request.getRequestURI(),
                    WebUtils.getClientIp(request),
                    request.getQueryString(),
                    response.getStatus() >= 400 || ex != null ? 1 : 0,
                    ex == null ? null : ex.getMessage(),
                    costTime
            );
        } catch (Exception auditError) {
            log.warn("record operation audit failed for {}: {}", request.getRequestURI(), auditError.getMessage());
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null
                || uri.startsWith("/swagger")
                || uri.startsWith("/v2/api-docs")
                || uri.startsWith("/doc.html")
                || "/tenants/list".equals(uri)
                || "/api/config/theme/public".equals(uri)
                || "/api/config/theme/current".equals(uri)
                || uri.startsWith("/api/audit/internal/")
                || uri.startsWith("/admin/logs/");
    }

    private Long resolveCurrentUserId(HttpServletRequest request) {
        String userInfo = request.getHeader(USER_HEADER);
        if (userInfo == null || userInfo.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userInfo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveOperatorName(HttpServletRequest request, Long authUserId) {
        Object cached = request.getAttribute(OPERATOR_NAME_ATTR);
        if (cached instanceof String && !((String) cached).isBlank()) {
            return (String) cached;
        }
        return safeResolveOperatorName(authUserId);
    }

    private String safeResolveOperatorName(Long authUserId) {
        try {
            return lookupOperatorName(authUserId);
        } catch (Exception e) {
            log.debug("resolve operator name fallback for authUserId={}: {}", authUserId, e.getMessage());
            return "auth:" + authUserId;
        }
    }

    private String lookupOperatorName(Long authUserId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getAuthUserId, authUserId)
                .last("LIMIT 1"));
        if (user == null) {
            user = userMapper.selectById(authUserId);
        }
        if (user != null && user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "auth:" + authUserId;
    }

    private String buildTitle(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return "request";
        }
        return uri.length() > 255 ? uri.substring(0, 255) : uri;
    }

    private Integer resolveBusinessType(String method) {
        if (method == null) {
            return 0;
        }
        switch (method.toUpperCase(Locale.ROOT)) {
            case "POST":
                return 1;
            case "PUT":
            case "PATCH":
                return 2;
            case "DELETE":
                return 3;
            default:
                return 0;
        }
    }
}
