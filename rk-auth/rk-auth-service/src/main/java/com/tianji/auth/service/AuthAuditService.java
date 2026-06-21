package com.tianji.auth.service;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.LoginAuditRecordDTO;
import com.tianji.auth.util.JwtTool;
import com.tianji.common.domain.dto.LoginUserDTO;
import com.tianji.common.utils.UserContext;
import com.tianji.common.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAuditService {

    private final UserClient userClient;
    private final JwtTool jwtTool;

    public void recordLogin(String loginName, String status, String msg) {
        if (loginName == null || loginName.isBlank()) {
            return;
        }
        try {
            LoginAuditRecordDTO dto = new LoginAuditRecordDTO();
            dto.setLoginName(loginName);
            dto.setIpaddr(WebUtils.getRemoteAddr());
            dto.setBrowser(resolveBrowser());
            dto.setOs(resolveOs());
            dto.setStatus(status);
            dto.setMsg(msg);
            UserContext.setUser(-1L);
            userClient.recordLoginAudit(dto);
        } catch (Exception e) {
            log.warn("record login audit failed for {}: {}", loginName, e.getMessage());
        } finally {
            UserContext.removeUser();
        }
    }

    public void recordLogout(String authorization) {
        try {
            String token = authorization == null ? null : authorization.replaceFirst("(?i)^Bearer\\s+", "");
            if (token == null || token.isBlank()) {
                return;
            }
            LoginUserDTO loginUserDTO = jwtTool.parseToken(token);
            if (loginUserDTO != null && loginUserDTO.getUsername() != null) {
                recordLogin(loginUserDTO.getUsername(), "0", "退出成功");
            }
        } catch (Exception e) {
            log.warn("record logout audit failed: {}", e.getMessage());
        }
    }

    private String resolveBrowser() {
        String userAgent = WebUtils.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }

    private String resolveOs() {
        String userAgent = WebUtils.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }
        String normalized = userAgent.toLowerCase();
        if (normalized.contains("windows")) {
            return "Windows";
        }
        if (normalized.contains("mac os") || normalized.contains("macintosh")) {
            return "macOS";
        }
        if (normalized.contains("android")) {
            return "Android";
        }
        if (normalized.contains("iphone") || normalized.contains("ios")) {
            return "iOS";
        }
        if (normalized.contains("linux")) {
            return "Linux";
        }
        return "Unknown";
    }
}
