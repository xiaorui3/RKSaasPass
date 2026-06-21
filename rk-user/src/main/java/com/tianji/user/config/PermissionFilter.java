package com.tianji.user.config;

import com.tianji.auth.common.constants.JwtConstants;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionFilter extends OncePerRequestFilter {

    private static final String ROLE_HEADER = "X-Role-Id";
    private static final String PERMISSIONS_HEADER = "X-Permissions";

    private static final List<String> TENANT_ADMIN_PERMISSIONS = List.of(
            "system:user:list", "system:user:query", "system:user:add", "system:user:edit", "system:user:remove", "system:user:reset_pwd",
            "system:tenant:view", "system:tenant:list", "system:tenant:query", "system:tenant:edit",
            "system:config:list", "system:config:query", "system:config:edit",
            "audit:member:list", "audit:member:review",
            "email:center:view", "email:center:send",
            "content:statistics:view",
            "content:works:view", "content:works:add", "content:works:edit", "content:works:remove",
            "content:news:view", "content:news:add", "content:news:edit", "content:news:remove",
            "content:activity:view", "content:activity:add", "content:activity:edit", "content:activity:remove",
            "content:competition:view", "content:competition:add", "content:competition:edit", "content:competition:remove",
            "content:notice:view", "content:notice:add", "content:notice:edit", "content:notice:remove",
            "alumni:list", "alumni:view", "alumni:add", "alumni:edit", "alumni:remove"
    );

    private static final List<String> MEMBER_PERMISSIONS = List.of(
            "content:works:view",
            "content:news:view",
            "content:activity:view",
            "content:competition:view",
            "content:notice:view",
            "alumni:list", "alumni:view"
    );

    private static final List<String> MANAGER_PERMISSIONS = TENANT_ADMIN_PERMISSIONS;

    private static final List<String> TEACHER_PERMISSIONS = List.of(
            "content:statistics:view",
            "email:center:view", "email:center:send",
            "content:works:view",
            "content:news:view", "content:news:approve", "content:news:reject",
            "content:activity:view",
            "content:competition:view",
            "content:notice:view", "content:notice:add", "content:notice:edit", "content:notice:remove",
            "alumni:list", "alumni:view"
    );

    private static final List<String> SUPER_ADMIN_PERMISSIONS = List.of(
            "system:user:list", "system:user:query", "system:user:add", "system:user:edit", "system:user:remove", "system:user:reset_pwd",
            "system:tenant:view", "system:tenant:list", "system:tenant:query", "system:tenant:add", "system:tenant:edit", "system:tenant:remove",
            "system:config:list", "system:config:query", "system:config:add", "system:config:edit", "system:config:remove",
            "system:operlog:list", "system:operlog:remove", "system:logininfor:list", "system:logininfor:remove",
            "audit:member:*", "audit:member:list", "audit:member:review", "audit:member:delete",
            "email:center:view", "email:center:send",
            "content:statistics:view",
            "content:works:view", "content:works:add", "content:works:edit", "content:works:remove",
            "content:news:view", "content:news:add", "content:news:edit", "content:news:remove", "content:news:approve", "content:news:reject",
            "content:activity:view", "content:activity:add", "content:activity:edit", "content:activity:remove",
            "content:competition:view", "content:competition:add", "content:competition:edit", "content:competition:remove",
            "content:notice:view", "content:notice:add", "content:notice:edit", "content:notice:remove",
            "alumni:list", "alumni:view", "alumni:add", "alumni:edit", "alumni:remove"
    );

    private static final Map<Long, List<String>> ROLE_PERMISSIONS = Map.of(
            1L, SUPER_ADMIN_PERMISSIONS,
            2L, MEMBER_PERMISSIONS,
            3L, TENANT_ADMIN_PERMISSIONS,
            4L, MEMBER_PERMISSIONS,
            5L, TENANT_ADMIN_PERMISSIONS,
            6L, MEMBER_PERMISSIONS,
            7L, MANAGER_PERMISSIONS,
            8L, TEACHER_PERMISSIONS
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (isPublicEndpoint(requestURI)) {
            log.debug("PermissionFilter: Public endpoint {}, skipping authentication", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String userIdStr = request.getHeader(JwtConstants.USER_HEADER);
        String roleIdStr = request.getHeader(ROLE_HEADER);
        String permissionsHeader = request.getHeader(PERMISSIONS_HEADER);

        Long userId = null;
        Long roleId = null;

        if (userIdStr != null) {
            try {
                userId = Long.valueOf(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid user id in header: {}", userIdStr);
            }
        } else {
            userId = UserContext.getUser();
        }

        if (userId != null) {
            if (roleIdStr != null) {
                try {
                    roleId = Long.valueOf(roleIdStr);
                } catch (NumberFormatException e) {
                    log.warn("Invalid role id in header: {}", roleIdStr);
                }
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (permissionsHeader != null && !permissionsHeader.isEmpty()) {
                String[] permissions = permissionsHeader.split(",");
                for (String permission : permissions) {
                    authorities.add(new SimpleGrantedAuthority(permission.trim()));
                }
            } else if (roleId != null) {
                List<String> rolePermissions = ROLE_PERMISSIONS.get(roleId);
                if (rolePermissions != null) {
                    for (String permission : rolePermissions) {
                        authorities.add(new SimpleGrantedAuthority(permission));
                    }
                }
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserContext.setUser(userId);

            log.debug("Set authentication for user {} with {} authorities", userId, authorities.size());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.removeUser();
        }
    }

    private boolean isPublicEndpoint(String requestURI) {
        if (requestURI.equals("/tenants/list")) {
            return true;
        }
        if (requestURI.startsWith("/static/") || requestURI.startsWith("/public/")) {
            return true;
        }
        if (requestURI.startsWith("/admin/ops/public/deploy-package/")) {
            return true;
        }
        if (requestURI.startsWith("/api/alumni/profile-form/")) {
            return true;
        }
        if (requestURI.startsWith("/v2/api-docs") || requestURI.startsWith("/swagger") || requestURI.equals("/doc.html")) {
            return true;
        }
        return false;
    }
}
