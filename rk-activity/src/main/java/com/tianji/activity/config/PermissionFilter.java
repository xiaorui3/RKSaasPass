package com.tianji.activity.config;

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

    private static final List<String> ACTIVITY_ADMIN_PERMISSIONS = List.of(
            "content:activity:view", "content:activity:add", "content:activity:edit", "content:activity:remove",
            "content:competition:view", "content:competition:add", "content:competition:edit", "content:competition:remove",
            "content:category:view", "content:category:add", "content:category:edit", "content:category:remove"
    );

    private static final List<String> ACTIVITY_VIEW_PERMISSIONS = List.of(
            "content:activity:view",
            "content:competition:view",
            "content:category:view"
    );

    private static final Map<Long, List<String>> ROLE_PERMISSIONS = Map.of(
            1L, ACTIVITY_ADMIN_PERMISSIONS,
            2L, ACTIVITY_VIEW_PERMISSIONS,
            3L, ACTIVITY_ADMIN_PERMISSIONS,
            4L, ACTIVITY_VIEW_PERMISSIONS,
            5L, ACTIVITY_ADMIN_PERMISSIONS,
            6L, ACTIVITY_VIEW_PERMISSIONS,
            7L, ACTIVITY_ADMIN_PERMISSIONS,
            8L, ACTIVITY_VIEW_PERMISSIONS
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

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
            log.debug("Set authentication for user {} with {} authorities", userId, authorities.size());
        }

        filterChain.doFilter(request, response);
    }
}
