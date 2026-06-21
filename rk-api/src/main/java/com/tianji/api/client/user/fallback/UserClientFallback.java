package com.tianji.api.client.user.fallback;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.CreditGrantDTO;
import com.tianji.api.dto.user.EmailCenterSendDTO;
import com.tianji.api.dto.user.EmailVerificationSendDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

@Slf4j
public class UserClientFallback implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        log.error("create user client fallback", cause);
        return new UserClient() {
            @Override
            public Long exchangeUserIdWithPhone(String phone) {
                return null;
            }

            @Override
            public LoginUserDTO queryUserDetail(LoginFormDTO loginDTO, boolean isStaff) {
                return null;
            }

            @Override
            public Integer queryUserType(Long id) {
                return null;
            }

            @Override
            public List<UserDTO> queryUserByIds(Iterable<Long> ids) {
                return Collections.emptyList();
            }

            @Override
            public List<UserDTO> queryUsersByAuthIds(Iterable<Long> authUserIds) {
                return Collections.emptyList();
            }

            @Override
            public UserDTO queryUserById(Long id) {
                return null;
            }

            @Override
            public Long syncUserOnRegister(UserDTO userDTO) {
                log.error("sync user on register failed, username={}", userDTO.getUsername(), cause);
                return null;
            }

            @Override
            public Boolean sendEmailVerificationCode(EmailVerificationSendDTO dto) {
                log.error("send email verification code failed, email={}, scene={}", dto.getEmail(), dto.getScene(), cause);
                return false;
            }

            @Override
            public Boolean verifyEmailVerificationCode(EmailVerificationVerifyDTO dto) {
                log.error("verify email verification code failed, email={}, scene={}", dto.getEmail(), dto.getScene(), cause);
                return false;
            }

            @Override
            public List<EmailLoginCandidateDTO> queryEmailLoginCandidates(EmailLoginCandidateQueryDTO dto) {
                log.error("query email login candidates failed, email={}, tenantId={}",
                        dto != null ? dto.getEmail() : null,
                        dto != null ? dto.getTenantId() : null,
                        cause);
                return Collections.emptyList();
            }

            @Override
            public List<String> queryRecipientEmails(TenantUserRecipientQueryDTO dto) {
                log.error("query recipient emails failed, tenantId={}, authUserIds={}, usernames={}",
                        dto != null ? dto.getTenantId() : null,
                        dto != null ? dto.getAuthUserIds() : null,
                        dto != null ? dto.getUsernames() : null,
                        cause);
                return Collections.emptyList();
            }

            @Override
            public Boolean validateReferralCodeInternal(String code, Long tenantId) {
                log.error("validate referral code failed, code={}, tenantId={}", code, tenantId, cause);
                return false;
            }

            @Override
            public Boolean markGenericReferralRegisterSuccess(Long operatorUserId, Long tenantId, String referralCode, String targetEmail, Long authUserId) {
                log.error("mark generic referral register success failed, tenantId={}, referralCode={}, targetEmail={}, authUserId={}",
                        tenantId, referralCode, targetEmail, authUserId, cause);
                return false;
            }

            @Override
            public Boolean markInvitationRegisterSuccess(String inviteToken, String referralCode, String targetEmail, Long authUserId) {
                log.error("mark invitation register success failed, inviteToken={}, referralCode={}, targetEmail={}, authUserId={}",
                        inviteToken, referralCode, targetEmail, authUserId, cause);
                return false;
            }

            @Override
            public Boolean sendEmailCenterInternal(EmailCenterSendDTO dto) {
                log.error("send email center internal failed, subject={}, roleIds={}, activityId={}, competitionId={}",
                        dto != null ? dto.getSubject() : null,
                        dto != null ? dto.getRoleIds() : null,
                        dto != null ? dto.getActivityId() : null,
                        dto != null ? dto.getCompetitionId() : null,
                        cause);
                return false;
            }

            @Override
            public Boolean saveNotificationInternal(NotificationInternalSaveDTO dto) {
                log.error("save notification internal failed, tenantId={}, title={}, type={}",
                        dto != null ? dto.getTenantId() : null,
                        dto != null ? dto.getTitle() : null,
                        dto != null ? dto.getType() : null,
                        cause);
                return false;
            }

            @Override
            public Boolean notifyRegisterSuccess(RegisterSuccessNotifyDTO dto) {
                log.error("notify register success failed, tenantId={}, username={}, email={}, referralCode={}",
                        dto != null ? dto.getTenantId() : null,
                        dto != null ? dto.getUsername() : null,
                        dto != null ? dto.getEmail() : null,
                        dto != null ? dto.getReferralCode() : null,
                        cause);
                return false;
            }

            @Override
            public TenantWorkflowConfigDTO queryCurrentTenantWorkflowConfig(Long tenantId) {
                log.error("query current tenant workflow config failed, tenantId={}", tenantId, cause);
                return null;
            }

            @Override
            public TenantSelfServiceAdmissionPolicyDTO queryTenantSelfServiceAdmissionPolicy(Long tenantId) {
                log.error("query tenant self service admission policy failed, tenantId={}", tenantId, cause);
                return null;
            }

            @Override
            public Boolean markEmailSendRecipientSuccess(Long taskId, Long recipientId) {
                log.error("mark email recipient success failed, taskId={}, recipientId={}", taskId, recipientId, cause);
                return false;
            }

            @Override
            public Boolean markEmailSendRecipientFailure(Long taskId, Long recipientId, String errorMessage) {
                log.error("mark email recipient failure failed, taskId={}, recipientId={}, errorMessage={}",
                        taskId, recipientId, errorMessage, cause);
                return false;
            }

            @Override
            public Boolean recordLoginAudit(LoginAuditRecordDTO dto) {
                log.error("record login audit failed, loginName={}, status={}", dto.getLoginName(), dto.getStatus(), cause);
                return false;
            }

            @Override
            public Integer grantCredits(List<CreditGrantDTO> grants) {
                log.error("grant credits failed, grantCount={}", grants == null ? 0 : grants.size(), cause);
                return 0;
            }
        };
    }
}
