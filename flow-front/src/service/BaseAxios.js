import axios from 'axios'
import { Message } from 'element-ui'

// 独立 axios 实例，对接后端真实接口（Result 结构：code===200 为成功）
// baseURL 由 VUE_APP_BASE_API 提供（/api -> proxy -> http://localhost:9000）
// 登录态由后端会话 Cookie 自动携带，无需前端手动附加 token
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // /api
  timeout: 10000
})

service.interceptors.request.use(
  config => {
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      Message({
        message: res.message || '请求错误',
        type: 'error',
        duration: 3 * 1000
      })
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    const status = error.response ? error.response.status : null
    if (status === 401) {
      // 登录态失效：清除本地快照并跳转登录页（避免残留旧登录信息导致页面误判已登录）
      try {
        const { removeUserInfo } = require('@/utils/auth')
        removeUserInfo()
      } catch (e) {
        // 忽略：清除本地快照失败不影响跳转
      }
      Message({
        message: '未登录或登录已过期，请重新登录',
        type: 'error',
        duration: 2 * 1000
      })
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else {
      Message({
        message: (error.response && error.response.data && error.response.data.message) || error.message || '网络错误',
        type: 'error',
        duration: 3 * 1000
      })
    }
    return Promise.reject(error)
  }
)

export default service
