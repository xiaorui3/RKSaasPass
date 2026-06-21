package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.api.client.activity.ActivityRecipientClient;
import com.tianji.api.client.auth.AuthClient;
import com.tianji.api.dto.activity.ActivityRegistrationRecipientDTO;
import com.tianji.api.dto.activity.CompetitionRegistrationRecipientDTO;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.auth.RoleAccountDTO;
import com.tianji.api.dto.auth.RoleRecipientQueryDTO;
import com.tianji.common.domain.R;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.message.api.client.AsyncEmailClient;
import com.tianji.message.domain.dto.EmailInfoDTO;
import com.tianji.user.domain.dto.EmailCenterRecipientResolveDTO;
import com.tianji.user.domain.dto.EmailCenterSendDTO;
import com.tianji.user.domain.dto.EmailInvitationSendDTO;
import com.tianji.user.domain.po.EmailInvitation;
import com.tianji.user.domain.po.EmailInvitationEvent;
import com.tianji.user.domain.po.EmailSendRecipient;
import com.tianji.user.domain.po.EmailSendTask;
import com.tianji.user.domain.po.ReferralCode;
import com.tianji.user.domain.po.ReferralConversionRecord;
import com.tianji.user.domain.vo.EmailInvitationVO;
import com.tianji.user.domain.vo.EmailRecipientVO;
import com.tianji.user.domain.vo.EmailSendRecipientVO;
import com.tianji.user.domain.vo.EmailSendTaskVO;
import com.tianji.user.mapper.EmailInvitationEventMapper;
import com.tianji.user.mapper.EmailInvitationMapper;
import com.tianji.user.mapper.EmailSendRecipientMapper;
import com.tianji.user.mapper.EmailSendTaskMapper;
import com.tianji.user.mapper.ReferralCodeMapper;
import com.tianji.user.mapper.ReferralConversionRecordMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.IEmailCenterService;
import com.tianji.user.utils.PublicBaseUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailCenterServiceImpl implements IEmailCenterService {

    private final AuthClient authClient;
    private final ActivityRecipientClient activityRecipientClient;
    private final UserMapper userMapper;
    private final EmailInvitationMapper emailInvitationMapper;
    private final EmailInvitationEventMapper emailInvitationEventMapper;
    private final EmailSendTaskMapper emailSendTaskMapper;
    private final EmailSendRecipientMapper emailSendRecipientMapper;
    private final ReferralConversionRecordMapper referralConversionRecordMapper;
    private final ReferralCodeMapper referralCodeMapper;
    private final AsyncEmailClient asyncEmailClient;

    @Value("${rk.email.invitation.base-url:}")
    private String invitationBaseUrl;

    @Override
    public List<EmailRecipientVO> resolveRecipients(EmailCenterRecipientResolveDTO dto) {
        Long tenantId = resolveTenantId(dto == null ? null : dto.getTenantId());
        List<EmailRecipientVO> recipients = new ArrayList<>();

        if (dto != null && dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            recipients.addAll(resolveRoleRecipients(dto.getRoleIds(), tenantId));
        }
        if (dto != null && ((dto.getAuthUserIds() != null && !dto.getAuthUserIds().isEmpty())
                || (dto.getUsernames() != null && !dto.getUsernames().isEmpty()))) {
            recipients.addAll(resolveDirectUserRecipients(tenantId, dto.getAuthUserIds(), dto.getUsernames()));
        }
        if (dto != null && dto.getActivityId() != null) {
            recipients.addAll(resolveActivityRecipients(dto.getActivityId(), tenantId));
        }
        if (dto != null && dto.getCompetitionId() != null) {
            recipients.addAll(resolveCompetitionRecipients(dto.getCompetitionId(), tenantId));
        }

        return recipients.stream()
                .filter(item -> StringUtils.isNotBlank(item.getEmail()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(EmailRecipientVO::getEmail, item -> item, (left, right) -> left, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));
    }

    @Override
    public List<String> resolveTenantRecipientEmails(Long tenantId, List<Long> authUserIds, List<String> usernames) {
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return resolveDirectUserRecipients(tenantId, authUserIds, usernames).stream()
                .map(EmailRecipientVO::getEmail)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> send(EmailCenterSendDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getSubject())) {
            throw new BadRequestException("邮件主题不能为空");
        }
        if (dto == null || StringUtils.isBlank(dto.getContent())) {
            throw new BadRequestException("邮件正文不能为空");
        }
        Long tenantId = resolveTenantId(dto.getTenantId());

        LinkedHashMap<String, String> recipientSourceMap = new LinkedHashMap<>();
        if (dto.getManualEmails() != null) {
            dto.getManualEmails().stream()
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .forEach(email -> recipientSourceMap.putIfAbsent(email, "MANUAL"));
        }

        EmailCenterRecipientResolveDTO resolveDTO = new EmailCenterRecipientResolveDTO();
        resolveDTO.setTenantId(tenantId);
        resolveDTO.setRoleIds(dto.getRoleIds());
        resolveDTO.setAuthUserIds(dto.getAuthUserIds());
        resolveDTO.setUsernames(dto.getUsernames());
        resolveDTO.setActivityId(dto.getActivityId());
        resolveDTO.setCompetitionId(dto.getCompetitionId());
        resolveRecipients(resolveDTO).stream()
                .map(EmailRecipientVO::getEmail)
                .filter(StringUtils::isNotBlank)
                .forEach(email -> recipientSourceMap.putIfAbsent(email.toLowerCase(), resolveSourceType(dto)));

        LinkedHashSet<String> recipientEmails = new LinkedHashSet<>(recipientSourceMap.keySet());

        if (recipientEmails.isEmpty()) {
            throw new BadRequestException("至少选择一个收件人");
        }

        EmailSendTask task = new EmailSendTask();
        task.setTenantId(tenantId);
        task.setSenderUserId(UserContext.getUser());
        task.setSendType("NORMAL");
        task.setRecipientMode(resolveRecipientMode(dto));
        task.setSubject(dto.getSubject());
        task.setContent(dto.getContent());
        task.setHtml(Boolean.TRUE.equals(dto.getHtml()));
        task.setStatus("QUEUED");
        task.setQueuedCount(recipientEmails.size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setIsDeleted(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        emailSendTaskMapper.insert(task);

        List<Long> recipientIds = new ArrayList<>();
        for (String email : recipientEmails) {
            EmailSendRecipient recipient = new EmailSendRecipient();
            recipient.setTenantId(tenantId);
            recipient.setTaskId(task.getId());
            recipient.setRecipientEmail(email);
            recipient.setSourceType(recipientSourceMap.get(email));
            recipient.setSendStatus("QUEUED");
            recipient.setRetryCount(0);
            recipient.setQueuedTime(LocalDateTime.now());
            recipient.setIsDeleted(0);
            recipient.setCreateTime(LocalDateTime.now());
            recipient.setUpdateTime(LocalDateTime.now());
            emailSendRecipientMapper.insert(recipient);
            recipientIds.add(recipient.getId());
        }

        EmailInfoDTO emailInfoDTO = new EmailInfoDTO();
        emailInfoDTO.setSubject(dto.getSubject());
        emailInfoDTO.setContent(dto.getContent());
        emailInfoDTO.setHtml(Boolean.TRUE.equals(dto.getHtml()));
        emailInfoDTO.setEmails(recipientEmails);
        emailInfoDTO.setTaskId(task.getId());
        emailInfoDTO.setRecipientIds(recipientIds);
        asyncEmailClient.sendMessage(emailInfoDTO);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("queuedCount", recipientEmails.size());
        result.put("recipients", new ArrayList<>(recipientEmails));
        return result;
    }

    @Override
    public Map<String, Object> sendInvitation(EmailInvitationSendDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getInvitationType())) {
            throw new BadRequestException("邀请类型不能为空");
        }
        if (dto.getManualEmails() == null || dto.getManualEmails().isEmpty()) {
            throw new BadRequestException("至少填写一个邀请邮箱");
        }

        Long tenantId = currentTenantId();
        Long senderUserId = UserContext.getUser();
        Long senderRoleId = null;
        ReferralCode referralCode = findReferralCode(dto.getReferralCode(), tenantId);

        LinkedHashSet<String> emails = dto.getManualEmails().stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (emails.isEmpty()) {
            throw new BadRequestException("至少填写一个邀请邮箱");
        }

        int queuedCount = 0;
        List<String> tokens = new ArrayList<>();
        for (String email : emails) {
            String inviteToken = UUID.randomUUID().toString().replace("-", "");
            EmailInvitation record = new EmailInvitation()
                    .setTenantId(tenantId)
                    .setSenderUserId(senderUserId)
                    .setSenderRoleId(senderRoleId)
                    .setTargetEmail(email)
                    .setInvitationType(dto.getInvitationType())
                    .setInviteToken(inviteToken)
                    .setReferralCode(referralCode == null ? null : referralCode.getCode())
                    .setReferralCodeId(referralCode == null ? null : referralCode.getId())
                    .setStatus("SENT")
                    .setConversionStatus("SENT")
                    .setAccepted(false)
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now())
                    .setIsDeleted(0);
            emailInvitationMapper.insert(record);

            String path = "JOIN".equalsIgnoreCase(dto.getInvitationType()) ? "/join" : "/register";
            String invitationUrl = PublicBaseUrlResolver.resolve(invitationBaseUrl) + path + "?inviteToken=" + inviteToken;
            String html = buildInvitationContent(dto, invitationUrl, record.getReferralCode());

            EmailInfoDTO emailInfoDTO = new EmailInfoDTO();
            emailInfoDTO.setSubject(dto.getSubject());
            emailInfoDTO.setContent(html);
            emailInfoDTO.setHtml(true);
            emailInfoDTO.setEmails(Collections.singletonList(email));
            asyncEmailClient.sendMessage(emailInfoDTO);

            queuedCount += 1;
            tokens.add(inviteToken);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("queuedCount", queuedCount);
        result.put("tokens", tokens);
        return result;
    }

    @Override
    public EmailInvitationVO getInvitation(String inviteToken) {
        EmailInvitation invitation = findInvitation(inviteToken);
        if (invitation.getOpenTime() == null) {
            LocalDateTime now = LocalDateTime.now();
            invitation.setOpenTime(now);
            if (StringUtils.isBlank(invitation.getConversionStatus()) || "SENT".equalsIgnoreCase(invitation.getConversionStatus())) {
                invitation.setConversionStatus("OPENED");
            }
            invitation.setUpdateTime(now);
            emailInvitationMapper.updateById(invitation);
            recordInvitationEvent(invitation, "OPENED", invitation.getTargetEmail(), null, null);
            upsertReferralConversion(invitation, "REGISTER".equalsIgnoreCase(invitation.getInvitationType()) ? "REGISTER" : "JOIN",
                    "OPENED", null, null, now);
        }
        return toInvitationVO(invitation);
    }

    @Override
    public Boolean acceptInvitation(String inviteToken) {
        EmailInvitation invitation = findInvitation(inviteToken);
        if (!Boolean.TRUE.equals(invitation.getAccepted())) {
            LocalDateTime now = LocalDateTime.now();
            invitation.setAccepted(true);
            invitation.setAcceptedTime(now);
            invitation.setStatus("ACCEPTED");
            invitation.setUpdateTime(now);
            emailInvitationMapper.updateById(invitation);
            recordInvitationEvent(invitation, "ACCEPTED", invitation.getTargetEmail(), null, null);
        }
        return true;
    }

    @Override
    public Boolean markInvitationRegisterSuccess(String inviteToken, String referralCode, String targetEmail, Long authUserId) {
        EmailInvitation invitation = findInvitation(inviteToken);
        if (invitation.getRegisterSuccessTime() != null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(invitation.getAccepted())) {
            invitation.setAccepted(true);
            invitation.setAcceptedTime(now);
        }
        if (invitation.getRegisterSubmitTime() == null) {
            invitation.setRegisterSubmitTime(now);
        }
        invitation.setRegisterSuccessTime(now);
        invitation.setConversionStatus("REGISTER_SUCCESS");
        invitation.setConversionUserId(authUserId);
        invitation.setStatus("ACCEPTED");
        if (StringUtils.isBlank(invitation.getReferralCode()) && StringUtils.isNotBlank(referralCode)) {
            ReferralCode code = findReferralCode(referralCode, invitation.getTenantId());
            invitation.setReferralCode(referralCode);
            invitation.setReferralCodeId(code == null ? null : code.getId());
        }
        invitation.setUpdateTime(now);
        emailInvitationMapper.updateById(invitation);

        recordInvitationEvent(invitation, "REGISTER_SUCCESS", targetEmail, authUserId, null);
        upsertReferralConversion(invitation, "REGISTER", "REGISTER_SUCCESS", authUserId, null, now);
        incrementReferralUsedCount(invitation.getReferralCode(), invitation.getTenantId());
        return true;
    }

    @Override
    public Boolean markInvitationJoinSubmitted(String inviteToken, String referralCode, String targetEmail, Long joinRequestId) {
        EmailInvitation invitation = findInvitation(inviteToken);
        if (invitation.getJoinSubmitTime() != null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(invitation.getAccepted())) {
            invitation.setAccepted(true);
            invitation.setAcceptedTime(now);
        }
        if (StringUtils.isBlank(invitation.getReferralCode()) && StringUtils.isNotBlank(referralCode)) {
            ReferralCode code = findReferralCode(referralCode, invitation.getTenantId());
            invitation.setReferralCode(referralCode);
            invitation.setReferralCodeId(code == null ? null : code.getId());
        }
        invitation.setJoinSubmitTime(now);
        invitation.setConversionStatus("JOIN_SUBMITTED");
        invitation.setConversionJoinRequestId(joinRequestId);
        invitation.setStatus("ACCEPTED");
        invitation.setUpdateTime(now);
        emailInvitationMapper.updateById(invitation);

        recordInvitationEvent(invitation, "JOIN_SUBMITTED", targetEmail, null, joinRequestId);
        upsertReferralConversion(invitation, "JOIN", "JOIN_SUBMITTED", null, joinRequestId, now);
        return true;
    }

    @Override
    public Boolean markInvitationJoinApproved(String inviteToken, String referralCode, String targetEmail, Long authUserId, Long joinRequestId) {
        EmailInvitation invitation = findInvitation(inviteToken);
        if (invitation.getJoinApprovedTime() != null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(invitation.getAccepted())) {
            invitation.setAccepted(true);
            invitation.setAcceptedTime(now);
        }
        if (invitation.getJoinSubmitTime() == null) {
            invitation.setJoinSubmitTime(now);
        }
        if (StringUtils.isBlank(invitation.getReferralCode()) && StringUtils.isNotBlank(referralCode)) {
            ReferralCode code = findReferralCode(referralCode, invitation.getTenantId());
            invitation.setReferralCode(referralCode);
            invitation.setReferralCodeId(code == null ? null : code.getId());
        }
        invitation.setJoinApprovedTime(now);
        invitation.setConversionStatus("JOIN_APPROVED");
        invitation.setConversionUserId(authUserId);
        invitation.setConversionJoinRequestId(joinRequestId);
        invitation.setStatus("ACCEPTED");
        invitation.setUpdateTime(now);
        emailInvitationMapper.updateById(invitation);

        recordInvitationEvent(invitation, "JOIN_APPROVED", targetEmail, authUserId, joinRequestId);
        upsertReferralConversion(invitation, "JOIN", "JOIN_APPROVED", authUserId, joinRequestId, now);
        incrementReferralUsedCount(invitation.getReferralCode(), invitation.getTenantId());
        return true;
    }

    @Override
    public Boolean markEmailSendRecipientSuccess(Long taskId, Long recipientId) {
        EmailSendTask task = emailSendTaskMapper.selectById(taskId);
        EmailSendRecipient recipient = emailSendRecipientMapper.selectById(recipientId);
        if (task == null || recipient == null) {
            return false;
        }
        if ("SUCCESS".equalsIgnoreCase(recipient.getSendStatus())) {
            return true;
        }

        recipient.setSendStatus("SUCCESS");
        recipient.setSentTime(LocalDateTime.now());
        recipient.setLastAttemptTime(LocalDateTime.now());
        recipient.setErrorMessage(null);
        recipient.setUpdateTime(LocalDateTime.now());
        emailSendRecipientMapper.updateById(recipient);

        task.setSuccessCount((task.getSuccessCount() == null ? 0 : task.getSuccessCount()) + 1);
        refreshTaskStatus(task);
        emailSendTaskMapper.updateById(task);
        return true;
    }

    @Override
    public Boolean markEmailSendRecipientFailure(Long taskId, Long recipientId, String errorMessage) {
        EmailSendTask task = emailSendTaskMapper.selectById(taskId);
        EmailSendRecipient recipient = emailSendRecipientMapper.selectById(recipientId);
        if (task == null || recipient == null) {
            return false;
        }
        if ("FAILED".equalsIgnoreCase(recipient.getSendStatus())) {
            return true;
        }

        recipient.setSendStatus("FAILED");
        recipient.setErrorMessage(errorMessage);
        recipient.setLastAttemptTime(LocalDateTime.now());
        recipient.setRetryCount((recipient.getRetryCount() == null ? 0 : recipient.getRetryCount()) + 1);
        recipient.setUpdateTime(LocalDateTime.now());
        emailSendRecipientMapper.updateById(recipient);

        task.setFailCount((task.getFailCount() == null ? 0 : task.getFailCount()) + 1);
        refreshTaskStatus(task);
        emailSendTaskMapper.updateById(task);
        return true;
    }

    @Override
    public List<EmailInvitationVO> listInvitations() {
        List<EmailInvitation> invitations = emailInvitationMapper.selectList(
                new LambdaQueryWrapper<EmailInvitation>()
                        .eq(EmailInvitation::getTenantId, currentTenantId())
                        .orderByDesc(EmailInvitation::getCreateTime)
                        .last("LIMIT 50")
        );

        return invitations.stream().map(this::toInvitationVO).collect(Collectors.toList());
    }

    @Override
    public List<EmailSendTaskVO> listSendTasks() {
        List<EmailSendTask> tasks = emailSendTaskMapper.selectList(
                new LambdaQueryWrapper<EmailSendTask>()
                        .eq(EmailSendTask::getTenantId, currentTenantId())
                        .orderByDesc(EmailSendTask::getCreateTime)
                        .last("LIMIT 50")
        );
        return tasks.stream().map(task -> {
            EmailSendTaskVO vo = new EmailSendTaskVO();
            vo.setId(task.getId());
            vo.setTenantId(task.getTenantId());
            vo.setSendType(task.getSendType());
            vo.setRecipientMode(task.getRecipientMode());
            vo.setSubject(task.getSubject());
            vo.setContent(task.getContent());
            vo.setHtml(task.getHtml());
            vo.setStatus(task.getStatus());
            vo.setQueuedCount(task.getQueuedCount());
            vo.setSuccessCount(task.getSuccessCount());
            vo.setFailCount(task.getFailCount());
            vo.setCreateTime(task.getCreateTime() == null ? null : task.getCreateTime().toString());
            vo.setFinishedTime(task.getFinishedTime() == null ? null : task.getFinishedTime().toString());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<EmailSendRecipientVO> listSendRecipients(Long taskId) {
        List<EmailSendRecipient> recipients = emailSendRecipientMapper.selectList(
                new LambdaQueryWrapper<EmailSendRecipient>()
                        .eq(EmailSendRecipient::getTaskId, taskId)
                        .eq(EmailSendRecipient::getTenantId, currentTenantId())
                        .orderByAsc(EmailSendRecipient::getId)
        );
        return recipients.stream().map(item -> {
            EmailSendRecipientVO vo = new EmailSendRecipientVO();
            vo.setId(item.getId());
            vo.setTaskId(item.getTaskId());
            vo.setRecipientEmail(item.getRecipientEmail());
            vo.setRecipientName(item.getRecipientName());
            vo.setSourceType(item.getSourceType());
            vo.setSendStatus(item.getSendStatus());
            vo.setErrorMessage(item.getErrorMessage());
            vo.setQueuedTime(item.getQueuedTime() == null ? null : item.getQueuedTime().toString());
            vo.setSentTime(item.getSentTime() == null ? null : item.getSentTime().toString());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<EmailRecipientVO> resolveRoleRecipients(List<Long> roleIds, Long tenantId) {
        RoleRecipientQueryDTO queryDTO = new RoleRecipientQueryDTO();
        queryDTO.setTenantId(tenantId);
        queryDTO.setRoleIds(roleIds);
        List<RoleAccountDTO> accounts = authClient.queryAccountsByRoles(queryDTO);
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> authUserIds = accounts.stream()
                .map(RoleAccountDTO::getAccountId)
                .distinct()
                .collect(Collectors.toList());
        List<String> usernames = accounts.stream()
                .map(RoleAccountDTO::getUsername)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        List<com.tianji.user.domain.po.User> users = selectTenantUsersByRecipientIdentity(tenantId, authUserIds, usernames);

        return users.stream()
                .filter(user -> StringUtils.isNotBlank(user.getEmail()))
                .map(this::toRecipient)
                .collect(Collectors.toList());
    }

    private List<EmailRecipientVO> resolveDirectUserRecipients(Long tenantId, List<Long> authUserIds, List<String> usernames) {
        List<com.tianji.user.domain.po.User> users = selectTenantUsersByRecipientIdentity(tenantId, authUserIds, usernames);
        LinkedHashMap<String, EmailRecipientVO> recipients = users.stream()
                .filter(user -> StringUtils.isNotBlank(user.getEmail()))
                .map(this::toRecipient)
                .collect(Collectors.toMap(
                        item -> item.getEmail().trim().toLowerCase(),
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<AdminUserProvisionDTO> authUsers = resolveAuthUsersForRecipientFallback(tenantId, authUserIds, users);
        authUsers.stream()
                .filter(item -> StringUtils.isNotBlank(item.getEmail()))
                .map(this::toRecipient)
                .forEach(item -> recipients.putIfAbsent(item.getEmail().trim().toLowerCase(), item));

        List<String> fallbackUsernames = authUsers.stream()
                .map(AdminUserProvisionDTO::getUsername)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (!fallbackUsernames.isEmpty()) {
            selectTenantUsersByRecipientIdentity(tenantId, Collections.emptyList(), fallbackUsernames).stream()
                    .filter(user -> StringUtils.isNotBlank(user.getEmail()))
                    .map(this::toRecipient)
                    .forEach(item -> recipients.putIfAbsent(item.getEmail().trim().toLowerCase(), item));

            authUsers.stream()
                    .map(AdminUserProvisionDTO::getUsername)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .map(username -> userMapper.selectAnyByTenantAndUsername(tenantId, username))
                    .filter(Objects::nonNull)
                    .filter(user -> StringUtils.isNotBlank(user.getEmail()))
                    .map(this::toRecipient)
                    .forEach(item -> recipients.putIfAbsent(item.getEmail().trim().toLowerCase(), item));
        }

        return new ArrayList<>(recipients.values());
    }

    private List<AdminUserProvisionDTO> resolveAuthUsersForRecipientFallback(
            Long tenantId,
            List<Long> authUserIds,
            List<com.tianji.user.domain.po.User> localUsers
    ) {
        if (authUserIds == null || authUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> locallyResolvedAuthUserIds = localUsers == null ? Collections.emptySet() : localUsers.stream()
                .map(com.tianji.user.domain.po.User::getAuthUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<AdminUserProvisionDTO> authUsers = new ArrayList<>();
        authUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(authUserId -> !locallyResolvedAuthUserIds.contains(authUserId))
                .forEach(authUserId -> {
                    try {
                        AdminUserProvisionDTO authUser = authClient.queryAdminUserById(authUserId);
                        if (authUser != null
                                && (authUser.getTenantId() == null || Objects.equals(authUser.getTenantId(), tenantId))) {
                            authUsers.add(authUser);
                        }
                    } catch (Exception ignored) {
                    }
                });
        return authUsers;
    }

    private List<com.tianji.user.domain.po.User> selectTenantUsersByRecipientIdentity(
            Long tenantId,
            List<Long> authUserIds,
            List<String> usernames
    ) {
        boolean hasAuthIds = authUserIds != null && !authUserIds.isEmpty();
        boolean hasUsernames = usernames != null && !usernames.isEmpty();
        if (!hasAuthIds && !hasUsernames) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<com.tianji.user.domain.po.User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(com.tianji.user.domain.po.User::getTenantId, tenantId)
                .and(wrapper -> {
                    boolean hasCondition = false;
                    if (hasAuthIds) {
                        wrapper.in(com.tianji.user.domain.po.User::getAuthUserId, authUserIds);
                        hasCondition = true;
                    }
                    if (hasUsernames) {
                        if (hasCondition) {
                            wrapper.or();
                        }
                        wrapper.in(com.tianji.user.domain.po.User::getUsername, usernames);
                    }
                });
        return userMapper.selectList(queryWrapper);
    }

    private List<EmailRecipientVO> resolveActivityRecipients(Long activityId, Long tenantId) {
        R<List<ActivityRegistrationRecipientDTO>> response = activityRecipientClient.getActivityRegistrations(activityId);
        List<ActivityRegistrationRecipientDTO> registrations = response == null ? Collections.emptyList() : response.getData();
        if (registrations == null || registrations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> authUserIds = registrations.stream()
                .map(ActivityRegistrationRecipientDTO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (authUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<com.tianji.user.domain.po.User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(com.tianji.user.domain.po.User::getTenantId, tenantId)
                .in(com.tianji.user.domain.po.User::getAuthUserId, authUserIds);
        List<com.tianji.user.domain.po.User> users = userMapper.selectList(queryWrapper);

        return users.stream()
                .filter(user -> StringUtils.isNotBlank(user.getEmail()))
                .map(this::toRecipient)
                .collect(Collectors.toList());
    }

    private List<EmailRecipientVO> resolveCompetitionRecipients(Long competitionId, Long tenantId) {
        R<List<CompetitionRegistrationRecipientDTO>> response = activityRecipientClient.getCompetitionRegistrations(competitionId);
        List<CompetitionRegistrationRecipientDTO> registrations = response == null ? Collections.emptyList() : response.getData();
        if (registrations == null || registrations.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmailRecipientVO> recipients = registrations.stream()
                .filter(item -> StringUtils.isNotBlank(item.getEmail()))
                .map(item -> {
                    EmailRecipientVO recipient = new EmailRecipientVO();
                    recipient.setAuthUserId(item.getUserId());
                    recipient.setUsername(item.getName());
                    recipient.setName(item.getName());
                    recipient.setEmail(item.getEmail());
                    return recipient;
                })
                .collect(Collectors.toList());
        if (!recipients.isEmpty()) {
            return recipients;
        }

        List<Long> authUserIds = registrations.stream()
                .map(CompetitionRegistrationRecipientDTO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return resolveDirectUserRecipients(tenantId, authUserIds, Collections.emptyList());
    }

    private EmailInvitation findInvitation(String inviteToken) {
        EmailInvitation invitation = emailInvitationMapper.selectOne(
                new LambdaQueryWrapper<EmailInvitation>().eq(EmailInvitation::getInviteToken, inviteToken)
        );
        if (invitation == null) {
            throw new BadRequestException("邀请不存在或已失效");
        }
        return invitation;
    }

    private ReferralCode findReferralCode(String referralCode, Long tenantId) {
        if (StringUtils.isBlank(referralCode)) {
            return null;
        }
        LambdaQueryWrapper<ReferralCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReferralCode::getCode, referralCode)
                .eq(ReferralCode::getTenantId, tenantId)
                .eq(ReferralCode::getStatus, 1);
        ReferralCode code = referralCodeMapper.selectOne(queryWrapper);
        if (code == null) {
            throw new BadRequestException("内推码不存在或已失效");
        }
        if (code.getExpiresAt() != null && code.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("内推码已过期");
        }
        if (code.getUsedCount() != null && code.getMaxUses() != null && code.getUsedCount() >= code.getMaxUses()) {
            throw new BadRequestException("内推码已达最大使用次数");
        }
        return code;
    }

    private void incrementReferralUsedCount(String referralCode, Long tenantId) {
        ReferralCode code = findReferralCode(referralCode, tenantId);
        if (code == null) {
            return;
        }
        code.setUsedCount((code.getUsedCount() == null ? 0 : code.getUsedCount()) + 1);
        code.setUpdateTime(LocalDateTime.now());
        referralCodeMapper.updateById(code);
    }

    private void recordInvitationEvent(EmailInvitation invitation, String eventType, String actorEmail, Long actorUserId, Long joinRequestId) {
        EmailInvitationEvent event = new EmailInvitationEvent()
                .setInvitationId(invitation.getId())
                .setInviteToken(invitation.getInviteToken())
                .setReferralCode(invitation.getReferralCode())
                .setEventType(eventType)
                .setEventTime(LocalDateTime.now())
                .setActorEmail(actorEmail)
                .setActorUserId(actorUserId)
                .setJoinRequestId(joinRequestId);
        event.setTenantId(invitation.getTenantId());
        event.setIsDeleted(0);
        emailInvitationEventMapper.insert(event);
    }

    private void upsertReferralConversion(EmailInvitation invitation, String conversionType, String conversionStatus,
                                          Long authUserId, Long joinRequestId, LocalDateTime convertedTime) {
        if (StringUtils.isBlank(invitation.getReferralCode())) {
            return;
        }
        ReferralConversionRecord record = referralConversionRecordMapper.selectOne(
                new LambdaQueryWrapper<ReferralConversionRecord>()
                        .eq(ReferralConversionRecord::getInviteToken, invitation.getInviteToken())
                        .last("LIMIT 1")
        );
        if (record == null) {
            record = new ReferralConversionRecord()
                    .setReferralCodeId(invitation.getReferralCodeId())
                    .setReferralCode(invitation.getReferralCode())
                    .setInvitationId(invitation.getId())
                    .setInviteToken(invitation.getInviteToken())
                    .setTargetEmail(invitation.getTargetEmail())
                    .setConversionType(conversionType);
            record.setTenantId(invitation.getTenantId());
            record.setIsDeleted(0);
            record.setCreateTime(LocalDateTime.now());
        }
        record.setConversionStatus(conversionStatus);
        record.setAuthUserId(authUserId);
        record.setJoinRequestId(joinRequestId);
        record.setConvertedTime(convertedTime);
        record.setUpdateTime(LocalDateTime.now());

        if (record.getId() == null) {
            referralConversionRecordMapper.insert(record);
        } else {
            referralConversionRecordMapper.updateById(record);
        }
    }

    private EmailInvitationVO toInvitationVO(EmailInvitation invitation) {
        EmailInvitationVO vo = new EmailInvitationVO();
        vo.setId(invitation.getId());
        vo.setTenantId(invitation.getTenantId());
        vo.setTargetEmail(invitation.getTargetEmail());
        vo.setInvitationType(invitation.getInvitationType());
        vo.setReferralCode(invitation.getReferralCode());
        vo.setAccepted(Boolean.TRUE.equals(invitation.getAccepted()));
        vo.setStatus(invitation.getStatus());
        vo.setConversionStatus(invitation.getConversionStatus());
        vo.setCreateTime(invitation.getCreateTime() == null ? null : invitation.getCreateTime().toString());
        return vo;
    }

    private EmailRecipientVO toRecipient(com.tianji.user.domain.po.User user) {
        EmailRecipientVO item = new EmailRecipientVO();
        item.setAuthUserId(user.getAuthUserId());
        item.setUsername(user.getUsername());
        item.setName(StringUtils.isNotBlank(user.getRealName()) ? user.getRealName() : user.getNickname());
        item.setEmail(user.getEmail());
        return item;
    }

    private EmailRecipientVO toRecipient(AdminUserProvisionDTO authUser) {
        EmailRecipientVO item = new EmailRecipientVO();
        item.setAuthUserId(authUser.getAuthUserId());
        item.setUsername(authUser.getUsername());
        item.setName(StringUtils.isNotBlank(authUser.getName()) ? authUser.getName() : authUser.getUsername());
        item.setEmail(authUser.getEmail());
        return item;
    }

    private String buildInvitationContent(EmailInvitationSendDTO dto, String invitationUrl, String referralCode) {
        String body = StringUtils.isNotBlank(dto.getContent()) ? dto.getContent() : "请点击下方链接完成邀请流程。";
        String referralBlock = StringUtils.isNotBlank(referralCode)
                ? "<p><strong>内推码：</strong>" + referralCode + "</p>"
                : "";
        return "<html><body style='font-family:Arial,sans-serif;color:#333;'>"
                + "<h2>" + dto.getSubject() + "</h2>"
                + "<p>" + body + "</p>"
                + referralBlock
                + "<p><a href='" + invitationUrl + "' target='_blank'>点击进入邀请页面</a></p>"
                + "<p style='color:#888;font-size:12px;'>" + invitationUrl + "</p>"
                + "</body></html>";
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private Long resolveTenantId(Long tenantId) {
        return tenantId == null ? currentTenantId() : tenantId;
    }

    private String resolveRecipientMode(EmailCenterSendDTO dto) {
        if (dto.getActivityId() != null) {
            return "ACTIVITY";
        }
        if (dto.getCompetitionId() != null) {
            return "COMPETITION";
        }
        if ((dto.getAuthUserIds() != null && !dto.getAuthUserIds().isEmpty())
                || (dto.getUsernames() != null && !dto.getUsernames().isEmpty())) {
            return "USER";
        }
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty() && dto.getManualEmails() != null && !dto.getManualEmails().isEmpty()) {
            return "MIXED";
        }
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            return "ROLE";
        }
        return "MANUAL";
    }

    private String resolveSourceType(EmailCenterSendDTO dto) {
        if (dto.getActivityId() != null) {
            return "ACTIVITY";
        }
        if (dto.getCompetitionId() != null) {
            return "COMPETITION";
        }
        if ((dto.getAuthUserIds() != null && !dto.getAuthUserIds().isEmpty())
                || (dto.getUsernames() != null && !dto.getUsernames().isEmpty())) {
            return "USER";
        }
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            return "ROLE";
        }
        return "MANUAL";
    }

    private void refreshTaskStatus(EmailSendTask task) {
        int queued = task.getQueuedCount() == null ? 0 : task.getQueuedCount();
        int success = task.getSuccessCount() == null ? 0 : task.getSuccessCount();
        int fail = task.getFailCount() == null ? 0 : task.getFailCount();
        if (success + fail >= queued) {
            task.setFinishedTime(LocalDateTime.now());
            task.setStatus(fail == 0 ? "SUCCESS" : (success == 0 ? "FAILED" : "PARTIAL_SUCCESS"));
        } else {
            task.setStatus(fail > 0 ? "PARTIAL_SUCCESS" : "QUEUED");
        }
        task.setUpdateTime(LocalDateTime.now());
    }
}
