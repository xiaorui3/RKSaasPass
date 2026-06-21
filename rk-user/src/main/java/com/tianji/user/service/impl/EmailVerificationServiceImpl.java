package com.tianji.user.service.impl;

import com.tianji.api.dto.user.EmailVerificationSendDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.RandomUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.message.api.client.AsyncEmailClient;
import com.tianji.message.api.client.EmailTemplateClient;
import com.tianji.message.api.dto.EmailTemplateDTO;
import com.tianji.message.domain.dto.EmailInfoDTO;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.service.IEmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements IEmailVerificationService {

    private static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration EMAIL_SEND_LIMIT_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate stringRedisTemplate;
    private final AsyncEmailClient asyncEmailClient;
    private final EmailTemplateClient emailTemplateClient;
    private final RKTenantMapper tenantMapper;

    @Override
    public boolean sendCode(EmailVerificationSendDTO dto) {
        String codeKey = buildCodeKey(dto.getScene(), dto.getTenantId(), dto.getEmail());
        String limitKey = buildLimitKey(dto.getScene(), dto.getTenantId(), dto.getEmail());
        if (StringUtils.isNotBlank(stringRedisTemplate.opsForValue().get(limitKey))) {
            throw new BadRequestException("验证码发送过于频繁，请稍后再试");
        }

        String code = RandomUtils.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(codeKey, code, EMAIL_CODE_TTL);
        stringRedisTemplate.opsForValue().set(limitKey, "1", EMAIL_SEND_LIMIT_TTL);

        EmailTemplateDTO template = queryTemplate(dto.getScene());
        EmailInfoDTO emailInfoDTO = new EmailInfoDTO();
        emailInfoDTO.setEmails(CollUtils.singletonList(dto.getEmail()));
        emailInfoDTO.setSubject(buildSubject(dto.getScene(), template));
        emailInfoDTO.setContent(buildHtmlContent(dto, code, template));
        emailInfoDTO.setHtml(true);
        asyncEmailClient.sendMessage(emailInfoDTO);
        log.info("邮箱验证码已发送, scene={}, tenantId={}, email={}", dto.getScene(), dto.getTenantId(), dto.getEmail());
        return true;
    }

    @Override
    public boolean verifyCode(EmailVerificationVerifyDTO dto) {
        String codeKey = buildCodeKey(dto.getScene(), dto.getTenantId(), dto.getEmail());
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (!StringUtils.equals(cachedCode, dto.getCode())) {
            throw new BadRequestException("邮箱验证码错误或已过期");
        }
        stringRedisTemplate.delete(codeKey);
        return true;
    }

    private String buildCodeKey(String scene, Long tenantId, String email) {
        return "email:verify:code:" + scene + ":" + normalizeTenantId(tenantId) + ":" + normalizeEmail(email);
    }

    private String buildLimitKey(String scene, Long tenantId, String email) {
        return "email:verify:limit:" + scene + ":" + normalizeTenantId(tenantId) + ":" + normalizeEmail(email);
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 1L : tenantId;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String buildSubject(String scene, EmailTemplateDTO template) {
        if (template != null && StringUtils.isNotBlank(template.getEmailSubject())) {
            return template.getEmailSubject();
        }
        if ("REGISTER".equalsIgnoreCase(scene)) {
            return "注册邮箱验证码";
        }
        if ("LOGIN".equalsIgnoreCase(scene)) {
            return "登录邮箱验证码";
        }
        return "邮箱验证码";
    }

    private String buildHtmlContent(EmailVerificationSendDTO dto, String code, EmailTemplateDTO template) {
        if (template != null && StringUtils.isNotBlank(template.getTemplateContent())) {
            return renderTemplate(template.getTemplateContent(), dto, code);
        }
        return "<html><body style='font-family:Arial,sans-serif;color:#333;'>"
                + "<h2>邮箱验证码</h2>"
                + "<p>您的验证码为：<strong style='font-size:24px;'>" + code + "</strong></p>"
                + "<p>验证码 5 分钟内有效，请勿泄露给他人。</p>"
                + "</body></html>";
    }

    private EmailTemplateDTO queryTemplate(String scene) {
        try {
            if ("REGISTER".equalsIgnoreCase(scene)) {
                return emailTemplateClient.queryByCode("REGISTER_VERIFY");
            }
            if ("LOGIN".equalsIgnoreCase(scene)) {
                try {
                    return emailTemplateClient.queryByCode("LOGIN_VERIFY");
                } catch (Exception ignored) {
                    return emailTemplateClient.queryByCode("REGISTER_VERIFY");
                }
            }
            return emailTemplateClient.queryByCode("JOIN_VERIFY");
        } catch (Exception e) {
            log.warn("查询邮箱验证码模板失败, scene={}, reason={}", scene, e.getMessage());
            return null;
        }
    }

    private String renderTemplate(String template, EmailVerificationSendDTO dto, String code) {
        String tenantName = resolveTenantName(dto.getTenantId());
        return template
                .replace("{{code}}", code)
                .replace("{{tenantId}}", String.valueOf(normalizeTenantId(dto.getTenantId())))
                .replace("{{tenantName}}", tenantName == null ? "" : tenantName)
                .replace("{{scene}}", dto.getScene() == null ? "" : dto.getScene());
    }

    private String resolveTenantName(Long tenantId) {
        RKTenant tenant = tenantMapper.selectById(normalizeTenantId(tenantId));
        return tenant == null ? null : tenant.getTenantName();
    }
}
