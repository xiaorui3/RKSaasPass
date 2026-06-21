package com.tianji.auth.controller;


import cn.hutool.core.collection.CollectionUtil;
import com.tianji.auth.domain.dto.MenuDTO;
import com.tianji.auth.domain.po.Menu;
import com.tianji.auth.domain.vo.MenuOptionVO;
import com.tianji.auth.domain.vo.PermissionMatrixVO;
import com.tianji.auth.service.IMenuService;
import com.tianji.auth.service.IPermissionMatrixService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 权限表，包括菜单权限和访问路径权限 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2022-06-16
 */
@RestController
@RequestMapping("/menus")
@Api(tags = "菜单管理")
@RequiredArgsConstructor
public class MenuController {

    private final IMenuService menuService;
    private final IPermissionMatrixService permissionMatrixService;

    /**
     * 根据父菜单id查询子菜单
     * @param pid 父菜单id，如果给 0 就是查询1级菜单
     * @return 菜单集合
     */
    @GetMapping("/parent/{pid}")
    @ApiOperation("根据父菜单id查询子菜单")
    public List<MenuOptionVO> listMenusByParent(
            @ApiParam(value = "父菜单id", example = "0") @PathVariable("pid") Long pid){
        // 1.根据父id查询
        List<Menu> list = menuService.lambdaQuery().eq(Menu::getParentId, pid).list();
        // 2.非空判断
        if (CollectionUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 3.数据转换
        return list.stream().map(MenuOptionVO::new).collect(Collectors.toList());
    }

    @GetMapping("{id}")
    @ApiOperation("根据id查询菜单")
    public MenuOptionVO getMenuById(@ApiParam(value = "菜单id", example = "1") @PathVariable("id") Long id) {
        Menu menu = menuService.getById(id);
        if (menu == null) {
            return null;
        }
        return new MenuOptionVO(menu);
    }

    /**
     * 查询菜单，按照多级菜单组成树结构
     * @return 菜单列表，组成树结构
     */
    @GetMapping
    @ApiOperation("查询菜单，按照多级菜单组成树结构")
    public List<MenuOptionVO> listMenuTree(){
        // 1.查询所有菜单
        List<Menu> menus = menuService.list();
        return convert2MenuDTOs(menus);
    }

    private List<MenuOptionVO> convert2MenuDTOs(List<Menu> menus) {
        if (CollectionUtil.isEmpty(menus)) {
            return Collections.emptyList();
        }
        // 2.按照父菜单id分组
        Map<Long, List<MenuOptionVO>> menuMap = menus.stream()
                .map(MenuOptionVO::new)
                .collect(Collectors.groupingBy(MenuOptionVO::getParentId));
        // 3.组合
        // 3.1.获取1级菜单
        List<MenuOptionVO> parents = menuMap.get(0L);
        if (CollectionUtil.isEmpty(parents)) {
            return Collections.emptyList();
        }
        // 3.2.获取2级菜单
        for (MenuOptionVO parent : parents) {
            List<MenuOptionVO> subMenus = menuMap.get(parent.getId());
            if (CollectionUtil.isEmpty(subMenus)) {
                parent.setSubMenus(Collections.emptyList());
                continue;
            }
            subMenus.sort(Comparator.comparingInt(item -> item.getPriority() == null ? 0 : item.getPriority()));
            parent.setSubMenus(subMenus);
        }
        // 3.3.排序
        parents.sort(Comparator.comparingInt(item -> item.getPriority() == null ? 0 : item.getPriority()));
        return parents;
    }

    /**
     * 根据当前登录用户的权限查询菜单选项，按照多级菜单组成树结构
     * @return 菜单列表，组成树结构
     */
    @GetMapping("me")
    @ApiOperation("查询我的菜单，按照多级菜单组成树结构")
    public List<MenuOptionVO> listMenuTreeByUser(){
        // 1.查询所有菜单
        List<Menu> menus = menuService.listMenuByUser();
        return convert2MenuDTOs(menus);
    }

    @PostMapping
    @ApiOperation("新增菜单")
    public void saveMenu(@RequestBody MenuDTO menuDTO){
        // 1.数据转换
        Menu menu = new Menu(menuDTO);
        // 2.保存
        menuService.saveMenu(menu);
    }

    @PutMapping("{id}")
    @ApiOperation("更新菜单")
    public void updateMenu(
            @RequestBody MenuDTO menuDTO,
            @ApiParam(value = "菜单id", example = "1")@PathVariable("id") Long id) {
        menuDTO.setId(id);
        menuService.updateMenu(new Menu(menuDTO));
    }

    @DeleteMapping("{id}")
    @ApiOperation("根据id删除菜单")
    public void deleteMenu(
            @ApiParam(value = "菜单id", example = "1") @PathVariable("id") Long id) {
        menuService.deleteMenu(id);
    }

    @GetMapping("/role/{roleId}")
    @ApiOperation("查询角色已绑定菜单")
    public List<Long> queryRoleMenus(
            @ApiParam(value = "角色id", example = "1") @PathVariable("roleId") Long roleId) {
        return menuService.listRoleMenuIds(roleId);
    }

    @PostMapping("/role/{roleId}")
    @ApiOperation("绑定角色与菜单权限")
    public void bindRoleMenus(
            @ApiParam(value = "角色id", example = "1") @PathVariable("roleId") Long roleId,
            @ApiParam(value = "菜单id集合") @RequestBody List<Long> menuIds){
        menuService.replaceRoleMenus(roleId, menuIds);
    }

    @DeleteMapping("/role/{roleId}")
    @ApiOperation("解除角色的菜单权限")
    public void deleteRoleMenus(
            @ApiParam(value = "角色id", example = "1") @PathVariable("roleId") Long roleId,
            @ApiParam(value = "菜单id集合") @RequestBody List<Long> menuIds){
        menuService.deleteRoleMenus(roleId, menuIds);
    }

    @GetMapping("/permission-matrix")
    @ApiOperation("查询角色和菜单权限矩阵")
    public PermissionMatrixVO buildPermissionMatrix(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return permissionMatrixService.buildMatrix(tenantId);
    }
}
