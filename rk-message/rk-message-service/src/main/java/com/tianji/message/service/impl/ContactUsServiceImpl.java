package com.tianji.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.auth.AuthClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.auth.RoleAccountDTO;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.api.dto.auth.RoleRecipientQueryDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;
import com.tianji.api.dto.user.TenantUserRecipientQueryDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.message.domain.dto.ContactPublicShieldDTO;
import com.tianji.message.domain.po.ContactUsMessage;
import com.tianji.message.mapper.ContactUsMapper;
import com.tianji.message.service.IContactUsService;
import com.tianji.message.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactUsServiceImpl extends ServiceImpl<ContactUsMapper, ContactUsMessage>
        implements IContactUsService {

    private static final String CONTACT_CACHE_PREFIX = "contact:message:";
    private static final String CONTACT_STATS_CACHE = "contact:statistics";
    private static final String CONTACT_LIMIT_PREFIX = "contact:submit:limit:";
    private static final String CONTACT_DUPLICATE_PREFIX = "contact:submit:duplicate:";
    private static final String CONTACT_PUBLIC_SHIELD_PREFIX = "contact:public:shield:";
    private static final String CONTACT_EMAIL_VERIFY_SCENE = "CONTACT";
    private static final long CACHE_EXPIRE_HOURS = 24;
    private static final int CONTACT_SUBMIT_LIMIT = 3;
    private static final long CONTACT_SUBMIT_LIMIT_WINDOW_MINUTES = 10;
    private static final long CONTACT_DUPLICATE_WINDOW_MINUTES = 30;
    private static final long CONTACT_PUBLIC_SHIELD_EXPIRE_SECONDS = 15 * 60;
    private static final long CONTACT_PUBLIC_MIN_SUBMIT_DELAY_MS = 1200;
    private static final List<String> TENANT_ADMIN_ROLE_CODES = List.of("ADMIN", "CLUB_MANAGER");

    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthClient authClient;
    private final UserClient userClient;
    private final IEmailService emailService;

    @Override
    public boolean submitMessage(ContactUsMessage message) {
        try {
            normalizeMessage(message);
            if (!acquireSubmitPermit(message)) {
                log.warn("contact message rejected by rate limit, tenantId={}, ip={}, userId={}",
                        message.getTenantId(), message.getIpAddress(), UserContext.getUser());
                return false;
            }
            if (!acquireDuplicatePermit(message)) {
                log.warn("contact message rejected by duplicate guard, tenantId={}, ip={}, userId={}",
                        message.getTenantId(), message.getIpAddress(), UserContext.getUser());
                return false;
            }

            boolean saved = save(message);
            if (saved) {
                clearStatisticsCache();
                notifyTenantAdmins(message);
                log.info("contact message saved, subject={}", message.getSubject());
            }
            return saved;
        } catch (Exception e) {
            log.error("save contact message failed", e);
            return false;
        }
    }

    @Override
    public ContactPublicShieldDTO issuePublicShield(Long tenantId, String ipAddress, String userAgent) {
        Long effectiveTenantId = tenantId == null ? Optional.ofNullable(TenantContext.getTenantId()).orElse(1L) : tenantId;
        String token = UUID.randomUUID().toString().replace("-", "");
        long issuedAt = System.currentTimeMillis();
        String payload = issuedAt + "|" + buildPublicShieldFingerprint(effectiveTenantId, ipAddress, userAgent);
        redisTemplate.opsForValue().set(
                CONTACT_PUBLIC_SHIELD_PREFIX + token,
                payload,
                CONTACT_PUBLIC_SHIELD_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );

        ContactPublicShieldDTO shield = new ContactPublicShieldDTO();
        shield.setToken(token);
        shield.setIssuedAt(issuedAt);
        shield.setMinSubmitDelayMs(CONTACT_PUBLIC_MIN_SUBMIT_DELAY_MS);
        shield.setExpiresInSeconds(CONTACT_PUBLIC_SHIELD_EXPIRE_SECONDS);
        return shield;
    }

    @Override
    public boolean submitPublicMessage(ContactUsMessage message, String shieldToken, Long issuedAt, String honeypot) {
        return submitPublicMessage(message, shieldToken, issuedAt, honeypot, null);
    }

    @Override
    public boolean submitPublicMessage(ContactUsMessage message, String shieldToken, Long issuedAt, String honeypot, String verificationCode) {
        normalizeMessage(message);
        hydrateCurrentUserContact(message);
        if (!validatePublicShield(message, shieldToken, issuedAt, honeypot)) {
            return false;
        }
        if (!verifyContactEmailCodeIfNeeded(message, verificationCode)) {
            return false;
        }
        return submitMessage(message);
    }

    @Override
    public List<ContactUsMessage> getAllMessages() {
        try {
            LambdaQueryWrapper<ContactUsMessage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(ContactUsMessage::getCreateTime);
            return list(queryWrapper);
        } catch (Exception e) {
            log.error("list all contact messages failed", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<ContactUsMessage> getMessagesByStatus(String status) {
        try {
            String cacheKey = CONTACT_CACHE_PREFIX + "status:" + status;
            @SuppressWarnings("unchecked")
            List<ContactUsMessage> cachedMessages = (List<ContactUsMessage>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedMessages != null) {
                return cachedMessages;
            }

            LambdaQueryWrapper<ContactUsMessage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ContactUsMessage::getStatus, status)
                    .orderByDesc(ContactUsMessage::getCreateTime);
            List<ContactUsMessage> messages = list(queryWrapper);
            redisTemplate.opsForValue().set(cacheKey, messages, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            return messages;
        } catch (Exception e) {
            log.error("list contact messages by status failed", e);
            return new ArrayList<>();
        }
    }

    @Override
    public ContactUsMessage getMessageById(Long id) {
        String cacheKey = CONTACT_CACHE_PREFIX + id;
        try {
            ContactUsMessage cachedMessage = (ContactUsMessage) redisTemplate.opsForValue().get(cacheKey);
            if (cachedMessage != null) {
                return cachedMessage;
            }
        } catch (Exception e) {
            log.warn("read contact message cache failed, id={}, reason={}", id, e.getMessage());
        }

        try {
            ContactUsMessage message = getById(id);
            if (message != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, message, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("write contact message cache failed, id={}, reason={}", id, e.getMessage());
                }
            }
            return message;
        } catch (Exception e) {
            log.error("get contact message detail failed", e);
            return null;
        }
    }

    @Override
    public boolean replyToMessage(Long id, String response) {
        try {
            ContactUsMessage message = getById(id);
            if (message == null) {
                return false;
            }

            message.setResponse(response);
            message.setStatus(ContactUsMessage.STATUS_REPLIED);
            message.setResponseTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());

            boolean updated = updateById(message);
            if (updated) {
                clearMessageCache(id);
                clearStatisticsCache();
                sendReplyEmail(message);
            }
            return updated;
        } catch (Exception e) {
            log.error("reply contact message failed", e);
            return false;
        }
    }

    @Override
    public boolean updateMessageStatus(Long id, String status) {
        try {
            ContactUsMessage message = getById(id);
            if (message == null) {
                return false;
            }

            message.setStatus(status);
            message.setUpdateTime(LocalDateTime.now());
            boolean updated = updateById(message);
            if (updated) {
                clearMessageCache(id);
                clearStatisticsCache();
            }
            return updated;
        } catch (Exception e) {
            log.error("update contact message status failed", e);
            return false;
        }
    }

    @Override
    public boolean deleteMessage(Long id) {
        try {
            boolean deleted = removeById(id);
            if (deleted) {
                clearMessageCache(id);
                clearStatisticsCache();
            }
            return deleted;
        } catch (Exception e) {
            log.error("delete contact message failed", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(CONTACT_STATS_CACHE);
            if (cachedStats != null) {
                return cachedStats;
            }

            List<ContactUsMessage> allMessages = getAllMessages();
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", allMessages.size());
            statistics.put("statusDistribution", allMessages.stream()
                    .collect(Collectors.groupingBy(msg -> defaultString(msg.getStatus(), "unknown"), Collectors.counting())));

            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            statistics.put("todayCount", allMessages.stream()
                    .filter(msg -> msg.getCreateTime() != null && msg.getCreateTime().isAfter(todayStart))
                    .count());

            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            statistics.put("weekCount", allMessages.stream()
                    .filter(msg -> msg.getCreateTime() != null && msg.getCreateTime().isAfter(weekStart))
                    .count());

            statistics.put("pendingCount", allMessages.stream()
                    .filter(msg -> ContactUsMessage.STATUS_PENDING.equals(msg.getStatus()))
                    .count());

            statistics.put("repliedCount", allMessages.stream()
                    .filter(msg -> ContactUsMessage.STATUS_REPLIED.equals(msg.getStatus()))
                    .count());

            redisTemplate.opsForValue().set(CONTACT_STATS_CACHE, statistics, 1, TimeUnit.HOURS);
            return statistics;
        } catch (Exception e) {
            log.error("get contact statistics failed", e);
            return new HashMap<>();
        }
    }

    private void normalizeMessage(ContactUsMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        message.setName(trimToNull(message.getName()));
        message.setEmail(defaultString(trimToNull(message.getEmail())));
        message.setTargetEmail(trimToNull(message.getTargetEmail()));
        message.setPhone(trimToNull(message.getPhone()));
        message.setSubject(trimToNull(message.getSubject()));
        message.setMessage(trimToNull(message.getMessage()));
        message.setIpAddress(trimToNull(message.getIpAddress()));
        message.setUserAgent(trimToNull(message.getUserAgent()));

        if (message.getName() == null || message.getSubject() == null || message.getMessage() == null) {
            throw new IllegalArgumentException("name, subject and message are required");
        }
        if (message.getTenantId() == null) {
            Long tenantId = TenantContext.getTenantId();
            message.setTenantId(tenantId == null ? 1L : tenantId);
        }
        if (message.getStatus() == null || message.getStatus().isBlank()) {
            message.setStatus(ContactUsMessage.STATUS_PENDING);
        }
        LocalDateTime now = LocalDateTime.now();
        if (message.getCreateTime() == null) {
            message.setCreateTime(now);
        }
        message.setUpdateTime(now);
    }

    private boolean acquireSubmitPermit(ContactUsMessage message) {
        String key = CONTACT_LIMIT_PREFIX + message.getTenantId() + ":" + resolveRateLimitIdentity(message);
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        Long count = operations.increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            redisTemplate.expire(key, CONTACT_SUBMIT_LIMIT_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        return count <= CONTACT_SUBMIT_LIMIT;
    }

    private boolean acquireDuplicatePermit(ContactUsMessage message) {
        String dedupeKey = CONTACT_DUPLICATE_PREFIX
                + message.getTenantId()
                + ":"
                + resolveRateLimitIdentity(message)
                + ":"
                + Integer.toHexString(Objects.hash(
                defaultString(message.getSubject(), "").toLowerCase(Locale.ROOT),
                defaultString(message.getMessage(), "").toLowerCase(Locale.ROOT)
        ));
        Boolean created = redisTemplate.opsForValue().setIfAbsent(
                dedupeKey,
                1,
                CONTACT_DUPLICATE_WINDOW_MINUTES,
                TimeUnit.MINUTES
        );
        return created == null || created;
    }

    private boolean validatePublicShield(ContactUsMessage message, String shieldToken, Long issuedAt, String honeypot) {
        if (honeypot != null && !honeypot.isBlank()) {
            return false;
        }
        if (shieldToken == null || shieldToken.isBlank() || issuedAt == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (issuedAt <= 0 || now - issuedAt < CONTACT_PUBLIC_MIN_SUBMIT_DELAY_MS) {
            return false;
        }

        String key = CONTACT_PUBLIC_SHIELD_PREFIX + shieldToken.trim();
        Object cachedPayload = redisTemplate.opsForValue().get(key);
        if (!(cachedPayload instanceof String)) {
            return false;
        }

        String[] parts = ((String) cachedPayload).split("\\|", 2);
        if (parts.length != 2) {
            return false;
        }

        long cachedIssuedAt;
        try {
            cachedIssuedAt = Long.parseLong(parts[0]);
        } catch (NumberFormatException ex) {
            return false;
        }

        if (cachedIssuedAt != issuedAt.longValue()) {
            return false;
        }

        String expectedFingerprint = buildPublicShieldFingerprint(message.getTenantId(), message.getIpAddress(), message.getUserAgent());
        if (!Objects.equals(parts[1], expectedFingerprint)) {
            return false;
        }

        redisTemplate.delete(key);
        return true;
    }

    private String buildPublicShieldFingerprint(Long tenantId, String ipAddress, String userAgent) {
        return new StringBuilder()
                .append(tenantId == null ? 1L : tenantId)
                .append('|')
                .append(defaultString(trimToNull(ipAddress), "unknown"))
                .append('|')
                .append(Integer.toHexString(defaultString(trimToNull(userAgent), "").hashCode()))
                .toString();
    }

    private String resolveRateLimitIdentity(ContactUsMessage message) {
        if (message.getIpAddress() != null && !message.getIpAddress().isBlank()) {
            return "ip:" + message.getIpAddress();
        }
        Long userId = UserContext.getUser();
        if (userId != null) {
            return "user:" + userId;
        }
        return "name:" + defaultString(message.getName(), "anonymous");
    }

    private void notifyTenantAdmins(ContactUsMessage message) {
        List<String> recipients = resolveContactRecipients(message);
        if (recipients.isEmpty()) {
            log.warn("no tenant admin recipients for contact message, tenantId={}", message.getTenantId());
            return;
        }
        String subject = "【租户 " + message.getTenantId() + "】新的联系我们消息：" + message.getSubject();
        String content = buildEmailContent(message);
        for (String recipient : recipients) {
            if (recipient == null || recipient.isBlank()) {
                continue;
            }
            try {
                emailService.sendHtmlEmail(recipient, subject, content);
            } catch (Exception e) {
                log.warn("send contact notification failed, tenantId={}, recipient={}, reason={}",
                        message.getTenantId(), recipient, e.getMessage());
            }
        }
    }

    private void sendReplyEmail(ContactUsMessage message) {
        if (message == null) {
            return;
        }
        String recipient = trimToNull(message.getEmail());
        if (recipient == null || trimToNull(message.getResponse()) == null) {
            return;
        }
        String subject = "联系回复: " + defaultString(message.getSubject(), "-");
        String content = "<html><body style='font-family:Arial,sans-serif;color:#1f2937;'>"
                + "<div style='max-width:720px;margin:0 auto;padding:24px;border:1px solid #e5e7eb;border-radius:12px;'>"
                + "<h2 style='margin-top:0;'>管理员已回复你的留言</h2>"
                + "<p><strong>主题：</strong>" + escapeHtml(defaultString(message.getSubject(), "-")) + "</p>"
                + "<div style='margin-top:16px;padding:16px;background:#f9fafb;border-radius:8px;white-space:pre-wrap;'>"
                + escapeHtml(message.getResponse())
                + "</div>"
                + "</div></body></html>";
        try {
            emailService.sendHtmlEmail(recipient, subject, content);
        } catch (Exception e) {
            log.warn("send contact reply email failed, id={}, recipient={}, reason={}",
                    message.getId(), recipient, e.getMessage());
        }
    }

    private void hydrateCurrentUserContact(ContactUsMessage message) {
        Long userId = UserContext.getUser();
        if (userId == null || message == null) {
            return;
        }
        try {
            UserDTO user = userClient.queryUserById(userId);
            if (user == null) {
                return;
            }
            if (message.getName() == null || message.getName().isBlank()) {
                message.setName(firstNonBlank(user.getName(), user.getUsername(), "用户-" + userId));
            }
            if (message.getEmail() == null || message.getEmail().isBlank()) {
                message.setEmail(user.getEmail());
            }
            if (message.getPhone() == null || message.getPhone().isBlank()) {
                message.setPhone(user.getCellPhone());
            }
        } catch (Exception e) {
            log.warn("hydrate current user contact failed, userId={}, reason={}", userId, e.getMessage());
        }
    }

    private boolean verifyContactEmailCodeIfNeeded(ContactUsMessage message, String verificationCode) {
        if (UserContext.getUser() != null) {
            return true;
        }
        String email = trimToNull(message == null ? null : message.getEmail());
        if (email == null) {
            return false;
        }
        if (verificationCode == null || verificationCode.isBlank()) {
            return false;
        }
        try {
            EmailVerificationVerifyDTO dto = new EmailVerificationVerifyDTO();
            dto.setTenantId(message.getTenantId());
            dto.setEmail(email);
            dto.setScene(CONTACT_EMAIL_VERIFY_SCENE);
            dto.setCode(verificationCode.trim());
            return Boolean.TRUE.equals(userClient.verifyEmailVerificationCode(dto));
        } catch (Exception e) {
            log.warn("verify contact email code failed, tenantId={}, email={}, reason={}",
                    message == null ? null : message.getTenantId(), email, e.getMessage());
            return false;
        }
    }

    private List<String> resolveTenantAdminEmails(Long tenantId) {
        if (tenantId == null) {
            return List.of();
        }
        List<RoleDTO> roles = authClient.listAllRoles(tenantId);
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = roles.stream()
                .filter(Objects::nonNull)
                .filter(role -> role.getCode() != null)
                .filter(role -> TENANT_ADMIN_ROLE_CODES.stream().anyMatch(code -> code.equalsIgnoreCase(role.getCode())))
                .map(RoleDTO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return List.of();
        }

        RoleRecipientQueryDTO queryDTO = new RoleRecipientQueryDTO();
        queryDTO.setTenantId(tenantId);
        queryDTO.setRoleIds(roleIds);
        List<RoleAccountDTO> accounts = authClient.queryAccountsByRoles(queryDTO);
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }

        List<Long> authUserIds = accounts.stream()
                .filter(Objects::nonNull)
                .map(RoleAccountDTO::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<String> usernames = accounts.stream()
                .filter(Objects::nonNull)
                .map(RoleAccountDTO::getUsername)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .collect(Collectors.toList());

        TenantUserRecipientQueryDTO dto = new TenantUserRecipientQueryDTO();
        dto.setTenantId(tenantId);
        dto.setAuthUserIds(authUserIds);
        dto.setUsernames(usernames);
        List<String> emails = userClient.queryRecipientEmails(dto);
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        return emails.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private String buildEmailContent(ContactUsMessage message) {
        return new StringBuilder()
                .append("<html><body style='font-family:Arial,sans-serif;color:#1f2937;'>")
                .append("<div style='max-width:720px;margin:0 auto;padding:24px;border:1px solid #e5e7eb;border-radius:12px;'>")
                .append("<h2 style='margin-top:0;'>收到新的联系我们留言</h2>")
                .append("<p><strong>租户ID：</strong>").append(escapeHtml(String.valueOf(message.getTenantId()))).append("</p>")
                .append("<p><strong>发送人：</strong>").append(escapeHtml(defaultString(message.getName(), "-"))).append("</p>")
                .append("<p><strong>邮箱：</strong>").append(escapeHtml(defaultString(message.getEmail(), "未提供"))).append("</p>")
                .append("<p><strong>电话：</strong>").append(escapeHtml(defaultString(message.getPhone(), "未提供"))).append("</p>")
                .append("<p><strong>登录用户ID：</strong>").append(escapeHtml(String.valueOf(UserContext.getUser()))).append("</p>")
                .append("<p><strong>来源IP：</strong>").append(escapeHtml(defaultString(message.getIpAddress(), "unknown"))).append("</p>")
                .append("<p><strong>主题：</strong>").append(escapeHtml(defaultString(message.getSubject(), "-"))).append("</p>")
                .append("<div style='margin-top:16px;padding:16px;background:#f9fafb;border-radius:8px;white-space:pre-wrap;'>")
                .append(escapeHtml(defaultString(message.getMessage(), "")))
                .append("</div>")
                .append("</div></body></html>")
                .toString();
    }

    private List<String> resolveContactRecipients(ContactUsMessage message) {
        LinkedHashSet<String> recipients = new LinkedHashSet<>();
        String targetEmail = message == null ? null : trimToNull(message.getTargetEmail());
        if (targetEmail != null) {
            recipients.add(targetEmail);
        }
        recipients.addAll(resolveTenantAdminEmails(message == null ? null : message.getTenantId()));
        return recipients.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private void clearMessageCache(Long id) {
        try {
            redisTemplate.delete(CONTACT_CACHE_PREFIX + id);
        } catch (Exception e) {
            log.warn("clear contact message cache failed", e);
        }
    }

    private void clearStatisticsCache() {
        try {
            redisTemplate.delete(CONTACT_STATS_CACHE);
            Set<String> keys = redisTemplate.keys(CONTACT_CACHE_PREFIX + "status:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("clear contact statistics cache failed", e);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
