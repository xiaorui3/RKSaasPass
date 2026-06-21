package com.tianji.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.auth.domain.dto.UserUpdateDTO;
import com.tianji.auth.domain.po.User;
import com.tianji.auth.domain.po.AccountRole;
import com.tianji.auth.domain.po.Role;
import com.tianji.auth.domain.vo.UserInfoVO;
import com.tianji.auth.mapper.UserMapper;
import com.tianji.auth.mapper.AccountRoleMapper;
import com.tianji.auth.mapper.RoleMapper;
import com.tianji.auth.service.IUserService;
import com.tianji.auth.util.JwtTool;
import com.tianji.common.domain.dto.LoginUserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 基于master分支的核心业务逻辑实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;
    private final AccountRoleMapper accountRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTool jwtTool;

    @Override
    public UserInfoVO getCurrentUser(String token) {
        log.info("获取当前用户信息");

        // 1.从token中解析用户信息
        LoginUserDTO loginUserDTO = jwtTool.parseToken(token);
        Long userId = loginUserDTO.getUserId();

        // 2.查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3.查询用户角色
        LambdaQueryWrapper<AccountRole> roleQueryWrapper = new LambdaQueryWrapper<>();
        roleQueryWrapper.eq(AccountRole::getAccountId, userId);
        List<AccountRole> accountRoles = accountRoleMapper.selectList(roleQueryWrapper);

        // 4.构建返回对象
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setOrganizationId(user.getTenantId().toString());
        userInfoVO.setStatus(user.getStatus());
        userInfoVO.setRoles(accountRoles);

        log.info("获取用户信息成功：userId={}", userId);

        return userInfoVO;
    }

    @Override
    @Transactional
    public void updateCurrentUser(String token, UserUpdateDTO dto) {
        log.info("更新当前用户信息");

        // 1.从token中解析用户信息
        LoginUserDTO loginUserDTO = jwtTool.parseToken(token);
        Long userId = loginUserDTO.getUserId();

        // 2.查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3.更新用户信息
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(user);

        log.info("更新用户信息成功：userId={}", userId);
    }

    @Override
    public UserInfoVO getUserById(Long userId) {
        log.info("获取指定用户信息：userId={}", userId);

        // 1.查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2.查询用户角色
        LambdaQueryWrapper<AccountRole> roleQueryWrapper = new LambdaQueryWrapper<>();
        roleQueryWrapper.eq(AccountRole::getAccountId, userId);
        List<AccountRole> accountRoles = accountRoleMapper.selectList(roleQueryWrapper);

        // 3.构建返回对象
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setOrganizationId(user.getTenantId().toString());
        userInfoVO.setStatus(user.getStatus());
        userInfoVO.setRoles(accountRoles);

        log.info("获取用户信息成功：userId={}", userId);

        return userInfoVO;
    }
}