package com.tianji.api.client.user;

import com.tianji.api.client.user.fallback.UserClientFallback;
import com.tianji.api.dto.user.EmailVerificationSendDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;
import com.tianji.api.dto.user.CreditGrantDTO;
import com.tianji.api.dto.user.EmailCenterSendDTO;
import com.tianji.api.dto.user.EmailLoginCandidateDTO;
import com.tianji.api.dto.user.EmailLoginCandidateQueryDTO;
import com.tianji.api.dto.user.LoginAuditRecordDTO;
import com.tianji.api.dto.user.LoginFormDTO;
import com.tianji.api.dto.user.NotificationInternalSaveDTO;
import com.tianji.api.dto.user.RegisterSuccessNotifyDTO;
import com.tianji.api.dto.user.TenantSelfServiceAdmissionPolicyDTO;
import com.tianji.api.dto.user.TenantUserRecipientQueryDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.LoginUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "rk-user", fallbackFactory = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/users/ids")
    Long exchangeUserIdWithPhone(@RequestParam("phone") String phone);

    @PostMapping("/users/detail/{isStaff}")
    LoginUserDTO queryUserDetail(@RequestBody LoginFormDTO loginDTO, @PathVariable("isStaff") boolean isStaff);

    @GetMapping("/users/{id}/type")
    Integer queryUserType(@PathVariable("id") Long id);

    @GetMapping("/users/list")
    List<UserDTO> queryUserByIds(@RequestParam("ids") Iterable<Long> ids);

    @GetMapping("/users/internal/by-auth-ids")
    List<UserDTO> queryUsersByAuthIds(@RequestParam("authUserIds") Iterable<Long> authUserIds);

    @GetMapping("/users/{id}")
    UserDTO queryUserById(@PathVariable("id") Long id);

    @PostMapping("/users/internal/sync")
    Long syncUserOnRegister(@RequestBody UserDTO userDTO);

    @PostMapping("/api/email-verification/internal/send")
    Boolean sendEmailVerificationCode(@RequestBody EmailVerificationSendDTO dto);

    @PostMapping("/api/email-verification/internal/verify")
    Boolean verifyEmailVerificationCode(@RequestBody EmailVerificationVerifyDTO dto);

    @PostMapping("/users/internal/email-login-candidates")
    List<EmailLoginCandidateDTO> queryEmailLoginCandidates(@RequestBody EmailLoginCandidateQueryDTO dto);

    @PostMapping("/users/internal/recipient-emails")
    List<String> queryRecipientEmails(@RequestBody TenantUserRecipientQueryDTO dto);

    @PostMapping("/api/referral-codes/internal/validate")
    Boolean validateReferralCodeInternal(@RequestParam("code") String code, @RequestParam("tenantId") Long tenantId);

    @PostMapping("/api/referral-codes/internal/register-success")
    Boolean markGenericReferralRegisterSuccess(
            @RequestHeader("user-info") Long operatorUserId,
            @RequestParam("tenantId") Long tenantId,
            @RequestParam("referralCode") String referralCode,
            @RequestParam("targetEmail") String targetEmail,
            @RequestParam("authUserId") Long authUserId
    );

    @PostMapping("/api/email-center/internal/invitations/register-success")
    Boolean markInvitationRegisterSuccess(
            @RequestParam("inviteToken") String inviteToken,
            @RequestParam(value = "referralCode", required = false) String referralCode,
            @RequestParam("targetEmail") String targetEmail,
            @RequestParam("authUserId") Long authUserId
    );

    @PostMapping("/api/email-center/internal/send")
    Boolean sendEmailCenterInternal(@RequestBody EmailCenterSendDTO dto);

    @PostMapping("/api/notifications/internal/save")
    Boolean saveNotificationInternal(@RequestBody NotificationInternalSaveDTO dto);

    @PostMapping("/api/admission/internal/register-success-notify")
    Boolean notifyRegisterSuccess(@RequestBody RegisterSuccessNotifyDTO dto);

    @GetMapping("/api/workflow/config/current/internal")
    TenantWorkflowConfigDTO queryCurrentTenantWorkflowConfig(@RequestParam("tenantId") Long tenantId);

    @GetMapping("/api/config/tenant-self-service/internal/admission-policy")
    TenantSelfServiceAdmissionPolicyDTO queryTenantSelfServiceAdmissionPolicy(@RequestParam("tenantId") Long tenantId);

    @PostMapping("/api/email-center/internal/tasks/{taskId}/recipients/{recipientId}/success")
    Boolean markEmailSendRecipientSuccess(@PathVariable("taskId") Long taskId, @PathVariable("recipientId") Long recipientId);

    @PostMapping("/api/email-center/internal/tasks/{taskId}/recipients/{recipientId}/failure")
    Boolean markEmailSendRecipientFailure(
            @PathVariable("taskId") Long taskId,
            @PathVariable("recipientId") Long recipientId,
            @RequestParam(value = "errorMessage", required = false) String errorMessage
    );

    @PostMapping("/api/audit/internal/logininfor")
    Boolean recordLoginAudit(@RequestBody LoginAuditRecordDTO dto);

    @PostMapping("/api/credit/internal/grants/batch")
    Integer grantCredits(@RequestBody List<CreditGrantDTO> grants);
}
