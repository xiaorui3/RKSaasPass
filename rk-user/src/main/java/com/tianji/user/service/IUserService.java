package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.user.EmailLoginCandidateDTO;
import com.tianji.api.dto.user.EmailLoginCandidateQueryDTO;
import com.tianji.api.dto.user.LoginFormDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.LoginUserDTO;
import com.tianji.user.domain.dto.UserFormDTO;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.vo.ScopedUserStatisticsVO;
import com.tianji.user.domain.vo.PeopleDomainReconcileResultVO;
import com.tianji.user.domain.vo.UserDetailVO;

/**
 * <p>
 * 学员用户表 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2022-06-28
 */
public interface IUserService extends IService<User> {
    LoginUserDTO queryUserDetail(LoginFormDTO loginDTO, boolean isStaff);

    void resetPassword(Long userId);

    UserDetailVO myInfo();

    void addUserByPhone(User user, String code);

    void updatePasswordByPhone(String cellPhone, String code, String password);

    Long saveUser(UserDTO userDTO);

    void updateUser(UserDTO userDTO);

    void updateUserWithPassword(UserFormDTO userDTO);

    Page<UserDTO> queryUserByPage(Integer pageNo, Integer size, String username, String mobile, Integer status);

    Page<UserDTO> queryUserByPage(Integer pageNo, Integer size, String username, String mobile, Integer status, Long tenantId);

    Page<UserDTO> queryUserByPage(Integer pageNo, Integer size, String username, String mobile, Integer status, Long tenantId, Long roleId);

    java.util.List<UserDTO> queryUsersByAuthIds(java.util.List<Long> authUserIds);

    ScopedUserStatisticsVO queryScopedUserStatistics(Long tenantId);

    UserDTO queryManagedUserById(Long localUserId);

    Long provisionManagedUser(AdminUserProvisionDTO dto);

    void updateManagedUser(Long localUserId, AdminUserProvisionDTO dto);

    void updateManagedUserStatus(Long localUserId, Integer status);

    void resetManagedUserPassword(Long localUserId, String password);

    void deleteManagedUser(Long localUserId);

    int importManagedUsers(java.util.List<AdminUserProvisionDTO> rows);

    PeopleDomainReconcileResultVO reconcilePeopleDomainForCurrentScope();

    java.util.List<EmailLoginCandidateDTO> queryEmailLoginCandidates(EmailLoginCandidateQueryDTO dto);
}
