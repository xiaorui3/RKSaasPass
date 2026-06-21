package com.tianji.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.auth.AuthClient;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.auth.ApprovedApplicantProvisionDTO;
import com.tianji.api.dto.auth.CurrentUserPasswordUpdateDTO;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.api.dto.user.EmailLoginCandidateDTO;
import com.tianji.api.dto.user.EmailLoginCandidateQueryDTO;
import com.tianji.api.dto.user.LoginFormDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.media.MediaPathHelper;
import com.tianji.common.domain.dto.LoginUserDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.ForbiddenException;
import com.tianji.common.exceptions.UnauthorizedException;
import com.tianji.common.utils.AssertUtils;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.UserFormDTO;
import com.tianji.user.domain.po.ClubAlumni;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.po.UserDetail;
import com.tianji.user.domain.vo.ScopedUserStatisticsVO;
import com.tianji.user.domain.vo.PeopleDomainReconcileResultVO;
import com.tianji.user.domain.vo.UserDetailVO;
import com.tianji.user.enums.UserStatus;
import com.tianji.user.mapper.ClubAlumniMapper;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.ICodeService;
import com.tianji.user.service.ISysOperLogService;
import com.tianji.user.service.IUserDetailService;
import com.tianji.user.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.tianji.user.constants.UserConstants.*;
import static com.tianji.user.constants.UserErrorInfo.Msg.*;

/*
 * Managed-member role hints are kept local to the user service because
 * the sync path needs to normalize imported roster rows before they drift
 * into the member ledger with the wrong user type.
 */
@SuppressWarnings("unused")
class UserServiceImplManagedMemberHints {
    static final List<String> MANAGER_POSITIONS = List.of(
            "管理员",
            "社团负责人",
            "负责人",
            "社长",
            "副社长"
    );
    static final List<String> TEACHER_POSITIONS = List.of("指导老师");
}


/**
 * <p>
 * 学员用户表 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-06-28
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private enum ManagedMemberSyncStatus {
        CREATED,
        UPDATED,
        RESTORED,
        DELETED,
        UNCHANGED,
        SKIPPED_NO_STUDENT_ID
    }

    private static class ManagedMemberSyncResult {
        private final ManagedMemberSyncStatus status;
        private final String detail;

        private ManagedMemberSyncResult(ManagedMemberSyncStatus status, String detail) {
            this.status = status;
            this.detail = detail;
        }

        private static ManagedMemberSyncResult of(ManagedMemberSyncStatus status, String detail) {
            return new ManagedMemberSyncResult(status, detail);
        }
    }

    private static class MemberUserReconcileResult {
        private boolean createdUser;
        private boolean updatedUser;
        private boolean createdAuthAccount;
        private boolean skippedSyntheticMember;
        private boolean skippedMismatchedSyntheticMember;
        private String detail;
    }

    private static class AlumniReconcileResult {
        private boolean created;
        private boolean updated;
        private boolean skipped = true;
    }

    private static final Long CLUB_MANAGER_ROLE_ID = 7L;
    private static final Long REVIEW_TEACHER_ROLE_ID = 8L;
    private static final Long TENANT_ADMIN_ROLE_ID = 3L;
    private static final Long TENANT_ADMIN_COMPAT_ROLE_ID = 5L;
    private static final String TENANT_ADMIN_ROLE_NAME = "\u79df\u6237\u7ba1\u7406\u5458";
    private static final String SUPER_ADMIN_ROLE_NAME = "\u8d85\u7ea7\u7ba1\u7406\u5458";
    private static final String CLUB_MANAGER_ROLE_NAME = "负责人";
    private static final String REVIEW_TEACHER_ROLE_NAME = "指导老师";

    private static final String MANAGED_STUDENT_ID_REQUIRED_MESSAGE = "\u5b66\u751f\u7c7b\u578b\u5fc5\u987b\u586b\u5199\u5b66\u53f7";
    private static final String MANAGED_STUDENT_EMAIL_REQUIRED_MESSAGE = "学生类型必须填写邮箱";

    private static final String MEMBER_STATUS_ACTIVE = "正常";
    private static final String AUTH_MEMBER_STUDENT_ID_PREFIX = "AUTH-";
    private static final String LOCAL_MEMBER_STUDENT_ID_PREFIX = "LOCAL-";

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ICodeService codeService;
    @Autowired
    private AuthClient authClient;
    @Autowired
    private IUserDetailService detailService;
    @Autowired
    private RKTenantMapper tenantMapper;
    @Autowired
    private ClubMemberMapper clubMemberMapper;
    @Autowired
    private ClubAlumniMapper clubAlumniMapper;
    @Autowired
    private MediaPathHelper mediaPathHelper;
    @Autowired(required = false)
    private ISysOperLogService sysOperLogService;

    @Override
    public LoginUserDTO queryUserDetail(LoginFormDTO loginDTO, boolean isStaff) {
        // 1.判断登录方式
        Integer type = loginDTO.getType();
        User user = null;
        // 2.用户名和密码登录
        if (type == 1) {
            user = loginByPw(loginDTO);
        }
        // 3.验证码登录
        if (type == 2) {
            user = loginByVerifyCode(loginDTO.getCellPhone(), loginDTO.getPassword());
        }
        // 4.错误的登录方式
        if (user == null) {
            throw new BadRequestException(ILLEGAL_LOGIN_TYPE);
        }
        // 5.判断用户类型与登录方式是否匹配
        if (isStaff ^ user.getType() != UserType.STUDENT) {
            throw new BadRequestException(isStaff ? "非管理端用户" : "非学生端用户");
        }
        // 6.封装返回
        LoginUserDTO userDTO = new LoginUserDTO();
        userDTO.setUserId(user.getId());
        userDTO.setRoleId(handleRoleId(user));
        return userDTO;
    }

    @Override
    public void resetPassword(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        AssertUtils.isTrue(updateById(user), USER_ID_NOT_EXISTS);
    }

    @Override
    public UserDetailVO myInfo() {
        // 1.获取登录用户id（来自auth侧）
        User currentUser = resolveCurrentLocalUser();
        Long authUserId = currentUser == null ? null : currentUser.getAuthUserId();
        if (authUserId == null) {
            return null;
        }
        // 2.通过authUserId查找rk_user的记录
        User rkUser = lambdaQuery().eq(User::getAuthUserId, authUserId).one();
        Long userId;
        if (rkUser != null) {
            userId = rkUser.getId();
        } else {
            // fallback: 兼容旧用户（authUserId为null的老数据），尝试直接用id查
            userId = authUserId;
        }
        // 3.查询用户详情
        UserDetail userDetail = detailService.queryById(userId);
        AssertUtils.isNotNull(userDetail, USER_ID_NOT_EXISTS);
        // 3.封装vo
        String typeStr = userDetail.getType();
        UserType type = null;
        if (typeStr != null) {
            try {
                type = UserType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                // 数据库存储的是数字值，需要通过of方法转换
                try {
                    type = UserType.of(Integer.parseInt(typeStr));
                } catch (NumberFormatException | BadRequestException ex) {
                    log.warn("未知的用户类型: {}", typeStr);
                }
            }
        }
        // 3.1.基本信息
        UserDetailVO vo = BeanUtils.toBean(userDetail, UserDetailVO.class);
        if (StringUtils.isBlank(vo.getName())) {
            vo.setName(userDetail.getUsername());
        }
        // 3.2.详情信息
        vo.setRoleName(resolveCurrentRoleName(currentUser, userDetail, type));
        return vo;
    }

    @Override
    public void addUserByPhone(User user, String code) {
        // 1.验证码校验
        codeService.verifyCode(user.getCellPhone(), code);
        // 2.判断手机号是否存在
        Integer count = lambdaQuery().eq(User::getCellPhone, user.getCellPhone()).count();
        if (count > 0) {
            throw new BadRequestException(PHONE_ALREADY_EXISTS);
        }
        // 3.加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 4.新增
        user.setUsername(user.getCellPhone());
        save(user);
    }

    @Override
    public void updatePasswordByPhone(String cellPhone, String code, String password) {
        // 1.验证码校验
        codeService.verifyCode(cellPhone, code);
        // 2.查询用户
        User oldUser = lambdaQuery().eq(User::getCellPhone, cellPhone).one();
        if (oldUser == null) {
            // 手机号不存在
            throw new BadRequestException(PHONE_NOT_EXISTS);
        }
        // 2.修改密码
        User user = new User();
        user.setId(user.getId());
        user.setPassword(passwordEncoder.encode(password));
        updateById(user);
    }

    public void updatePhoneById(Long id, String cellPhone) {
        // 1.1.判断是否需要修改手机号
        if (StringUtils.isNotBlank(cellPhone)) {
            // 1.2.需要修改，封装数据
            User user = new User();
            user.setId(id);
            user.setUsername(cellPhone);
            user.setCellPhone(cellPhone);
            // 1.3.修改
            updateById(user);
        }
    }

    @Override
    @Transactional
    public Long saveUser(UserDTO userDTO) {
        Long tenantId = userDTO.getTenantId() != null ? userDTO.getTenantId() : currentTenantId();
        return executeInTenantScope(tenantId, () -> doSaveUser(userDTO, tenantId));
    }

    private Long doSaveUser(UserDTO userDTO, Long tenantId) {
        UserType type = userDTO.getType() == null ? UserType.STUDENT : UserType.of(userDTO.getType());
        LocalDateTime normalizedJoinTime = normalizeManagedMemberJoinDate(userDTO.getJoinDate());

        // 内部同步：按authUserId执行upsert
        User existUser = null;
        if (userDTO.getId() != null) {
            existUser = findTenantScopedLocalUserByAuthUserId(tenantId, userDTO.getId());
        }
        if (existUser == null) {
            existUser = resolveExistingLocalUserForSave(tenantId, userDTO);
        }
        if (existUser != null) {
            String previousStudentId = existUser.getStudentId();
            existUser.setTenantId(tenantId);
            if (userDTO.getId() != null) {
                existUser.setAuthUserId(userDTO.getId());
            }
            String resolvedCellPhone = resolveAvailableCellPhone(tenantId, userDTO.getCellPhone(), existUser.getId());
            existUser.setUsername(resolveMergedUsername(existUser, userDTO));
            if (resolvedCellPhone != null) {
                existUser.setCellPhone(resolvedCellPhone);
            }
            existUser.setRealName(StringUtils.isNotBlank(userDTO.getName()) ? userDTO.getName() : existUser.getRealName());
            existUser.setEmail(StringUtils.isNotBlank(userDTO.getEmail()) ? userDTO.getEmail() : existUser.getEmail());
            existUser.setStudentId(StringUtils.isNotBlank(userDTO.getStudentId()) ? userDTO.getStudentId() : existUser.getStudentId());
            existUser.setCollege(StringUtils.isNotBlank(userDTO.getCollege()) ? userDTO.getCollege() : existUser.getCollege());
            existUser.setMajor(StringUtils.isNotBlank(userDTO.getMajor()) ? userDTO.getMajor() : existUser.getMajor());
            existUser.setGrade(StringUtils.isNotBlank(userDTO.getGrade()) ? userDTO.getGrade() : existUser.getGrade());
            existUser.setJoinTime(normalizedJoinTime != null ? normalizedJoinTime : existUser.getJoinTime());
            existUser.setType(type);
            existUser.setUpdateTime(LocalDateTime.now());
            updateById(existUser);
            upsertLocalUserDetail(existUser.getId(), userDTO);
            syncManagedMemberFromUser(existUser);
            retirePreviousManagedMemberLedger(existUser, previousStudentId, existUser.getStudentId());
            return existUser.getId();
        }

        // 1.创建本地用户（不设id，让数据库自增）
        String resolvedCellPhone = resolveAvailableCellPhone(tenantId, userDTO.getCellPhone(), null);
        User user = new User();
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setCellPhone(resolvedCellPhone);
        user.setUsername(resolveUsername(userDTO.getUsername(), resolvedCellPhone, null));
        user.setRealName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setStudentId(userDTO.getStudentId());
        user.setCollege(userDTO.getCollege());
        user.setMajor(userDTO.getMajor());
        user.setGrade(userDTO.getGrade());
        if (userDTO.getId() != null) {
            user.setAuthUserId(userDTO.getId());
        }
        user.setTenantId(tenantId);
        user.setType(type);
        user.setStatus(UserStatus.NORMAL);
        user.setJoinTime(normalizedJoinTime != null ? normalizedJoinTime : LocalDateTime.now());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(user);
        upsertLocalUserDetail(user.getId(), userDTO);
        syncManagedMemberFromUser(user);
        return user.getId();
    }

    @Override
    @Transactional
    public void updateUser(UserDTO userDTO) {
        // 1.如果传递了手机号，则修改手机号
        String cellphone = userDTO.getCellPhone();
        if(StringUtils.isNotBlank(cellphone)){
            User user = new User();
            user.setId(userDTO.getId());
            user.setCellPhone(cellphone);
            user.setUsername(cellphone);
            updateById(user);
        }
        // 2.修改详情
        UserDetail detail = BeanUtils.toBean(userDTO, UserDetail.class);
        detail.setType(null);
        detailService.updateById(detail);
    }

    @Override
    public void updateUserWithPassword(UserFormDTO userDTO) {
        // 1.尝试更新密码（密码字段可选，只有都填写时才修改密码）
        String pw = userDTO.getPassword();
        String oldPw = userDTO.getOldPassword();
        if(StringUtils.isNotBlank(pw) && StringUtils.isNotBlank(oldPw)) {
            User currentUser = resolveCurrentLocalUser();
            Long userId = currentUser == null ? null : currentUser.getId();
            // 1.1.查询用户
            User user = currentUser;
            if (user == null) {
                throw new UnauthorizedException(USER_ID_NOT_EXISTS);
            }
            Long authUserId = currentUser.getAuthUserId() != null ? currentUser.getAuthUserId() : userId;
            CurrentUserPasswordUpdateDTO passwordUpdateDTO = new CurrentUserPasswordUpdateDTO();
            passwordUpdateDTO.setOldPassword(oldPw);
            passwordUpdateDTO.setNewPassword(pw);
            authClient.updateCurrentUserPassword(authUserId, passwordUpdateDTO);
            // 1.4.修改密码
            user = new User();
            user.setId(userId);
            user.setPassword(passwordEncoder.encode(pw));
            user.setUpdateTime(LocalDateTime.now());
            updateById(user);
        }
        // 2.更新用户详情
        User currentUser = resolveCurrentLocalUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (userId == null) {
            throw new UnauthorizedException(USER_ID_NOT_EXISTS);
        }
        List<User> linkedUsers = resolveLinkedUsersForSharedProfile(currentUser);
        if (linkedUsers.isEmpty()) {
            linkedUsers = List.of(currentUser);
        }
        for (User linkedUser : linkedUsers) {
            updateSharedProfileFields(linkedUser, userDTO);
            upsertSharedUserDetail(linkedUser, userDTO);
        }
    }

    public User loginByPw(LoginFormDTO loginDTO) {
        // 1.数据校验
        String username = loginDTO.getUsername();
        String cellPhone = loginDTO.getCellPhone();
        if (StrUtil.isBlank(username) && StrUtil.isBlank(cellPhone)) {
            throw new BadRequestException(INVALID_UN);
        }
        // 2.根据用户名或手机号查询
        User user = lambdaQuery()
                .eq(StrUtil.isNotBlank(username), User::getUsername, username)
                .eq(StrUtil.isNotBlank(cellPhone), User::getCellPhone, cellPhone)
                .one();
        AssertUtils.isNotNull(user, INVALID_UN_OR_PW);
        // 3.校验是否禁用
        if (user.getStatus() == UserStatus.FROZEN) {
            throw new ForbiddenException(USER_FROZEN);
        }
        // 4.校验密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BadRequestException(INVALID_UN_OR_PW);
        }

        return user;
    }

    private Long handleRoleId(User user) {
        Long roleId = 0L;
        switch (user.getType()) {
            case STUDENT:
                roleId = STUDENT_ROLE_ID;
                break;
            case TEACHER:
                roleId = TEACHER_ROLE_ID;
                break;
            case STAFF:
                UserDetail detail = detailService.getById(user.getId());
                roleId = detail.getRoleId();
                break;
        }
        return roleId;
    }

    public User loginByVerifyCode(String phone, String code) {
        // 1.校验验证码
        codeService.verifyCode(phone, code);
        // 2.根据手机号查询
        User user = lambdaQuery().eq(User::getCellPhone, phone).one();
        if (user == null) {
            throw new BadRequestException(PHONE_NOT_EXISTS);
        }
        // 3.校验是否禁用
        if (user.getStatus() == UserStatus.FROZEN) {
            throw new ForbiddenException(USER_FROZEN);
        }
        return user;
    }

    @Override
    public Page<UserDTO> queryUserByPage(Integer pageNo, Integer size, String username, String mobile, Integer status) {
        log.info("分页查询用户列表: pageNo={}, size={}, username={}, mobile={}, status={}",
                pageNo, size, username, mobile, status);

        // 1.创建分页对象
        Page<User> page = new Page<>(pageNo, size);

        // 2.构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .like(StringUtils.isNotBlank(username), User::getUsername, username)
                .like(StringUtils.isNotBlank(mobile), User::getCellPhone, mobile);
        if (canManageAllTenants()) {
            applyActiveTenantScope(queryWrapper);
        } else {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                queryWrapper.eq(User::getTenantId, currentTenantId());
            } else {
                queryWrapper.eq(User::getTenantId, currentTenantId());
            }
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, UserStatus.of(status));
        }

        // 3.执行分页查询
        Page<User> userPage = page(page, queryWrapper);
        log.info("查询到用户总数: {}", userPage.getTotal());

        // 4.转换User为UserDTO
        Page<UserDTO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserDTO> userDTOList = new ArrayList<>();

        for (User user : userPage.getRecords()) {
            UserDTO userDTO = toManagedUserDTO(user);
            enrichRoleInfo(userDTO, user.getAuthUserId());
            applyFallbackRoleInfo(userDTO, user);
            userDTOList.add(userDTO);
        }

        resultPage.setRecords(userDTOList);
        return resultPage;
    }

    @Override
    public Page<UserDTO> queryUserByPage(Integer pageNo, Integer size, String username, String mobile, Integer status, Long tenantId) {
        return queryUserByPage(pageNo, size, username, mobile, status, tenantId, null);
    }

    @Override
    public Page<UserDTO> queryUserByPage(Integer pageNo, Integer size, String username, String mobile, Integer status, Long tenantId, Long roleId) {
        if (roleId != null) {
            return queryUserByPageWithRoleFilter(pageNo, size, username, mobile, status, tenantId, roleId);
        }

        if (!canManageAllTenants() || tenantId == null) {
            return queryUserByPage(pageNo, size, username, mobile, status);
        }

        Page<User> page = new Page<>(pageNo, size);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .like(StringUtils.isNotBlank(username), User::getUsername, username)
                .like(StringUtils.isNotBlank(mobile), User::getCellPhone, mobile)
                .eq(User::getTenantId, tenantId);
        if (status != null) {
            queryWrapper.eq(User::getStatus, UserStatus.of(status));
        }

        Page<User> userPage = page(page, queryWrapper);
        Page<UserDTO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserDTO> userDTOList = new ArrayList<>();
        for (User user : userPage.getRecords()) {
            UserDTO userDTO = toManagedUserDTO(user);
            enrichRoleInfo(userDTO, user.getAuthUserId());
            applyFallbackRoleInfo(userDTO, user);
            if (roleMatches(userDTO, roleId)) {
                userDTOList.add(userDTO);
            }
        }
        if (roleId != null) {
            resultPage.setTotal(userDTOList.size());
        }
        resultPage.setRecords(userDTOList);
        return resultPage;
    }

    private Page<UserDTO> queryUserByPageWithRoleFilter(
            Integer pageNo,
            Integer size,
            String username,
            String mobile,
            Integer status,
            Long tenantId,
            Long roleId) {
        long requestedPageNo = pageNo == null || pageNo < 1 ? 1L : pageNo;
        long requestedSize = size == null || size < 1 ? 10L : size;
        long scanSize = Math.max(requestedSize, 200L);
        long scanPageNo = 1L;
        List<UserDTO> matched = new ArrayList<>();

        while (true) {
            Page<User> scanPage = new Page<>(scanPageNo, scanSize);
            Page<User> userPage = page(scanPage, buildScopedUserPageQuery(username, mobile, status, tenantId));
            List<User> records = userPage.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }

            for (User user : records) {
                UserDTO userDTO = toManagedUserDTO(user);
                enrichRoleInfo(userDTO, user.getAuthUserId());
                applyFallbackRoleInfo(userDTO, user);
                if (roleMatches(userDTO, roleId)) {
                    matched.add(userDTO);
                }
            }

            if (scanPageNo * scanSize >= userPage.getTotal()) {
                break;
            }
            scanPageNo++;
        }

        long fromIndex = Math.min((requestedPageNo - 1) * requestedSize, matched.size());
        long toIndex = Math.min(fromIndex + requestedSize, matched.size());
        Page<UserDTO> resultPage = new Page<>(requestedPageNo, requestedSize, matched.size());
        resultPage.setRecords(new ArrayList<>(matched.subList((int) fromIndex, (int) toIndex)));
        return resultPage;
    }

    private LambdaQueryWrapper<User> buildScopedUserPageQuery(String username, String mobile, Integer status, Long tenantId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .like(StringUtils.isNotBlank(username), User::getUsername, username)
                .like(StringUtils.isNotBlank(mobile), User::getCellPhone, mobile);
        if (canManageAllTenants()) {
            if (tenantId != null) {
                queryWrapper.eq(User::getTenantId, tenantId);
            } else {
                applyActiveTenantScope(queryWrapper);
            }
        } else {
            queryWrapper.eq(User::getTenantId, currentTenantId());
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, UserStatus.of(status));
        }
        return queryWrapper;
    }

    private boolean roleMatches(UserDTO userDTO, Long roleId) {
        return roleId == null || (userDTO != null && Objects.equals(userDTO.getRoleId(), roleId));
    }

    @Override
    public List<UserDTO> queryUsersByAuthIds(List<Long> authUserIds) {
        if (authUserIds == null || authUserIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = authUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return List.of();
        }
        Long tenantId = currentTenantId();
        return executeWithoutTenantScope(() -> lambdaQuery()
                .in(User::getAuthUserId, ids)
                .eq(User::getTenantId, tenantId)
                .eq(User::getIsDeleted, 0)
                .list()
                .stream()
                .map(this::toManagedUserDTO)
                .collect(Collectors.toList()));
    }

    @Override
    public ScopedUserStatisticsVO queryScopedUserStatistics(Long tenantId) {
        List<User> scopedUsers = list(buildScopedUserQuery(tenantId));

        long accountCount = scopedUsers.size();
        long adminAccountCount = scopedUsers.stream()
                .filter(user -> resolveUserTypeValue(user) == 1)
                .count();
        long teacherAccountCount = scopedUsers.stream()
                .filter(user -> resolveUserTypeValue(user) == 3)
                .count();
        List<User> studentAccounts = scopedUsers.stream()
                .filter(user -> resolveUserTypeValue(user) == 2)
                .collect(Collectors.toList());
        long studentAccountCount = studentAccounts.size();
        List<User> studentAccountsWithStudentId = studentAccounts.stream()
                .filter(user -> StringUtils.isNotBlank(user.getStudentId()))
                .collect(Collectors.toList());
        long studentAccountWithStudentIdCount = studentAccountsWithStudentId.size();
        long studentAccountWithoutStudentIdCount = studentAccounts.stream()
                .filter(user -> StringUtils.isBlank(user.getStudentId()))
                .count();
        long memberLinkedAccountCount = studentAccountsWithStudentId.stream()
                .filter(user -> findManagedMember(user) != null)
                .count();
        long studentAccountMissingMemberLedgerCount = studentAccountsWithStudentId.stream()
                .filter(user -> findManagedMember(user) == null)
                .count();
        long standaloneStudentAccountCount = studentAccountWithoutStudentIdCount + studentAccountMissingMemberLedgerCount;
        List<User> standaloneStudentAccounts = studentAccounts.stream()
                .filter(user -> StringUtils.isBlank(user.getStudentId()) || findManagedMember(user) == null)
                .collect(Collectors.toList());
        List<String> standaloneStudentAccountSamples = standaloneStudentAccounts.stream()
                .map(User::getUsername)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
        long standaloneAccountCount = Math.max(accountCount - memberLinkedAccountCount, 0);

        ScopedUserStatisticsVO vo = new ScopedUserStatisticsVO();
        vo.setAccountCount(accountCount);
        vo.setAdminAccountCount(adminAccountCount);
        vo.setTeacherAccountCount(teacherAccountCount);
        vo.setStudentAccountCount(studentAccountCount);
        vo.setStudentAccountWithStudentIdCount(studentAccountWithStudentIdCount);
        vo.setStudentAccountWithoutStudentIdCount(studentAccountWithoutStudentIdCount);
        vo.setStudentAccountMissingMemberLedgerCount(studentAccountMissingMemberLedgerCount);
        vo.setStandaloneStudentAccountCount(standaloneStudentAccountCount);
        vo.setStandaloneStudentAccountSamples(standaloneStudentAccountSamples);
        vo.setMemberLinkedAccountCount(memberLinkedAccountCount);
        vo.setStandaloneAccountCount(standaloneAccountCount);
        return vo;
    }

    @Override
    public UserDTO queryManagedUserById(Long localUserId) {
        User user = getById(localUserId);
        AssertUtils.isNotNull(user, USER_ID_NOT_EXISTS);
        UserDTO dto = toManagedUserDTO(user);
        enrichRoleInfo(dto, user.getAuthUserId());
        applyFallbackRoleInfo(dto, user);
        return dto;
    }

    @Override
    @Transactional
    public Long provisionManagedUser(AdminUserProvisionDTO dto) {
        AdminUserProvisionDTO payload = buildProvisionPayload(dto, null, true);
        Long authUserId = authClient.provisionAdminUser(payload);
        return upsertManagedLocalUser(authUserId, payload, null);
    }

    @Override
    @Transactional
    public void updateManagedUser(Long localUserId, AdminUserProvisionDTO dto) {
        User existing = getById(localUserId);
        AssertUtils.isNotNull(existing, USER_ID_NOT_EXISTS);
        AdminUserProvisionDTO payload = buildProvisionPayload(dto, existing, false);

        Long authUserId = existing.getAuthUserId();
        if (authUserId == null) {
            authUserId = authClient.provisionAdminUser(payload);
        } else {
            authClient.updateAdminUser(authUserId, payload);
        }
        upsertManagedLocalUser(authUserId, payload, localUserId);
    }

    @Override
    @Transactional
    public void updateManagedUserStatus(Long localUserId, Integer status) {
        User existing = getById(localUserId);
        AssertUtils.isNotNull(existing, USER_ID_NOT_EXISTS);
        Long authUserId = ensureManagedAuthUserId(existing);

        authClient.updateAdminUserStatus(authUserId, status);
        existing.setStatus(UserStatus.of(status));
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    @Transactional
    public void resetManagedUserPassword(Long localUserId, String password) {
        User existing = getById(localUserId);
        AssertUtils.isNotNull(existing, USER_ID_NOT_EXISTS);
        Long authUserId = ensureManagedAuthUserId(existing);
        String targetPassword = StringUtils.isNotBlank(password) ? password : DEFAULT_PASSWORD;

        authClient.resetAdminUserPassword(authUserId, targetPassword);
        existing.setPassword(passwordEncoder.encode(targetPassword));
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    @Transactional
    public void deleteManagedUser(Long localUserId) {
        User existing = getById(localUserId);
        if (existing == null) {
            return;
        }
        if (existing.getAuthUserId() != null) {
            authClient.deleteAdminUser(existing.getAuthUserId());
        }
        deleteManagedMember(existing);
        removeById(localUserId);
    }

    @Override
    @Transactional
    public int importManagedUsers(List<AdminUserProvisionDTO> rows) {
        int count = 0;
        if (rows == null) {
            return 0;
        }
        for (AdminUserProvisionDTO row : rows) {
            validateManagedProvisionSource(row);
        }
        for (AdminUserProvisionDTO row : rows) {
            provisionManagedUser(row);
            count++;
        }
        return count;
    }

    private Long upsertManagedLocalUser(Long authUserId, AdminUserProvisionDTO dto, Long localUserId) {
        User localUser = localUserId == null ? lambdaQuery().eq(User::getAuthUserId, authUserId).one() : getById(localUserId);
        boolean creating = localUser == null;
        String previousStudentId = creating ? null : localUser.getStudentId();
        if (creating) {
            localUser = new User();
            localUser.setAuthUserId(authUserId);
            localUser.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            localUser.setCreateTime(LocalDateTime.now());
            localUser.setJoinTime(LocalDateTime.now());
            localUser.setIsDeleted(0);
        }

        localUser.setAuthUserId(authUserId);
        localUser.setTenantId(dto.getTenantId() != null ? dto.getTenantId() : resolveTenantId(localUser));
        String resolvedCellPhone = resolveAvailableCellPhone(localUser.getTenantId(), dto.getCellPhone(), localUser.getId());
        localUser.setUsername(resolveUsername(dto.getUsername(), resolvedCellPhone != null ? resolvedCellPhone : localUser.getCellPhone(), localUser.getUsername()));
        localUser.setRealName(StringUtils.isNotBlank(dto.getName()) ? dto.getName() : localUser.getRealName());
        localUser.setNickname(StringUtils.isNotBlank(dto.getName()) ? dto.getName() : localUser.getNickname());
        if (resolvedCellPhone != null) {
            localUser.setCellPhone(resolvedCellPhone);
        }
        localUser.setEmail(StringUtils.isNotBlank(dto.getEmail()) ? dto.getEmail() : localUser.getEmail());
        localUser.setStudentId(StringUtils.isNotBlank(dto.getStudentId()) ? dto.getStudentId() : localUser.getStudentId());
        localUser.setCollege(StringUtils.isNotBlank(dto.getCollege()) ? dto.getCollege() : localUser.getCollege());
        localUser.setMajor(StringUtils.isNotBlank(dto.getMajor()) ? dto.getMajor() : localUser.getMajor());
        localUser.setGrade(StringUtils.isNotBlank(dto.getGrade()) ? dto.getGrade() : localUser.getGrade());
        ClubMember existingManagedMember = findManagedMember(localUser.getTenantId(), localUser.getStudentId());
        localUser.setType(resolveManagedType(
                dto.getType(),
                localUser.getType(),
                localUser.getStudentId(),
                dto.getPosition(),
                existingManagedMember == null ? null : existingManagedMember.getPosition()
        ));
        localUser.setStatus(UserStatus.of(dto.getStatus() == null ? (localUser.getStatus() == null ? 1 : localUser.getStatus().getValue()) : dto.getStatus()));
        if (StringUtils.isNotBlank(dto.getPassword())) {
            localUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        String managedMemberStudentId = isManagedMemberSyncOptional(localUser)
                ? firstNonBlank(dto.getStudentId(), localUser.getStudentId())
                : resolveManagedMemberStudentId(localUser, dto.getStudentId(), authUserId);
        if (StringUtils.isBlank(localUser.getStudentId()) && StringUtils.isNotBlank(managedMemberStudentId)) {
            localUser.setStudentId(managedMemberStudentId);
        }
        localUser.setUpdateTime(LocalDateTime.now());

        if (creating) {
            baseMapper.insert(localUser);
        } else {
            updateById(localUser);
        }

        syncManagedMemberFromProvision(localUser, dto);
        retirePreviousManagedMemberLedger(localUser, previousStudentId, localUser.getStudentId());
        return localUser.getId();
    }

    private AdminUserProvisionDTO buildProvisionPayload(AdminUserProvisionDTO source, User existing, boolean creating) {
        AdminUserProvisionDTO payload = new AdminUserProvisionDTO();
        Long payloadTenantId = source.getTenantId() != null ? source.getTenantId() : resolveTenantId(existing);
        String payloadStudentId = StringUtils.isNotBlank(source.getStudentId()) ? source.getStudentId() : (existing == null ? null : existing.getStudentId());
        String payloadPosition = source.getPosition();
        ClubMember existingManagedMember = findManagedMember(payloadTenantId, payloadStudentId);
        payload.setTenantId(payloadTenantId);
        String fallbackPhone = creating ? source.getCellPhone() : null;
        payload.setUsername(resolveUsername(source.getUsername(), fallbackPhone, existing == null ? null : existing.getUsername()));
        payload.setName(firstNonBlank(source.getName(), existing == null ? null : existing.getRealName(),
                existingManagedMember == null ? null : existingManagedMember.getName()));
        payload.setCellPhone(firstNonBlank(source.getCellPhone(), existing == null ? null : existing.getCellPhone(),
                existingManagedMember == null ? null : existingManagedMember.getPhone()));
        payload.setEmail(firstNonBlank(source.getEmail(), existing == null ? null : existing.getEmail(),
                existingManagedMember == null ? null : existingManagedMember.getEmail()));
        payload.setStudentId(payloadStudentId);
        payload.setCollege(firstNonBlank(source.getCollege(), existing == null ? null : existing.getCollege()));
        payload.setMajor(firstNonBlank(source.getMajor(), existing == null ? null : existing.getMajor(),
                existingManagedMember == null ? null : existingManagedMember.getMajor()));
        payload.setGrade(firstNonBlank(source.getGrade(), existing == null ? null : existing.getGrade(),
                existingManagedMember == null ? null : existingManagedMember.getGrade()));
        payload.setDepartment(firstNonBlank(source.getDepartment(), existingManagedMember == null ? null : existingManagedMember.getDepartment()));
        payload.setPosition(firstNonBlank(payloadPosition, existingManagedMember == null ? null : existingManagedMember.getPosition()));
        payload.setJoinDate(source.getJoinDate());
        payload.setType(resolveManagedType(
                source.getType(),
                existing == null ? null : existing.getType(),
                payloadStudentId,
                payloadPosition,
                existingManagedMember == null ? null : existingManagedMember.getPosition()
        ).getValue());
        validateManagedStudentIdentity(payload.getType(), payload.getStudentId(), payload.getEmail());
        payload.setRoleId(source.getRoleId());
        payload.setStatus(source.getStatus() != null ? source.getStatus() : (existing == null || existing.getStatus() == null ? 1 : existing.getStatus().getValue()));
        if (creating) {
            payload.setPassword(StringUtils.isNotBlank(source.getPassword()) ? source.getPassword() : DEFAULT_PASSWORD);
        } else if (StringUtils.isNotBlank(source.getPassword())) {
            payload.setPassword(source.getPassword());
        }
        return payload;
    }

    private void validateManagedProvisionSource(AdminUserProvisionDTO source) {
        if (source == null) {
            return;
        }
        validateManagedStudentIdentity(source.getType(), source.getStudentId(), source.getEmail());
    }

    private void validateManagedStudentIdentity(Integer type, String studentId, String email) {
        UserType resolvedType = type == null ? UserType.STUDENT : UserType.of(type);
        if (resolvedType == UserType.STUDENT && StringUtils.isBlank(studentId)) {
            throw new BadRequestException(MANAGED_STUDENT_ID_REQUIRED_MESSAGE);
        }
        if (resolvedType == UserType.STUDENT && StringUtils.isBlank(email)) {
            throw new BadRequestException(MANAGED_STUDENT_EMAIL_REQUIRED_MESSAGE);
        }
    }

    private Long ensureManagedAuthUserId(User existing) {
        if (existing.getAuthUserId() != null) {
            return existing.getAuthUserId();
        }
        AdminUserProvisionDTO payload = buildProvisionPayload(new AdminUserProvisionDTO(), existing, true);
        Long authUserId = authClient.provisionAdminUser(payload);
        existing.setAuthUserId(authUserId);
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
        return authUserId;
    }

    private UserType resolveManagedType(Integer dtoType, UserType current) {
        return resolveManagedType(dtoType, current, null, null, null);
    }

    private UserType resolveManagedType(
            Integer dtoType,
            UserType current,
            String studentId,
            String preferredPosition,
            String existingPosition
    ) {
        UserType resolved = dtoType != null ? UserType.of(dtoType) : current;
        if (isTeacherPosition(preferredPosition) || isTeacherPosition(existingPosition) || resolved == UserType.TEACHER) {
            return UserType.TEACHER;
        }
        if (resolved == null) {
            resolved = UserType.STUDENT;
        }
        if (resolved == UserType.STAFF
                && StringUtils.isNotBlank(studentId)
                && !isSyntheticMemberStudentId(studentId)
                && !isManagerPosition(preferredPosition)
                && !isManagerPosition(existingPosition)) {
            return UserType.STUDENT;
        }
        return resolved;
    }

    private boolean isManagerPosition(String position) {
        if (StringUtils.isBlank(position)) {
            return false;
        }
        String normalized = position.trim();
        return UserServiceImplManagedMemberHints.MANAGER_POSITIONS.stream()
                .anyMatch(normalized::equals);
    }

    private boolean isTeacherPosition(String position) {
        if (StringUtils.isBlank(position)) {
            return false;
        }
        String normalized = position.trim();
        return UserServiceImplManagedMemberHints.TEACHER_POSITIONS.stream()
                .anyMatch(normalized::equals);
    }

    private String resolveUsername(String preferred, String fallbackPhone, String current) {
        if (StringUtils.isNotBlank(preferred)) {
            return preferred;
        }
        if (StringUtils.isNotBlank(fallbackPhone)) {
            return fallbackPhone;
        }
        return current;
    }

    private String resolveAvailableCellPhone(Long tenantId, String preferredCellPhone, Long currentUserId) {
        if (StringUtils.isBlank(preferredCellPhone)) {
            return null;
        }
        String normalized = preferredCellPhone.trim();
        Long resolvedTenantId = tenantId == null ? 1L : tenantId;
        User owner = baseMapper.selectAnyByTenantAndCellPhone(resolvedTenantId, normalized);
        if (owner == null || Objects.equals(owner.getId(), currentUserId)) {
            return normalized;
        }
        log.warn("skip duplicate cell phone while syncing people domain, tenantId={}, phone={}, ownerUserId={}, currentUserId={}",
                resolvedTenantId, normalized, owner.getId(), currentUserId);
        return null;
    }

    private String resolveMergedUsername(User existingUser, UserDTO userDTO) {
        if (existingUser != null && StringUtils.isNotBlank(existingUser.getUsername())) {
            return existingUser.getUsername();
        }
        return resolveUsername(
                userDTO == null ? null : userDTO.getUsername(),
                userDTO == null ? null : userDTO.getCellPhone(),
                existingUser == null ? null : existingUser.getUsername()
        );
    }

    private Long resolveTenantId(User existing) {
        if (existing != null && existing.getTenantId() != null) {
            return existing.getTenantId();
        }
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private void upsertLocalUserDetail(Long userId, UserDTO userDTO) {
        UserDetail detail = BeanUtils.toBean(userDTO, UserDetail.class);
        detail.setUserId(userId);
        detail.setTenantId(userDTO.getTenantId() != null ? userDTO.getTenantId() : resolveTenantId(null));
        detail.setRoleId(null);
        detail.setType(null);
        detail.setUpdateTime(LocalDateTime.now());

        UserDetail existingDetail = findUserDetailByUserId(userId);
        if (existingDetail == null) {
            detail.setId(null);
            detail.setCreateTime(LocalDateTime.now());
            detailService.save(detail);
        } else {
            detail.setId(existingDetail.getId());
            detail.setCreateTime(existingDetail.getCreateTime());
            detailService.updateById(detail);
        }
    }

    private UserDTO toManagedUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setAuthUserId(user.getAuthUserId());
        userDTO.setTenantId(user.getTenantId());
        userDTO.setCellPhone(user.getCellPhone());
        userDTO.setUsername(user.getUsername());
        userDTO.setName(user.getRealName());
        userDTO.setIcon(user.getAvatar());
        userDTO.setPhoto(user.getAvatar());
        userDTO.setEmail(user.getEmail());
        userDTO.setStudentId(user.getStudentId());
        userDTO.setCollege(user.getCollege());
        userDTO.setMajor(user.getMajor());
        userDTO.setGrade(user.getGrade());
        userDTO.setType(user.getType() == null ? null : user.getType().getValue());
        userDTO.setStatus(user.getStatus() == null ? null : user.getStatus().getValue());
        ClubMember member = findManagedMember(user);
        if (member != null) {
            userDTO.setDepartment(member.getDepartment());
            userDTO.setPosition(member.getPosition());
            userDTO.setJoinDate(member.getJoinDate());
        }
        return userDTO;
    }

    @Override
    @Transactional
    public PeopleDomainReconcileResultVO reconcilePeopleDomainForCurrentScope() {
        long startedAt = System.currentTimeMillis();
        PeopleDomainReconcileResultVO result = new PeopleDomainReconcileResultVO();
        result.setScopeLabel(resolvePeopleDomainReconcileScopeLabel());

        List<User> scopedUsers = baseMapper.selectList(buildScopedUserReconcileQuery());
        if (scopedUsers != null) {
            for (User scopedUser : scopedUsers) {
                result.setScannedAccountCount(result.getScannedAccountCount() + 1);
                ManagedMemberSyncResult syncResult = syncManagedMemberFromUser(scopedUser);
                applyManagedMemberSyncResult(result, syncResult);
            }
        }
        List<ClubMember> scopedMembers = clubMemberMapper.selectList(buildScopedMemberReconcileQuery());
        if (scopedMembers == null || scopedMembers.isEmpty()) {
            finishPeopleDomainReconcileResult(result, startedAt);
            return result;
        }
        for (ClubMember scopedMember : scopedMembers) {
            result.setScannedMemberCount(result.getScannedMemberCount() + 1);
            MemberUserReconcileResult userResult = reconcileManagedUserFromMember(scopedMember);
            applyMemberUserReconcileResult(result, userResult);
            AlumniReconcileResult alumniResult = reconcileAlumniFromMember(scopedMember);
            applyAlumniReconcileResult(result, alumniResult);
        }
        finishPeopleDomainReconcileResult(result, startedAt);
        return result;
    }

    private void applyManagedMemberSyncResult(PeopleDomainReconcileResultVO result, ManagedMemberSyncResult syncResult) {
        if (result == null || syncResult == null || syncResult.status == null) {
            return;
        }
        switch (syncResult.status) {
            case CREATED:
                result.setCreatedMemberCount(result.getCreatedMemberCount() + 1);
                break;
            case UPDATED:
                result.setUpdatedMemberCount(result.getUpdatedMemberCount() + 1);
                break;
            case RESTORED:
                result.setRestoredMemberCount(result.getRestoredMemberCount() + 1);
                break;
            case DELETED:
                result.setDeletedMemberCount(result.getDeletedMemberCount() + 1);
                break;
            case UNCHANGED:
                result.setUnchangedMemberCount(result.getUnchangedMemberCount() + 1);
                break;
            case SKIPPED_NO_STUDENT_ID:
                result.setSkippedAccountWithoutStudentIdCount(result.getSkippedAccountWithoutStudentIdCount() + 1);
                break;
            default:
                break;
        }
        result.addDetail(syncResult.detail);
    }

    private void applyMemberUserReconcileResult(PeopleDomainReconcileResultVO result, MemberUserReconcileResult userResult) {
        if (result == null || userResult == null) {
            return;
        }
        if (userResult.createdUser) {
            result.setCreatedUserCount(result.getCreatedUserCount() + 1);
        }
        if (userResult.updatedUser) {
            result.setUpdatedUserCount(result.getUpdatedUserCount() + 1);
        }
        if (userResult.createdAuthAccount) {
            result.setCreatedAuthAccountCount(result.getCreatedAuthAccountCount() + 1);
        }
        if (userResult.skippedSyntheticMember) {
            result.setSkippedSyntheticMemberCount(result.getSkippedSyntheticMemberCount() + 1);
        }
        if (userResult.skippedMismatchedSyntheticMember) {
            result.setSkippedMismatchedSyntheticMemberCount(result.getSkippedMismatchedSyntheticMemberCount() + 1);
        }
        result.addDetail(userResult.detail);
    }

    private void applyAlumniReconcileResult(PeopleDomainReconcileResultVO result, AlumniReconcileResult alumniResult) {
        if (result == null || alumniResult == null) {
            return;
        }
        if (alumniResult.created) {
            result.setCreatedAlumniCount(result.getCreatedAlumniCount() + 1);
        } else if (alumniResult.updated) {
            result.setUpdatedAlumniCount(result.getUpdatedAlumniCount() + 1);
        } else if (alumniResult.skipped) {
            result.setSkippedAlumniCount(result.getSkippedAlumniCount() + 1);
        }
    }

    private void finishPeopleDomainReconcileResult(PeopleDomainReconcileResultVO result, long startedAt) {
        result.setCostTimeMs(Math.max(0L, System.currentTimeMillis() - startedAt));
        result.finishSummary();
        log.info("people domain reconcile finished, scope={}, {}", result.getScopeLabel(), result.getSummary());
        recordPeopleDomainReconcileAudit(result);
    }

    private String resolvePeopleDomainReconcileScopeLabel() {
        if (canManageAllTenants()) {
            return "平台可见租户";
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return "租户 " + tenantId;
        }
        return "当前租户 " + currentTenantId();
    }

    private void recordPeopleDomainReconcileAudit(PeopleDomainReconcileResultVO result) {
        if (sysOperLogService == null || result == null) {
            return;
        }
        try {
            sysOperLogService.recordOperation(
                    "修复账号/成员差额",
                    "UserServiceImpl.reconcilePeopleDomainForCurrentScope",
                    "POST",
                    2,
                    resolvePeopleDomainAuditOperator(),
                    "/users/reconcile/people-domain",
                    "server",
                    result.getSummary(),
                    0,
                    null,
                    result.getCostTimeMs()
            );
        } catch (Exception e) {
            log.warn("record people domain reconcile audit failed: {}", e.getMessage());
        }
    }

    private String resolvePeopleDomainAuditOperator() {
        Long authUserId = UserContext.getUser();
        if (authUserId == null) {
            return "system";
        }
        try {
            User user = findTenantScopedLocalUserByAuthUserId(TenantContext.getTenantId(), authUserId);
            if (user == null) {
                user = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getAuthUserId, authUserId)
                        .last("LIMIT 1"));
            }
            if (user != null && StringUtils.isNotBlank(user.getUsername())) {
                return user.getUsername();
            }
        } catch (Exception e) {
            log.debug("resolve people domain audit operator fallback, authUserId={}: {}", authUserId, e.getMessage());
        }
        return "auth:" + authUserId;
    }

    @Override
    public List<EmailLoginCandidateDTO> queryEmailLoginCandidates(EmailLoginCandidateQueryDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getEmail())) {
            return List.of();
        }
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        return executeWithoutTenantScope(() -> lambdaQuery()
                .eq(User::getEmail, normalizedEmail)
                .isNotNull(User::getAuthUserId)
                .eq(User::getIsDeleted, 0)
                .eq(User::getStatus, UserStatus.NORMAL)
                .list()
                .stream()
                .filter(item -> dto.getTenantId() == null || Objects.equals(item.getTenantId(), dto.getTenantId()))
                .sorted(Comparator.comparing(User::getTenantId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(User::getId, Comparator.nullsLast(Long::compareTo)))
                .map(item -> {
                    EmailLoginCandidateDTO candidate = new EmailLoginCandidateDTO();
                    candidate.setAuthUserId(item.getAuthUserId());
                    candidate.setTenantId(item.getTenantId());
                    candidate.setLocalUserId(item.getId());
                    candidate.setUsername(item.getUsername());
                    candidate.setDisplayName(resolveEmailLoginDisplayName(item));
                    candidate.setAvatar(item.getAvatar());
                    candidate.setEmail(normalizedEmail);
                    return candidate;
                })
                .collect(Collectors.toList()));
    }

    private LambdaQueryWrapper<User> buildScopedUserReconcileQuery() {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getIsDeleted, 0)
                .eq(User::getStatus, UserStatus.NORMAL);
        if (canManageAllTenants()) {
            applyActiveTenantScope(queryWrapper);
        } else {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                queryWrapper.eq(User::getTenantId, tenantId);
            } else {
                queryWrapper.eq(User::getTenantId, currentTenantId());
            }
        }
        return queryWrapper;
    }

    private LambdaQueryWrapper<ClubMember> buildScopedMemberReconcileQuery() {
        LambdaQueryWrapper<ClubMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ClubMember::getIsDeleted, 0);
        if (canManageAllTenants()) {
            List<Long> activeTenantIds = resolveActiveTenantIds();
            if (activeTenantIds.isEmpty()) {
                queryWrapper.eq(ClubMember::getTenantId, -1L);
            } else {
                queryWrapper.in(ClubMember::getTenantId, activeTenantIds);
            }
        } else if (TenantContext.getTenantId() != null) {
            queryWrapper.eq(ClubMember::getTenantId, TenantContext.getTenantId());
        } else {
            queryWrapper.eq(ClubMember::getTenantId, currentTenantId());
        }
        return queryWrapper;
    }

    private MemberUserReconcileResult reconcileManagedUserFromMember(ClubMember member) {
        MemberUserReconcileResult result = new MemberUserReconcileResult();
        if (member == null || member.getTenantId() == null) {
            result.detail = "跳过成员：成员为空或缺少租户";
            return result;
        }
        User existingLocalUser = findTenantScopedLocalUserByStudentId(member.getTenantId(), member.getStudentId());
        if (existingLocalUser != null && !syntheticMemberMatchesLocalUser(member, existingLocalUser)) {
            log.warn("skip mismatched synthetic member ledger, tenantId={}, studentId={}, matchedAuthUserId={}",
                    member.getTenantId(), member.getStudentId(), existingLocalUser.getAuthUserId());
            result.skippedMismatchedSyntheticMember = true;
            result.detail = "合成管理人员台账与账号不匹配，已跳过（需核查）：tenantId=" + member.getTenantId() + "，studentId=" + member.getStudentId();
            existingLocalUser = null;
        }
        if (existingLocalUser == null && isSyntheticMemberStudentId(member.getStudentId())) {
            result.skippedSyntheticMember = true;
            if (result.detail == null) {
                result.detail = "合成管理人员台账已跳过（非异常）：tenantId=" + member.getTenantId() + "，studentId=" + member.getStudentId();
            }
            return result;
        }
        if (existingLocalUser == null) {
            existingLocalUser = findTenantScopedLocalUserByEmail(member.getTenantId(), member.getEmail());
        }
        boolean creatingLocalUser = existingLocalUser == null;
        Long authUserId = existingLocalUser == null ? null : existingLocalUser.getAuthUserId();
        if (authUserId == null) {
            authUserId = provisionAuthAccountForMember(member);
            result.createdAuthAccount = true;
        }
        saveUser(buildUserDTOFromMember(member, authUserId));
        if (creatingLocalUser) {
            result.createdUser = true;
            result.detail = "成员补建用户账号：" + firstNonBlank(member.getEmail(), member.getStudentId(), member.getName());
        } else {
            result.updatedUser = true;
        }
        return result;
    }

    private boolean syntheticMemberMatchesLocalUser(ClubMember member, User localUser) {
        if (member == null || !isSyntheticMemberStudentId(member.getStudentId())) {
            return true;
        }
        Long authUserId = parseAuthUserIdFromSyntheticMemberStudentId(member.getStudentId());
        return authUserId == null || (localUser != null && Objects.equals(localUser.getAuthUserId(), authUserId));
    }

    private Long parseAuthUserIdFromSyntheticMemberStudentId(String studentId) {
        if (StringUtils.isBlank(studentId)) {
            return null;
        }
        String normalized = studentId.trim();
        if (!normalized.startsWith(AUTH_MEMBER_STUDENT_ID_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(normalized.substring(AUTH_MEMBER_STUDENT_ID_PREFIX.length()));
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private AlumniReconcileResult reconcileAlumniFromMember(ClubMember member) {
        AlumniReconcileResult result = new AlumniReconcileResult();
        if (!shouldSyncAlumniFromMember(member)) {
            return result;
        }
        Integer enrollmentYear = resolveMemberEnrollmentYear(member);
        if (enrollmentYear == null) {
            return result;
        }
        ClubAlumni alumni = findExistingAlumni(member);
        boolean creating = alumni == null;
        if (creating) {
            alumni = new ClubAlumni();
            alumni.setCreateTime(LocalDateTime.now());
        }
        alumni.setTenantId(member.getTenantId());
        alumni.setName(member.getName());
        alumni.setStudentId(member.getStudentId());
        alumni.setEmail(member.getEmail());
        alumni.setMajor(member.getMajor());
        alumni.setDepartment(member.getDepartment());
        alumni.setPosition(member.getPosition());
        alumni.setGenerationYear(enrollmentYear);
        alumni.setEnrollmentYear(enrollmentYear);
        alumni.setExpectedGraduationYear(enrollmentYear + 4);
        alumni.setShowTable(true);
        alumni.setIsActive(true);
        alumni.setIsCoreMember(Boolean.TRUE.equals(alumni.getIsCoreMember()));
        alumni.setMemberStatus("已毕业");
        alumni.setGraduationStatus("已毕业");
        alumni.setIsDeleted(0);
        alumni.setUpdateTime(LocalDateTime.now());
        if (creating) {
            clubAlumniMapper.insert(alumni);
            result.created = true;
        } else {
            clubAlumniMapper.updateById(alumni);
            result.updated = true;
        }
        result.skipped = false;
        return result;
    }

    private UserDTO buildUserDTOFromMember(ClubMember member, Long authUserId) {
        UserDTO dto = new UserDTO();
        dto.setId(authUserId);
        dto.setTenantId(member.getTenantId());
        dto.setUsername(resolveManagedMemberUsername(member));
        dto.setName(member.getName());
        dto.setEmail(member.getEmail());
        dto.setCellPhone(member.getPhone());
        dto.setStudentId(member.getStudentId());
        dto.setCollege(member.getDepartment());
        dto.setMajor(member.getMajor());
        dto.setGrade(member.getGrade());
        dto.setType(resolveManagedType(null, UserType.STUDENT, member.getStudentId(), member.getPosition(), member.getPosition()).getValue());
        return dto;
    }

    private boolean shouldSyncAlumniFromMember(ClubMember member) {
        if (member == null || member.getTenantId() == null || StringUtils.isBlank(member.getStudentId())) {
            return false;
        }
        Integer enrollmentYear = resolveMemberEnrollmentYear(member);
        return enrollmentYear != null && LocalDate.now().getYear() - enrollmentYear >= 4;
    }

    private Integer resolveMemberEnrollmentYear(ClubMember member) {
        if (member == null) {
            return null;
        }
        return parseEnrollmentYear(member.getGrade());
    }

    private Integer parseEnrollmentYear(String grade) {
        if (StringUtils.isBlank(grade)) {
            return null;
        }
        String digitsOnly = grade.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 4) {
            return null;
        }
        int candidate = Integer.parseInt(digitsOnly.substring(0, 4));
        int currentYear = LocalDate.now().getYear();
        if (candidate < 2000 || candidate > currentYear + 1) {
            return null;
        }
        return candidate;
    }

    private ClubAlumni findExistingAlumni(ClubMember member) {
        if (member == null || member.getTenantId() == null) {
            return null;
        }
        LambdaQueryWrapper<ClubAlumni> queryWrapper = new LambdaQueryWrapper<ClubAlumni>()
                .eq(ClubAlumni::getTenantId, member.getTenantId())
                .eq(ClubAlumni::getIsDeleted, 0);
        if (StringUtils.isNotBlank(member.getStudentId())) {
            queryWrapper.eq(ClubAlumni::getStudentId, member.getStudentId());
        } else if (StringUtils.isNotBlank(member.getEmail())) {
            queryWrapper.eq(ClubAlumni::getEmail, member.getEmail());
        } else {
            queryWrapper.eq(ClubAlumni::getName, member.getName());
        }
        queryWrapper.last("LIMIT 1");
        return clubAlumniMapper.selectOne(queryWrapper);
    }

    private Long provisionAuthAccountForMember(ClubMember member) {
        ApprovedApplicantProvisionDTO dto = new ApprovedApplicantProvisionDTO();
        dto.setTenantId(member.getTenantId());
        dto.setUsername(resolveManagedMemberUsername(member));
        dto.setEncodedPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        return authClient.provisionApprovedApplicant(dto);
    }

    private String resolveManagedMemberUsername(ClubMember member) {
        if (StringUtils.isNotBlank(member.getEmail())) {
            return member.getEmail();
        }
        if (StringUtils.isNotBlank(member.getPhone())) {
            return member.getPhone();
        }
        if (StringUtils.isNotBlank(member.getStudentId())) {
            return member.getStudentId();
        }
        return member.getName();
    }

    private User resolveExistingLocalUserForSave(Long tenantId, UserDTO userDTO) {
        User existing = findTenantScopedLocalUserByStudentId(tenantId, userDTO.getStudentId());
        if (existing != null) {
            return existing;
        }
        return findTenantScopedLocalUserByEmail(tenantId, userDTO.getEmail());
    }

    private User findTenantScopedLocalUserByAuthUserId(Long tenantId, Long authUserId) {
        if (tenantId == null || authUserId == null) {
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .eq(User::getAuthUserId, authUserId)
                .last("LIMIT 1"));
    }

    private User findTenantScopedLocalUserByStudentId(Long tenantId, String studentId) {
        if (tenantId == null || StringUtils.isBlank(studentId)) {
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .eq(User::getStudentId, studentId)
                .eq(User::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private User findTenantScopedLocalUserByEmail(Long tenantId, String email) {
        if (tenantId == null || StringUtils.isBlank(email)) {
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .eq(User::getEmail, email.trim())
                .eq(User::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private void syncManagedMemberFromProvision(User localUser, AdminUserProvisionDTO dto) {
        if (localUser == null || dto == null) {
            return;
        }
        if (isManagedMemberSyncOptional(localUser)) {
            return;
        }
        String managedStudentId = resolveManagedMemberStudentId(localUser, dto.getStudentId());
        normalizeManagedLocalUserType(localUser, managedStudentId, dto.getPosition());
        if (!shouldSyncManagedMember(localUser, managedStudentId)) {
            deleteManagedMember(localUser, managedStudentId);
            return;
        }
        upsertManagedMember(
                localUser,
                managedStudentId,
                dto.getName(),
                dto.getEmail(),
                dto.getCellPhone(),
                dto.getMajor(),
                dto.getGrade(),
                dto.getDepartment(),
                resolveManagedMemberPositionOverride(localUser, managedStudentId, dto.getPosition()),
                dto.getJoinDate()
        );
    }

    private ManagedMemberSyncResult syncManagedMemberFromUser(User localUser) {
        if (localUser == null) {
            return ManagedMemberSyncResult.of(ManagedMemberSyncStatus.SKIPPED_NO_STUDENT_ID, "跳过账号：账号为空");
        }
        if (isManagedMemberSyncOptional(localUser)) {
            return ManagedMemberSyncResult.of(
                    ManagedMemberSyncStatus.SKIPPED_NO_STUDENT_ID,
                    "跳过可选同步账号：" + firstNonBlank(localUser.getUsername(), localUser.getEmail(), String.valueOf(localUser.getId()))
            );
        }
        String managedStudentId = resolveManagedMemberStudentId(localUser, localUser.getStudentId());
        normalizeManagedLocalUserType(localUser, managedStudentId, null);
        if (!shouldSyncManagedMember(localUser, managedStudentId)) {
            boolean deleted = deleteManagedMember(localUser, managedStudentId);
            if (deleted) {
                return ManagedMemberSyncResult.of(ManagedMemberSyncStatus.DELETED, "删除无效成员台账：" + localUser.getUsername());
            }
            return ManagedMemberSyncResult.of(
                    ManagedMemberSyncStatus.SKIPPED_NO_STUDENT_ID,
                    "跳过无学号账号：" + firstNonBlank(localUser.getUsername(), localUser.getEmail(), String.valueOf(localUser.getId()))
            );
        }
        return upsertManagedMember(
                localUser,
                managedStudentId,
                localUser.getRealName(),
                localUser.getEmail(),
                localUser.getCellPhone(),
                localUser.getMajor(),
                localUser.getGrade(),
                null,
                resolveManagedMemberPositionOverride(localUser, managedStudentId, null),
                localUser.getJoinTime()
        );
    }

    private boolean shouldSyncManagedMember(User localUser, String studentId) {
        return localUser != null
                && StringUtils.isNotBlank(studentId);
    }

    private String resolveManagedMemberStudentId(User localUser, String preferredStudentId) {
        Long authUserId = localUser == null ? null : localUser.getAuthUserId();
        return resolveManagedMemberStudentId(localUser, preferredStudentId, authUserId);
    }

    private String resolveManagedMemberStudentId(User localUser, String preferredStudentId, Long authUserId) {
        String studentId = firstNonBlank(preferredStudentId, localUser == null ? null : localUser.getStudentId());
        if (StringUtils.isNotBlank(studentId)) {
            return studentId;
        }
        if (localUser != null && isPrivilegedMemberType(localUser.getType()) && authUserId != null) {
            return AUTH_MEMBER_STUDENT_ID_PREFIX + authUserId;
        }
        if (localUser != null && isPrivilegedMemberType(localUser.getType()) && localUser.getId() != null) {
            return LOCAL_MEMBER_STUDENT_ID_PREFIX + localUser.getId();
        }
        return null;
    }

    private boolean isPrivilegedMemberType(UserType type) {
        return type == UserType.STAFF;
    }

    private boolean isManagedMemberSyncOptional(User localUser) {
        if (localUser == null) {
            return false;
        }
        if (localUser.getType() == UserType.TEACHER) {
            return true;
        }
        return localUser.getType() == UserType.STAFF && Objects.equals(resolveAuthPrimaryRoleId(localUser), 1L);
    }

    private Long resolveAuthPrimaryRoleId(User localUser) {
        if (localUser == null || localUser.getAuthUserId() == null) {
            return null;
        }
        try {
            AdminUserProvisionDTO authUser = authClient.queryAdminUserById(localUser.getAuthUserId());
            return authUser == null ? null : authUser.getRoleId();
        } catch (Exception e) {
            log.debug("resolve auth primary role failed, localUserId={}, authUserId={}: {}",
                    localUser.getId(), localUser.getAuthUserId(), e.getMessage());
            return null;
        }
    }

    private boolean isSyntheticMemberStudentId(String studentId) {
        if (StringUtils.isBlank(studentId)) {
            return false;
        }
        String normalized = studentId.trim();
        return normalized.startsWith(AUTH_MEMBER_STUDENT_ID_PREFIX)
                || normalized.startsWith(LOCAL_MEMBER_STUDENT_ID_PREFIX);
    }

    private String resolveManagedMemberPositionOverride(User localUser, String studentId, String currentPosition) {
        if (localUser != null && StringUtils.isNotBlank(studentId) && StringUtils.isBlank(currentPosition)) {
            return null;
        }
        if (StringUtils.isNotBlank(currentPosition)) {
            return currentPosition;
        }
        if (localUser == null || StringUtils.isBlank(studentId)) {
            return currentPosition;
        }
        if (localUser.getType() == UserType.STAFF) {
            return "成员";
        }
        return currentPosition;
    }

    private void normalizeManagedLocalUserType(User localUser, String studentId, String preferredPosition) {
        if (localUser == null) {
            return;
        }
        ClubMember existingManagedMember = findManagedMember(localUser.getTenantId(), studentId);
        UserType normalizedType = resolveManagedType(
                localUser.getType() == null ? null : localUser.getType().getValue(),
                localUser.getType(),
                studentId,
                preferredPosition,
                existingManagedMember == null ? null : existingManagedMember.getPosition()
        );
        if (normalizedType == localUser.getType()) {
            return;
        }
        localUser.setType(normalizedType);
        localUser.setUpdateTime(LocalDateTime.now());
        if (localUser.getId() != null) {
            updateById(localUser);
        }
    }

    private ManagedMemberSyncResult upsertManagedMember(
            User localUser,
            String studentId,
            String name,
            String email,
            String cellPhone,
            String major,
            String grade,
            String department,
            String position,
            LocalDateTime joinDate
    ) {
        Long tenantId = localUser.getTenantId() != null ? localUser.getTenantId() : 1L;
        ClubMember member = clubMemberMapper.selectOne(new LambdaQueryWrapper<ClubMember>()
                .eq(ClubMember::getTenantId, tenantId)
                .eq(ClubMember::getStudentId, studentId)
                .last("LIMIT 1"));

        boolean restoringDeletedMember = false;
        if (member == null) {
            ClubMember deletedMember = clubMemberMapper.selectAnyByTenantAndStudentId(tenantId, studentId);
            if (deletedMember != null) {
                member = deletedMember;
                restoringDeletedMember = true;
            } else {
                member = new ClubMember();
                member.setCreateTime(LocalDateTime.now());
            }
        }

        String resolvedName = firstNonBlank(name, localUser.getRealName(), member.getName(), localUser.getUsername());
        String resolvedEmail = firstNonBlank(email, localUser.getEmail(), member.getEmail());
        if (StringUtils.isBlank(resolvedEmail)) {
            resolvedEmail = buildMissingManagedMemberEmail(tenantId, studentId, localUser);
            log.warn("managed member email missing, tenantId={}, studentId={}, username={}, usePlaceholder={}",
                    tenantId, studentId, localUser.getUsername(), resolvedEmail);
        }
        String resolvedCellPhone = firstNonBlank(cellPhone, localUser.getCellPhone(), member.getPhone());
        String resolvedMajor = firstNonBlank(major, localUser.getMajor(), member.getMajor());
        String resolvedGrade = firstNonBlank(grade, localUser.getGrade(), member.getGrade());
        String resolvedDepartment = firstNonBlank(department, member.getDepartment());
        String resolvedPosition = resolveMemberPosition(localUser.getType(), position, member.getPosition());
        LocalDateTime resolvedJoinDate = normalizeManagedMemberJoinDate(
                joinDate != null ? joinDate : (member.getJoinDate() != null ? member.getJoinDate() : LocalDateTime.now())
        );

        if (member.getId() != null
                && !restoringDeletedMember
                && !managedMemberNeedsUpdate(member, tenantId, studentId, resolvedName, resolvedEmail, resolvedCellPhone,
                resolvedMajor, resolvedGrade, resolvedDepartment, resolvedPosition, resolvedJoinDate)) {
            return ManagedMemberSyncResult.of(ManagedMemberSyncStatus.UNCHANGED, null);
        }

        member.setTenantId(tenantId);
        member.setName(resolvedName);
        member.setStudentId(studentId);
        member.setEmail(resolvedEmail);
        member.setPhone(resolvedCellPhone);
        member.setMajor(resolvedMajor);
        member.setGrade(resolvedGrade);
        member.setDepartment(resolvedDepartment);
        member.setPosition(resolvedPosition);
        member.setJoinDate(resolvedJoinDate);
        member.setStatus(MEMBER_STATUS_ACTIVE);
        member.setIsDeleted(0);
        member.setUpdateTime(LocalDateTime.now());

        if (member.getId() == null) {
            clubMemberMapper.insert(member);
            return ManagedMemberSyncResult.of(ManagedMemberSyncStatus.CREATED, "账号补建成员台账：" + firstNonBlank(resolvedEmail, studentId, resolvedName));
        } else if (restoringDeletedMember) {
            clubMemberMapper.updateIncludingDeleted(member);
            return ManagedMemberSyncResult.of(ManagedMemberSyncStatus.RESTORED, "恢复成员台账：" + firstNonBlank(resolvedEmail, studentId, resolvedName));
        } else {
            clubMemberMapper.updateById(member);
            return ManagedMemberSyncResult.of(ManagedMemberSyncStatus.UPDATED, "更新成员台账：" + firstNonBlank(resolvedEmail, studentId, resolvedName));
        }
    }

    private String buildMissingManagedMemberEmail(Long tenantId, String studentId, User localUser) {
        String rawIdentity = firstNonBlank(
                studentId,
                localUser == null ? null : localUser.getUsername(),
                localUser == null || localUser.getId() == null ? null : String.valueOf(localUser.getId()),
                "unknown"
        );
        String normalizedIdentity = rawIdentity.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalizedIdentity.isBlank()) {
            normalizedIdentity = "unknown";
        }
        return "no-email-" + (tenantId == null ? 1L : tenantId) + "-" + normalizedIdentity + "@invalid.local";
    }

    private boolean managedMemberNeedsUpdate(
            ClubMember member,
            Long tenantId,
            String studentId,
            String name,
            String email,
            String cellPhone,
            String major,
            String grade,
            String department,
            String position,
            LocalDateTime joinDate
    ) {
        return !Objects.equals(member.getTenantId(), tenantId)
                || !Objects.equals(member.getStudentId(), studentId)
                || !Objects.equals(member.getName(), name)
                || !Objects.equals(member.getEmail(), email)
                || !Objects.equals(member.getPhone(), cellPhone)
                || !Objects.equals(member.getMajor(), major)
                || !Objects.equals(member.getGrade(), grade)
                || !Objects.equals(member.getDepartment(), department)
                || !Objects.equals(member.getPosition(), position)
                || !Objects.equals(normalizeManagedMemberJoinDate(member.getJoinDate()), normalizeManagedMemberJoinDate(joinDate))
                || !Objects.equals(member.getStatus(), MEMBER_STATUS_ACTIVE)
                || !Objects.equals(member.getIsDeleted(), 0);
    }

    private LocalDateTime normalizeManagedMemberJoinDate(LocalDateTime joinDate) {
        if (joinDate == null) {
            return null;
        }
        return joinDate.toLocalDate().atStartOfDay();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String resolveMemberPosition(UserType userType, String preferredPosition, String existingPosition) {
        if (StringUtils.isNotBlank(preferredPosition)) {
            return preferredPosition;
        }
        if (StringUtils.isNotBlank(existingPosition)) {
            return existingPosition;
        }
        if (userType == UserType.TEACHER) {
            return "指导老师";
        }
        if (userType == UserType.STAFF) {
            return "管理员";
        }
        return "成员";
    }

    private ClubMember findManagedMember(User user) {
        String studentId = resolveManagedMemberStudentId(user, user == null ? null : user.getStudentId());
        if (user == null || StringUtils.isBlank(studentId) || user.getTenantId() == null) {
            return null;
        }
        return findManagedMember(user.getTenantId(), studentId);
    }

    private ClubMember findManagedMember(Long tenantId, String studentId) {
        if (tenantId == null || StringUtils.isBlank(studentId)) {
            return null;
        }
        return clubMemberMapper.selectOne(new LambdaQueryWrapper<ClubMember>()
                .eq(ClubMember::getTenantId, tenantId)
                .eq(ClubMember::getStudentId, studentId)
                .last("LIMIT 1"));
    }

    private boolean deleteManagedMember(User user) {
        return deleteManagedMember(user, user == null ? null : user.getStudentId());
    }

    private boolean deleteManagedMember(User user, String studentId) {
        String managedStudentId = resolveManagedMemberStudentId(user, studentId);
        if (user == null || StringUtils.isBlank(managedStudentId) || user.getTenantId() == null) {
            return false;
        }
        ClubMember member = clubMemberMapper.selectOne(new LambdaQueryWrapper<ClubMember>()
                .eq(ClubMember::getTenantId, user.getTenantId())
                .eq(ClubMember::getStudentId, managedStudentId)
                .last("LIMIT 1"));
        if (member == null || member.getId() == null) {
            return false;
        }
        clubMemberMapper.deleteById(member.getId());
        return true;
    }

    private void retirePreviousManagedMemberLedger(User user, String previousStudentId, String currentStudentId) {
        if (user == null || StringUtils.isBlank(previousStudentId) || Objects.equals(previousStudentId, currentStudentId)) {
            return;
        }
        deleteManagedMember(user, previousStudentId);
    }

    private UserDetail findUserDetailByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return detailService.getOne(new LambdaQueryWrapper<UserDetail>()
                .eq(UserDetail::getUserId, userId)
                .last("LIMIT 1"));
    }

    private void enrichRoleInfo(UserDTO userDTO, Long authUserId) {
        if (authUserId == null) {
            return;
        }
        try {
            AdminUserProvisionDTO authUser = authClient.queryAdminUserById(authUserId);
            if (authUser == null) {
                return;
            }
            userDTO.setRoleId(authUser.getRoleId());
            if (authUser.getRoleId() != null) {
                RoleDTO roleDTO = authClient.queryRoleById(authUser.getRoleId());
                userDTO.setRoleName(roleDTO == null ? null : roleDTO.getName());
            }
        } catch (Exception e) {
            log.warn("查询auth角色信息失败, localUserId={}, authUserId={}", userDTO.getId(), authUserId, e);
        }
    }

    private void applyFallbackRoleInfo(UserDTO userDTO, User user) {
        if (userDTO == null || userDTO.getRoleId() != null || user == null || user.getType() == null) {
            return;
        }
        switch (user.getType()) {
            case STAFF:
                userDTO.setRoleId(CLUB_MANAGER_ROLE_ID);
                userDTO.setRoleName(CLUB_MANAGER_ROLE_NAME);
                break;
            case TEACHER:
                userDTO.setRoleId(REVIEW_TEACHER_ROLE_ID);
                userDTO.setRoleName(REVIEW_TEACHER_ROLE_NAME);
                break;
            case STUDENT:
                userDTO.setRoleId(STUDENT_ROLE_ID);
                userDTO.setRoleName(STUDENT_ROLE_NAME);
                break;
            default:
                break;
        }
    }

    private String resolveCurrentRoleName(User currentUser, UserDetail userDetail, UserType type) {
        Long authRoleId = null;
        if (currentUser != null && currentUser.getAuthUserId() != null) {
            try {
                AdminUserProvisionDTO authUser = authClient.queryAdminUserById(currentUser.getAuthUserId());
                if (authUser != null && authUser.getRoleId() != null) {
                    authRoleId = authUser.getRoleId();
                    String roleName = resolveRoleNameById(authRoleId);
                    if (StringUtils.isNotBlank(roleName)) {
                        return roleName;
                    }
                }
            } catch (Exception e) {
                log.warn("查询当前用户 auth 角色失败, localUserId={}, authUserId={}",
                        currentUser.getId(), currentUser.getAuthUserId(), e);
            }
        }
        String knownAuthRoleName = resolveKnownSessionRoleName(authRoleId);
        if (StringUtils.isNotBlank(knownAuthRoleName)) {
            return knownAuthRoleName;
        }
        switch (type) {
            case STAFF:
                Long detailRoleId = userDetail == null ? null : userDetail.getRoleId();
                String detailRoleName = resolveRoleNameById(detailRoleId);
                if (StringUtils.isNotBlank(detailRoleName)) {
                    return detailRoleName;
                }
                String knownDetailRoleName = resolveKnownSessionRoleName(detailRoleId);
                return StringUtils.isNotBlank(knownDetailRoleName) ? knownDetailRoleName : TENANT_ADMIN_ROLE_NAME;
            case STUDENT:
                return STUDENT_ROLE_NAME;
            case TEACHER:
                return TEACHER_ROLE_NAME;
            default:
                return "";
        }
    }

    private String resolveRoleNameById(Long roleId) {
        if (roleId == null) {
            return null;
        }
        try {
            RoleDTO roleDTO = authClient.queryRoleById(roleId);
            return roleDTO == null ? null : roleDTO.getName();
        } catch (Exception e) {
            log.warn("鏌ヨ瑙掕壊鍚嶇О澶辫触, roleId={}", roleId, e);
            return null;
        }
    }

    private String resolveKnownSessionRoleName(Long roleId) {
        if (roleId == null) {
            return null;
        }
        if (Objects.equals(roleId, 1L)) {
            return SUPER_ADMIN_ROLE_NAME;
        }
        if (Objects.equals(roleId, TENANT_ADMIN_ROLE_ID) || Objects.equals(roleId, TENANT_ADMIN_COMPAT_ROLE_ID)) {
            return TENANT_ADMIN_ROLE_NAME;
        }
        if (Objects.equals(roleId, CLUB_MANAGER_ROLE_ID)) {
            return CLUB_MANAGER_ROLE_NAME;
        }
        if (Objects.equals(roleId, REVIEW_TEACHER_ROLE_ID)) {
            return REVIEW_TEACHER_ROLE_NAME;
        }
        if (Objects.equals(roleId, STUDENT_ROLE_ID)) {
            return STUDENT_ROLE_NAME;
        }
        return null;
    }

    private User resolveCurrentLocalUser() {
        Long authUserId = UserContext.getUser();
        if (authUserId == null) {
            return null;
        }
        User localUser = lambdaQuery().eq(User::getAuthUserId, authUserId).one();
        if (localUser != null) {
            return localUser;
        }
        User legacyUser = getById(authUserId);
        if (legacyUser != null) {
            return legacyUser;
        }
        try {
            AdminUserProvisionDTO authUser = authClient.queryAdminUserById(authUserId);
            if (authUser != null) {
                Long localUserId = upsertManagedLocalUser(authUserId, authUser, null);
                return getById(localUserId);
            }
        } catch (Exception e) {
            log.warn("当前登录用户本地同步失败, authUserId={}", authUserId, e);
        }
        return null;
    }

    private List<User> resolveLinkedUsersForSharedProfile(User currentUser) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (currentUser.getAuthUserId() != null) {
            queryWrapper.eq(User::getAuthUserId, currentUser.getAuthUserId());
        } else if (StringUtils.isNotBlank(currentUser.getUsername())) {
            queryWrapper.eq(User::getUsername, currentUser.getUsername());
        } else {
            return List.of(currentUser);
        }
        List<User> linkedUsers = baseMapper.selectList(queryWrapper);
        return linkedUsers == null || linkedUsers.isEmpty() ? List.of(currentUser) : linkedUsers;
    }

    private void updateSharedProfileFields(User existingUser, UserFormDTO userDTO) {
        User user = new User();
        user.setId(existingUser.getId());
        user.setUsername(StringUtils.isNotBlank(userDTO.getUsername()) ? userDTO.getUsername() : existingUser.getUsername());
        user.setRealName(StringUtils.isNotBlank(userDTO.getName()) ? userDTO.getName() : existingUser.getRealName());
        user.setNickname(StringUtils.isNotBlank(userDTO.getName()) ? userDTO.getName() : existingUser.getNickname());
        user.setCellPhone(StringUtils.isNotBlank(userDTO.getCellPhone()) ? userDTO.getCellPhone() : existingUser.getCellPhone());
        user.setEmail(StringUtils.isNotBlank(userDTO.getEmail()) ? userDTO.getEmail() : existingUser.getEmail());
        user.setGender(userDTO.getGender() != null ? userDTO.getGender() : existingUser.getGender());
        user.setAvatar(StringUtils.isNotBlank(userDTO.getIcon())
                ? mediaPathHelper.normalizeForStorage(userDTO.getIcon())
                : existingUser.getAvatar());
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
    }

    private String resolveEmailLoginDisplayName(User user) {
        if (StringUtils.isNotBlank(user.getRealName())) {
            return user.getRealName();
        }
        if (StringUtils.isNotBlank(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
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

    private <T> T executeInTenantScope(Long tenantId, Supplier<T> supplier) {
        Long previousTenantId = TenantContext.getTenantId();
        Boolean previousSuperAdmin = TenantContext.isSuperAdmin();
        try {
            if (tenantId == null) {
                TenantContext.removeTenantId();
            } else {
                TenantContext.setTenantId(tenantId);
            }
            TenantContext.setSuperAdmin(false);
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

    private void upsertSharedUserDetail(User linkedUser, UserFormDTO userDTO) {
        UserDetail detail = findUserDetailByUserId(linkedUser.getId());
        log.debug("updateUserWithPassword detail lookup by userId={}, detailId={}", linkedUser.getId(), detail == null ? null : detail.getId());
        boolean creatingDetail = detail == null;
        if (creatingDetail) {
            detail = new UserDetail();
            detail.setCreateTime(LocalDateTime.now());
            detail.setUserId(linkedUser.getId());
        }
        detail.setTenantId(linkedUser.getTenantId() != null ? linkedUser.getTenantId() : resolveTenantId(linkedUser));
        detail.setIntroduction(userDTO.getIntro() != null ? userDTO.getIntro() : detail.getIntroduction());
        detail.setUpdateTime(LocalDateTime.now());
        detail.setRoleId(null);
        detail.setType(null);
        if (creatingDetail) {
            detailService.save(detail);
        } else {
            detailService.updateById(detail);
        }
    }

    private boolean canManageAllTenants() {
        return Boolean.TRUE.equals(TenantContext.isSuperAdmin());
    }

    private LambdaQueryWrapper<User> buildScopedUserQuery(Long tenantId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (canManageAllTenants() && tenantId != null) {
            queryWrapper.eq(User::getTenantId, tenantId);
            return queryWrapper;
        }
        if (canManageAllTenants()) {
            applyActiveTenantScope(queryWrapper);
            return queryWrapper;
        }
        Long scopedTenantId = TenantContext.getTenantId();
        if (scopedTenantId != null) {
            queryWrapper.eq(User::getTenantId, currentTenantId());
            return queryWrapper;
        }
        queryWrapper.eq(User::getTenantId, currentTenantId());
        return queryWrapper;
    }

    private int resolveUserTypeValue(User user) {
        if (user == null || user.getType() == null) {
            return 0;
        }
        return user.getType().getValue();
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private void applyActiveTenantScope(LambdaQueryWrapper<User> queryWrapper) {
        List<Long> activeTenantIds = resolveActiveTenantIds();
        if (activeTenantIds.isEmpty()) {
            queryWrapper.eq(User::getTenantId, -1L);
            return;
        }
        queryWrapper.in(User::getTenantId, activeTenantIds);
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
}
