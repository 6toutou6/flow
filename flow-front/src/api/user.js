import { createMockResponse } from '@/utils/request'

const mockUsers = {
  admin: {
    token: 'admin-token',
    name: '管理员',
    avatar: 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif'
  },
  editor: {
    token: 'editor-token',
    name: '编辑',
    avatar: 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif'
  }
}

export function login(data) {
  const { username } = data
  if (mockUsers[username]) {
    return createMockResponse({
      token: mockUsers[username].token
    }, '登录成功')
  }
  return Promise.reject({
    code: 50000,
    message: '账号或密码错误'
  })
}

export function getInfo(token) {
  const user = Object.values(mockUsers).find(u => u.token === token)
  if (user) {
    return createMockResponse({
      name: user.name,
      avatar: user.avatar,
      roles: token === 'admin-token' ? ['admin'] : ['editor']
    })
  }
  return Promise.reject({
    code: 50008,
    message: 'Token无效'
  })
}

export function logout() {
  return createMockResponse(null, '退出成功')
}
