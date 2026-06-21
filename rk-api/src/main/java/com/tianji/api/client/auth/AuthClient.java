package com.tianji.api.client.auth;

import com.tianji.api.dto.auth.ApprovedApplicantProvisionDTO;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.auth.CurrentUserPasswordUpdateDTO;
import com.tianji.api.dto.auth.RoleAccountDTO;
import com.tianji.api.dto.auth.RoleRecipientQueryDTO;
import com.tianji.api.dto.auth.RoleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("rk-auth")
public interface AuthClient {

    @GetMapping("/roles/{id}")
    RoleDTO queryRoleById(@PathVariable("id") Long id);

    @GetMapping("/roles/list")
    List<RoleDTO> listAllRoles(@RequestParam(value = "tenantId", required = false) Long tenantId);

    @PostMapping("/auth/internal/provision-approved-applicant")
    Long provisionApprovedApplicant(@RequestBody ApprovedApplicantProvisionDTO dto);

    @PostMapping("/auth/internal/accounts/by-roles")
    List<RoleAccountDTO> queryAccountsByRoles(@RequestBody RoleRecipientQueryDTO dto);

    @GetMapping("/auth/internal/admin/users/{authUserId}")
    AdminUserProvisionDTO queryAdminUserById(@PathVariable("authUserId") Long authUserId);

    @PostMapping("/auth/internal/admin/users")
    Long provisionAdminUser(@RequestBody AdminUserProvisionDTO dto);

    @PutMapping("/auth/internal/admin/users/{authUserId}")
    void updateAdminUser(@PathVariable("authUserId") Long authUserId, @RequestBody AdminUserProvisionDTO dto);

    @PutMapping("/auth/internal/admin/users/{authUserId}/status/{status}")
    void updateAdminUserStatus(@PathVariable("authUserId") Long authUserId, @PathVariable("status") Integer status);

    @PutMapping("/auth/internal/admin/users/{authUserId}/password/default")
    void resetAdminUserPassword(@PathVariable("authUserId") Long authUserId, @RequestParam("password") String password);

    @PutMapping("/auth/internal/users/{authUserId}/password")
    void updateCurrentUserPassword(@PathVariable("authUserId") Long authUserId, @RequestBody CurrentUserPasswordUpdateDTO dto);

    @PostMapping("/auth/internal/admin/users/{authUserId}/delete")
    void deleteAdminUser(@PathVariable("authUserId") Long authUserId);
}
