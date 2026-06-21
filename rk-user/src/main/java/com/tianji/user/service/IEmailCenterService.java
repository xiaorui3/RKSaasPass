package com.tianji.user.service;

import com.tianji.user.domain.dto.EmailCenterRecipientResolveDTO;
import com.tianji.user.domain.dto.EmailCenterSendDTO;
import com.tianji.user.domain.dto.EmailInvitationSendDTO;
import com.tianji.user.domain.vo.EmailRecipientVO;
import com.tianji.user.domain.vo.EmailInvitationVO;
import com.tianji.user.domain.vo.EmailSendRecipientVO;
import com.tianji.user.domain.vo.EmailSendTaskVO;

import java.util.List;
import java.util.Map;

public interface IEmailCenterService {

    List<EmailRecipientVO> resolveRecipients(EmailCenterRecipientResolveDTO dto);

    List<String> resolveTenantRecipientEmails(Long tenantId, List<Long> authUserIds, List<String> usernames);

    Map<String, Object> send(EmailCenterSendDTO dto);

    Map<String, Object> sendInvitation(EmailInvitationSendDTO dto);

    EmailInvitationVO getInvitation(String inviteToken);

    Boolean acceptInvitation(String inviteToken);

    Boolean markInvitationRegisterSuccess(String inviteToken, String referralCode, String targetEmail, Long authUserId);

    Boolean markInvitationJoinSubmitted(String inviteToken, String referralCode, String targetEmail, Long joinRequestId);

    Boolean markInvitationJoinApproved(String inviteToken, String referralCode, String targetEmail, Long authUserId, Long joinRequestId);

    Boolean markEmailSendRecipientSuccess(Long taskId, Long recipientId);

    Boolean markEmailSendRecipientFailure(Long taskId, Long recipientId, String errorMessage);

    List<EmailSendTaskVO> listSendTasks();

    List<EmailSendRecipientVO> listSendRecipients(Long taskId);

    List<EmailInvitationVO> listInvitations();
}
