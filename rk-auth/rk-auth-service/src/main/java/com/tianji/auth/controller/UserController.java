package com.tianji.auth.controller;

import com.tianji.auth.domain.dto.UserUpdateDTO;
import com.tianji.auth.domain.vo.UserInfoVO;
import com.tianji.auth.service.IUserService;
import com.tianji.common.domain.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * RK-Auth 用户控制器
 * 重构版本：用户信息管理
 */
@Api(tags = "用户管理接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * 获取当前用户信息
     */
    @ApiOperation("获取当前用户信息")
    @GetMapping("/me")
    public R<UserInfoVO> getCurrentUser(@RequestHeader("Authorization") String token) {
        UserInfoVO userInfoVO = userService.getCurrentUser(token);
        return R.ok(userInfoVO);
    }

    /**
     * 更新当前用户信息
     */
    @ApiOperation("更新当前用户信息")
    @PutMapping("/me")
    public R<Void> updateCurrentUser(
            @RequestHeader("Authorization") String token,
            @Validated @RequestBody UserUpdateDTO dto) {
        userService.updateCurrentUser(token, dto);
        return R.ok();
    }

    /**
     * 获取指定用户信息
     */
    @ApiOperation("获取指定用户信息")
    @GetMapping("/{userId}")
    public R<UserInfoVO> getUserById(@PathVariable Long userId) {
        UserInfoVO userInfoVO = userService.getUserById(userId);
        return R.ok(userInfoVO);
    }
}