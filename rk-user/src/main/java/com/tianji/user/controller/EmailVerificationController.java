package com.tianji.user.controller;

import com.tianji.api.dto.user.EmailVerificationSendDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;
import com.tianji.common.domain.R;
import com.tianji.user.service.IEmailVerificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@Api(tags = "邮箱验证码接口")
@Validated
@RestController
@RequestMapping("/api/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final IEmailVerificationService emailVerificationService;

    @ApiOperation("发送邮箱验证码")
    @PostMapping("/send")
    public R<Boolean> send(@Validated @RequestBody EmailVerificationSendDTO dto) {
        try {
            return R.ok(emailVerificationService.sendCode(dto));
        } catch (Exception e) {
            log.error("发送邮箱验证码失败, email={}, scene={}", dto.getEmail(), dto.getScene(), e);
            return R.error(e.getMessage());
        }
    }

    @ApiOperation("校验邮箱验证码")
    @PostMapping("/verify")
    public R<Boolean> verify(@Validated @RequestBody EmailVerificationVerifyDTO dto) {
        try {
            return R.ok(emailVerificationService.verifyCode(dto));
        } catch (Exception e) {
            log.error("校验邮箱验证码失败, email={}, scene={}", dto.getEmail(), dto.getScene(), e);
            return R.error(e.getMessage());
        }
    }

    @ApiIgnore
    @PostMapping("/internal/send")
    public Boolean sendInternal(@RequestBody EmailVerificationSendDTO dto) {
        return emailVerificationService.sendCode(dto);
    }

    @ApiIgnore
    @PostMapping("/internal/verify")
    public Boolean verifyInternal(@RequestBody EmailVerificationVerifyDTO dto) {
        return emailVerificationService.verifyCode(dto);
    }
}
