import axios from 'axios'
import { Message } from 'element-ui'
import { getToken } from '@/utils/auth'

// 独立 axios 实例，对接后端真实接口（Result 结构：code===200 为成功）
// 不复用 request.js：其拦截器只认 mock 的 code===20000，且会带 X-Token 头
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // /dev-api -> proxy -> http://localhost:9000/house-service
  timeout: 10000
})

// 请求拦截器：带 Authorization token（后端读 Authorization 头，区别于 request.js 的 X-Token）
service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = token
    }
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
    Message({
      message: error.message || '网络错误',
      type: 'error',
      duration: 3 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
