package com.tianji.auth.service;

import com.tianji.auth.domain.dto.UserUpdateDTO;
import com.tianji.auth.domain.vo.UserInfoVO;

/**
 * 用户服务接口
 */
public interface IUserService {

    /**
     * 获取当前用户信息
     */
    UserInfoVO getCurrentUser(String token);

    /**
     * 更新当前用户信息
     */
    void updateCurrentUser(String token, UserUpdateDTO dto);

    /**
     * 获取指定用户信息
     */
    UserInfoVO getUserById(Long userId);
}