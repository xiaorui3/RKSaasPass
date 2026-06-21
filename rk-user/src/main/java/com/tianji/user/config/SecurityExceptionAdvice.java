package com.tianji.user.config;

import com.tianji.common.constants.Constant;
import com.tianji.common.domain.R;
import com.tianji.common.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class SecurityExceptionAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDeniedException(AccessDeniedException e) {
        log.warn("access denied uri : {} -> {}", WebUtils.getRequest().getRequestURI(), e.getMessage());
        WebUtils.setResponseHeader(Constant.BODY_PROCESSED_MARK_HEADER, "true");
        if (WebUtils.isGatewayRequest()) {
            return R.error(HttpStatus.FORBIDDEN.value(), "拒绝访问，权限不足")
                    .requestId(MDC.get(Constant.REQUEST_ID_HEADER));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("拒绝访问，权限不足");
    }
}
