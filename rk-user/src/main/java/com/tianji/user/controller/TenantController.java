package com.tianji.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.R;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.query.TenantPageQuery;
import com.tianji.user.domain.vo.TenantVO;
import com.tianji.user.service.IRKTenantService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "租户接口")
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final IRKTenantService tenantService;

    @ApiOperation("获取租户列表")
    @GetMapping("/list")
    public R<List<RKTenant>> getTenantList() {
        return R.ok(tenantService.getActiveTenants());
    }

    @ApiOperation("分页查询租户")
    @GetMapping("/page")
    public R<Page<TenantVO>> getTenantPage(TenantPageQuery query) {
        return R.ok(tenantService.pageTenants(query));
    }

    @ApiOperation("新增租户")
    @PostMapping
    public R<Void> addTenant(@RequestBody RKTenant tenant) {
        tenantService.save(tenant);
        return R.ok();
    }

    @ApiOperation("更新租户")
    @PutMapping
    public R<Void> updateTenant(@RequestBody RKTenant tenant) {
        tenantService.updateById(tenant);
        return R.ok();
    }

    @ApiOperation("删除租户")
    @DeleteMapping("/{id}")
    public R<Void> deleteTenant(@PathVariable Long id) {
        tenantService.removeById(id);
        return R.ok();
    }

    @ApiOperation("获取租户详情")
    @GetMapping("/{id}")
    public R<RKTenant> getTenantDetail(@PathVariable Long id) {
        return R.ok(tenantService.getById(id));
    }
}
