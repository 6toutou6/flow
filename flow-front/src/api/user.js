import request from '@/utils/request-flow'

// 登录：后端 /user/login 校验 sys_user 表，返回 {token, userInfo}
export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

// 获取用户信息：后端 /user/info，token 由 request-flow 自动带 Authorization 头
export function getInfo(token) {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

// 退出：前端清 token 即可（后端无状态 JWT，无需调接口）
export function logout() {
  return Promise.resolve({ code: 200, message: '退出成功', data: null })
}
