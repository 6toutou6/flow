import { login, logout } from '@/service/base/UserService'
import { getUserInfo, setUserInfo, removeUserInfo } from '@/utils/auth'
import { resetRouter } from '@/router'

const getDefaultState = () => {
  return {
    userInfo: getUserInfo()
  }
}

const state = getDefaultState()

const mutations = {
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },
  SET_USER_INFO: (state, userInfo) => {
    state.userInfo = userInfo || {}
  }
}

const actions = {
  // 登录：后端校验并写入会话，前端仅保存返回的用户信息快照（yyytId/userName/deptId/deptName）
  login({ commit }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login({ username: username.trim(), password: password }).then(response => {
        const { data } = response
        commit('SET_USER_INFO', data)
        setUserInfo(data)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 退出：清本地快照
  logout({ commit }) {
    return new Promise((resolve, reject) => {
      logout().then(() => {
        removeUserInfo()
        resetRouter()
        commit('RESET_STATE')
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 清除登录态
  resetToken({ commit }) {
    return new Promise(resolve => {
      removeUserInfo()
      commit('RESET_STATE')
      resolve()
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
