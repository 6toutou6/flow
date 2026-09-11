import { getDg, postDg } from '@/service/BaseAxios'

// ==================== 登录 ====================

/**
 * @desc: 登录（POST /autuser/login；按用户号/姓名 + 密码，成功写入会话）
 *        返回 {yyytId, userName, deptId, deptName, superAdmin}
 */
export function login(data) {
  return postDg('autuser', 'login', data)
}

// 退出：前端清除本地 userInfo 即可（登录态由后端会话管理，退出由内网框架处理）
export function logout() {
  return Promise.resolve({ code: 200, message: '退出成功', data: null })
}

// ==================== 用户选择（登录页下拉 / 选人联想） ====================

/** 全部正常用户（POST /autuser/list） */
export function listActiveUsers() {
  return postDg('autuser', 'list')
}

/** 模糊搜索用户（POST /autuser/search；按用户号/中文姓名，type 支持 9=综合室 / 0=普通用户） */
export function searchUsers(data) {
  return postDg('autuser', 'search', data)
}

// ==================== 用户管理（/sys-user） ====================

/** 用户列表（POST /sys-user/list；param: 分页与筛选） */
export function getUserList(params) {
  return postDg('sys-user', 'list', params)
}

/** 用户管理页统计卡（POST /sys-user/stats） */
export function getSysUserStats() {
  return postDg('sys-user', 'stats')
}

/** 新增用户（POST /sys-user/add） */
export function addUser(data) {
  return postDg('sys-user', 'add', data)
}

/** 修改用户（POST /sys-user/update） */
export function updateUser(data) {
  return postDg('sys-user', 'update', data)
}

/** 删除用户（POST /sys-user/delete/{yyytId}） */
export function deleteUser(id) {
  return postDg('sys-user', `delete/${id}`)
}

/** 用户详情（GET /sys-user/detail/{yyytId}） */
export function getUserDetail(id) {
  return getDg('sys-user', 'detail', id)
}
