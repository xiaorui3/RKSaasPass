package com.tianji.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.R;
import com.tianji.user.domain.dto.TenantDTO;
import com.tianji.user.domain.query.TenantPageQuery;
import com.tianji.user.domain.vo.TenantVO;
import com.tianji.user.service.IRKTenantService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 租户管理接口控制器（管理员）
 * 提供租户CRUD等管理接口
 */
@Slf4j
@Api(tags = "租户管理接口")
@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class AdminTenantController {

    private final IRKTenantService tenantService;

    /**
     * 分页获取租户列表
     */
    @ApiOperation("分页获取租户列表")
    @GetMapping({"", "/page"})
    public R<Page<TenantVO>> getTenantList(TenantPageQuery query) {
        try {
            Page<TenantVO> page = tenantService.pageTenants(query);
            return R.ok(page);
        } catch (Exception e) {
            log.error("获取租户列表失败", e);
            return R.error("获取租户列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取租户详情
     */
    @ApiOperation("获取租户详情")
    @GetMapping("/{tenantId}")
    public R<TenantVO> getTenantDetail(@PathVariable Long tenantId) {
        try {
            TenantVO tenant = tenantService.getTenantDetail(tenantId);
            if (tenant == null) {
                return R.error("租户不存在");
            }
            return R.ok(tenant);
        } catch (Exception e) {
            log.error("获取租户详情失败", e);
            return R.error("获取租户详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建租户
     */
    @ApiOperation("创建租户")
    @PostMapping
    public R<Long> createTenant(@Validated @RequestBody TenantDTO dto) {
        try {
            Long tenantId = tenantService.createTenant(dto);
            log.info("✅ 创建租户成功: {}", dto.getTenantName());
            return R.ok(tenantId);
        } catch (Exception e) {
            log.error("创建租户失败", e);
            return R.error("创建租户失败：" + e.getMessage());
        }
    }

    /**
     * 更新租户
     */
    @ApiOperation("更新租户")
    @PutMapping("/{tenantId}")
    public R<String> updateTenant(@PathVariable Long tenantId, @Validated @RequestBody TenantDTO dto) {
        try {
            boolean result = tenantService.updateTenant(tenantId, dto);
            if (result) {
                log.info("✅ 更新租户成功: {}", dto.getTenantName());
                return R.ok("更新成功");
            }
            return R.error("更新失败");
        } catch (Exception e) {
            log.error("更新租户失败", e);
            return R.error("更新租户失败：" + e.getMessage());
        }
    }

    /**
     * 更新租户状态
     */
    @ApiOperation("更新租户状态")
    @PutMapping("/{tenantId}/status")
    public R<String> updateTenantStatus(@PathVariable Long tenantId, @RequestBody Map<String, Integer> body) {
        try {
            Integer status = body.get("status");
            if (status == null) {
                return R.error("状态不能为空");
            }
            boolean result = tenantService.updateTenantStatus(tenantId, status);
            if (result) {
                log.info("✅ 更新租户状态成功: {} -> {}", tenantId, status);
                return R.ok("状态更新成功");
            }
            return R.error("状态更新失败");
        } catch (Exception e) {
            log.error("更新租户状态失败", e);
            return R.error("更新租户状态失败：" + e.getMessage());
        }
    }

    /**
     * 删除租户
     */
    @ApiOperation("删除租户")
    @DeleteMapping("/{tenantId}")
    public R<String> deleteTenant(@PathVariable Long tenantId) {
        try {
            boolean result = tenantService.deleteTenant(tenantId);
            if (result) {
                log.info("✅ 删除租户成功: {}", tenantId);
                return R.ok("删除成功");
            }
            return R.error("删除失败");
        } catch (Exception e) {
            log.error("删除租户失败", e);
            return R.error("删除租户失败：" + e.getMessage());
        }
    }

    /**
     * 获取租户初始化状态
     */
    @ApiOperation("获取租户初始化状态")
    @GetMapping("/{tenantId}/init-status")
    public R<Map<String, Object>> getTenantInitStatus(@PathVariable Long tenantId) {
        try {
            Map<String, Object> status = tenantService.getTenantInitStatus(tenantId);
            return R.ok(status);
        } catch (Exception e) {
            log.error("获取租户初始化状态失败", e);
            return R.error("获取初始化状态失败：" + e.getMessage());
        }
    }

    /**
     * 重新初始化租户
     */
    @ApiOperation("重新初始化租户")
    @PostMapping("/{tenantId}/reinit")
    public R<String> reinitTenant(@PathVariable Long tenantId) {
        try {
            boolean result = tenantService.reinitTenant(tenantId);
            if (result) {
                log.info("✅ 重新初始化租户成功: {}", tenantId);
                return R.ok("重新初始化成功");
            }
            return R.error("重新初始化失败");
        } catch (Exception e) {
            log.error("重新初始化租户失败", e);
            return R.error("重新初始化失败：" + e.getMessage());
        }
    }

    /**
     * 修复租户数据
     */
    @ApiOperation("修复租户数据")
    @PostMapping("/{tenantId}/repair")
    public R<String> repairTenant(@PathVariable Long tenantId) {
        try {
            boolean result = tenantService.repairTenant(tenantId);
            if (result) {
                log.info("✅ 修复租户数据成功: {}", tenantId);
                return R.ok("修复成功");
            }
            return R.error("修复失败");
        } catch (Exception e) {
            log.error("修复租户数据失败", e);
            return R.error("修复失败：" + e.getMessage());
        }
    }
}
