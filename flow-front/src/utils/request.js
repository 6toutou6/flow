import axios from 'axios'
import { Message } from 'element-ui'

// 本地 axios 实例（URL 由框架风格拼接 /{system}/{controller}/{method}[/{getParam}]）
// withCredentials：跨域请求携带会话 Cookie，后端靠会话识别当前登录用户
const http = axios.create({ timeout: 30000, withCredentials: true })

// 错误提示（模拟内网框架的 handleError）
function handleError(errorResponse) {
  const data = errorResponse.response && errorResponse.response.data
  Message.error((data && data.message) || errorResponse.message || '网络异常')
}

// 业务响应处理：code !== 200 时弹错误提示，其余原样返回 res.data（调用形态与内网框架一致）
function handleResponse(res) {
  if (res && res.code !== 200) {
    Message.error(res.message || '请求失败')
  }
  return res
}

/**
 * @desc: get方式发送请求（参数拼 URL 路径 /system/controller/method/{getParam}）
 * @param: system 后端系统
 * @param: controller 控制层
 * @param: method 接口
 * @param: getParam 请求参数（字符串）
 */
function get(system, controller, method, getParam) {
  axios.defaults.headers.post = { 'Content-Type': 'application/json;charset=UTF-8' }
  return http.get(`/${system}/${controller}/${method}/${getParam}`)
    .then((res) => {
      return handleResponse(res.data)
    }).catch((errorResponse) => {
      handleError(errorResponse)
    })
}

/**
 * @desc: post方式发送请求（参数在 body）
 * @param: system 后端系统
 * @param: controller 控制层
 * @param: method 接口
 * @param: postParam 请求参数
 */
function post(system, controller, method, postParam) {
  axios.defaults.headers.post = { 'Content-Type': 'application/json;charset=UTF-8' }
  return http.post(`/${system}/${controller}/${method}`, postParam)
    .then((res) => {
      return handleResponse(res.data)
    }).catch((errorResponse) => {
      handleError(errorResponse)
      return ''
    })
}

// ===== 本地开发兜底：模拟内网框架提供的全局请求函数 get/post =====
// 内网框架已挂载同名全局函数（window）时不覆盖，搬回内网直接复用框架能力
if (typeof window.get !== 'function') window.get = get
if (typeof window.post !== 'function') window.post = post

export default http
