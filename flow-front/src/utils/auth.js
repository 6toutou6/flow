// 登录用户信息本地快照（localStorage，仅展示用；权限/身份判断一律由后端返回）
const UserInfoKey = 'flow_user_info'

export function getUserInfo() {
  try {
    return JSON.parse(localStorage.getItem(UserInfoKey) || '{}')
  } catch (e) {
    return {}
  }
}

export function setUserInfo(info) {
  localStorage.setItem(UserInfoKey, JSON.stringify(info || {}))
}

export function removeUserInfo() {
  localStorage.removeItem(UserInfoKey)
}
