package com.tianji.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.user.EmailLoginCandidateDTO;
import com.tianji.api.dto.user.EmailLoginCandidateQueryDTO;
import com.tianji.api.dto.user.LoginFormDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.R;
import com.tianji.common.domain.dto.LoginUserDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.user.constants.UserErrorInfo;
import com.tianji.user.domain.dto.UserFormDTO;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.po.UserDetail;
import com.tianji.user.domain.vo.ScopedUserStatisticsVO;
import com.tianji.user.domain.vo.PeopleDomainReconcileResultVO;
import com.tianji.user.domain.vo.UserDetailVO;
import com.tianji.user.service.IUserDetailService;
import com.tianji.user.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

import static com.tianji.user.constants.UserConstants.DEFAULT_PASSWORD;
import static com.tianji.user.constants.UserConstants.STUDENT_ROLE_NAME;
import static com.tianji.user.constants.UserConstants.TEACHER_ROLE_NAME;

@Slf4j
@RestController
@RequestMapping("users")
@Api(tags = "用户管理接口")
public class UserController {

    @Autowired
    private IUserService userService;
    @Autowired
    private IUserDetailService detailService;

    @ApiOperation("新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Long saveUser(@Valid @RequestBody AdminUserProvisionDTO userDTO) {
        userDTO.setAuthUserId(null);
        return userService.provisionManagedUser(userDTO);
    }

    @ApiOperation("更新用户信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public void updateUser(@PathVariable("id") Long id, @RequestBody AdminUserProvisionDTO userDTO) {
        userService.updateManagedUser(id, userDTO);
    }

    @ApiOperation("更新当前登录用户信息")
    @PutMapping
    public void updateCurrentUser(@Valid @RequestBody UserFormDTO userDTO) {
        userService.updateUserWithPassword(userDTO);
    }

    @ApiOperation("重置密码")
    @PutMapping("/{id}/password/default")
    @PreAuthorize("hasAuthority('system:user:reset_pwd')")
    public void resetPassword(
            @ApiParam(value = "用户ID", example = "1") @PathVariable("id") Long userId) {
        userService.resetManagedUserPassword(userId, DEFAULT_PASSWORD);
    }

    @ApiOperation("修改用户状态")
    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public void updateUserStatus(
            @ApiParam(value = "用户ID", example = "1") @PathVariable("id") Long userId,
            @ApiParam(value = "状态", example = "1") @PathVariable("status") Integer status) {
        userService.updateManagedUserStatus(userId, status);
    }

    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/me")
    public UserDetailVO me() {
        UserDetailVO detail = userService.myInfo();
        if (detail != null) {
            return detail;
        }
        Long currentUserId = UserContext.getUser();
        if (currentUserId == null) {
            return null;
        }
        User user = userService.lambdaQuery().eq(User::getAuthUserId, currentUserId).one();
        if (user == null) {
            user = userService.getById(currentUserId);
        }
        if (user == null) {
            return null;
        }
        UserDetailVO fallback = new UserDetailVO();
        fallback.setId(user.getId());
        fallback.setName(user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : (user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername()));
        fallback.setIcon(user.getAvatar());
        fallback.setCellPhone(user.getCellPhone());
        fallback.setUsername(user.getUsername());
        fallback.setEmail(user.getEmail());
        fallback.setGender(user.getGender());
        fallback.setCreateTime(user.getCreateTime());
        if (user.getType() != null) {
            switch (user.getType()) {
                case STUDENT:
                    fallback.setRoleName(STUDENT_ROLE_NAME);
                    break;
                case TEACHER:
                    fallback.setRoleName(TEACHER_ROLE_NAME);
                    break;
                default:
                    fallback.setRoleName("管理员");
                    break;
            }
        }
        return fallback;
    }

    @GetMapping("/info")
    public UserDetailVO info() {
        return me();
    }

    @ApiOperation("根据ID查询用户信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:query')")
    public UserDTO queryUserById(@ApiParam("用户ID") @PathVariable("id") Long id) {
        return userService.queryManagedUserById(id);
    }

    @ApiIgnore
    @PostMapping("/detail/{isStaff}")
    public LoginUserDTO queryUserDetail(
            @Valid @RequestBody LoginFormDTO loginDTO, @PathVariable("isStaff") boolean isStaff) {
        return userService.queryUserDetail(loginDTO, isStaff);
    }

    @ApiIgnore
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:user:list')")
    public List<UserDTO> queryUserByIds(@ApiParam("用户ID列表") @RequestParam("ids") List<Long> ids) {
        if (CollUtils.isEmpty(ids)) {
            return CollUtils.emptyList();
        }
        List<UserDetail> list = detailService.queryByIds(ids);
        return BeanUtils.copyList(list, UserDTO.class, (detail, dto) -> {
            if (detail.getType() != null) {
                try {
                    UserType userType = UserType.valueOf(detail.getType());
                    dto.setType(userType.getValue());
                } catch (IllegalArgumentException e) {
                    dto.setType(null);
                }
            }
        });
    }

    @ApiIgnore
    @GetMapping("/internal/by-auth-ids")
    public List<UserDTO> queryUsersByAuthIds(@RequestParam("authUserIds") List<Long> authUserIds) {
        return userService.queryUsersByAuthIds(authUserIds);
    }

    @ApiIgnore
    @GetMapping("/{id}/type")
    public Integer queryUserType(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BadRequestException(UserErrorInfo.Msg.USER_ID_NOT_EXISTS);
        }
        return user.getType().getValue();
    }

    @ApiIgnore
    @GetMapping("/ids")
    public Long exchangeUserIdWithPhone(@RequestParam("phone") String phone) {
        User user = userService.lambdaQuery().eq(User::getCellPhone, phone).one();
        if (user == null) {
            throw new BadRequestException(UserErrorInfo.Msg.USER_ID_NOT_EXISTS);
        }
        return user.getId();
    }

    @ApiOperation("检查手机号是否存在")
    @GetMapping("checkCellphone")
    public Boolean checkCellPhone(@RequestParam("cellphone") String cellPhone) {
        return userService.lambdaQuery().eq(User::getCellPhone, cellPhone).count() <= 0;
    }

    @ApiIgnore
    @PostMapping("/internal/sync")
    public Long syncUserOnRegister(@RequestBody UserDTO userDTO) {
        log.info("sync registered user to rk_user, username={}", userDTO.getUsername());
        return userService.saveUser(userDTO);
    }

    @ApiIgnore
    @PostMapping("/internal/email-login-candidates")
    public List<EmailLoginCandidateDTO> queryEmailLoginCandidates(@RequestBody EmailLoginCandidateQueryDTO dto) {
        return userService.queryEmailLoginCandidates(dto);
    }

    @ApiOperation("分页查询用户列表")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<Page<UserDTO>> queryUserByPage(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNo,
            @ApiParam("页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("用户名") @RequestParam(required = false) String username,
            @ApiParam("手机号") @RequestParam(required = false) String mobile,
            @ApiParam("状态") @RequestParam(required = false) Integer status) {
        Page<UserDTO> page = userService.queryUserByPage(pageNo, size, username, mobile, status);
        return R.ok(page);
    }

    @ApiOperation("按租户分页查询用户列表")
    @GetMapping("/page/scoped")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<Page<UserDTO>> queryUserByPageScoped(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNo,
            @ApiParam("页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("用户名") @RequestParam(required = false) String username,
            @ApiParam("手机号") @RequestParam(required = false) String mobile,
            @ApiParam("状态") @RequestParam(required = false) Integer status,
            @ApiParam("租户ID") @RequestParam(required = false) Long tenantId,
            @ApiParam("角色ID") @RequestParam(required = false) Long roleId) {
        return R.ok(userService.queryUserByPage(pageNo, size, username, mobile, status, tenantId, roleId));
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:remove')")
    public R<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteManagedUser(id);
        return R.ok();
    }

    @ApiOperation("批量导入用户")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('system:user:add')")
    public R<Integer> importUsers(@RequestBody List<AdminUserProvisionDTO> rows) {
        return R.ok(userService.importManagedUsers(rows));
    }

    @ApiOperation("查询当前作用域账号统计")
    @GetMapping("/statistics/scoped")
    @PreAuthorize("hasAnyAuthority('system:user:list', 'content:statistics:view')")
    public R<ScopedUserStatisticsVO> queryScopedUserStatistics(
            @ApiParam("租户ID") @RequestParam(required = false) Long tenantId) {
        return R.ok(userService.queryScopedUserStatistics(tenantId));
    }
    @ApiOperation("对账并修复当前作用域的人域数据")
    @PostMapping("/reconcile/people-domain")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<PeopleDomainReconcileResultVO> reconcilePeopleDomain() {
        return R.ok(userService.reconcilePeopleDomainForCurrentScope());
    }
}
