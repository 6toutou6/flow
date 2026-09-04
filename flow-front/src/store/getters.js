const getters = {
  sidebar: state => state.app.sidebar,
  device: state => state.app.device,
  name: state => state.user.userInfo.userName || '',
  userInfo: state => state.user.userInfo,
  // 用户号（一切匹配/关联/通知用；前端仅展示用，不做权限判断）
  yyytId: state => state.user.userInfo.yyytId,
  userName: state => state.user.userInfo.userName,
  deptId: state => state.user.userInfo.deptId,
  deptName: state => state.user.userInfo.deptName,
  superAdmin: state => state.user.userInfo.superAdmin
}
export default getters
