package com.tianji.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.auth.ApprovedApplicantProvisionDTO;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.auth.CurrentUserPasswordUpdateDTO;
import com.tianji.api.dto.auth.RoleAccountDTO;
import com.tianji.api.dto.auth.RoleRecipientQueryDTO;
import com.tianji.api.dto.auth.SwitchableTenantDTO;
import com.tianji.api.dto.user.EmailLoginCandidateDTO;
import com.tianji.api.dto.user.EmailLoginCandidateQueryDTO;
import com.tianji.api.dto.user.EmailVerificationVerifyDTO;
import com.tianji.api.dto.user.RegisterSuccessNotifyDTO;
import com.tianji.api.dto.user.TenantSelfServiceAdmissionPolicyDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.auth.common.constants.JwtConstants;
import com.tianji.auth.domain.dto.EmailLoginConfirmDTO;
import com.tianji.auth.domain.dto.EmailLoginPrepareDTO;
import com.tianji.auth.domain.dto.LoginDTO;
import com.tianji.auth.domain.dto.RegisterDTO;
import com.tianji.auth.domain.po.AccountRole;
import com.tianji.auth.domain.po.LoginRecord;
import com.tianji.auth.domain.po.Role;
import com.tianji.auth.domain.po.User;
import com.tianji.auth.domain.vo.EmailLoginCandidateVO;
import com.tianji.auth.domain.vo.EmailLoginPrepareVO;
import com.tianji.auth.domain.vo.LoginVO;
import com.tianji.auth.mapper.AccountRoleMapper;
import com.tianji.auth.mapper.LoginRecordMapper;
import com.tianji.auth.mapper.RoleMapper;
import com.tianji.auth.mapper.UserMapper;
import com.tianji.auth.service.IAuthService;
import com.tianji.auth.service.IRoleService;
import com.tianji.auth.util.JwtTool;
import com.tianji.common.domain.dto.LoginUserDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TOKEN_REDIS_KEY_PREFIX = JwtConstants.SESSION_REDIS_KEY_PREFIX;
    private static final String EMAIL_LOGIN_TICKET_REDIS_KEY_PREFIX = "rk:auth:email-login:ticket:";
    private static final long EMAIL_LOGIN_TICKET_TTL_MINUTES = 5;
    private static final String DEFAULT_MANAGED_USER_PASSWORD = "123456";
    private static final long DORMANT_LOGIN_DAYS = 3;

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserClient userClient;
    private final RoleMapper roleMapper;
    private final AccountRoleMapper accountRoleMapper;
    private final LoginRecordMapper loginRecordMapper;
    private final IRoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTool jwtTool;

    @Override
    @Transactional
    public LoginVO login(LoginDTO dto) {
        log.info("用户登录请求: username={}, organizationId={}", dto.getUsername(), dto.getOrganizationId());

        TenantContext.setSuperAdmin(true);
        TenantContext.setTenantId(1L);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, dto.getUsername())
                .eq(User::getIsDeleted, 0);

        Long tenantId = null;
        if (dto.getOrganizationId() != null && !dto.getOrganizationId().isEmpty()) {
            try {
                tenantId = Long.parseLong(dto.getOrganizationId());
                queryWrapper.eq(User::getTenantId, tenantId);
            } catch (NumberFormatException e) {
                log.warn("无效的 organizationId: {}", dto.getOrganizationId());
            }
        }

        User user;
        if (tenantId == null) {
            List<User> users = userMapper.selectList(queryWrapper);
            if (users.isEmpty()) {
                TenantContext.clear();
                throw new BadRequestException("用户名不存在");
            }
            if (users.size() > 1) {
                TenantContext.clear();
                throw new BadRequestException("请先选择租户再登录");
            }
            user = users.get(0);
        } else {
            user = userMapper.selectOne(queryWrapper);
        }
        if (user == null) {
            TenantContext.clear();
            throw new BadRequestException("用户名不存在");
        }

        tenantId = user.getTenantId();
        roleService.listAssignableRoles(tenantId);
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            TenantContext.clear();
            throw new BadRequestException("密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            TenantContext.clear();
            throw new BadRequestException("用户已被禁用，请联系管理员");
        }

        enforceDormantLoginVerification(user, dto, tenantId);

        LambdaQueryWrapper<AccountRole> roleQueryWrapper = new LambdaQueryWrapper<>();
        roleQueryWrapper.eq(AccountRole::getAccountId, user.getId());
        List<AccountRole> accountRoles = accountRoleMapper.selectList(roleQueryWrapper);
        List<AccountRole> sessionRoles = normalizeSessionRoles(accountRoles, tenantId);

        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setUserId(user.getId());
        loginUserDTO.setUsername(user.getUsername());
        loginUserDTO.setOrganizationId(user.getTenantId().toString());
        loginUserDTO.setTenantId(user.getTenantId());
        loginUserDTO.setClientType(normalizeClientType(dto.getClientType()));
        if (!sessionRoles.isEmpty()) {
            loginUserDTO.setRoleId(sessionRoles.get(0).getRoleId());
        }

        String token = jwtTool.createToken(loginUserDTO);
        String refreshToken = jwtTool.createRefreshToken(loginUserDTO);

        storeSessionToken(loginUserDTO, token);

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        recordLoginSuccess(user);
        TenantContext.clear();

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setOrganizationId(user.getTenantId().toString());
        loginVO.setRoles(sessionRoles);
        applyLoginAvatar(loginVO, user.getId());
        return loginVO;
    }

    @Override
    public EmailLoginPrepareVO prepareEmailLogin(EmailLoginPrepareDTO dto) {
        EmailVerificationVerifyDTO verifyDTO = new EmailVerificationVerifyDTO();
        verifyDTO.setEmail(dto.getEmail());
        verifyDTO.setTenantId(dto.getTenantId() == null ? 0L : dto.getTenantId());
        verifyDTO.setScene("LOGIN");
        verifyDTO.setCode(dto.getEmailCode());
        Boolean verified = userClient.verifyEmailVerificationCode(verifyDTO);
        if (!Boolean.TRUE.equals(verified)) {
            throw new BadRequestException("Email verification failed");
        }

        EmailLoginCandidateQueryDTO queryDTO = new EmailLoginCandidateQueryDTO();
        queryDTO.setEmail(dto.getEmail());
        queryDTO.setTenantId(dto.getTenantId());
        List<EmailLoginCandidateVO> candidates = normalizeEmailLoginCandidates(userClient.queryEmailLoginCandidates(queryDTO));
        if (candidates.isEmpty()) {
            throw new BadRequestException("No available account for this email");
        }

        String loginTicket = UUID.randomUUID().toString();
        EmailLoginTicketPayload payload = new EmailLoginTicketPayload();
        payload.setEmail(dto.getEmail());
        payload.setCandidates(candidates);
        stringRedisTemplate.opsForValue().set(
                buildEmailLoginTicketKey(loginTicket),
                writeEmailLoginTicket(payload),
                EMAIL_LOGIN_TICKET_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        EmailLoginPrepareVO result = new EmailLoginPrepareVO();
        result.setLoginTicket(loginTicket);
        result.setCandidates(candidates);
        return result;
    }

    @Override
    public LoginVO confirmEmailLogin(EmailLoginConfirmDTO dto) {
        String ticketKey = buildEmailLoginTicketKey(dto.getLoginTicket());
        String ticketPayload = stringRedisTemplate.opsForValue().get(ticketKey);
        if (StrUtil.isBlank(ticketPayload)) {
            throw new BadRequestException("Email login ticket expired");
        }
        EmailLoginTicketPayload payload = readEmailLoginTicket(ticketPayload);
        EmailLoginCandidateVO candidate = payload.getCandidates() == null ? null : payload.getCandidates().stream()
                .filter(item -> item.getAuthUserId() != null && item.getAuthUserId().equals(dto.getAuthUserId()))
                .filter(item -> item.getTenantId() != null && item.getTenantId().equals(dto.getTenantId()))
                .filter(item -> dto.getRoleId() == null || dto.getRoleId().equals(item.getRoleId()))
                .findFirst()
                .orElse(null);
        if (candidate == null) {
            throw new BadRequestException("Email login candidate mismatch");
        }

        User user = lookupAuthUserWithoutTenantScope(dto.getAuthUserId());
        if (!isAvailableForEmailLogin(user, dto.getTenantId())) {
            throw new BadRequestException("Auth account unavailable");
        }

        LoginVO loginVO = buildLoginSession(user, "email_code", candidate.getRoleId());
        stringRedisTemplate.delete(ticketKey);
        return loginVO;
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        log.info("用户注册请求: username={}, organizationId={}", dto.getUsername(), dto.getOrganizationId());

        Long tenantId = 1L;
        if (dto.getOrganizationId() != null && !dto.getOrganizationId().isEmpty()) {
            try {
                tenantId = Long.parseLong(dto.getOrganizationId());
            } catch (NumberFormatException e) {
                log.warn("无效的 organizationId: {}, 使用默认租户ID: 1", dto.getOrganizationId());
            }
        }

        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setTenantId(tenantId);
            TenantContext.setSuperAdmin(false);

            TenantSelfServiceAdmissionPolicyDTO admissionPolicy = userClient.queryTenantSelfServiceAdmissionPolicy(tenantId);
            if (admissionPolicy != null && Boolean.FALSE.equals(admissionPolicy.getAllowPublicRegister())) {
                throw new BadRequestException("当前租户已关闭公开注册");
            }

            EmailVerificationVerifyDTO verifyDTO = new EmailVerificationVerifyDTO();
            verifyDTO.setEmail(dto.getEmail());
            verifyDTO.setTenantId(tenantId);
            verifyDTO.setScene("REGISTER");
            verifyDTO.setCode(dto.getEmailCode());
            Boolean verified = userClient.verifyEmailVerificationCode(verifyDTO);
            if (!Boolean.TRUE.equals(verified)) {
                throw new BadRequestException("邮箱验证码校验失败");
            }
            if (dto.getReferralCode() != null && !dto.getReferralCode().isBlank() &&
                    (dto.getInviteToken() == null || dto.getInviteToken().isBlank())) {
                Boolean referralValid = userClient.validateReferralCodeInternal(dto.getReferralCode(), tenantId);
                if (!Boolean.TRUE.equals(referralValid)) {
                    throw new BadRequestException("内推码校验失败");
                }
            }

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, dto.getUsername())
                    .eq(User::getTenantId, tenantId)
                    .eq(User::getIsDeleted, 0);
            User existUser = userMapper.selectOne(queryWrapper);
            if (existUser != null) {
                throw new BadRequestException("用户名已存在");
            }

            TenantWorkflowConfigDTO workflowConfig = userClient.queryCurrentTenantWorkflowConfig(tenantId);
            boolean requireApproval = workflowConfig == null
                    || workflowConfig.getRegistration() == null
                    || (!Boolean.TRUE.equals(workflowConfig.getRegistration().getOpenRegistration())
                    && !Boolean.FALSE.equals(workflowConfig.getRegistration().getRequireApproval()));

            User user = new User();
            user.setUsername(dto.getUsername());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setIdentityKey(dto.getUsername());
            user.setTenantId(tenantId);
            user.setStatus(requireApproval ? 0 : 1);
            user.setIsDeleted(0);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);

            Role defaultRole = getOrCreateDefaultUserRole(tenantId);
            AccountRole accountRole = new AccountRole();
            accountRole.setAccountId(user.getId());
            accountRole.setRoleId(defaultRole.getId());
            accountRole.setCreateTime(LocalDateTime.now());
            accountRole.setUpdateTime(LocalDateTime.now());
            accountRoleMapper.insert(accountRole);

            if (!requireApproval) {
                try {
                    Map<String, Object> formPayload = dto.getFormPayload();
                    UserDTO userDTO = new UserDTO();
                    userDTO.setId(user.getId());
                    userDTO.setTenantId(tenantId);
                    userDTO.setUsername(dto.getUsername());
                    userDTO.setCellPhone(dto.getPhone());
                    userDTO.setType(UserType.STUDENT.getValue());
                    userDTO.setName(dto.getName() != null ? dto.getName() : dto.getUsername());
                    userDTO.setEmail(dto.getEmail());
                    userDTO.setStudentId(resolveRegisterPayloadValue(formPayload, "studentId"));
                    userDTO.setCollege(resolveRegisterPayloadValue(formPayload, "college", "department", "faculty"));
                    userDTO.setMajor(resolveRegisterPayloadValue(formPayload, "major"));
                    userDTO.setGrade(resolveRegisterPayloadValue(formPayload, "grade"));
                    Long rkUserId = userClient.syncUserOnRegister(userDTO);
                    log.info("用户同步到 rk_user 成功: rkUserId={}, username={}", rkUserId, dto.getUsername());
                } catch (Exception e) {
                    log.error("同步用户到 rk_user 失败: username={}, error={}", dto.getUsername(), e.getMessage(), e);
                }
            }

            if (dto.getInviteToken() != null && !dto.getInviteToken().isBlank()) {
                try {
                    userClient.markInvitationRegisterSuccess(dto.getInviteToken(), dto.getReferralCode(), dto.getEmail(), user.getId());
                } catch (Exception e) {
                    log.error("标记邀请注册成功失败: inviteToken={}, username={}, error={}",
                            dto.getInviteToken(), dto.getUsername(), e.getMessage(), e);
                }
            } else if (dto.getReferralCode() != null && !dto.getReferralCode().isBlank()) {
                try {
                    userClient.markGenericReferralRegisterSuccess(user.getId(), tenantId, dto.getReferralCode(), dto.getEmail(), user.getId());
                } catch (Exception e) {
                    log.error("标记普通注册内推转化成功失败: referralCode={}, username={}, error={}",
                            dto.getReferralCode(), dto.getUsername(), e.getMessage(), e);
                }
            }

            if (requireApproval) {
                try {
                    RegisterSuccessNotifyDTO notifyDTO = new RegisterSuccessNotifyDTO();
                    notifyDTO.setTenantId(tenantId);
                    notifyDTO.setAuthUserId(user.getId());
                    notifyDTO.setUsername(dto.getUsername());
                    notifyDTO.setName(dto.getName());
                    notifyDTO.setEmail(dto.getEmail());
                    notifyDTO.setReferralCode(dto.getReferralCode());
                    notifyDTO.setFormPayload(dto.getFormPayload());
                    userClient.notifyRegisterSuccess(notifyDTO);
                } catch (Exception e) {
                    log.error("发送注册成功通知失败: tenantId={}, username={}, error={}",
                            tenantId, dto.getUsername(), e.getMessage(), e);
                }
            }

            log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        } finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(previousTenantId);
            }
            TenantContext.setSuperAdmin(previousSuperAdmin);
        }
    }

    @Override
    public void logout(String token) {
        log.info("用户登出");
        try {
            LoginUserDTO loginUserDTO = jwtTool.parseToken(token);
            if (loginUserDTO != null && loginUserDTO.getUserId() != null) {
                stringRedisTemplate.delete(TOKEN_REDIS_KEY_PREFIX + loginUserDTO.getUserId());
            }
        } catch (Exception e) {
            log.warn("登出时解析 token 失败: {}", e.getMessage());
        }
    }

    @Override
    public String refreshToken(String refreshToken) {
        try {
            LoginUserDTO loginUserDTO = jwtTool.parseRefreshToken(normalizeBearerToken(refreshToken));
            String token = jwtTool.createToken(loginUserDTO);
            storeSessionToken(loginUserDTO, token);
            return token;
        } catch (Exception e) {
            log.error("刷新 token 失败", e);
            throw new BadRequestException("刷新token失败：无效的refresh token");
        }
    }

    @Override
    @Transactional
    public Long provisionApprovedApplicant(ApprovedApplicantProvisionDTO dto) {
        return executeWithoutTenantIsolation(() -> doProvisionApprovedApplicant(dto));
    }

    private Long doProvisionApprovedApplicant(ApprovedApplicantProvisionDTO dto) {
        log.info("审核通过后创建认证账号: username={}, tenantId={}", dto.getUsername(), dto.getTenantId());

        User sourceUser = dto.getSourceAuthUserId() == null ? null : userMapper.selectById(dto.getSourceAuthUserId());
        String username = sourceUser != null ? sourceUser.getUsername() : dto.getUsername();
        String encodedPassword = sourceUser != null ? sourceUser.getPassword() : dto.getEncodedPassword();
        String identityKey = resolveIdentityKey(sourceUser, username);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getTenantId, dto.getTenantId())
                .eq(User::getIsDeleted, 0)
                .and(wrapper -> wrapper.eq(User::getIdentityKey, identityKey).or().eq(User::getUsername, username));
        User existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            if (existingUser.getIdentityKey() == null || existingUser.getIdentityKey().isBlank()) {
                existingUser.setIdentityKey(identityKey);
                userMapper.updateById(existingUser);
            }
            ensureDefaultUserRole(existingUser.getId(), dto.getTenantId());
            return existingUser.getId();
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setIdentityKey(identityKey);
        user.setTenantId(dto.getTenantId());
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        ensureDefaultUserRole(user.getId(), dto.getTenantId());
        return user.getId();
    }

    @Override
    public List<RoleAccountDTO> queryAccountsByRoles(RoleRecipientQueryDTO dto) {
        return executeWithoutTenantIsolation(() -> doQueryAccountsByRoles(dto));
    }

    private List<RoleAccountDTO> doQueryAccountsByRoles(RoleRecipientQueryDTO dto) {
        if (dto == null || dto.getTenantId() == null || dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<AccountRole> accountRoleQueryWrapper = new LambdaQueryWrapper<>();
        accountRoleQueryWrapper.in(AccountRole::getRoleId, dto.getRoleIds());
        List<AccountRole> accountRoles = accountRoleMapper.selectList(accountRoleQueryWrapper);
        if (accountRoles.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> accountIds = accountRoles.stream()
                .map(AccountRole::getAccountId)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, accountIds)
                .eq(User::getTenantId, dto.getTenantId())
                .eq(User::getIsDeleted, 0);
        List<User> users = userMapper.selectList(userQueryWrapper);
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        return users.stream().map(user -> {
            RoleAccountDTO item = new RoleAccountDTO();
            item.setAccountId(user.getId());
            item.setUsername(user.getUsername());
            item.setTenantId(user.getTenantId());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public AdminUserProvisionDTO queryAdminUserById(Long authUserId) {
        return executeWithoutTenantIsolation(() -> doQueryAdminUserById(authUserId));
    }

    private AdminUserProvisionDTO doQueryAdminUserById(Long authUserId) {
        User user = userMapper.selectById(authUserId);
        if (user == null || user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            return null;
        }

        AdminUserProvisionDTO dto = new AdminUserProvisionDTO();
        dto.setAuthUserId(user.getId());
        dto.setTenantId(user.getTenantId());
        dto.setUsername(user.getUsername());
        dto.setStatus(user.getStatus());
        dto.setRoleId(queryPrimaryRoleId(user.getId()));
        return dto;
    }

    @Override
    @Transactional
    public Long provisionAdminUser(AdminUserProvisionDTO dto) {
        return executeWithoutTenantIsolation(() -> doProvisionAdminUser(dto));
    }

    private Long doProvisionAdminUser(AdminUserProvisionDTO dto) {
        if (dto == null) {
            throw new BadRequestException("参数不能为空");
        }
        Long tenantId = dto.getTenantId() == null ? 1L : dto.getTenantId();
        String username = resolveUsername(dto.getUsername(), dto.getCellPhone());
        String rawPassword = StrUtil.isBlank(dto.getPassword()) ? DEFAULT_MANAGED_USER_PASSWORD : dto.getPassword();

        validateUsernameUniqueness(username, tenantId, null);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setIdentityKey(username);
        user.setTenantId(tenantId);
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setIsDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        assignPrimaryRole(user.getId(), tenantId, dto.getRoleId());
        return user.getId();
    }

    @Override
    @Transactional
    public void updateAdminUser(Long authUserId, AdminUserProvisionDTO dto) {
        executeWithoutTenantIsolation(() -> {
            doUpdateAdminUser(authUserId, dto);
            return null;
        });
    }

    private void doUpdateAdminUser(Long authUserId, AdminUserProvisionDTO dto) {
        User existing = userMapper.selectById(authUserId);
        if (existing == null || existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            throw new BadRequestException("认证账号不存在");
        }

        Long tenantId = dto.getTenantId() == null ? existing.getTenantId() : dto.getTenantId();
        String username = StrUtil.isBlank(dto.getUsername()) ? existing.getUsername() : dto.getUsername();

        validateUsernameUniqueness(username, tenantId, authUserId);
        existing.setUsername(username);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        existing.setTenantId(tenantId);
        existing.setStatus(dto.getStatus() == null ? existing.getStatus() : dto.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);

        if (dto.getRoleId() != null) {
            assignPrimaryRole(authUserId, tenantId, dto.getRoleId());
        }
    }

    @Override
    @Transactional
    public void updateAdminUserStatus(Long authUserId, Integer status) {
        executeWithoutTenantIsolation(() -> {
            doUpdateAdminUserStatus(authUserId, status);
            return null;
        });
    }

    private void doUpdateAdminUserStatus(Long authUserId, Integer status) {
        User existing = userMapper.selectById(authUserId);
        if (existing == null || existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            throw new BadRequestException("认证账号不存在");
        }
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void resetAdminUserPassword(Long authUserId, String password) {
        executeWithoutTenantIsolation(() -> {
            doResetAdminUserPassword(authUserId, password);
            return null;
        });
    }

    @Override
    @Transactional
    public void updateCurrentUserPassword(Long authUserId, CurrentUserPasswordUpdateDTO dto) {
        executeWithoutTenantIsolation(() -> {
            doUpdateCurrentUserPassword(authUserId, dto);
            return null;
        });
    }

    private void doResetAdminUserPassword(Long authUserId, String password) {
        User existing = userMapper.selectById(authUserId);
        if (existing == null || existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            throw new BadRequestException("认证账号不存在");
        }
        existing.setPassword(passwordEncoder.encode(password));
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);
    }

    private void doUpdateCurrentUserPassword(Long authUserId, CurrentUserPasswordUpdateDTO dto) {
        if (dto == null || StrUtil.isBlank(dto.getOldPassword()) || StrUtil.isBlank(dto.getNewPassword())) {
            throw new BadRequestException("old/new password must not be blank");
        }
        User existing = userMapper.selectById(authUserId);
        if (existing == null || existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            throw new BadRequestException("auth account not found");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), existing.getPassword())) {
            throw new BadRequestException("old password invalid");
        }
        existing.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteAdminUser(Long authUserId) {
        executeWithoutTenantIsolation(() -> {
            doDeleteAdminUser(authUserId);
            return null;
        });
    }

    private void doDeleteAdminUser(Long authUserId) {
        User existing = userMapper.selectById(authUserId);
        if (existing == null || existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            return;
        }
        userMapper.deleteById(authUserId);

        LambdaQueryWrapper<AccountRole> accountRoleQueryWrapper = new LambdaQueryWrapper<>();
        accountRoleQueryWrapper.eq(AccountRole::getAccountId, authUserId);
        accountRoleMapper.delete(accountRoleQueryWrapper);
    }

    @Override
    public List<SwitchableTenantDTO> querySwitchableTenants(String authorization) {
        return executeWithoutTenantIsolation(() -> {
            User currentUser = resolveCurrentUserFromAuthorization(authorization);
            String identityKey = resolveIdentityKey(currentUser, currentUser.getUsername());
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getIsDeleted, 0)
                    .eq(User::getStatus, 1)
                    .and(item -> item.eq(User::getIdentityKey, identityKey).or().eq(User::getUsername, currentUser.getUsername()));
            List<SwitchableTenantDTO> result = new ArrayList<>();
            for (User user : userMapper.selectList(wrapper)) {
                List<AccountRole> sessionRoles = querySessionRoles(user.getId(), user.getTenantId());
                if (sessionRoles.isEmpty()) {
                    result.add(buildSwitchableTenantDTO(user, null));
                    continue;
                }
                for (AccountRole sessionRole : sessionRoles) {
                    result.add(buildSwitchableTenantDTO(user, sessionRole.getRoleId()));
                }
            }
            return result;
        });
    }

    @Override
    public LoginVO switchTenant(String authorization, Long tenantId) {
        return switchTenant(authorization, tenantId, null);
    }

    @Override
    public LoginVO switchTenant(String authorization, Long tenantId, Long roleId) {
        return executeWithoutTenantIsolation(() -> {
            LoginUserDTO currentSession = resolveLoginUserDTOFromAuthorization(authorization);
            User currentUser = resolveCurrentUser(currentSession);
            String identityKey = resolveIdentityKey(currentUser, currentUser.getUsername());

            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getTenantId, tenantId)
                    .eq(User::getIsDeleted, 0)
                    .eq(User::getStatus, 1)
                    .and(item -> item.eq(User::getIdentityKey, identityKey).or().eq(User::getUsername, currentUser.getUsername()))
                    .last("LIMIT 1");
            User targetUser = userMapper.selectOne(wrapper);
            if (targetUser == null) {
                throw new BadRequestException("目标租户账号不存在");
            }

            List<AccountRole> sessionRoles = selectSessionRoles(targetUser.getId(), targetUser.getTenantId(), roleId);
            LoginUserDTO loginUserDTO = new LoginUserDTO();
            loginUserDTO.setUserId(targetUser.getId());
            loginUserDTO.setTenantId(targetUser.getTenantId());
            loginUserDTO.setOrganizationId(String.valueOf(targetUser.getTenantId()));
            if (!sessionRoles.isEmpty()) {
                loginUserDTO.setRoleId(sessionRoles.get(0).getRoleId());
            }
            loginUserDTO.setUsername(targetUser.getUsername());
            loginUserDTO.setClientType(normalizeClientType(currentSession.getClientType()));

            String token = jwtTool.createToken(loginUserDTO);
            String refreshToken = jwtTool.createRefreshToken(loginUserDTO);
            storeSessionToken(loginUserDTO, token);

            LoginVO vo = new LoginVO();
            vo.setToken(token);
            vo.setRefreshToken(refreshToken);
            vo.setUserId(targetUser.getId());
            vo.setUsername(targetUser.getUsername());
            vo.setOrganizationId(String.valueOf(targetUser.getTenantId()));
            vo.setRoles(sessionRoles);
            applyLoginAvatar(vo, targetUser.getId());
            return vo;
        });
    }

    private void ensureDefaultUserRole(Long accountId, Long tenantId) {
        LambdaQueryWrapper<AccountRole> accountRoleQueryWrapper = new LambdaQueryWrapper<>();
        accountRoleQueryWrapper.eq(AccountRole::getAccountId, accountId);
        if (!accountRoleMapper.selectList(accountRoleQueryWrapper).isEmpty()) {
            return;
        }

        Role defaultRole = getOrCreateDefaultUserRole(tenantId);

        AccountRole accountRole = new AccountRole();
        accountRole.setAccountId(accountId);
        accountRole.setRoleId(defaultRole.getId());
        accountRole.setCreateTime(LocalDateTime.now());
        accountRole.setUpdateTime(LocalDateTime.now());
        accountRoleMapper.insert(accountRole);
    }

    private void validateUsernameUniqueness(String username, Long tenantId, Long ignoredAuthUserId) {
        if (StrUtil.isBlank(username)) {
            throw new BadRequestException("用户名不能为空");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username)
                .eq(User::getTenantId, tenantId)
                .eq(User::getIsDeleted, 0);
        if (ignoredAuthUserId != null) {
            queryWrapper.ne(User::getId, ignoredAuthUserId);
        }
        if (userMapper.selectOne(queryWrapper) != null) {
            throw new BadRequestException("用户名已存在");
        }
    }

    private Long queryPrimaryRoleId(Long accountId) {
        LambdaQueryWrapper<AccountRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AccountRole::getAccountId, accountId);
        List<AccountRole> accountRoles = accountRoleMapper.selectList(queryWrapper);
        return accountRoles.isEmpty() ? null : accountRoles.get(0).getRoleId();
    }

    private List<AccountRole> querySessionRoles(Long accountId, Long tenantId) {
        LambdaQueryWrapper<AccountRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AccountRole::getAccountId, accountId);
        return normalizeSessionRoles(accountRoleMapper.selectList(queryWrapper), tenantId);
    }

    private List<AccountRole> selectSessionRoles(Long accountId, Long tenantId, Long requestedRoleId) {
        List<AccountRole> sessionRoles = querySessionRoles(accountId, tenantId);
        if (requestedRoleId == null || sessionRoles.isEmpty()) {
            return sessionRoles;
        }
        List<AccountRole> orderedRoles = new ArrayList<>(sessionRoles.size());
        AccountRole selectedRole = null;
        for (AccountRole role : sessionRoles) {
            if (requestedRoleId.equals(role.getRoleId())) {
                selectedRole = role;
                continue;
            }
            orderedRoles.add(role);
        }
        if (selectedRole == null) {
            throw new BadRequestException("Role is not bound to this account");
        }
        orderedRoles.add(0, selectedRole);
        return orderedRoles;
    }

    private SwitchableTenantDTO buildSwitchableTenantDTO(User user, Long roleId) {
        SwitchableTenantDTO dto = new SwitchableTenantDTO();
        dto.setTenantId(user.getTenantId());
        dto.setAuthUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRoleId(roleId);
        return dto;
    }

    private void assignPrimaryRole(Long accountId, Long tenantId, Long roleId) {
        Long targetRoleId = roleId;
        if (targetRoleId == null) {
            Role defaultRole = getOrCreateDefaultUserRole(tenantId);
            targetRoleId = defaultRole.getId();
        } else {
            Role assignedRole = roleMapper.selectById(targetRoleId);
            if (assignedRole == null || assignedRole.getIsDeleted() != null && assignedRole.getIsDeleted() == 1) {
                throw new BadRequestException("角色不存在");
            }
            if (tenantId != null && assignedRole.getDepId() != null && !tenantId.equals(assignedRole.getDepId())) {
                throw new BadRequestException("角色不属于当前租户");
            }
        }

        LambdaQueryWrapper<AccountRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(AccountRole::getAccountId, accountId);
        accountRoleMapper.delete(deleteWrapper);

        AccountRole accountRole = new AccountRole();
        accountRole.setAccountId(accountId);
        accountRole.setRoleId(targetRoleId);
        accountRole.setCreateTime(LocalDateTime.now());
        accountRole.setUpdateTime(LocalDateTime.now());
        accountRoleMapper.insert(accountRole);
    }

    private String resolveUsername(String username, String cellPhone) {
        String resolved = StrUtil.isBlank(username) ? cellPhone : username;
        if (StrUtil.isBlank(resolved)) {
            throw new BadRequestException("用户名不能为空");
        }
        return resolved;
    }

    private void enforceDormantLoginVerification(User user, LoginDTO dto, Long tenantId) {
        LoginRecord latestLoginRecord = queryLatestLoginRecord(user.getId());
        if (latestLoginRecord == null || latestLoginRecord.getLoginTime() == null) {
            return;
        }
        if (latestLoginRecord.getLoginTime().isAfter(LocalDateTime.now().minusDays(DORMANT_LOGIN_DAYS))) {
            return;
        }
        if (StrUtil.isBlank(dto.getEmail()) || StrUtil.isBlank(dto.getEmailCode())) {
            throw new BadRequestException("Dormant login requires email verification");
        }

        EmailVerificationVerifyDTO verifyDTO = new EmailVerificationVerifyDTO();
        verifyDTO.setEmail(dto.getEmail());
        verifyDTO.setTenantId(tenantId);
        verifyDTO.setScene("LOGIN");
        verifyDTO.setCode(dto.getEmailCode());
        Boolean verified = userClient.verifyEmailVerificationCode(verifyDTO);
        if (!Boolean.TRUE.equals(verified)) {
            throw new BadRequestException("Email verification failed");
        }
    }

    private LoginRecord queryLatestLoginRecord(Long userId) {
        QueryWrapper<LoginRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("login_time")
                .last("LIMIT 1");
        return loginRecordMapper.selectOne(queryWrapper);
    }

    private List<EmailLoginCandidateVO> normalizeEmailLoginCandidates(List<EmailLoginCandidateDTO> rawCandidates) {
        if (rawCandidates == null || rawCandidates.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, EmailLoginCandidateVO> candidates = new LinkedHashMap<>();
        for (EmailLoginCandidateDTO rawCandidate : rawCandidates) {
            if (rawCandidate == null || rawCandidate.getAuthUserId() == null || rawCandidate.getTenantId() == null) {
                continue;
            }
            User authUser = lookupAuthUserWithoutTenantScope(rawCandidate.getAuthUserId());
            if (!isAvailableForEmailLogin(authUser, rawCandidate.getTenantId())) {
                continue;
            }
            List<AccountRole> sessionRoles = querySessionRoles(rawCandidate.getAuthUserId(), rawCandidate.getTenantId());
            if (sessionRoles.isEmpty()) {
                EmailLoginCandidateVO candidate = buildEmailLoginCandidate(rawCandidate, null);
                candidates.put(buildEmailLoginCandidateKey(candidate), candidate);
                continue;
            }
            for (AccountRole sessionRole : sessionRoles) {
                if (rawCandidate.getRoleId() != null && !rawCandidate.getRoleId().equals(sessionRole.getRoleId())) {
                    continue;
                }
                EmailLoginCandidateVO candidate = buildEmailLoginCandidate(rawCandidate, sessionRole.getRoleId());
                candidates.put(buildEmailLoginCandidateKey(candidate), candidate);
            }
        }
        return new ArrayList<>(candidates.values());
    }

    private EmailLoginCandidateVO buildEmailLoginCandidate(EmailLoginCandidateDTO rawCandidate, Long roleId) {
        EmailLoginCandidateVO candidate = new EmailLoginCandidateVO();
        candidate.setAuthUserId(rawCandidate.getAuthUserId());
        candidate.setTenantId(rawCandidate.getTenantId());
        candidate.setRoleId(roleId);
        candidate.setUsername(rawCandidate.getUsername());
        candidate.setDisplayName(rawCandidate.getDisplayName());
        candidate.setAvatar(rawCandidate.getAvatar());
        return candidate;
    }

    private String buildEmailLoginCandidateKey(EmailLoginCandidateVO candidate) {
        return candidate.getAuthUserId() + ":" + candidate.getTenantId() + ":" + candidate.getRoleId();
    }

    private boolean isAvailableForEmailLogin(User user, Long tenantId) {
        return user != null
                && (user.getIsDeleted() == null || user.getIsDeleted() == 0)
                && user.getStatus() != null
                && user.getStatus() == 1
                && tenantId != null
                && tenantId.equals(user.getTenantId());
    }

    private User lookupAuthUserWithoutTenantScope(Long authUserId) {
        return executeWithoutTenantScope(() -> userMapper.selectById(authUserId));
    }

    private <T> T executeWithoutTenantScope(Supplier<T> supplier) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.removeTenantId();
            TenantContext.setSuperAdmin(true);
            return supplier.get();
        } finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(previousTenantId);
            }
            TenantContext.setSuperAdmin(previousSuperAdmin);
        }
    }

    private LoginVO buildLoginSession(User user, String loginType) {
        return buildLoginSession(user, loginType, null);
    }

    private LoginVO buildLoginSession(User user, String loginType, Long roleId) {
        List<AccountRole> sessionRoles = selectSessionRoles(user.getId(), user.getTenantId(), roleId);

        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setUserId(user.getId());
        loginUserDTO.setUsername(user.getUsername());
        loginUserDTO.setOrganizationId(String.valueOf(user.getTenantId()));
        loginUserDTO.setTenantId(user.getTenantId());
        loginUserDTO.setClientType(JwtConstants.CLIENT_TYPE_WEB);
        if (!sessionRoles.isEmpty()) {
            loginUserDTO.setRoleId(sessionRoles.get(0).getRoleId());
        }

        String token = jwtTool.createToken(loginUserDTO);
        String refreshToken = jwtTool.createRefreshToken(loginUserDTO);
        storeSessionToken(loginUserDTO, token);

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        recordLoginSuccess(user, loginType);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setOrganizationId(String.valueOf(user.getTenantId()));
        loginVO.setRoles(sessionRoles);
        applyLoginAvatar(loginVO, user.getId());
        return loginVO;
    }

    private String buildEmailLoginTicketKey(String loginTicket) {
        return EMAIL_LOGIN_TICKET_REDIS_KEY_PREFIX + loginTicket;
    }

    private String writeEmailLoginTicket(EmailLoginTicketPayload payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Serialize email login ticket failed");
        }
    }

    private EmailLoginTicketPayload readEmailLoginTicket(String payload) {
        try {
            return OBJECT_MAPPER.readValue(payload, EmailLoginTicketPayload.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Parse email login ticket failed");
        }
    }

    private void recordLoginSuccess(User user) {
        recordLoginSuccess(user, "password");
    }

    private void recordLoginSuccess(User user, String loginType) {
        LoginRecord loginRecord = new LoginRecord();
        LocalDateTime now = LocalDateTime.now();
        loginRecord.setUserId(user.getId());
        loginRecord.setUsername(user.getUsername());
        loginRecord.setLoginType(loginType);
        loginRecord.setLoginIp(WebUtils.getRemoteAddr());
        loginRecord.setLoginStatus(1);
        loginRecord.setLoginTime(now);
        loginRecordMapper.insert(loginRecord);
    }

    private void applyLoginAvatar(LoginVO loginVO, Long authUserId) {
        String avatar = resolveLoginAvatar(authUserId);
        if (StrUtil.isBlank(avatar)) {
            return;
        }
        loginVO.setAvatar(avatar);
        loginVO.setAvatarUrl(avatar);
        loginVO.setIcon(avatar);
    }

    private String resolveLoginAvatar(Long authUserId) {
        if (authUserId == null) {
            return "";
        }
        try {
            List<UserDTO> users = userClient.queryUsersByAuthIds(List.of(authUserId));
            if (users == null || users.isEmpty()) {
                return "";
            }
            UserDTO user = users.stream()
                    .filter(item -> item != null && authUserId.equals(item.getAuthUserId()))
                    .findFirst()
                    .orElse(users.get(0));
            if (user == null) {
                return "";
            }
            if (StrUtil.isNotBlank(user.getIcon())) {
                return user.getIcon();
            }
            if (StrUtil.isNotBlank(user.getPhoto())) {
                return user.getPhoto();
            }
        } catch (Exception e) {
            log.warn("resolve login avatar failed for authUserId={}: {}", authUserId, e.getMessage());
        }
        return "";
    }

    private List<AccountRole> normalizeSessionRoles(List<AccountRole> accountRoles, Long tenantId) {
        if (accountRoles == null || accountRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<AccountRole> normalized = new ArrayList<>(accountRoles.size());
        for (AccountRole accountRole : accountRoles) {
            AccountRole sessionRole = new AccountRole();
            sessionRole.setAccountId(accountRole.getAccountId());
            sessionRole.setRoleId(normalizeRoleIdForSession(accountRole.getRoleId(), tenantId));
            sessionRole.setCreateTime(accountRole.getCreateTime());
            sessionRole.setUpdateTime(accountRole.getUpdateTime());
            normalized.add(sessionRole);
        }
        return normalized;
    }

    private Long normalizeRoleIdForSession(Long roleId, Long tenantId) {
        if (roleId == null) {
            return null;
        }
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return roleId;
        }
        String code = role.getRoleCode();
        if (code == null || code.isBlank()) {
            code = role.getCode();
        }
        if (code == null || code.isBlank()) {
            return roleId;
        }
        switch (code.toUpperCase(Locale.ROOT)) {
            case "ADMIN":
                return tenantId != null && tenantId == 1L ? 1L : 3L;
            case "USER":
                return 2L;
            case "CLUB_MANAGER":
                return 7L;
            case "TEACHER":
                return 8L;
            default:
                return roleId;
        }
    }

    @Override
    public LoginVO refreshSession(String refreshToken) {
        try {
            LoginUserDTO loginUserDTO = jwtTool.parseRefreshToken(normalizeBearerToken(refreshToken));
            String token = jwtTool.createToken(loginUserDTO);
            String newRefreshToken = jwtTool.createRefreshToken(loginUserDTO);
            storeSessionToken(loginUserDTO, token);

            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            loginVO.setRefreshToken(newRefreshToken);
            loginVO.setUserId(loginUserDTO.getUserId());
            loginVO.setUsername(loginUserDTO.getUsername());
            loginVO.setOrganizationId(loginUserDTO.getTenantId() == null ? loginUserDTO.getOrganizationId() : String.valueOf(loginUserDTO.getTenantId()));
            applyLoginAvatar(loginVO, loginUserDTO.getUserId());
            return loginVO;
        } catch (Exception e) {
            log.error("鍒锋柊 session 澶辫触", e);
            throw new BadRequestException("鍒锋柊session澶辫触锛氭棤鏁堢殑refresh token");
        }
    }

    private void storeSessionToken(LoginUserDTO loginUserDTO, String token) {
        Duration ttl = jwtTool.resolveTokenTtl(loginUserDTO);
        if (ttl == null) {
            ttl = JwtConstants.JWT_TOKEN_TTL;
        }
        stringRedisTemplate.opsForValue().set(
                TOKEN_REDIS_KEY_PREFIX + loginUserDTO.getUserId(),
                token,
                ttl.getSeconds(),
                TimeUnit.SECONDS
        );
    }

    private String normalizeClientType(String clientType) {
        if (clientType != null && JwtConstants.CLIENT_TYPE_MOBILE.equalsIgnoreCase(clientType.trim())) {
            return JwtConstants.CLIENT_TYPE_MOBILE;
        }
        return JwtConstants.CLIENT_TYPE_WEB;
    }

    private String normalizeBearerToken(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim();
        if (normalized.length() >= 2
                && ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.replaceFirst("(?i)^Bearer\\s+", "");
    }

    private LoginUserDTO resolveLoginUserDTOFromAuthorization(String authorization) {
        return jwtTool.parseToken(normalizeBearerToken(authorization));
    }

    private User resolveCurrentUser(LoginUserDTO loginUserDTO) {
        User currentUser = userMapper.selectById(loginUserDTO.getUserId());
        if (currentUser == null || currentUser.getIsDeleted() != null && currentUser.getIsDeleted() == 1) {
            throw new BadRequestException("当前账号不存在");
        }
        return currentUser;
    }

    private User resolveCurrentUserFromAuthorization(String authorization) {
        return resolveCurrentUser(resolveLoginUserDTOFromAuthorization(authorization));
    }

    private String resolveIdentityKey(User sourceUser, String fallbackUsername) {
        if (sourceUser != null && sourceUser.getIdentityKey() != null && !sourceUser.getIdentityKey().isBlank()) {
            return sourceUser.getIdentityKey();
        }
        return fallbackUsername;
    }

    private Role getOrCreateDefaultUserRole(Long tenantId) {
        LambdaQueryWrapper<Role> roleQueryWrapper = new LambdaQueryWrapper<>();
        roleQueryWrapper.eq(Role::getCode, "USER")
                .eq(Role::getDepId, tenantId)
                .last("LIMIT 1");
        Role defaultRole = roleMapper.selectOne(roleQueryWrapper);
        if (defaultRole != null) {
            return defaultRole;
        }

        log.warn("租户 {} 缺少默认 USER 角色，开始自动补齐", tenantId);
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setDepId(tenantId);
        role.setCode("USER");
        role.setRoleCode("USER");
        role.setName("普通用户");
        role.setType(Role.RoleType.CONSTANT.getValue());
        role.setIsDeleted(0);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return role;
    }

    private String resolveRegisterPayloadValue(Map<String, Object> payload, String... keys) {
        if (payload == null || payload.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (!payload.containsKey(key)) {
                continue;
            }
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            String text = value.toString().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    @lombok.Data
    private static class EmailLoginTicketPayload {
        private String email;
        private List<EmailLoginCandidateVO> candidates;
    }

    private <T> T executeWithoutTenantIsolation(Supplier<T> supplier) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            TenantContext.setSuperAdmin(true);
            TenantContext.removeTenantId();
            return supplier.get();
        } finally {
            if (previousTenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(previousTenantId);
            }
            TenantContext.setSuperAdmin(previousSuperAdmin);
        }
    }
}
