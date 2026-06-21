package com.tianji.auth.controller;


import cn.hutool.core.collection.CollectionUtil;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.auth.domain.po.Role;
import com.tianji.auth.service.IRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author 虎哥
 * @since 2022-06-16
 */
@Api(tags = "角色管理")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService roleService;

    @ApiOperation("查询员工角色列表")
    @GetMapping("/list")
    public List<RoleDTO> listAllRoles(@RequestParam(value = "tenantId", required = false) Long tenantId){
        List<Role> list = roleService.listAssignableRoles(tenantId);
        if (CollectionUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(Role::toDTO).collect(Collectors.toList());
    }

    @ApiOperation("查询员工角色列表")
    @GetMapping
    public List<RoleDTO> listStaffRoles(){
        List<Role> list = roleService.listAssignableRoles(null);
        if (CollectionUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(Role::toDTO).collect(Collectors.toList());
    }

    @ApiOperation("根据id查询角色")
    @GetMapping("/{id}")
    public RoleDTO queryRoleById(@PathVariable("id") Long id){
        // 1.查询 - 只允许访问当前租户作用域内的角色
        Role role = roleService.queryAccessibleRoleById(id);
        if (role == null) {
            return null;
        }
        // 2.数据转换
        return role.toDTO();
    }



    @ApiOperation("新增角色")
    @PostMapping
    public RoleDTO saveRole(@RequestBody RoleDTO roleDTO) {
        return roleService.createTenantRole(roleDTO);
    }

    @ApiOperation("修改角色信息")
    @PutMapping("{id}")
    public void updateRole(
            @RequestBody RoleDTO roleDTO,
            @ApiParam(value = "角色id", example = "1") @PathVariable("id") Long id
    ) {
        roleService.updateTenantRole(id, roleDTO);
    }

    @ApiOperation("删除角色信息")
    @DeleteMapping("{id}")
    public void deleteRole(@ApiParam(value = "角色id", example = "1") @PathVariable("id") Long id) {
        roleService.deleteRole(id);
    }
}
