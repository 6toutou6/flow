import request from '@/utils/request-flow'

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
