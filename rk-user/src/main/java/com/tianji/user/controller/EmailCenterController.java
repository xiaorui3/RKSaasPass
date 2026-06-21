package com.tianji.user.controller;

import com.tianji.api.dto.user.TenantUserRecipientQueryDTO;
import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.EmailCenterRecipientResolveDTO;
import com.tianji.user.domain.dto.EmailCenterSendDTO;
import com.tianji.user.domain.dto.EmailInvitationSendDTO;
import com.tianji.user.domain.vo.EmailInvitationVO;
import com.tianji.user.domain.vo.EmailRecipientVO;
import com.tianji.user.domain.vo.EmailSendRecipientVO;
import com.tianji.user.domain.vo.EmailSendTaskVO;
import com.tianji.user.service.IEmailCenterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "Email Center API")
@RestController
@RequestMapping("/api/email-center")
@RequiredArgsConstructor
@Slf4j
public class EmailCenterController {

    private final IEmailCenterService emailCenterService;

    @ApiOperation("Resolve recipients")
    @PostMapping("/recipients/resolve")
    @PreAuthorize("hasAuthority('email:center:view')")
    public R<List<EmailRecipientVO>> resolveRecipients(@RequestBody EmailCenterRecipientResolveDTO dto) {
        return R.ok(emailCenterService.resolveRecipients(dto));
    }

    @ApiOperation("Resolve recipient emails for internal service calls")
    @PostMapping("/internal/recipient-emails")
    public List<String> resolveRecipientEmailsInternal(@RequestBody TenantUserRecipientQueryDTO dto) {
        return emailCenterService.resolveTenantRecipientEmails(
                dto == null ? null : dto.getTenantId(),
                dto == null ? null : dto.getAuthUserIds(),
                dto == null ? null : dto.getUsernames()
        );
    }

    @ApiOperation("Send email for internal service calls")
    @PostMapping("/internal/send")
    public Boolean sendInternal(@RequestBody com.tianji.api.dto.user.EmailCenterSendDTO dto) {
        try {
            emailCenterService.send(toDomainSendDTO(dto));
            return true;
        } catch (Exception e) {
            log.warn("internal send email failed, subject={}, roleIds={}, activityId={}, competitionId={}, reason={}",
                    dto == null ? null : dto.getSubject(),
                    dto == null ? null : dto.getRoleIds(),
                    dto == null ? null : dto.getActivityId(),
                    dto == null ? null : dto.getCompetitionId(),
                    e.getMessage());
            return false;
        }
    }

    @ApiOperation("Send email")
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('email:center:send')")
    public R<Map<String, Object>> send(@RequestBody EmailCenterSendDTO dto) {
        return R.ok(emailCenterService.send(dto));
    }

    @ApiOperation("Send invitation email")
    @PostMapping("/invitations/send")
    @PreAuthorize("hasAuthority('email:center:send')")
    public R<Map<String, Object>> sendInvitation(@RequestBody EmailInvitationSendDTO dto) {
        return R.ok(emailCenterService.sendInvitation(dto));
    }

    @ApiOperation("Get invitation details")
    @GetMapping("/invitations/{inviteToken}")
    public R<EmailInvitationVO> getInvitation(@PathVariable String inviteToken) {
        return R.ok(emailCenterService.getInvitation(inviteToken));
    }

    @ApiOperation("Accept invitation")
    @PostMapping("/invitations/{inviteToken}/accept")
    public R<Boolean> acceptInvitation(@PathVariable String inviteToken) {
        return R.ok(emailCenterService.acceptInvitation(inviteToken));
    }

    @ApiOperation("Mark invitation register success for internal callback")
    @PostMapping("/internal/invitations/register-success")
    public Boolean markInvitationRegisterSuccess(
            @RequestParam String inviteToken,
            @RequestParam(required = false) String referralCode,
            @RequestParam String targetEmail,
            @RequestParam Long authUserId) {
        return emailCenterService.markInvitationRegisterSuccess(inviteToken, referralCode, targetEmail, authUserId);
    }

    @ApiOperation("Mark recipient send success for internal callback")
    @PostMapping("/internal/tasks/{taskId}/recipients/{recipientId}/success")
    public Boolean markEmailSendRecipientSuccess(@PathVariable Long taskId, @PathVariable Long recipientId) {
        return emailCenterService.markEmailSendRecipientSuccess(taskId, recipientId);
    }

    @ApiOperation("Mark recipient send failure for internal callback")
    @PostMapping("/internal/tasks/{taskId}/recipients/{recipientId}/failure")
    public Boolean markEmailSendRecipientFailure(
            @PathVariable Long taskId,
            @PathVariable Long recipientId,
            @RequestParam(required = false) String errorMessage) {
        return emailCenterService.markEmailSendRecipientFailure(taskId, recipientId, errorMessage);
    }

    @ApiOperation("List send tasks")
    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('email:center:view')")
    public R<List<EmailSendTaskVO>> listSendTasks() {
        return R.ok(emailCenterService.listSendTasks());
    }

    @ApiOperation("List task recipients")
    @GetMapping("/tasks/{taskId}/recipients")
    @PreAuthorize("hasAuthority('email:center:view')")
    public R<List<EmailSendRecipientVO>> listSendRecipients(@PathVariable Long taskId) {
        return R.ok(emailCenterService.listSendRecipients(taskId));
    }

    @ApiOperation("List invitations")
    @GetMapping("/invitations")
    @PreAuthorize("hasAuthority('email:center:view')")
    public R<List<EmailInvitationVO>> listInvitations() {
        return R.ok(emailCenterService.listInvitations());
    }

    private EmailCenterSendDTO toDomainSendDTO(com.tianji.api.dto.user.EmailCenterSendDTO dto) {
        EmailCenterSendDTO target = new EmailCenterSendDTO();
        if (dto == null) {
            return target;
        }
        target.setTenantId(dto.getTenantId());
        target.setRoleIds(dto.getRoleIds());
        target.setAuthUserIds(dto.getAuthUserIds());
        target.setUsernames(dto.getUsernames());
        target.setActivityId(dto.getActivityId());
        target.setCompetitionId(dto.getCompetitionId());
        target.setManualEmails(dto.getManualEmails());
        target.setSubject(dto.getSubject());
        target.setContent(dto.getContent());
        target.setHtml(dto.getHtml());
        return target;
    }
}
