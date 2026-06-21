package com.tianji.auth.service;

import com.tianji.api.dto.auth.ApprovedApplicantProvisionDTO;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
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

import java.util.List;

public interface IAuthService {

    LoginVO login(LoginDTO dto);

    EmailLoginPrepareVO prepareEmailLogin(EmailLoginPrepareDTO dto);

    LoginVO confirmEmailLogin(EmailLoginConfirmDTO dto);

    void register(RegisterDTO dto);

    void logout(String token);

    String refreshToken(String refreshToken);

    LoginVO refreshSession(String refreshToken);

    Long provisionApprovedApplicant(ApprovedApplicantProvisionDTO dto);

    List<RoleAccountDTO> queryAccountsByRoles(RoleRecipientQueryDTO dto);

    AdminUserProvisionDTO queryAdminUserById(Long authUserId);

    Long provisionAdminUser(AdminUserProvisionDTO dto);

    void updateAdminUser(Long authUserId, AdminUserProvisionDTO dto);

    void updateAdminUserStatus(Long authUserId, Integer status);

    void resetAdminUserPassword(Long authUserId, String password);

    void updateCurrentUserPassword(Long authUserId, CurrentUserPasswordUpdateDTO dto);

    void deleteAdminUser(Long authUserId);

    List<SwitchableTenantDTO> querySwitchableTenants(String authorization);

    LoginVO switchTenant(String authorization, Long tenantId);

    LoginVO switchTenant(String authorization, Long tenantId, Long roleId);
}
