import request from '@/service/BaseAxios'

/** 部门管理员列表 */
export function getDeptAdminList() {
  return request({
    url: '/dept-admin/list',
    method: 'get'
  })
}

/** 可用部门选项（dept_admin 去重） */
export function getDeptOptions() {
  return request({
    url: '/dept-admin/dept-options',
    method: 'get'
  })
}

/** 当前登录用户是否为部门管理员（超管亦为 true；可用作「交接审批」菜单的角色判据） */
export function getCurrentDeptAdminCheck() {
  return request({
    url: '/dept-admin/current-check',
    method: 'get'
  })
}

/** 新增部门管理员 */
export function addDeptAdmin(data) {
  return request({
    url: '/dept-admin/add',
    method: 'post',
    data
  })
}

/** 更新部门管理员（按 adminYstId + deptId 联合主键） */
export function updateDeptAdmin(data) {
  return request({
    url: '/dept-admin/update',
    method: 'post',
    data
  })
}

/** 删除部门管理员（body: { adminYstId, deptId }） */
export function deleteDeptAdmin(data) {
  return request({
    url: '/dept-admin/delete',
    method: 'post',
    data
  })
}
