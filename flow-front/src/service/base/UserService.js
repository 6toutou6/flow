import request from '@/service/BaseAxios'

// ==================== 登录 ====================

// 登录：POST /autuser/login 按用户号/姓名 + 密码，成功写入会话（后端），返回 {yyytId, userName, deptId, deptName, superAdmin}
export function login(data) {
  return request({
    url: '/autuser/login',
    method: 'post',
    data
  })
}

// 退出：前端清除本地 userInfo 即可（登录态由后端会话管理，退出由内网框架处理）
export function logout() {
  return Promise.resolve({ code: 200, message: '退出成功', data: null })
}

// ==================== 用户选择（登录页下拉 / 选人联想） ====================

// 全部正常用户（登录页下拉选择）
export function listActiveUsers() {
  return request({
    url: '/autuser/list',
    method: 'post'
  })
}

// 模糊搜索用户（按用户号/中文姓名；type 支持 9=综合室 / 0=普通用户）
export function searchUsers(data) {
  return request({
    url: '/autuser/search',
    method: 'post',
    data
  })
}

// ==================== 用户管理（/sys-user） ====================

export function getUserList(params) {
  return request({
    url: '/sys-user/list',
    method: 'post',
    data: params
  })
}

// 用户管理页统计卡
export function getSysUserStats() {
  return request({
    url: '/sys-user/stats',
    method: 'get'
  })
}

export function addUser(data) {
  return request({
    url: '/sys-user/add',
    method: 'post',
    data
  })
}

export function updateUser(data) {
  return request({
    url: '/sys-user/update',
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/sys-user/delete/${id}`,
    method: 'delete'
  })
}

export function getUserDetail(id) {
  return request({
    url: `/sys-user/${id}`,
    method: 'get'
  })
}
