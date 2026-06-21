/**
 * 角色与菜单管理 API
 */
import request from '@/utils/request'

// ==================== 角色管理接口 ====================

export function getRoleList(tenantId) {
  return request({
    url: '/roles/list',
    method: 'get',
    params: tenantId ? { tenantId } : {}
  })
}

export function getStaffRoles() {
  return request({
    url: '/roles',
    method: 'get'
  })
}

export function getRoleById(id) {
  return request({
    url: `/roles/${id}`,
    method: 'get'
  })
}

export function createRole(data) {
  return request({
    url: '/roles',
    method: 'post',
    data
  })
}

export function updateRole(id, data) {
  return request({
    url: `/roles/${id}`,
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/roles/${id}`,
    method: 'delete'
  })
}

// ==================== 菜单管理接口 ====================

export function getMenuTree() {
  return request({
    url: '/menus',
    method: 'get'
  })
}

export function getMyMenuTree() {
  return request({
    url: '/menus/me',
    method: 'get'
  })
}

export function getMenuById(id) {
  return request({
    url: `/menus/${id}`,
    method: 'get'
  })
}

export function createMenu(data) {
  return request({
    url: '/menus',
    method: 'post',
    data
  })
}

export function updateMenu(id, data) {
  return request({
    url: `/menus/${id}`,
    method: 'put',
    data
  })
}

export function deleteMenu(id) {
  return request({
    url: `/menus/${id}`,
    method: 'delete'
  })
}

export function getRoleMenuIds(roleId) {
  return request({
    url: `/menus/role/${roleId}`,
    method: 'get'
  })
}

export function assignRoleMenus(roleId, menuIds) {
  return request({
    url: `/menus/role/${roleId}`,
    method: 'post',
    data: menuIds
  })
}

export function getPermissionMatrix(tenantId) {
  return request({
    url: '/menus/permission-matrix',
    method: 'get',
    params: tenantId ? { tenantId } : {}
  })
}
