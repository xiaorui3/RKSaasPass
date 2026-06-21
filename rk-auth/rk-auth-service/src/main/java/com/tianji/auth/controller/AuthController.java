package com.tianji.auth.controller;

import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.auth.ApprovedApplicantProvisionDTO;
import com.tianji.api.dto.auth.CurrentUserPasswordUpdateDTO;
import com.tianji.api.dto.auth.RoleAccountDTO;
import com.tianji.api.dto.auth.RoleRecipientQueryDTO;
import com.tianji.api.dto.auth.SwitchableTenantDTO;
import com.tianji.auth.domain.dto.EmailLoginConfirmDTO;
import com.tianji.auth.domain.dto.EmailLoginPrepareDTO;
import com.tianji.auth.domain.dto.LoginDTO;
import com.tianji.auth.domain.dto.RegisterDTO;
import com.tianji.auth.domain.vo.EmailLoginPrepareVO;
import com.tianji.auth.domain.vo.LoginVO;
import com.tianji.auth.service.AuthAuditService;
import com.tianji.auth.service.IAuthService;
import com.tianji.common.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@Api(tags = "认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final AuthAuditService authAuditService;

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        try {
            LoginVO loginVO = authService.login(dto);
            authAuditService.recordLogin(dto.getUsername(), "0", "登录成功");
            return R.ok(loginVO);
        } catch (RuntimeException e) {
            authAuditService.recordLogin(dto.getUsername(), "1", e.getMessage());
            throw e;
        }
    }

    @ApiOperation("邮箱验证码登录预处理")
    @PostMapping("/email-login/prepare")
    public R<EmailLoginPrepareVO> prepareEmailLogin(@Validated @RequestBody EmailLoginPrepareDTO dto) {
        return R.ok(authService.prepareEmailLogin(dto));
    }

    @ApiOperation("邮箱验证码登录确认")
    @PostMapping("/email-login/confirm")
    public R<LoginVO> confirmEmailLogin(@Validated @RequestBody EmailLoginConfirmDTO dto) {
        return R.ok(authService.confirmEmailLogin(dto));
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public R<String> register(@Validated @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return R.ok("注册成功");
    }

    @ApiOperation("用户退出")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        authAuditService.recordLogout(token);
        return R.ok();
    }

    @ApiOperation("刷新Token")
    @PostMapping("/refresh")
    public R<String> refreshToken(@RequestBody String refreshToken) {
        return R.ok(authService.refreshToken(refreshToken));
    }

    @ApiOperation("Refresh login session")
    @PostMapping("/refresh-session")
    public R<LoginVO> refreshSession(@RequestBody String refreshToken) {
        return R.ok(authService.refreshSession(refreshToken));
    }

    @ApiIgnore
    @PostMapping("/internal/provision-approved-applicant")
    public Long provisionApprovedApplicant(@Validated @RequestBody ApprovedApplicantProvisionDTO dto) {
        return authService.provisionApprovedApplicant(dto);
    }

    @ApiIgnore
    @PostMapping("/internal/accounts/by-roles")
    public List<RoleAccountDTO> queryAccountsByRoles(@RequestBody RoleRecipientQueryDTO dto) {
        return authService.queryAccountsByRoles(dto);
    }

    @ApiIgnore
    @GetMapping("/internal/admin/users/{authUserId}")
    public AdminUserProvisionDTO queryAdminUserById(@PathVariable Long authUserId) {
        return authService.queryAdminUserById(authUserId);
    }

    @ApiIgnore
    @PostMapping("/internal/admin/users")
    public Long provisionAdminUser(@RequestBody AdminUserProvisionDTO dto) {
        return authService.provisionAdminUser(dto);
    }

    @ApiIgnore
    @PutMapping("/internal/admin/users/{authUserId}")
    public void updateAdminUser(@PathVariable Long authUserId, @RequestBody AdminUserProvisionDTO dto) {
        authService.updateAdminUser(authUserId, dto);
    }

    @ApiIgnore
    @PutMapping("/internal/admin/users/{authUserId}/status/{status}")
    public void updateAdminUserStatus(@PathVariable Long authUserId, @PathVariable Integer status) {
        authService.updateAdminUserStatus(authUserId, status);
    }

    @ApiIgnore
    @PutMapping("/internal/admin/users/{authUserId}/password/default")
    public void resetAdminUserPassword(@PathVariable Long authUserId, @RequestParam String password) {
        authService.resetAdminUserPassword(authUserId, password);
    }

    @ApiIgnore
    @PutMapping("/internal/users/{authUserId}/password")
    public void updateCurrentUserPassword(@PathVariable Long authUserId, @RequestBody CurrentUserPasswordUpdateDTO dto) {
        authService.updateCurrentUserPassword(authUserId, dto);
    }

    @ApiIgnore
    @PostMapping("/internal/admin/users/{authUserId}/delete")
    public void deleteAdminUser(@PathVariable Long authUserId) {
        authService.deleteAdminUser(authUserId);
    }

    @ApiOperation("查询当前账号可切换租户")
    @GetMapping("/switchable-tenants")
    public R<List<SwitchableTenantDTO>> querySwitchableTenants(@RequestHeader("Authorization") String authorization) {
        return R.ok(authService.querySwitchableTenants(authorization));
    }

    @ApiOperation("切换当前账号租户")
    @PostMapping("/switch-tenant/{tenantId}")
    public R<LoginVO> switchTenant(@RequestHeader("Authorization") String authorization,
                                   @PathVariable Long tenantId,
                                   @RequestParam(required = false) Long roleId) {
        return R.ok(authService.switchTenant(authorization, tenantId, roleId));
    }
}
