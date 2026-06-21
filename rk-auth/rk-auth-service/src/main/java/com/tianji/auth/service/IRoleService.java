package com.tianji.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.auth.domain.po.Role;

import java.util.List;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2022-06-16
 */
public interface IRoleService extends IService<Role> {

    boolean exists(Long roleId);
    boolean exists(List<Long> roleIds);

    List<Role> listAssignableRoles(Long tenantId);

    Role queryAccessibleRoleById(Long id);

    Role queryRoleByIdAcrossTenants(Long id);

    Role requireAccessibleRole(Long roleId);

    RoleDTO createTenantRole(RoleDTO roleDTO);

    void updateTenantRole(Long id, RoleDTO roleDTO);

    void deleteRole(Long id);
}
