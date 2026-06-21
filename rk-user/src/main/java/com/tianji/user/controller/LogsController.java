package com.tianji.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.R;
import com.tianji.user.domain.vo.LogininforVO;
import com.tianji.user.domain.vo.OperLogVO;
import com.tianji.user.service.ISysLogininforService;
import com.tianji.user.service.ISysOperLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 日志管理Controller
 *
 * @author RK-Web
 * @since 2026-03-29
 */
@Api(tags = "日志管理")
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class LogsController {

    private final ISysOperLogService sysOperLogService;
    private final ISysLogininforService sysLogininforService;

    /**
     * 分页查询操作日志
     */
    @ApiOperation("分页查询操作日志")
    @PostMapping("/operlog/list")
    @PreAuthorize("hasAuthority('system:operlog:list')")
    public R<Page<OperLogVO>> queryOperLogPage(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("模块标题") @RequestParam(required = false) String title,
            @ApiParam("操作人员") @RequestParam(required = false) String operName,
            @ApiParam("业务类型") @RequestParam(required = false) Integer businessType,
            @ApiParam("操作状态") @RequestParam(required = false) Integer status,
            @ApiParam("开始时间") @RequestParam(required = false) String startTime,
            @ApiParam("结束时间") @RequestParam(required = false) String endTime) {

        Page<OperLogVO> pageParam = new Page<>(page, size);
        Page<OperLogVO> result = sysOperLogService.queryOperLogPage(pageParam, title, operName,
                businessType, status, startTime, endTime);
        return R.ok(result);
    }

    /**
     * 删除操作日志
     */
    @ApiOperation("删除操作日志")
    @DeleteMapping("/operlog/{id}")
    @PreAuthorize("hasAuthority('system:operlog:remove')")
    public R<Void> deleteOperLog(@ApiParam("日志ID") @PathVariable Long id) {
        sysOperLogService.deleteOperLog(id);
        return R.ok();
    }

    /**
     * 分页查询登录日志
     */
    @ApiOperation("分页查询登录日志")
    @PostMapping("/logininfor/list")
    @PreAuthorize("hasAuthority('system:logininfor:list')")
    public R<Page<LogininforVO>> queryLogininforPage(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("登录账号") @RequestParam(required = false) String loginName,
            @ApiParam("登录状态") @RequestParam(required = false) String status,
            @ApiParam("开始时间") @RequestParam(required = false) String startTime,
            @ApiParam("结束时间") @RequestParam(required = false) String endTime) {

        Page<LogininforVO> pageParam = new Page<>(page, size);
        Page<LogininforVO> result = sysLogininforService.queryLogininforPage(pageParam, loginName,
                status, startTime, endTime);
        return R.ok(result);
    }

    /**
     * 删除登录日志
     */
    @ApiOperation("删除登录日志")
    @DeleteMapping("/logininfor/{id}")
    @PreAuthorize("hasAuthority('system:logininfor:remove')")
    public R<Void> deleteLogininfor(@ApiParam("日志ID") @PathVariable Long id) {
        sysLogininforService.deleteLogininfor(id);
        return R.ok();
    }
}
