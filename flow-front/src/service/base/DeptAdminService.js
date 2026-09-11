import { postDg } from '@/service/BaseAxios'

// 后端控制器
const controller = 'dept-admin'

/** 部门管理员列表（POST /dept-admin/list） */
export function getDeptAdminList() {
  return postDg(controller, 'list')
}

/** 可用部门选项（POST /dept-admin/dept-options；dept_admin 去重） */
export function getDeptOptions() {
  return postDg(controller, 'dept-options')
}

/** 当前登录用户是否为部门管理员（POST /dept-admin/current-check；超管亦为 true，可用作「交接审批」菜单的角色判据） */
export function getCurrentDeptAdminCheck() {
  return postDg(controller, 'current-check')
}

/** 新增部门管理员（POST /dept-admin/add） */
export function addDeptAdmin(data) {
  return postDg(controller, 'add', data)
}

/** 更新部门管理员（POST /dept-admin/update；按 adminYstId + deptId 联合主键） */
export function updateDeptAdmin(data) {
  return postDg(controller, 'update', data)
}

/** 删除部门管理员（POST /dept-admin/delete；body: { adminYstId, deptId }） */
export function deleteDeptAdmin(data) {
  return postDg(controller, 'delete', data)
}
