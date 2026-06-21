/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.baomidou.mybatisplus.core.toolkit.support.SFunction
 *  com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.tianji.api.client.auth.AuthClient
 *  com.tianji.api.dto.auth.ApprovedApplicantProvisionDTO
 *  com.tianji.api.dto.auth.RoleAccountDTO
 *  com.tianji.api.dto.auth.RoleDTO
 *  com.tianji.api.dto.auth.RoleRecipientQueryDTO
 *  com.tianji.api.dto.user.EmailVerificationVerifyDTO
 *  com.tianji.api.dto.user.TenantWorkflowConfigDTO
 *  com.tianji.api.dto.user.UserDTO
 *  com.tianji.api.dto.user.WorkflowPolicyDTO
 *  com.tianji.common.enums.UserType
 *  com.tianji.common.exceptions.BadRequestException
 *  com.tianji.common.utils.StringUtils
 *  com.tianji.common.utils.TenantContext
 *  com.tianji.message.api.client.EmailTemplateClient
 *  com.tianji.message.api.dto.EmailTemplateDTO
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.data.redis.core.RedisTemplate
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.security.crypto.password.PasswordEncoder
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.tianji.api.client.auth.AuthClient;
import com.tianji.api.dto.auth.ApprovedApplicantProvisionDTO;
import com.tianji.api.dto.auth.RoleAccountDTO;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.api.dto.auth.RoleRecipientQueryDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.message.api.client.EmailTemplateClient;
import com.tianji.message.api.dto.EmailTemplateDTO;
import com.tianji.user.domain.dto.EmailCenterSendDTO;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.JoinRequest;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.ReferralCode;
import com.tianji.user.domain.po.RegisterReviewRequest;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.domain.po.User;
import com.tianji.user.enums.UserStatus;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.JoinRequestMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.ReferralCodeMapper;
import com.tianji.user.mapper.RegisterReviewRequestMapper;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.domain.dto.TenantSelfServiceConfigDTO;
import com.tianji.user.service.IAdmissionService;
import com.tianji.user.service.IEmailCenterService;
import com.tianji.user.service.IEmailVerificationService;
import com.tianji.user.service.IReferralCodeService;
import com.tianji.user.service.ITenantWorkflowConfigService;
import com.tianji.user.service.IUserService;
import com.tianji.user.service.impl.ReviewActionTokenServiceImpl;
import com.tianji.user.utils.PublicBaseUrlResolver;
import java.beans.ConstructorProperties;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionServiceImpl
extends ServiceImpl<JoinRequestMapper, JoinRequest>
implements IAdmissionService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(AdmissionServiceImpl.class);
    private static final String ADMISSION_CACHE_PREFIX = "admission:application:";
    private static final String ADMISSION_STATS_CACHE = "admission:statistics";
    private static final long CACHE_EXPIRE_HOURS = 2L;
    private static final String DEFAULT_APPROVED_PASSWORD = "123456";
    private static final String TENANT_SELF_SERVICE_CONFIG_KEY = "tenant.self-service.config";
    private static final String REVIEW_STATUS_PENDING = "\u5f85\u5ba1\u6838";
    private static final String REVIEW_STATUS_APPROVED = "\u901a\u8fc7";
    private static final Long PLATFORM_SUPER_ADMIN_TENANT_ID = 1L;
    private static final List<String> TENANT_ADMIN_ROLE_CODES = List.of("ADMIN", "CLUB_MANAGER");
    private static final List<String> PLATFORM_SUPER_ADMIN_ROLE_CODES = List.of("ADMIN");
    private static final Set<String> ADMIN_EMAILS = Set.of("3505469466@qq.com", "ruimeilademaye@163.com", "2148906016@qq.com", "lxy521521456@163.com", "w87027619332@163.com");
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuthClient authClient;
    private final IEmailCenterService emailCenterService;
    private final IEmailVerificationService emailVerificationService;
    private final IReferralCodeService referralCodeService;
    private final IUserService userService;
    private final UserMapper userMapper;
    private final RKTenantMapper tenantMapper;
    private final ReferralCodeMapper referralCodeMapper;
    private final ClubMemberMapper clubMemberMapper;
    private final EmailTemplateClient emailTemplateClient;
    private final RegisterReviewRequestMapper registerReviewRequestMapper;
    private final ReviewActionTokenServiceImpl reviewActionTokenService;
    private final ITenantWorkflowConfigService tenantWorkflowConfigService;
    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    @Value(value="${rk.web.public-base-url:}")
    private String publicWebBaseUrl;

    @Override
    @Transactional
    public boolean submitApplication(JoinRequest joinRequest) {
        try {
            this.ensureJoinApplicationOpen(joinRequest);
            this.verifyJoinEmailCode(joinRequest);
            this.prepareJoinRequestCredentials(joinRequest);
            this.validateGenericReferral(joinRequest);
            joinRequest.setFormPayloadJson(this.writeJson(this.normalizeJoinFormPayload(joinRequest)));
            JoinRequest existingApplication = this.resolveExistingJoinApplicationForSubmit(joinRequest);
            if (existingApplication != null && REVIEW_STATUS_PENDING.equals(existingApplication.getReviewStatus())) {
                return this.updatePendingJoinApplication(existingApplication, joinRequest);
            }
            if (existingApplication != null && REVIEW_STATUS_APPROVED.equals(existingApplication.getReviewStatus())) {
                throw new BadRequestException("\u60a8\u5df2\u662f\u5f53\u524d\u793e\u56e2\u6210\u5458\uff0c\u8bf7\u52ff\u91cd\u590d\u7533\u8bf7");
            }
            joinRequest.setApplicationTime(LocalDateTime.now());
            joinRequest.setReviewStatus(REVIEW_STATUS_PENDING);
            joinRequest.setEmailSent(false);
            joinRequest.setCreateTime(LocalDateTime.now());
            joinRequest.setUpdateTime(LocalDateTime.now());
            boolean saved = this.save(joinRequest);
            if (saved) {
                this.recordJoinSubmittedTracking(joinRequest);
                this.notifyJoinSubmitted(joinRequest);
                this.clearStatisticsCache();
                this.clearApplicationCache(joinRequest.getStudentId());
                log.info("\u2705 \u5165\u793e\u7533\u8bf7\u63d0\u4ea4\u6210\u529f\uff1a\u5b66\u53f7={}, \u59d3\u540d={}", (Object)joinRequest.getStudentId(), (Object)joinRequest.getName());
            }
            return saved;
        }
        catch (Exception e) {
            log.error("\u63d0\u4ea4\u5165\u793e\u7533\u8bf7\u5931\u8d25", (Throwable)e);
            throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
        }
    }

    private void ensureJoinApplicationOpen(JoinRequest joinRequest) {
        Long tenantId = joinRequest != null && joinRequest.getTenantId() != null ? joinRequest.getTenantId() : this.currentTenantId();
        TenantSelfServiceConfigDTO config = this.loadTenantSelfServiceConfig(tenantId);
        if (config != null
                && config.getAdmissionSettings() != null
                && Boolean.FALSE.equals(config.getAdmissionSettings().getAllowJoinApplication())) {
            throw new BadRequestException("当前租户已关闭入社申请");
        }
    }

    private TenantSelfServiceConfigDTO loadTenantSelfServiceConfig(Long tenantId) {
        if (systemConfigMapper == null || objectMapper == null) {
            return null;
        }
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, tenantId == null ? 1L : tenantId)
                .eq(SystemConfig::getConfigKey, TENANT_SELF_SERVICE_CONFIG_KEY)
                .last("LIMIT 1"));
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(config.getConfigValue(), TenantSelfServiceConfigDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("load tenant self service config failed, tenantId={}, reason={}", tenantId, e.getMessage());
            return null;
        }
    }

    private JoinRequest resolveExistingJoinApplicationForSubmit(JoinRequest joinRequest) {
        JoinRequest existingByStudentId = this.getApplicationByStudentId(joinRequest.getStudentId());
        if (existingByStudentId != null) {
            return existingByStudentId;
        }
        if (joinRequest == null || joinRequest.getSourceAuthUserId() == null) {
            return null;
        }
        LambdaQueryWrapper<JoinRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(JoinRequest::getTenantId, joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L)
                .eq(JoinRequest::getSourceAuthUserId, joinRequest.getSourceAuthUserId())
                .eq(JoinRequest::getIsDeleted, 0)
                .orderByDesc(JoinRequest::getApplicationTime)
                .orderByDesc(JoinRequest::getId)
                .last("LIMIT 1");
        return this.getOne(queryWrapper);
    }

    private boolean updatePendingJoinApplication(JoinRequest existingApplication, JoinRequest joinRequest) {
        String previousStudentId = existingApplication.getStudentId();
        existingApplication.setName(joinRequest.getName());
        existingApplication.setStudentId(joinRequest.getStudentId());
        existingApplication.setMajor(joinRequest.getMajor());
        existingApplication.setGrade(joinRequest.getGrade());
        existingApplication.setPhone(joinRequest.getPhone());
        existingApplication.setEmail(joinRequest.getEmail());
        existingApplication.setUsername(joinRequest.getUsername());
        existingApplication.setPasswordHash(joinRequest.getPasswordHash());
        existingApplication.setInviteToken(joinRequest.getInviteToken());
        existingApplication.setReferralCode(joinRequest.getReferralCode());
        existingApplication.setSourceAuthUserId(joinRequest.getSourceAuthUserId());
        existingApplication.setInterest(joinRequest.getInterest());
        existingApplication.setExperience(joinRequest.getExperience());
        existingApplication.setFormPayloadJson(joinRequest.getFormPayloadJson());
        existingApplication.setApplicationTime(LocalDateTime.now());
        existingApplication.setUpdateTime(LocalDateTime.now());
        boolean updated = this.updateById(existingApplication);
        if (updated) {
            this.recordJoinSubmittedTracking(existingApplication);
            this.notifyJoinSubmitted(existingApplication);
            this.clearApplicationCache(previousStudentId);
            this.clearApplicationCache(existingApplication.getStudentId());
            this.clearStatisticsCache();
        }
        return updated;
    }

    @Override
    public Map<String, Object> checkApplicationStatus(String studentId, String email) {
        try {
            String cacheKey = "admission:application:status:" + studentId;
            Map cachedResult = (Map)this.redisTemplate.opsForValue().get((Object)cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
            HashMap<String, Object> result = new HashMap<String, Object>();
            JoinRequest joinRequest = this.getApplicationByStudentId(studentId);
            if (joinRequest == null) {
                result.put("status", "not_applied");
                result.put("message", "\u60a8\u8fd8\u672a\u63d0\u4ea4\u7533\u8bf7");
                result.put("redirectUrl", "/2025/index_2025.html");
            } else if (!joinRequest.getEmail().equals(email)) {
                result.put("status", "email_mismatch");
                result.put("message", "\u90ae\u7bb1\u4e0e\u7533\u8bf7\u65f6\u4e0d\u5339\u914d");
                result.put("redirectUrl", "/2025/index_2025.html");
            } else {
                switch (joinRequest.getReviewStatus()) {
                    case "\u5f85\u5ba1\u6838": {
                        result.put("status", "pending");
                        result.put("message", "\u60a8\u7684\u7533\u8bf7\u6b63\u5728\u5ba1\u6838\u4e2d");
                        break;
                    }
                    case "\u901a\u8fc7": {
                        result.put("status", "approved");
                        result.put("message", "\u606d\u559c\u60a8\uff01\u7533\u8bf7\u5df2\u901a\u8fc7");
                        result.put("loginReady", joinRequest.getAuthUserId() != null);
                        result.put("username", joinRequest.getUsername());
                        break;
                    }
                    case "\u672a\u901a\u8fc7": {
                        result.put("status", "rejected");
                        result.put("message", "\u5f88\u9057\u61be\uff0c\u60a8\u7684\u7533\u8bf7\u672a\u901a\u8fc7");
                        if (joinRequest.getReviewComment() == null) break;
                        result.put("reason", joinRequest.getReviewComment());
                        break;
                    }
                    default: {
                        result.put("status", "unknown");
                        result.put("message", "\u7533\u8bf7\u72b6\u6001\u5f02\u5e38");
                    }
                }
            }
            if (this.isAdminUser(studentId, email)) {
                result.put("isAdmin", true);
                result.put("adminStatus", "admin");
            }
            this.redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            return result;
        }
        catch (Exception e) {
            log.error("\u68c0\u67e5\u7533\u8bf7\u72b6\u6001\u5931\u8d25", (Throwable)e);
            HashMap<String, Object> errorResult = new HashMap<String, Object>();
            errorResult.put("status", "error");
            errorResult.put("message", "\u68c0\u67e5\u7533\u8bf7\u72b6\u6001\u5931\u8d25: " + e.getMessage());
            return errorResult;
        }
    }

    @Override
    @Transactional
    public boolean reviewApplication(Long id, String reviewStatus, String reviewComment, Long reviewerId) {
        try {
            JoinRequest joinRequest = (JoinRequest)this.getById(id);
            if (joinRequest == null) {
                log.warn("\u5ba1\u6838\u5931\u8d25\uff1a\u7533\u8bf7\u4e0d\u5b58\u5728\uff0cID={}", (Object)id);
                return false;
            }
            boolean approved = "\u901a\u8fc7".equals(reviewStatus);
            if (approved) {
                this.provisionApprovedApplicant(joinRequest);
            }
            joinRequest.setReviewStatus(reviewStatus);
            joinRequest.setReviewComment(reviewComment);
            joinRequest.setReviewerId(reviewerId);
            joinRequest.setReviewTime(LocalDateTime.now());
            joinRequest.setUpdateTime(LocalDateTime.now());
            boolean updated = this.updateById(joinRequest);
            if (updated) {
                if (approved) {
                    this.recordJoinApprovedTracking(joinRequest);
                }
                this.notifyJoinReviewResult(joinRequest, approved);
                this.clearApplicationCache(joinRequest.getStudentId());
                this.clearStatisticsCache();
                log.info("\u2705 \u7533\u8bf7\u5ba1\u6838\u5b8c\u6210\uff1a\u5b66\u53f7={}, \u72b6\u6001={}, \u5ba1\u6838\u4ebaID={}", new Object[]{joinRequest.getStudentId(), reviewStatus, reviewerId});
            }
            return updated;
        }
        catch (Exception e) {
            log.error("\u5ba1\u6838\u7533\u8bf7\u5931\u8d25", (Throwable)e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean reviewRegisterRequest(Long id, String reviewStatus, String reviewComment, Long reviewerAuthUserId) {
        try {
            RegisterReviewRequest request = (RegisterReviewRequest)this.registerReviewRequestMapper.selectById(id);
            if (request == null) {
                log.warn("register review request not found, id={}", (Object)id);
                return false;
            }
            boolean approved = "APPROVED".equalsIgnoreCase(reviewStatus);
            if (approved) {
                this.provisionApprovedRegisterReview(request);
            }
            request.setReviewStatus(reviewStatus);
            request.setReviewComment(reviewComment);
            request.setReviewerAuthUserId(reviewerAuthUserId);
            request.setReviewTime(LocalDateTime.now());
            request.setUpdateTime(LocalDateTime.now());
            this.registerReviewRequestMapper.updateById(request);
            if (approved && request.getAuthUserId() != null) {
                this.authClient.updateAdminUserStatus(request.getAuthUserId(), Integer.valueOf(1));
            }
            this.notifyRegisterReviewResult(request, approved);
            this.clearStatisticsCache();
            return true;
        }
        catch (Exception e) {
            log.error("review register request failed, id={}", (Object)id, (Object)e);
            return false;
        }
    }

    @Override
    public List<JoinRequest> getAllApplications() {
        try {
            repairApprovedApplicationsForCurrentTenant();
            LambdaQueryWrapper<JoinRequest> queryWrapper = new LambdaQueryWrapper<>();
            applyReadableTenantScope(queryWrapper)
                    .orderByDesc(JoinRequest::getApplicationTime);
            return list(queryWrapper);
        }
        catch (Exception e) {
            log.error("\u83b7\u53d6\u6240\u6709\u7533\u8bf7\u5931\u8d25", (Throwable)e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<JoinRequest> getApplicationsByStatus(String status) {
        try {
            LambdaQueryWrapper<JoinRequest> queryWrapper = new LambdaQueryWrapper<>();
            applyReadableTenantScope(queryWrapper)
                    .eq(JoinRequest::getReviewStatus, status)
                    .orderByDesc(JoinRequest::getApplicationTime);
            return list(queryWrapper);
        }
        catch (Exception e) {
            log.error("\u6309\u72b6\u6001\u83b7\u53d6\u7533\u8bf7\u5931\u8d25", (Throwable)e);
            return new ArrayList<>();
        }
    }

    @Override
    public JoinRequest getApplicationByStudentId(String studentId) {
        try {
            String cacheKey = ADMISSION_CACHE_PREFIX + "student:" + studentId;
            JoinRequest cachedApplication = (JoinRequest) redisTemplate.opsForValue().get(cacheKey);
            if (cachedApplication != null) {
                return cachedApplication;
            }

            LambdaQueryWrapper<JoinRequest> queryWrapper = new LambdaQueryWrapper<>();
            applyTenantScope(queryWrapper)
                    .eq(JoinRequest::getStudentId, studentId)
                    .orderByDesc(JoinRequest::getApplicationTime)
                    .last("LIMIT 1");
            JoinRequest joinRequest = getOne(queryWrapper);
            if (joinRequest != null) {
                redisTemplate.opsForValue().set(cacheKey, joinRequest, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            return joinRequest;
        }
        catch (Exception e) {
            log.error("\u6839\u636e\u5b66\u53f7\u83b7\u53d6\u7533\u8bf7\u5931\u8d25", (Throwable)e);
            return null;
        }
    }

    @Override
    public boolean deleteApplication(Long id) {
        try {
            JoinRequest joinRequest = (JoinRequest)this.getById(id);
            boolean deleted = this.removeById(id);
            if (deleted && joinRequest != null) {
                this.clearApplicationCache(joinRequest.getStudentId());
                this.clearStatisticsCache();
                log.info("\u2705 \u7533\u8bf7\u5220\u9664\u6210\u529f\uff1a\u5b66\u53f7={}", (Object)joinRequest.getStudentId());
            }
            return deleted;
        }
        catch (Exception e) {
            log.error("\u5220\u9664\u7533\u8bf7\u5931\u8d25", (Throwable)e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getApplicationStatistics() {
        try {
            String statsCacheKey = buildStatisticsCacheKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(statsCacheKey);
            if (isStatisticsCacheCompatible(cachedStats)) {
                return cachedStats;
            }

            List<JoinRequest> allApplications = getAllApplications();
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", allApplications.size());
            statistics.put("statusDistribution", allApplications.stream()
                    .collect(Collectors.groupingBy(
                            app -> app.getReviewStatus() != null ? app.getReviewStatus() : "unknown",
                            Collectors.counting()
                    )));

            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            statistics.put("todayCount", allApplications.stream()
                    .filter(app -> app.getApplicationTime().isAfter(todayStart))
                    .count());

            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            statistics.put("weekCount", allApplications.stream()
                    .filter(app -> app.getApplicationTime().isAfter(weekStart))
                    .count());

            statistics.put("pendingCount", allApplications.stream().filter(app -> REVIEW_STATUS_PENDING.equals(app.getReviewStatus())).count());
            statistics.put("approvedCount", allApplications.stream().filter(app -> REVIEW_STATUS_APPROVED.equals(app.getReviewStatus())).count());
            statistics.put("approvedUniqueApplicantCount", countUniqueApprovedJoinApplicants(allApplications));
            statistics.put("rejectedCount", allApplications.stream().filter(app -> "\u672a\u901a\u8fc7".equals(app.getReviewStatus())).count());

            redisTemplate.opsForValue().set(statsCacheKey, statistics, 1L, TimeUnit.HOURS);
            return statistics;
        }
        catch (Exception e) {
            log.error("\u83b7\u53d6\u7533\u8bf7\u7edf\u8ba1\u6570\u636e\u5931\u8d25", (Throwable)e);
            return new HashMap<>();
        }
    }

    private boolean isStatisticsCacheCompatible(Map<String, Object> cachedStats) {
        return cachedStats != null && cachedStats.containsKey("approvedUniqueApplicantCount");
    }

    private long countUniqueApprovedJoinApplicants(List<JoinRequest> applications) {
        if (applications == null || applications.isEmpty()) {
            return 0L;
        }
        return applications.stream()
                .filter(app -> app != null && REVIEW_STATUS_APPROVED.equals(app.getReviewStatus()))
                .map(this::buildJoinApplicantIdentityKey)
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .count();
    }

    private String buildJoinApplicantIdentityKey(JoinRequest application) {
        Long tenantId = application.getTenantId() != null ? application.getTenantId() : 1L;
        if (application.getAuthUserId() != null) {
            return tenantId + ":auth:" + application.getAuthUserId();
        }
        if (application.getSourceAuthUserId() != null) {
            return tenantId + ":source:" + application.getSourceAuthUserId();
        }
        if (application.getStudentId() != null && !application.getStudentId().isBlank()) {
            return tenantId + ":student:" + application.getStudentId().trim().toLowerCase();
        }
        if (application.getEmail() != null && !application.getEmail().isBlank()) {
            return tenantId + ":email:" + application.getEmail().trim().toLowerCase();
        }
        return application.getId() == null ? null : tenantId + ":request:" + application.getId();
    }

    @Override
    public boolean validateEmailMatch(String studentId, String email) {
        try {
            JoinRequest joinRequest = this.getApplicationByStudentId(studentId);
            return joinRequest != null && joinRequest.getEmail().equals(email);
        }
        catch (Exception e) {
            log.error("\u9a8c\u8bc1\u90ae\u7bb1\u5339\u914d\u5931\u8d25", (Throwable)e);
            return false;
        }
    }

    @Override
    public boolean isAdminUser(String studentId, String email) {
        return ADMIN_EMAILS.contains(email);
    }

    @Override
    public boolean notifyRegisterSuccess(Long tenantId, Long authUserId, String username, String name, String email, String referralCode, Map<String, Object> formPayload) {
        try {
            TenantWorkflowConfigDTO workflowConfig = loadWorkflowConfig(tenantId);
            WorkflowPolicyDTO registrationPolicy = workflowConfig == null ? null : workflowConfig.getRegistration();
            if (!shouldNotifyRegistrationAdmins(registrationPolicy)) {
                return true;
            }
            if (!isEmailNoticeEnabled(tenantId)) {
                executeWithTenantContext(tenantId, () -> {
                    saveRegisterReviewRequest(tenantId, authUserId, username, name, email, referralCode, formPayload);
                    return null;
                });
                return true;
            }

            executeWithTenantContext(tenantId, () -> {
                RegisterReviewRequest reviewRequest = saveRegisterReviewRequest(
                        tenantId,
                        authUserId,
                        username,
                        name,
                        email,
                        referralCode,
                        formPayload
                );
                List<String> recipientEmails = resolveNotificationRecipientEmails(tenantId, registrationPolicy);
                if (recipientEmails.isEmpty()) {
                    log.warn("registration notification recipients empty, tenantId={}, username={}", tenantId, username);
                    notifyApplicantOnApprovalRoutingFailure(tenantId, name, username, email, registrationPolicy, "registration review");
                    return null;
                }

                EmailCenterSendDTO dto = new EmailCenterSendDTO();
                dto.setManualEmails(recipientEmails);
                dto.setHtml(true);

                Map<String, String> reviewUrls = buildReviewActionUrls(tenantId, "REGISTER", reviewRequest.getId());
                EmailTemplateDTO template = queryRegisterNoticeTemplate();
                dto.setSubject(buildRegisterNoticeSubject(tenantId, template));
                dto.setContent(buildRegisterNoticeContent(
                        tenantId,
                        username,
                        name,
                        email,
                        referralCode,
                        formPayload,
                        reviewUrls.get("approveUrl"),
                        reviewUrls.get("rejectUrl"),
                        template
                ));
                emailCenterService.send(dto);
                return null;
            });
            return true;
        }
        catch (Exception e) {
            log.warn("notify register success failed, tenantId={}, username={}, reason={}", tenantId, username, e.getMessage());
            return false;
        }
    }

    @Override
    public void reconcileApprovedMembersForCurrentTenant() {
        this.repairApprovedApplicationsForCurrentTenant();
        this.repairApprovedRegisterReviewsForCurrentTenant();
    }

    public boolean notifyRegisterSuccess(Long tenantId, String username, String name, String email, String referralCode, Map<String, Object> formPayload) {
        return this.notifyRegisterSuccess(tenantId, null, username, name, email, referralCode, formPayload);
    }

    private void validateGenericReferral(JoinRequest joinRequest) {
        if (joinRequest.getReferralCode() == null || joinRequest.getReferralCode().isBlank()) {
            return;
        }
        if (joinRequest.getInviteToken() != null && !joinRequest.getInviteToken().isBlank()) {
            return;
        }
        Long tenantId = joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L;
        this.referralCodeService.validateReferralCode(joinRequest.getReferralCode(), tenantId);
    }

    private void recordJoinSubmittedTracking(JoinRequest joinRequest) {
        if (joinRequest.getInviteToken() != null && !joinRequest.getInviteToken().isBlank()) {
            this.emailCenterService.markInvitationJoinSubmitted(joinRequest.getInviteToken(), joinRequest.getReferralCode(), joinRequest.getEmail(), joinRequest.getId());
            return;
        }
        if (joinRequest.getReferralCode() != null && !joinRequest.getReferralCode().isBlank()) {
            Long tenantId = joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L;
            this.referralCodeService.markGenericJoinSubmitted(tenantId, joinRequest.getReferralCode(), joinRequest.getEmail(), joinRequest.getId());
        }
    }

    private void notifyJoinSubmitted(JoinRequest joinRequest) {
        try {
            WorkflowPolicyDTO joinPolicy;
            TenantWorkflowConfigDTO workflowConfig = this.loadWorkflowConfig(joinRequest.getTenantId());
            WorkflowPolicyDTO workflowPolicyDTO = joinPolicy = workflowConfig == null ? null : workflowConfig.getJoinReview();
            if (!isEmailNoticeEnabled(joinRequest.getTenantId())) {
                log.info("skip join pending review email by tenant self service config, targetTenantId={}, studentId={}",
                        joinRequest.getTenantId(), joinRequest.getStudentId());
                return;
            }
            if (!this.shouldNotifyJoinAdmins(joinPolicy)) {
                return;
            }
            List<String> recipientEmails = this.resolveNotificationRecipientEmails(joinRequest.getTenantId(), joinPolicy);
            if (recipientEmails.isEmpty()) {
                log.warn("join notification recipients empty, targetTenantId={}, studentId={}", (Object)joinRequest.getTenantId(), (Object)joinRequest.getStudentId());
                this.executeWithTenantContext(joinRequest.getTenantId(), () -> {
                    this.notifyApplicantOnApprovalRoutingFailure(joinRequest.getTenantId(), joinRequest.getName(), joinRequest.getUsername(), joinRequest.getEmail(), joinPolicy, "join review");
                    return null;
                });
                return;
            }
            EmailCenterSendDTO dto = new EmailCenterSendDTO();
            dto.setManualEmails(recipientEmails);
            dto.setHtml(true);
            this.executeWithTenantContext(joinRequest.getTenantId(), () -> {
                Map<String, String> reviewUrls = this.buildReviewActionUrls(joinRequest.getTenantId(), "JOIN", joinRequest.getId());
                EmailTemplateDTO template = this.queryJoinNoticeTemplate();
                dto.setSubject(this.buildJoinNoticeSubject(joinRequest, template));
                dto.setContent(this.buildJoinNoticeContent(joinRequest, reviewUrls.get("approveUrl"), reviewUrls.get("rejectUrl"), template));
                this.emailCenterService.send(dto);
                this.markJoinNotificationQueued(joinRequest);
                return null;
            });
        }
        catch (Exception e) {
            log.warn("send join notification failed, targetTenantId={}, studentId={}, reason={}", new Object[]{joinRequest.getTenantId(), joinRequest.getStudentId(), e.getMessage()});
        }
    }

    private void markJoinNotificationQueued(JoinRequest joinRequest) {
        if (joinRequest == null) {
            return;
        }
        joinRequest.setEmailSent(true);
        joinRequest.setUpdateTime(LocalDateTime.now());
        if (joinRequest.getId() == null) {
            return;
        }
        JoinRequest update = new JoinRequest();
        update.setId(joinRequest.getId());
        update.setEmailSent(true);
        update.setUpdateTime(joinRequest.getUpdateTime());
        this.updateById(update);
    }

    private List<String> resolveNotificationRecipientEmails(Long tenantId, WorkflowPolicyDTO workflowPolicy) {
        LinkedHashSet<String> emails = new LinkedHashSet<>();
        if (usesDesignatedAdvisorRouting(workflowPolicy)) {
            emails.addAll(safeResolveRoleRecipientEmailsByIds(tenantId, workflowPolicy.getDesignatedAdvisorRoleIds()));
            if (emails.isEmpty()) {
                addFallbackNotificationEmails(emails, tenantId);
            }
            return new ArrayList<>(emails);
        }
        int beforeTenantRecipients = emails.size();
        emails.addAll(safeResolveRoleRecipientEmailsByCodes(tenantId, TENANT_ADMIN_ROLE_CODES));
        if (emails.size() == beforeTenantRecipients) {
            addLocalPrivilegedUserEmails(emails, tenantId);
            addTenantContactEmail(emails, tenantId);
        }
        if (!Objects.equals(tenantId, PLATFORM_SUPER_ADMIN_TENANT_ID)) {
            int beforePlatformRecipients = emails.size();
            emails.addAll(safeResolveRoleRecipientEmailsByCodes(PLATFORM_SUPER_ADMIN_TENANT_ID, PLATFORM_SUPER_ADMIN_ROLE_CODES));
            if (emails.size() == beforePlatformRecipients) {
                addLocalPrivilegedUserEmails(emails, PLATFORM_SUPER_ADMIN_TENANT_ID);
                addTenantContactEmail(emails, PLATFORM_SUPER_ADMIN_TENANT_ID);
            }
        }
        if (emails.isEmpty()) {
            addFallbackNotificationEmails(emails, tenantId);
        }
        return new ArrayList<>(emails);
    }

    private void addFallbackNotificationEmails(LinkedHashSet<String> emails, Long tenantId) {
        addLocalPrivilegedUserEmails(emails, tenantId);
        addTenantContactEmail(emails, tenantId);
        if (!Objects.equals(tenantId, PLATFORM_SUPER_ADMIN_TENANT_ID)) {
            addLocalPrivilegedUserEmails(emails, PLATFORM_SUPER_ADMIN_TENANT_ID);
            addTenantContactEmail(emails, PLATFORM_SUPER_ADMIN_TENANT_ID);
        }
    }

    private void addLocalPrivilegedUserEmails(LinkedHashSet<String> emails, Long tenantId) {
        if (tenantId == null) {
            return;
        }
        try {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getTenantId, tenantId)
                    .eq(User::getIsDeleted, 0)
                    .in(User::getType, List.of(UserType.STAFF, UserType.TEACHER)));
            if (users == null || users.isEmpty()) {
                return;
            }
            users.stream()
                    .map(User::getEmail)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(email -> !email.isBlank())
                    .map(String::toLowerCase)
                    .forEach(emails::add);
        }
        catch (Exception e) {
            log.warn("resolve local privileged user emails failed, tenantId={}, reason={}", tenantId, e.getMessage());
        }
    }

    private void addTenantContactEmail(LinkedHashSet<String> emails, Long tenantId) {
        if (tenantId == null) {
            return;
        }
        try {
            RKTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null || StringUtils.isBlank((CharSequence) tenant.getContactEmail())) {
                return;
            }
            String email = tenant.getContactEmail().trim().toLowerCase();
            if (!email.isBlank()) {
                emails.add(email);
            }
        }
        catch (Exception e) {
            log.warn("resolve tenant contact email failed, tenantId={}, reason={}", tenantId, e.getMessage());
        }
    }

    private List<String> safeResolveRoleRecipientEmailsByCodes(Long tenantId, List<String> roleCodes) {
        try {
            return resolveRoleRecipientEmailsByCodes(tenantId, roleCodes);
        }
        catch (Exception e) {
            log.warn("resolve role recipient emails by codes failed, tenantId={}, roleCodes={}, reason={}", tenantId, roleCodes, e.getMessage());
            return List.of();
        }
    }

    private List<String> safeResolveRoleRecipientEmailsByIds(Long tenantId, List<Long> roleIds) {
        try {
            return resolveRoleRecipientEmailsByIds(tenantId, roleIds);
        }
        catch (Exception e) {
            log.warn("resolve role recipient emails by ids failed, tenantId={}, roleIds={}, reason={}", tenantId, roleIds, e.getMessage());
            return List.of();
        }
    }

    private boolean shouldNotifyRegistrationAdmins(WorkflowPolicyDTO registrationPolicy) {
        if (registrationPolicy == null) {
            return true;
        }
        Boolean openRegistration = registrationPolicy.getOpenRegistration();
        Boolean requireApproval = registrationPolicy.getRequireApproval();
        Boolean notifyAdmins = registrationPolicy.getNotifyAdmins();
        if (Boolean.TRUE.equals(openRegistration) || Boolean.FALSE.equals(requireApproval)) {
            return false;
        }
        return !Boolean.FALSE.equals(notifyAdmins);
    }

    private boolean shouldNotifyJoinAdmins(WorkflowPolicyDTO joinPolicy) {
        if (joinPolicy == null) {
            return true;
        }
        return !Boolean.FALSE.equals(joinPolicy.getNotifyAdmins());
    }

    private boolean isEmailNoticeEnabled(Long tenantId) {
        TenantSelfServiceConfigDTO config = loadTenantSelfServiceConfig(tenantId);
        if (config == null || config.getNotificationSettings() == null) {
            return true;
        }
        return !Boolean.FALSE.equals(config.getNotificationSettings().getEmailNoticeEnabled());
    }

    private boolean isApprovalNoticeEnabled(Long tenantId) {
        TenantSelfServiceConfigDTO config = loadTenantSelfServiceConfig(tenantId);
        if (config == null || config.getNotificationSettings() == null) {
            return true;
        }
        return !Boolean.FALSE.equals(config.getNotificationSettings().getApprovalNoticeEnabled());
    }

    private void notifyRegisterReviewResult(RegisterReviewRequest request, boolean approved) {
        if (request == null || StringUtils.isBlank((CharSequence)request.getEmail())) {
            return;
        }
        if (!isApprovalNoticeEnabled(request.getTenantId())) {
            log.info("skip register review result email by tenant self service config, requestId={}, tenantId={}",
                    request.getId(), request.getTenantId());
            return;
        }
        try {
            EmailCenterSendDTO dto = new EmailCenterSendDTO();
            dto.setManualEmails(List.of(request.getEmail().trim().toLowerCase()));
            dto.setHtml(true);
            dto.setSubject(this.buildRegisterReviewResultSubject(request));
            dto.setContent(this.buildRegisterReviewResultContent(request, approved));
            this.executeWithTenantContext(request.getTenantId(), () -> {
                this.emailCenterService.send(dto);
                return null;
            });
        }
        catch (Exception e) {
            log.warn("send register review result email failed, requestId={}, tenantId={}, email={}, reason={}", new Object[]{request.getId(), request.getTenantId(), request.getEmail(), e.getMessage()});
        }
    }

    private void notifyJoinReviewResult(JoinRequest request, boolean approved) {
        if (request == null || StringUtils.isBlank((CharSequence)request.getEmail())) {
            return;
        }
        if (!isApprovalNoticeEnabled(request.getTenantId())) {
            log.info("skip join review result email by tenant self service config, requestId={}, tenantId={}",
                    request.getId(), request.getTenantId());
            return;
        }
        try {
            EmailCenterSendDTO dto = new EmailCenterSendDTO();
            dto.setManualEmails(List.of(request.getEmail().trim().toLowerCase()));
            dto.setHtml(true);
            dto.setSubject(this.buildJoinReviewResultSubject(request));
            dto.setContent(this.buildJoinReviewResultContent(request, approved));
            this.executeWithTenantContext(request.getTenantId(), () -> {
                this.emailCenterService.send(dto);
                return null;
            });
        }
        catch (Exception e) {
            log.warn("send join review result email failed, requestId={}, tenantId={}, email={}, reason={}", new Object[]{request.getId(), request.getTenantId(), request.getEmail(), e.getMessage()});
        }
    }

    private TenantWorkflowConfigDTO loadWorkflowConfig(Long tenantId) {
        try {
            return this.tenantWorkflowConfigService.loadCurrentConfig(tenantId);
        }
        catch (Exception e) {
            log.warn("load tenant workflow config failed, tenantId={}, reason={}", (Object)tenantId, (Object)e.getMessage());
            return null;
        }
    }

    private List<String> resolveRoleRecipientEmailsByCodes(Long tenantId, List<String> roleCodes) {
        if (tenantId == null || roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }

        List<RoleDTO> roles = authClient.listAllRoles(tenantId);
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = roles.stream()
                .filter(role -> role.getCode() != null)
                .filter(role -> roleCodes.stream().anyMatch(code -> code.equalsIgnoreCase(role.getCode())))
                .map(RoleDTO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return resolveRoleRecipientEmailsByIds(tenantId, roleIds);
    }

    private List<String> resolveRoleRecipientEmailsByIds(Long tenantId, List<Long> roleIds) {
        if (tenantId == null || roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctRoleIds = roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctRoleIds.isEmpty()) {
            return List.of();
        }

        RoleRecipientQueryDTO queryDTO = new RoleRecipientQueryDTO();
        queryDTO.setTenantId(tenantId);
        queryDTO.setRoleIds(distinctRoleIds);

        List<RoleAccountDTO> accounts = authClient.queryAccountsByRoles(queryDTO);
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }

        List<Long> authUserIds = accounts.stream()
                .map(RoleAccountDTO::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<String> usernames = accounts.stream()
                .map(RoleAccountDTO::getUsername)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getTenantId, tenantId)
                .and(wrapper -> {
            boolean hasCondition = false;
            if (!authUserIds.isEmpty()) {
                wrapper.in(User::getAuthUserId, authUserIds);
                hasCondition = true;
            }
            if (!usernames.isEmpty()) {
                if (hasCondition) {
                    wrapper.or();
                }
                wrapper.in(User::getUsername, usernames);
            }
        });
        return userMapper.selectList(queryWrapper).stream()
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean usesDesignatedAdvisorRouting(WorkflowPolicyDTO workflowPolicy) {
        return workflowPolicy != null && "DESIGNATED".equalsIgnoreCase(this.defaultString(workflowPolicy.getAdvisorMode(), "")) && workflowPolicy.getDesignatedAdvisorRoleIds() != null && !workflowPolicy.getDesignatedAdvisorRoleIds().isEmpty();
    }

    private void notifyApplicantOnApprovalRoutingFailure(Long tenantId, String name, String username, String email, WorkflowPolicyDTO workflowPolicy, String workflowLabel) {
        if (StringUtils.isBlank((CharSequence)email) || workflowPolicy == null || !Boolean.TRUE.equals(workflowPolicy.getNotifyApplicantOnFailure())) {
            return;
        }
        EmailCenterSendDTO dto = new EmailCenterSendDTO();
        dto.setManualEmails(List.of(email.trim().toLowerCase()));
        dto.setHtml(true);
        dto.setSubject(this.buildApprovalRoutingFailureSubject(tenantId, workflowLabel));
        dto.setContent(this.buildApprovalRoutingFailureContent(tenantId, name, username, workflowLabel));
        this.emailCenterService.send(dto);
    }

    private String buildApprovalRoutingFailureSubject(Long tenantId, String workflowLabel) {
        return this.defaultString(this.resolveTenantName(tenantId), "Tenant") + " " + this.defaultString(workflowLabel, "review") + " reminder";
    }

    private String buildApprovalRoutingFailureContent(Long tenantId, String name, String username, String workflowLabel) {
        String tenantName = this.defaultString(this.resolveTenantName(tenantId), "Tenant");
        String applicantName = this.defaultString(name, this.defaultString(username, "student"));
        String workflowName = this.defaultString(workflowLabel, "review");
        return "<html><body style='font-family:Arial,sans-serif;background:#f8fafc;color:#1f2937;padding:24px;'>" + "<div style='max-width:680px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px;border:1px solid #e5e7eb;'>" + "<h2 style='margin:0 0 16px;'>" + this.escapeHtml(tenantName) + " " + this.escapeHtml(workflowName) + " reminder</h2>" + "<p style='margin:0 0 12px;'>Hello, " + this.escapeHtml(applicantName) + ".</p>" + "<p style='margin:0 0 12px;'>Your request was stored, but no approver could be notified automatically.</p>" + "<p style='margin:0 0 12px;'>Please contact " + this.escapeHtml(tenantName) + " administrators for manual follow-up.</p>" + "</div></body></html>";
    }

    private String buildJoinNoticeSubject(JoinRequest joinRequest, EmailTemplateDTO template) {
        if (template != null && template.getEmailSubject() != null && !template.getEmailSubject().isBlank()) {
            return this.renderJoinNoticeTemplate(template.getEmailSubject(), joinRequest);
        }
        String targetTenantName = this.resolveTenantName(joinRequest.getTenantId());
        return targetTenantName + " \u65b0\u5165\u793e\u7533\u8bf7\u901a\u77e5";
    }

    private String buildJoinNoticeContent(JoinRequest joinRequest, String approveUrl, String rejectUrl, EmailTemplateDTO template) {
        if (template != null && template.getTemplateContent() != null && !template.getTemplateContent().isBlank()) {
            return this.renderJoinNoticeTemplate(template.getTemplateContent(), joinRequest).replace("{{formSummaryHtml}}", this.buildFormSummaryHtml(this.parseJsonMap(joinRequest.getFormPayloadJson(), this.normalizeJoinFormPayload(joinRequest)))).replace("{{approveUrl}}", this.defaultString(approveUrl, "")).replace("{{rejectUrl}}", this.defaultString(rejectUrl, ""));
        }
        String targetTenantName = this.resolveTenantName(joinRequest.getTenantId());
        String sourceTenantName = this.resolveTenantName(joinRequest.getSourceTenantId());
        String sourceRoleName = joinRequest.getSourceRoleName() == null || joinRequest.getSourceRoleName().isBlank() ? "\u672a\u8bc6\u522b\u89d2\u8272" : joinRequest.getSourceRoleName();
        String sourcePosition = this.resolveSourcePosition(joinRequest);
        String referralOwner = this.resolveReferralOwnerName(joinRequest);
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body style='font-family:Arial,sans-serif;color:#333;'>").append("<h2>\u65b0\u5165\u793e\u7533\u8bf7\u901a\u77e5</h2>").append("<p><strong>\u76ee\u6807\u793e\u56e2\uff1a</strong>").append(this.escapeHtml(targetTenantName)).append("</p>").append("<p><strong>\u7533\u8bf7\u4eba\u59d3\u540d\uff1a</strong>").append(this.escapeHtml(this.defaultString(joinRequest.getName(), "-"))).append("</p>").append("<p><strong>\u7533\u8bf7\u4eba\u90ae\u7bb1\uff1a</strong>").append(this.escapeHtml(this.defaultString(joinRequest.getEmail(), "-"))).append("</p>");
        if (joinRequest.getSourceAuthUserId() != null) {
            builder.append("<p><strong>\u6765\u6e90\u793e\u56e2\uff1a</strong>").append(this.escapeHtml(this.defaultString(sourceTenantName, "-"))).append("</p>").append("<p><strong>\u6765\u6e90\u8eab\u4efd\uff1a</strong>").append(this.escapeHtml(sourceRoleName)).append("</p>").append("<p><strong>\u6765\u6e90\u804c\u4f4d\uff1a</strong>").append(this.escapeHtml(this.defaultString(sourcePosition, "\u672a\u8bbe\u7f6e"))).append("</p>");
        } else {
            builder.append("<p><strong>\u7533\u8bf7\u6765\u6e90\uff1a</strong>\u672a\u767b\u5f55\u516c\u5f00\u7533\u8bf7</p>");
        }
        if (joinRequest.getReferralCode() != null && !joinRequest.getReferralCode().isBlank()) {
            builder.append("<p><strong>\u5185\u63a8\u7801\uff1a</strong>").append(this.escapeHtml(joinRequest.getReferralCode())).append("</p>").append("<p><strong>\u5185\u63a8\u4eba\uff1a</strong>").append(this.escapeHtml(this.defaultString(referralOwner, "\u672a\u8bc6\u522b"))).append("</p>");
        }
        builder.append("<p><strong>\u8868\u5355\u6458\u8981\uff1a</strong></p>").append(this.buildFormSummaryHtml(this.parseJsonMap(joinRequest.getFormPayloadJson(), this.normalizeJoinFormPayload(joinRequest)))).append("<p><strong>\u81ea\u6211\u4ecb\u7ecd\uff1a</strong></p>").append("<div style='white-space:pre-line;border:1px solid #e5e7eb;padding:12px;border-radius:8px;'>").append(this.escapeHtml(this.defaultString(joinRequest.getExperience(), "-"))).append("</div>");
        if (!this.isBlank(approveUrl) || !this.isBlank(rejectUrl)) {
            builder.append("<p><strong>\u5feb\u6377\u5ba1\u6838\uff1a</strong></p>");
            if (!this.isBlank(approveUrl)) {
                builder.append("<p><a href='").append(this.escapeHtml(approveUrl)).append("' target='_blank'>\u4e00\u952e\u540c\u610f</a></p>");
            }
            if (!this.isBlank(rejectUrl)) {
                builder.append("<p><a href='").append(this.escapeHtml(rejectUrl)).append("' target='_blank'>\u4e00\u952e\u62d2\u7edd</a></p>");
            }
        }
        builder.append("</body></html>");
        return builder.toString();
    }

    private EmailTemplateDTO queryJoinNoticeTemplate() {
        try {
            return this.emailTemplateClient.queryByCode("JOIN_APPLY_NOTIFY");
        }
        catch (Exception e) {
            log.warn("\u67e5\u8be2\u5165\u793e\u7533\u8bf7\u901a\u77e5\u6a21\u677f\u5931\u8d25, reason={}", (Object)e.getMessage());
            return null;
        }
    }

    private EmailTemplateDTO queryRegisterNoticeTemplate() {
        for (String code : Arrays.asList("REGISTER_APPLY_NOTIFY", "JOIN_APPLY_NOTIFY")) {
            try {
                EmailTemplateDTO template = this.emailTemplateClient.queryByCode(code);
                if (template == null) continue;
                return template;
            }
            catch (Exception e) {
                log.warn("\u93cc\u30e8\ue1d7\u5a09\u3125\u553d\u93b4\u612c\u59db\u95ab\u6c31\u7161\u59af\u2103\u6f98\u6fb6\u8fab\u89e6, code={}, reason={}", (Object)code, (Object)e.getMessage());
            }
        }
        return null;
    }

    private String renderJoinNoticeTemplate(String template, JoinRequest joinRequest) {
        return template.replace("{{targetTenantName}}", this.defaultString(this.resolveTenantName(joinRequest.getTenantId()), "")).replace("{{sourceTenantName}}", this.defaultString(this.resolveTenantName(joinRequest.getSourceTenantId()), "")).replace("{{applicantName}}", this.defaultString(joinRequest.getName(), "")).replace("{{applicantEmail}}", this.defaultString(joinRequest.getEmail(), "")).replace("{{sourceRoleName}}", this.defaultString(joinRequest.getSourceRoleName(), "")).replace("{{sourcePosition}}", this.defaultString(this.resolveSourcePosition(joinRequest), "")).replace("{{referralCode}}", this.defaultString(joinRequest.getReferralCode(), "")).replace("{{referralOwner}}", this.defaultString(this.resolveReferralOwnerName(joinRequest), "")).replace("{{experience}}", this.defaultString(joinRequest.getExperience(), ""));
    }

    private String buildRegisterNoticeSubject(Long tenantId, EmailTemplateDTO template) {
        if (template != null && template.getEmailSubject() != null && !template.getEmailSubject().isBlank()) {
            return this.renderRegisterNoticeTemplate(template.getEmailSubject(), tenantId, null, null, null, null);
        }
        return this.defaultString(this.resolveTenantName(tenantId), "\u76ee\u6807\u79df\u6237") + " \u65b0\u6ce8\u518c\u901a\u77e5";
    }

    private String buildRegisterReviewResultSubject(RegisterReviewRequest request) {
        return this.defaultString(this.resolveTenantName(request.getTenantId()), "Tenant") + " registration review result";
    }

    private String buildRegisterReviewResultContent(RegisterReviewRequest request, boolean approved) {
        String tenantName = this.defaultString(this.resolveTenantName(request.getTenantId()), "Tenant");
        String applicantName = this.defaultString(request.getName(), this.defaultString(request.getUsername(), "student"));
        String reviewComment = this.defaultString(request.getReviewComment(), "");
        String resultText = approved ? "approved" : "rejected";
        String resultColor = approved ? "#166534" : "#b91c1c";
        String loginUrl = PublicBaseUrlResolver.resolve(this.publicWebBaseUrl);
        Object loginHref = this.isBlank(loginUrl) ? "" : loginUrl + "/login";
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body style='font-family:Arial,sans-serif;background:#f8fafc;color:#1f2937;padding:24px;'>").append("<div style='max-width:680px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px;border:1px solid #e5e7eb;'>").append("<h2 style='margin:0 0 16px;'>").append(this.escapeHtml(tenantName)).append(" registration review result</h2>").append("<p style='margin:0 0 12px;'>Hello, ").append(this.escapeHtml(applicantName)).append(".</p>").append("<p style='margin:0 0 12px;'>Your registration request is currently <strong style='color:").append(resultColor).append(";'>").append(resultText).append("</strong>.</p>");
        if (StringUtils.isNotBlank((CharSequence)reviewComment)) {
            builder.append("<p style='margin:0 0 12px;'>Review comment: ").append(this.escapeHtml(reviewComment)).append("</p>");
        }
        if (approved && StringUtils.isNotBlank((CharSequence)loginHref)) {
            builder.append("<p style='margin:20px 0 0;'><a href='").append(this.escapeHtml((String)loginHref)).append("' target='_blank' style='display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:8px;'>Login</a></p>").append("<p style='margin:12px 0 0;color:#6b7280;'>Login URL: ").append(this.escapeHtml((String)loginHref)).append("</p>");
        }
        builder.append("</div></body></html>");
        return builder.toString();
    }

    private String buildJoinReviewResultSubject(JoinRequest request) {
        return this.defaultString(this.resolveTenantName(request.getTenantId()), "Tenant") + " join review result";
    }

    private String buildJoinReviewResultContent(JoinRequest request, boolean approved) {
        String tenantName = this.defaultString(this.resolveTenantName(request.getTenantId()), "Tenant");
        String applicantName = this.defaultString(request.getName(), this.defaultString(request.getUsername(), "student"));
        String reviewComment = this.defaultString(request.getReviewComment(), "");
        String resultText = approved ? "approved" : "rejected";
        String resultColor = approved ? "#166534" : "#b91c1c";
        String loginUrl = PublicBaseUrlResolver.resolve(this.publicWebBaseUrl);
        Object loginHref = this.isBlank(loginUrl) ? "" : loginUrl + "/login";
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body style='font-family:Arial,sans-serif;background:#f8fafc;color:#1f2937;padding:24px;'>").append("<div style='max-width:680px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px;border:1px solid #e5e7eb;'>").append("<h2 style='margin:0 0 16px;'>").append(this.escapeHtml(tenantName)).append(" join review result</h2>").append("<p style='margin:0 0 12px;'>Hello, ").append(this.escapeHtml(applicantName)).append(".</p>").append("<p style='margin:0 0 12px;'>Your join request is currently <strong style='color:").append(resultColor).append(";'>").append(resultText).append("</strong>.</p>");
        if (StringUtils.isNotBlank((CharSequence)reviewComment)) {
            builder.append("<p style='margin:0 0 12px;'>Review comment: ").append(this.escapeHtml(reviewComment)).append("</p>");
        }
        if (approved && StringUtils.isNotBlank((CharSequence)loginHref)) {
            builder.append("<p style='margin:20px 0 0;'><a href='").append(this.escapeHtml((String)loginHref)).append("' target='_blank' style='display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:8px;'>Login</a></p>").append("<p style='margin:12px 0 0;color:#6b7280;'>Login URL: ").append(this.escapeHtml((String)loginHref)).append("</p>");
        }
        builder.append("</div></body></html>");
        return builder.toString();
    }

    private String buildRegisterNoticeContent(Long tenantId, String username, String name, String email, String referralCode, Map<String, Object> formPayload, String approveUrl, String rejectUrl, EmailTemplateDTO template) {
        if (template != null && template.getTemplateContent() != null && !template.getTemplateContent().isBlank()) {
            return this.renderRegisterNoticeTemplate(template.getTemplateContent(), tenantId, username, name, email, referralCode).replace("{{formSummaryHtml}}", this.buildFormSummaryHtml(this.normalizeRegisterFormPayload(username, name, email, referralCode, formPayload))).replace("{{approveUrl}}", this.defaultString(approveUrl, "")).replace("{{rejectUrl}}", this.defaultString(rejectUrl, ""));
        }
        String tenantName = this.resolveTenantName(tenantId);
        String referralOwner = this.resolveReferralOwnerName(tenantId, referralCode);
        String applicantName = this.defaultString(name, this.defaultString(username, "-"));
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body style='font-family:Arial,sans-serif;color:#333;'>").append("<h2>\u65b0\u6ce8\u518c\u901a\u77e5</h2>").append("<p><strong>\u76ee\u6807\u79df\u6237\uff1a</strong>").append(this.escapeHtml(this.defaultString(tenantName, "-"))).append("</p>").append("<p><strong>\u6ce8\u518c\u4eba\uff1a</strong>").append(this.escapeHtml(applicantName)).append("</p>").append("<p><strong>\u6ce8\u518c\u8d26\u53f7\uff1a</strong>").append(this.escapeHtml(this.defaultString(username, "-"))).append("</p>").append("<p><strong>\u6ce8\u518c\u90ae\u7bb1\uff1a</strong>").append(this.escapeHtml(this.defaultString(email, "-"))).append("</p>").append("<p><strong>\u6765\u6e90\uff1a</strong>\u672a\u767b\u5f55\u76f4\u63a5\u6ce8\u518c</p>");
        if (referralCode != null && !referralCode.isBlank()) {
            builder.append("<p><strong>\u5185\u63a8\u7801\uff1a</strong>").append(this.escapeHtml(referralCode)).append("</p>").append("<p><strong>\u5185\u63a8\u4eba\uff1a</strong>").append(this.escapeHtml(this.defaultString(referralOwner, "\u672a\u8bc6\u522b"))).append("</p>");
        }
        builder.append("<p><strong>\u8868\u5355\u6458\u8981\uff1a</strong></p>").append(this.buildFormSummaryHtml(this.normalizeRegisterFormPayload(username, name, email, referralCode, formPayload)));
        if (!this.isBlank(approveUrl) || !this.isBlank(rejectUrl)) {
            builder.append("<p><strong>\u5feb\u6377\u5ba1\u6838\uff1a</strong></p>");
            if (!this.isBlank(approveUrl)) {
                builder.append("<p><a href='").append(this.escapeHtml(approveUrl)).append("' target='_blank'>\u4e00\u952e\u540c\u610f</a></p>");
            }
            if (!this.isBlank(rejectUrl)) {
                builder.append("<p><a href='").append(this.escapeHtml(rejectUrl)).append("' target='_blank'>\u4e00\u952e\u62d2\u7edd</a></p>");
            }
        }
        builder.append("</body></html>");
        return builder.toString();
    }

    private String renderRegisterNoticeTemplate(String template, Long tenantId, String username, String name, String email, String referralCode) {
        return template.replace("{{targetTenantName}}", this.defaultString(this.resolveTenantName(tenantId), "")).replace("{{applicantName}}", this.defaultString(name, this.defaultString(username, ""))).replace("{{username}}", this.defaultString(username, "")).replace("{{applicantEmail}}", this.defaultString(email, "")).replace("{{referralCode}}", this.defaultString(referralCode, "")).replace("{{referralOwner}}", this.defaultString(this.resolveReferralOwnerName(tenantId, referralCode), ""));
    }

    private RegisterReviewRequest saveRegisterReviewRequest(Long tenantId, Long authUserId, String username, String name, String email, String referralCode, Map<String, Object> formPayload) {
        RegisterReviewRequest request = new RegisterReviewRequest();
        request.setTenantId(tenantId);
        request.setAuthUserId(authUserId);
        request.setUsername(username);
        request.setName(name);
        request.setEmail(email);
        request.setReferralCode(referralCode);
        request.setReviewStatus("PENDING");
        request.setFormPayloadJson(this.writeJson(this.normalizeRegisterFormPayload(username, name, email, referralCode, formPayload)));
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        request.setIsDeleted(0);
        this.registerReviewRequestMapper.insert(request);
        return request;
    }

    private Map<String, String> buildReviewActionUrls(Long tenantId, String targetType, Long targetId) {
        String approveToken = this.reviewActionTokenService.createToken(tenantId, targetType, targetId, "APPROVE");
        String rejectToken = this.reviewActionTokenService.createToken(tenantId, targetType, targetId, "REJECT");
        String resolvedBaseUrl = PublicBaseUrlResolver.resolve(this.publicWebBaseUrl);
        HashMap<String, String> urls = new HashMap<String, String>();
        urls.put("approveUrl", resolvedBaseUrl + "/review/admission?token=" + approveToken);
        urls.put("rejectUrl", resolvedBaseUrl + "/review/admission?token=" + rejectToken);
        return urls;
    }

    private Map<String, Object> normalizeJoinFormPayload(JoinRequest joinRequest) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        if (joinRequest.getFormPayload() != null && !joinRequest.getFormPayload().isEmpty()) {
            payload.putAll(joinRequest.getFormPayload());
        }
        this.putIfAbsent(payload, "name", joinRequest.getName());
        this.putIfAbsent(payload, "studentId", joinRequest.getStudentId());
        this.putIfAbsent(payload, "major", joinRequest.getMajor());
        this.putIfAbsent(payload, "grade", joinRequest.getGrade());
        this.putIfAbsent(payload, "phone", joinRequest.getPhone());
        this.putIfAbsent(payload, "email", joinRequest.getEmail());
        this.putIfAbsent(payload, "interests", joinRequest.getInterest());
        this.putIfAbsent(payload, "intro", joinRequest.getExperience());
        this.putIfAbsent(payload, "referralCode", joinRequest.getReferralCode());
        return payload;
    }

    private Map<String, Object> normalizeRegisterFormPayload(String username, String name, String email, String referralCode, Map<String, Object> formPayload) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        if (formPayload != null && !formPayload.isEmpty()) {
            payload.putAll(formPayload);
        }
        this.putIfAbsent(payload, "username", username);
        this.putIfAbsent(payload, "name", name);
        this.putIfAbsent(payload, "email", email);
        this.putIfAbsent(payload, "referralCode", referralCode);
        return payload;
    }

    private void putIfAbsent(Map<String, Object> payload, String key, Object value) {
        if (!payload.containsKey(key) && value != null) {
            payload.put(key, value);
        }
    }

    private String buildFormSummaryHtml(Map<String, Object> formPayload) {
        if (formPayload == null || formPayload.isEmpty()) {
            return "<div style='border:1px solid #e5e7eb;padding:12px;border-radius:8px;'>\u65e0\u989d\u5916\u8868\u5355\u5185\u5bb9</div>";
        }
        StringBuilder builder = new StringBuilder("<div style='border:1px solid #e5e7eb;padding:12px;border-radius:8px;'>");
        for (Map.Entry<String, Object> entry : formPayload.entrySet()) {
            builder.append("<p><strong>").append(this.escapeHtml(entry.getKey())).append("\uff1a</strong>").append(this.escapeHtml(this.stringifyValue(entry.getValue()))).append("</p>");
        }
        builder.append("</div>");
        return builder.toString();
    }

    private String stringifyValue(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(item -> item == null ? "" : item.toString())
                    .collect(Collectors.joining(", "));
        }
        return value.toString();
    }

    private Map<String, Object> parseJsonMap(String json, Map<String, Object> fallback) {
        try {
            if (json == null || json.isBlank()) {
                return fallback;
            }
            return (Map)this.objectMapper.readValue(json, LinkedHashMap.class);
        }
        catch (Exception e) {
            return fallback;
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return this.objectMapper.writeValueAsString(value);
        }
        catch (Exception e) {
            throw new RuntimeException("serialize form payload failed", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveTenantName(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        RKTenant tenant = (RKTenant)this.tenantMapper.selectById(tenantId);
        return tenant == null ? null : tenant.getTenantName();
    }

    private String resolveSourcePosition(JoinRequest joinRequest) {
        if (joinRequest.getSourceAuthUserId() == null || joinRequest.getSourceTenantId() == null) {
            return null;
        }

        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(User::getTenantId, joinRequest.getSourceTenantId())
                .eq(User::getAuthUserId, joinRequest.getSourceAuthUserId())
                .last("LIMIT 1");
        User sourceUser = userMapper.selectOne(userQuery);
        if (sourceUser == null || sourceUser.getStudentId() == null || sourceUser.getStudentId().isBlank()) {
            return null;
        }

        LambdaQueryWrapper<ClubMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(ClubMember::getTenantId, joinRequest.getSourceTenantId())
                .eq(ClubMember::getStudentId, sourceUser.getStudentId())
                .last("LIMIT 1");
        ClubMember member = clubMemberMapper.selectOne(memberQuery);
        return member == null ? null : member.getPosition();
    }

    private String resolveReferralOwnerName(JoinRequest joinRequest) {
        return this.resolveReferralOwnerName(joinRequest.getTenantId(), joinRequest.getReferralCode());
    }

    private String resolveReferralOwnerName(Long tenantId, String referralCodeValue) {
        if (referralCodeValue == null || referralCodeValue.isBlank()) {
            return null;
        }

        LambdaQueryWrapper<ReferralCode> codeQuery = new LambdaQueryWrapper<>();
        codeQuery.eq(ReferralCode::getTenantId, tenantId)
                .eq(ReferralCode::getCode, referralCodeValue)
                .last("LIMIT 1");
        ReferralCode referralCode = referralCodeMapper.selectOne(codeQuery);
        if (referralCode == null || referralCode.getGeneratorId() == null) {
            return null;
        }

        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(User::getTenantId, tenantId)
                .eq(User::getAuthUserId, referralCode.getGeneratorId())
                .last("LIMIT 1");
        User owner = userMapper.selectOne(userQuery);
        if (owner == null) {
            return null;
        }
        if (owner.getRealName() != null && !owner.getRealName().isBlank()) {
            return owner.getRealName();
        }
        if (owner.getNickname() != null && !owner.getNickname().isBlank()) {
            return owner.getNickname();
        }
        return owner.getUsername();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escapeHtml(String value) {
        return this.defaultString(value, "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void verifyJoinEmailCode(JoinRequest joinRequest) {
        if (joinRequest == null) {
            throw new BadRequestException("\u7533\u8bf7\u53c2\u6570\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (joinRequest.getEmail() == null || joinRequest.getEmail().isBlank()) {
            throw new BadRequestException("\u90ae\u7bb1\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (joinRequest.getEmailCode() == null || joinRequest.getEmailCode().isBlank()) {
            throw new BadRequestException("\u90ae\u7bb1\u9a8c\u8bc1\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        EmailVerificationVerifyDTO verifyDTO = new EmailVerificationVerifyDTO();
        verifyDTO.setEmail(joinRequest.getEmail());
        verifyDTO.setTenantId(Long.valueOf(joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L));
        verifyDTO.setScene("JOIN");
        verifyDTO.setCode(joinRequest.getEmailCode());
        this.emailVerificationService.verifyCode(verifyDTO);
    }

    private void recordJoinApprovedTracking(JoinRequest joinRequest) {
        if (joinRequest.getInviteToken() != null && !joinRequest.getInviteToken().isBlank()) {
            this.emailCenterService.markInvitationJoinApproved(joinRequest.getInviteToken(), joinRequest.getReferralCode(), joinRequest.getEmail(), joinRequest.getAuthUserId(), joinRequest.getId());
            return;
        }
        if (joinRequest.getReferralCode() != null && !joinRequest.getReferralCode().isBlank()) {
            Long tenantId = joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L;
            this.referralCodeService.markGenericJoinApproved(tenantId, joinRequest.getReferralCode(), joinRequest.getEmail(), joinRequest.getAuthUserId(), joinRequest.getId());
        }
    }

    private void prepareJoinRequestCredentials(JoinRequest joinRequest) {
        if (joinRequest.getUsername() == null || joinRequest.getUsername().trim().isEmpty()) {
            joinRequest.setUsername(joinRequest.getStudentId());
        }
        if (joinRequest.getSourceAuthUserId() != null) {
            if ((joinRequest.getPasswordHash() == null || joinRequest.getPasswordHash().isEmpty()) && joinRequest.getPassword() != null && !joinRequest.getPassword().isEmpty()) {
                joinRequest.setPasswordHash(this.passwordEncoder.encode((CharSequence)joinRequest.getPassword()));
            }
            if (joinRequest.getPasswordHash() == null || joinRequest.getPasswordHash().isEmpty()) {
                joinRequest.setPasswordHash(this.passwordEncoder.encode((CharSequence)DEFAULT_APPROVED_PASSWORD));
            }
            return;
        }
        if ((joinRequest.getPasswordHash() == null || joinRequest.getPasswordHash().isEmpty()) && joinRequest.getPassword() != null && !joinRequest.getPassword().isEmpty()) {
            joinRequest.setPasswordHash(this.passwordEncoder.encode((CharSequence)joinRequest.getPassword()));
        }
        if (joinRequest.getPasswordHash() == null || joinRequest.getPasswordHash().isEmpty()) {
            joinRequest.setPasswordHash(this.passwordEncoder.encode((CharSequence)DEFAULT_APPROVED_PASSWORD));
        }
    }

    private void provisionApprovedApplicant(JoinRequest joinRequest) {
        User existingLocalUser;
        this.prepareJoinRequestCredentials(joinRequest);
        Long authUserId = joinRequest.getAuthUserId();
        if (authUserId == null) {
            ApprovedApplicantProvisionDTO dto = new ApprovedApplicantProvisionDTO();
            dto.setTenantId(Long.valueOf(joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L));
            dto.setUsername(joinRequest.getUsername());
            dto.setEncodedPassword(joinRequest.getPasswordHash());
            dto.setSourceAuthUserId(joinRequest.getSourceAuthUserId());
            authUserId = this.authClient.provisionApprovedApplicant(dto);
            joinRequest.setAuthUserId(authUserId);
        }
        existingLocalUser = this.queryLocalUserByAuthUserId(authUserId);
        this.ensureLocalUser(joinRequest, authUserId);
    }

    private void ensureLocalUser(JoinRequest joinRequest, Long authUserId) {
        Long tenantId = joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L;
        String resolvedCellPhone = this.resolveCellPhone(tenantId, joinRequest.getPhone(), authUserId);
        Map<String, Object> payload = this.parseJsonMap(joinRequest.getFormPayloadJson(), this.normalizeJoinFormPayload(joinRequest));
        String resolvedUsername = this.firstNonBlank(this.resolveRegisterPayloadValue(payload, "username"), joinRequest.getUsername());
        String resolvedName = this.firstNonBlank(this.resolveRegisterPayloadValue(payload, "name", "realName"), joinRequest.getName());
        String resolvedEmail = this.firstNonBlank(this.resolveRegisterPayloadValue(payload, "email"), joinRequest.getEmail());
        String resolvedStudentId = this.firstNonBlank(this.resolveRegisterPayloadValue(payload, "studentId"), joinRequest.getStudentId());
        String resolvedCollege = this.resolveRegisterPayloadValue(payload, "college", "department", "faculty");
        String resolvedMajor = this.firstNonBlank(this.resolveRegisterPayloadValue(payload, "major"), joinRequest.getMajor());
        String resolvedGrade = this.firstNonBlank(this.resolveRegisterPayloadValue(payload, "grade"), joinRequest.getGrade());
        UserDTO userDTO = new UserDTO();
        userDTO.setId(authUserId);
        userDTO.setTenantId(tenantId);
        userDTO.setUsername(resolvedUsername);
        userDTO.setCellPhone(resolvedCellPhone);
        userDTO.setType(Integer.valueOf(UserType.STUDENT.getValue()));
        userDTO.setName(resolvedName);
        userDTO.setEmail(resolvedEmail);
        userDTO.setStudentId(resolvedStudentId);
        userDTO.setCollege(resolvedCollege);
        userDTO.setMajor(resolvedMajor);
        userDTO.setGrade(resolvedGrade);
        userDTO.setJoinDate(this.normalizeJoinTime(joinRequest.getReviewTime(), joinRequest.getCreateTime(), LocalDateTime.now()));
        this.executeWithTenantContext(tenantId, () -> this.userService.saveUser(userDTO));
        if (this.queryLocalUserByAuthUserId(authUserId) == null) {
            throw new IllegalStateException("\u5ba1\u6838\u901a\u8fc7\u540e\u672c\u5730\u7528\u6237\u521b\u5efa\u5931\u8d25");
        }
    }

    private User queryLocalUserByAuthUserId(Long authUserId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getAuthUserId, authUserId);
        return userMapper.selectOne(queryWrapper);
    }

    private String resolveCellPhone(Long tenantId, String requestedCellPhone, Long authUserId) {
        if (requestedCellPhone == null || requestedCellPhone.isBlank()) {
            return null;
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getTenantId, tenantId)
                .eq(User::getCellPhone, requestedCellPhone);
        User existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser == null || Objects.equals(existingUser.getAuthUserId(), authUserId)) {
            return requestedCellPhone;
        }
        log.warn("\u5165\u793e\u5ba1\u6838\u540c\u6b65\u7528\u6237\u65f6\u68c0\u6d4b\u5230\u624b\u673a\u53f7\u51b2\u7a81\uff0ctenantId={}, phone={}, existingAuthUserId={}, targetAuthUserId={}\uff0c\u672c\u6b21\u4e0d\u540c\u6b65\u624b\u673a\u53f7", new Object[]{tenantId, requestedCellPhone, existingUser.getAuthUserId(), authUserId});
        return null;
    }

    private void ensureClubMember(JoinRequest joinRequest, String previousStudentId) {
        Long tenantId = joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L;
        ClubMember clubMember = this.resolveApprovedJoinMember(tenantId, joinRequest.getStudentId(), previousStudentId);
        boolean restoringDeletedMember = clubMember != null && clubMember.getId() != null && Integer.valueOf(1).equals(clubMember.getIsDeleted());
        LocalDateTime now = LocalDateTime.now();
        if (clubMember == null) {
            ensureClubMemberQuota(tenantId);
            clubMember = new ClubMember();
            clubMember.setTenantId(tenantId);
            clubMember.setCreateTime(now);
        } else if (restoringDeletedMember) {
            ensureClubMemberQuota(tenantId);
        }
        clubMember.setName(joinRequest.getName());
        clubMember.setStudentId(joinRequest.getStudentId());
        clubMember.setEmail(joinRequest.getEmail());
        clubMember.setPhone(joinRequest.getPhone());
        clubMember.setMajor(joinRequest.getMajor());
        clubMember.setGrade(joinRequest.getGrade());
        clubMember.setPosition("\u6210\u5458");
        clubMember.setStatus("\u6d3b\u8dc3");
        clubMember.setJoinDate(now);
        clubMember.setUpdateTime(now);
        clubMember.setIsDeleted(0);
        if (clubMember.getId() == null) {
            this.clubMemberMapper.insert(clubMember);
        } else if (restoringDeletedMember) {
            this.clubMemberMapper.updateIncludingDeleted(clubMember);
        } else {
            this.clubMemberMapper.updateById(clubMember);
        }
    }

    private ClubMember resolveApprovedJoinMember(Long tenantId, String studentId, String previousStudentId) {
        ClubMember currentMember = this.findClubMemberByTenantAndStudentId(tenantId, studentId);
        if (StringUtils.isBlank((CharSequence)previousStudentId) || Objects.equals(previousStudentId, studentId)) {
            return currentMember;
        }
        ClubMember legacyMember = this.findClubMemberByTenantAndStudentId(tenantId, previousStudentId);
        if (currentMember == null) {
            return legacyMember;
        }
        if (legacyMember != null && legacyMember.getId() != null && !Objects.equals(legacyMember.getId(), currentMember.getId())) {
            this.clubMemberMapper.deleteById(legacyMember.getId());
        }
        return currentMember;
    }

    private void repairApprovedApplicationsForCurrentTenant() {
        LambdaQueryWrapper<JoinRequest> queryWrapper = new LambdaQueryWrapper<>();
        applyTenantScope(queryWrapper)
                .eq(JoinRequest::getReviewStatus, "\u901a\u8fc7")
                .orderByAsc(JoinRequest::getReviewTime);
        List<JoinRequest> approvedApplications = list(queryWrapper);
        for (JoinRequest joinRequest : approvedApplications) {
            boolean missingLocalUser;
            if (joinRequest == null) continue;
            boolean missingProvision = joinRequest.getAuthUserId() == null || joinRequest.getUsername() == null || joinRequest.getUsername().isBlank();
            boolean missingMember = !hasClubMember(joinRequest);
            boolean bl = missingLocalUser = !hasLocalUser(joinRequest);
            if (!missingProvision && !missingMember && !missingLocalUser) continue;
            try {
                provisionApprovedApplicant(joinRequest);
                updateById(joinRequest);
            }
            catch (Exception e) {
                log.warn("repair approved admission failed, studentId={}, tenantId={}, reason={}", new Object[]{joinRequest.getStudentId(), joinRequest.getTenantId(), e.getMessage()});
            }
        }
    }

    private boolean hasClubMember(JoinRequest joinRequest) {
        LambdaQueryWrapper<ClubMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClubMember::getStudentId, joinRequest.getStudentId())
                .eq(ClubMember::getTenantId, joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L)
                .last("LIMIT 1");
        return clubMemberMapper.selectOne(queryWrapper) != null;
    }

    private boolean hasLocalUser(JoinRequest joinRequest) {
        if (joinRequest.getAuthUserId() == null) {
            return false;
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getAuthUserId, joinRequest.getAuthUserId())
                .eq(User::getTenantId, joinRequest.getTenantId() != null ? joinRequest.getTenantId() : 1L)
                .last("LIMIT 1");
        return userMapper.selectOne(queryWrapper) != null;
    }

    private void repairApprovedRegisterReviewsForCurrentTenant() {
        LambdaQueryWrapper<RegisterReviewRequest> queryWrapper = new LambdaQueryWrapper<>();
        applyRegisterReviewTenantScope(queryWrapper)
                .eq(RegisterReviewRequest::getIsDeleted, 0)
                .eq(RegisterReviewRequest::getReviewStatus, "APPROVED")
                .orderByAsc(RegisterReviewRequest::getReviewTime, RegisterReviewRequest::getCreateTime);
        List<RegisterReviewRequest> approvedReviews = registerReviewRequestMapper.selectList(queryWrapper);
        for (RegisterReviewRequest request : approvedReviews) {
            if (request == null) continue;
            try {
                provisionApprovedRegisterReview(request);
                request.setUpdateTime(LocalDateTime.now());
                registerReviewRequestMapper.updateById(request);
            }
            catch (Exception e) {
                log.warn("repair approved register review failed, id={}, tenantId={}, reason={}", new Object[]{request.getId(), request.getTenantId(), e.getMessage()});
            }
        }
    }

    private void provisionApprovedRegisterReview(RegisterReviewRequest request) {
        User localUser;
        Map<String, Object> payload = this.parseJsonMap(request.getFormPayloadJson(), this.normalizeRegisterFormPayload(request.getUsername(), request.getName(), request.getEmail(), request.getReferralCode(), null));
        String studentId = this.resolveRegisterPayloadValue(payload, "studentId");
        String college = this.resolveRegisterPayloadValue(payload, "college", "department", "faculty");
        String major = this.resolveRegisterPayloadValue(payload, "major");
        String grade = this.resolveRegisterPayloadValue(payload, "grade");
        String phone = this.resolveRegisterPayloadValue(payload, "phone", "mobile", "cellPhone");
        String name = this.resolveRegisterPayloadValue(payload, "name", "realName");
        String email = this.resolveRegisterPayloadValue(payload, "email");
        String username = this.resolveRegisterPayloadValue(payload, "username");
        if (StringUtils.isBlank((CharSequence)username)) {
            username = request.getUsername();
        }
        if (StringUtils.isBlank((CharSequence)name)) {
            name = request.getName();
        }
        if (StringUtils.isBlank((CharSequence)email)) {
            email = request.getEmail();
        }
        localUser = this.resolveRegisterReviewLocalUser(request, username, email);
        if (request.getAuthUserId() == null && localUser != null && localUser.getAuthUserId() != null) {
            request.setAuthUserId(localUser.getAuthUserId());
        }
        if (request.getAuthUserId() != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(request.getAuthUserId());
            userDTO.setUsername(username);
            userDTO.setName(name);
            userDTO.setEmail(email);
            userDTO.setCellPhone(phone);
            userDTO.setStudentId(studentId);
            userDTO.setCollege(college);
            userDTO.setMajor(major);
            userDTO.setGrade(grade);
            userDTO.setJoinDate(this.normalizeJoinTime(request.getReviewTime(), request.getCreateTime(), LocalDateTime.now()));
            userDTO.setType(Integer.valueOf(UserType.STUDENT.getValue()));
            this.executeWithTenantContext(request.getTenantId(), () -> this.userService.saveUser(userDTO));
            localUser = this.resolveRegisterReviewLocalUser(request, username, email);
        }
        if (localUser == null) {
            return;
        }
    }

    private void syncRegisterReviewLocalUser(User localUser, RegisterReviewRequest request, String username, String name, String email, String phone, String studentId, String college, String major, String grade) {
        if (localUser == null) {
            return;
        }
        Long tenantId = localUser.getTenantId() != null ? localUser.getTenantId() : (request.getTenantId() != null ? request.getTenantId() : 1L);
        LocalDateTime now = LocalDateTime.now();
        localUser.setTenantId(tenantId);
        if (request.getAuthUserId() != null) {
            localUser.setAuthUserId(request.getAuthUserId());
        }
        if (StringUtils.isNotBlank((CharSequence)username)) {
            localUser.setUsername(username);
        }
        if (StringUtils.isNotBlank((CharSequence)name)) {
            localUser.setRealName(name);
            localUser.setNickname(name);
        }
        if (StringUtils.isNotBlank((CharSequence)email)) {
            localUser.setEmail(email);
        }
        if (StringUtils.isNotBlank((CharSequence)phone)) {
            localUser.setCellPhone(phone);
        }
        if (StringUtils.isNotBlank((CharSequence)studentId)) {
            localUser.setStudentId(studentId);
        }
        if (StringUtils.isNotBlank((CharSequence)college)) {
            localUser.setCollege(college);
        }
        if (StringUtils.isNotBlank((CharSequence)major)) {
            localUser.setMajor(major);
        }
        if (StringUtils.isNotBlank((CharSequence)grade)) {
            localUser.setGrade(grade);
        }
        localUser.setType(UserType.STUDENT);
        localUser.setStatus(UserStatus.NORMAL);
        localUser.setJoinTime(this.normalizeJoinTime(localUser.getJoinTime(), request.getReviewTime(), request.getCreateTime(), now));
        localUser.setUpdateTime(now);
        this.userMapper.updateById(localUser);
    }

    private ClubMember resolveRegisterReviewMember(Long tenantId, String studentId, String previousStudentId) {
        ClubMember currentMember = this.findClubMemberByTenantAndStudentId(tenantId, studentId);
        if (StringUtils.isBlank((CharSequence)previousStudentId) || Objects.equals(previousStudentId, studentId)) {
            return currentMember;
        }
        ClubMember legacyMember = this.findClubMemberByTenantAndStudentId(tenantId, previousStudentId);
        if (currentMember == null) {
            return legacyMember;
        }
        if (legacyMember != null && legacyMember.getId() != null && !Objects.equals(legacyMember.getId(), currentMember.getId())) {
            this.clubMemberMapper.deleteById(legacyMember.getId());
        }
        return currentMember;
    }

    private ClubMember findClubMemberByTenantAndStudentId(Long tenantId, String studentId) {
        if (tenantId == null || StringUtils.isBlank((CharSequence)studentId)) {
            return null;
        }
        LambdaQueryWrapper<ClubMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClubMember::getTenantId, tenantId)
                .eq(ClubMember::getStudentId, studentId)
                .last("LIMIT 1");
        ClubMember member = clubMemberMapper.selectOne(queryWrapper);
        if (member != null) {
            return member;
        }
        return clubMemberMapper.selectAnyByTenantAndStudentId(tenantId, studentId);
    }

    private void ensureClubMemberQuota(Long tenantId) {
        Integer maxClubMembers = resolveMaxClubMembers(tenantId);
        if (maxClubMembers == null || maxClubMembers <= 0) {
            return;
        }
        Integer currentCount = clubMemberMapper.selectCount(new LambdaQueryWrapper<ClubMember>()
                .eq(ClubMember::getTenantId, tenantId == null ? 1L : tenantId)
                .eq(ClubMember::getIsDeleted, 0));
        if (currentCount != null && currentCount >= maxClubMembers) {
            throw new BadRequestException("当前租户社团成员数已达到上限：" + maxClubMembers);
        }
    }

    private Integer resolveMaxClubMembers(Long tenantId) {
        TenantSelfServiceConfigDTO config = loadTenantSelfServiceConfig(tenantId);
        if (config == null || config.getQuotaSettings() == null) {
            return null;
        }
        return config.getQuotaSettings().getMaxClubMembers();
    }

    private User resolveRegisterReviewLocalUser(RegisterReviewRequest request, String username, String email) {
        Long tenantId = request.getTenantId() != null ? request.getTenantId() : 1L;
        if (request.getAuthUserId() != null) {
            LambdaQueryWrapper<User> byAuthUserIdQuery = new LambdaQueryWrapper<>();
            byAuthUserIdQuery.eq(User::getAuthUserId, request.getAuthUserId())
                    .eq(User::getTenantId, tenantId)
                    .last("LIMIT 1");
            User byAuthUserId = userMapper.selectOne(byAuthUserIdQuery);
            if (byAuthUserId != null) {
                return byAuthUserId;
            }
        }
        if (StringUtils.isNotBlank((CharSequence)username)) {
            LambdaQueryWrapper<User> byUsernameQuery = new LambdaQueryWrapper<>();
            byUsernameQuery.eq(User::getTenantId, tenantId)
                    .eq(User::getUsername, username)
                    .last("LIMIT 1");
            User byUsername = userMapper.selectOne(byUsernameQuery);
            if (byUsername != null) {
                return byUsername;
            }
        }
        if (StringUtils.isNotBlank((CharSequence)email)) {
            LambdaQueryWrapper<User> byEmailQuery = new LambdaQueryWrapper<>();
            byEmailQuery.eq(User::getTenantId, tenantId)
                    .eq(User::getEmail, email)
                    .last("LIMIT 1");
            return userMapper.selectOne(byEmailQuery);
        }
        return null;
    }

    private String resolveRegisterPayloadValue(Map<String, Object> payload, String ... keys) {
        if (payload == null || payload.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            String text;
            Object value;
            if (!payload.containsKey(key) || (value = payload.get(key)) == null || (text = value.toString().trim()).isEmpty()) continue;
            return text;
        }
        return null;
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.isNotBlank((CharSequence)primary) ? primary : fallback;
    }

    private LocalDateTime normalizeJoinTime(LocalDateTime ... candidates) {
        if (candidates == null) {
            return LocalDateTime.now();
        }
        for (LocalDateTime candidate : candidates) {
            if (candidate == null) continue;
            return candidate.toLocalDate().atStartOfDay();
        }
        return LocalDateTime.now();
    }

    private void clearApplicationCache(String studentId) {
        try {
            redisTemplate.delete(ADMISSION_CACHE_PREFIX + "student:" + studentId);
            redisTemplate.delete(ADMISSION_CACHE_PREFIX + "status:" + studentId);
        }
        catch (Exception e) {
            log.warn("\u6e05\u9664\u7533\u8bf7\u7f13\u5b58\u5931\u8d25", (Throwable)e);
        }
    }

    private void clearStatisticsCache() {
        try {
            redisTemplate.delete(ADMISSION_STATS_CACHE);
            redisTemplate.delete(buildTenantStatisticsCacheKey(currentTenantId()));
            redisTemplate.delete(buildAllTenantsStatisticsCacheKey());
        }
        catch (Exception e) {
            log.warn("\u6e05\u9664\u7edf\u8ba1\u7f13\u5b58\u5931\u8d25", (Throwable)e);
        }
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private LambdaQueryWrapper<JoinRequest> applyTenantScope(LambdaQueryWrapper<JoinRequest> queryWrapper) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            queryWrapper.eq(JoinRequest::getTenantId, (Object)this.currentTenantId());
            return queryWrapper;
        }
        if (this.canManageAllTenants()) {
            this.applyActiveTenantScope(queryWrapper);
            return queryWrapper;
        }
        queryWrapper.eq(JoinRequest::getTenantId, (Object)this.currentTenantId());
        return queryWrapper;
    }

    private LambdaQueryWrapper<JoinRequest> applyReadableTenantScope(LambdaQueryWrapper<JoinRequest> queryWrapper) {
        if (this.canManageAllTenants()) {
            this.applyActiveTenantScope(queryWrapper);
            return queryWrapper;
        }
        return this.applyTenantScope(queryWrapper);
    }

    private LambdaQueryWrapper<RegisterReviewRequest> applyRegisterReviewTenantScope(LambdaQueryWrapper<RegisterReviewRequest> queryWrapper) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            queryWrapper.eq(RegisterReviewRequest::getTenantId, currentTenantId());
            return queryWrapper;
        }
        if (canManageAllTenants()) {
            List<Long> activeTenantIds = resolveActiveTenantIds();
            if (activeTenantIds.isEmpty()) {
                queryWrapper.eq(RegisterReviewRequest::getTenantId, -1L);
            } else {
                queryWrapper.in(RegisterReviewRequest::getTenantId, activeTenantIds);
            }
            return queryWrapper;
        }
        queryWrapper.eq(RegisterReviewRequest::getTenantId, currentTenantId());
        return queryWrapper;
    }

    private String buildStatisticsCacheKey() {
        if (this.canManageAllTenants()) {
            return this.buildAllTenantsStatisticsCacheKey();
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return this.buildTenantStatisticsCacheKey(tenantId);
        }
        return this.buildTenantStatisticsCacheKey(this.currentTenantId());
    }

    private String buildTenantStatisticsCacheKey(Long tenantId) {
        return "admission:statistics:" + tenantId;
    }

    private String buildAllTenantsStatisticsCacheKey() {
        return "admission:statistics:all";
    }

    private boolean canManageAllTenants() {
        return Boolean.TRUE.equals(TenantContext.isSuperAdmin());
    }

    private void applyActiveTenantScope(LambdaQueryWrapper<JoinRequest> queryWrapper) {
        List<Long> activeTenantIds = this.resolveActiveTenantIds();
        if (activeTenantIds.isEmpty()) {
            queryWrapper.eq(JoinRequest::getTenantId, -1L);
            return;
        }
        queryWrapper.in(JoinRequest::getTenantId, activeTenantIds);
    }

    private List<Long> resolveActiveTenantIds() {
        LambdaQueryWrapper<RKTenant> tenantQuery = new LambdaQueryWrapper<>();
        tenantQuery.select(RKTenant::getId)
                .eq(RKTenant::getStatus, 1)
                .eq(RKTenant::getIsDeleted, 0)
                .and(query -> query.isNull(RKTenant::getExpireTime)
                        .or()
                        .gt(RKTenant::getExpireTime, LocalDateTime.now()));
        return tenantMapper.selectList(tenantQuery).stream()
                .map(RKTenant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private <T> T executeWithTenantContext(Long tenantId, Supplier<T> supplier) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            if (tenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId((Long)tenantId);
            }
            T t = supplier.get();
            return t;
        }
        finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId((Long)previousTenantId);
            }
            TenantContext.setSuperAdmin((Boolean)previousSuperAdmin);
        }
    }

    @ConstructorProperties(value={"redisTemplate", "passwordEncoder", "authClient", "emailCenterService", "emailVerificationService", "referralCodeService", "userService", "userMapper", "tenantMapper", "referralCodeMapper", "clubMemberMapper", "emailTemplateClient", "registerReviewRequestMapper", "reviewActionTokenService", "tenantWorkflowConfigService", "systemConfigMapper", "objectMapper", "stringRedisTemplate"})
    @Generated
    public AdmissionServiceImpl(RedisTemplate<String, Object> redisTemplate, PasswordEncoder passwordEncoder, AuthClient authClient, IEmailCenterService emailCenterService, IEmailVerificationService emailVerificationService, IReferralCodeService referralCodeService, IUserService userService, UserMapper userMapper, RKTenantMapper tenantMapper, ReferralCodeMapper referralCodeMapper, ClubMemberMapper clubMemberMapper, EmailTemplateClient emailTemplateClient, RegisterReviewRequestMapper registerReviewRequestMapper, ReviewActionTokenServiceImpl reviewActionTokenService, ITenantWorkflowConfigService tenantWorkflowConfigService, SystemConfigMapper systemConfigMapper, ObjectMapper objectMapper, StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.authClient = authClient;
        this.emailCenterService = emailCenterService;
        this.emailVerificationService = emailVerificationService;
        this.referralCodeService = referralCodeService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
        this.referralCodeMapper = referralCodeMapper;
        this.clubMemberMapper = clubMemberMapper;
        this.emailTemplateClient = emailTemplateClient;
        this.registerReviewRequestMapper = registerReviewRequestMapper;
        this.reviewActionTokenService = reviewActionTokenService;
        this.tenantWorkflowConfigService = tenantWorkflowConfigService;
        this.systemConfigMapper = systemConfigMapper;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }
}
