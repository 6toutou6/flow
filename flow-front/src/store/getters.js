const getters = {
  sidebar: state => state.app.sidebar,
  device: state => state.app.device,
  token: state => state.user.token,
  avatar: state => state.user.avatar,
  name: state => state.user.name,
  userInfo: state => state.user.userInfo,
  username: state => state.user.userInfo.username,
  empNo: state => state.user.userInfo.empNo,
  realName: state => state.user.userInfo.realName,
  deptId: state => state.user.userInfo.deptId,
  deptName: state => state.user.userInfo.deptName
}
export default getters
