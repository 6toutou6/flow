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
