/**
 * 错误提示的统一出口（解决「同一个错误弹两次」）。
 *
 * 与内网框架（gzfb 同款）保持一致的纪律：
 * 1. 接口错误只在请求层 `service/BaseAxios.js` 弹一次（handleResponse / handleError），
 *    业务码非 200 时 res 原样返回、**不 reject**，业务侧用 `if (!res || res.code !== 200) return` 提前返回；
 * 2. 业务侧 catch 只兜本地异常，统一调 `this.$notifyError(e, '兜底文案')`（main.js 注册）；
 * 3. 相同文案在 800ms 内只弹一次 —— 页面并发拉多个接口、同时失败时不刷屏。
 */
import { Message } from 'element-ui'

const DEDUP_WINDOW = 800
let lastMessage = ''
let lastMessageTime = 0

export function notifyError(message, duration) {
  const text = message || '操作失败'
  const now = Date.now()
  if (text === lastMessage && now - lastMessageTime < DEDUP_WINDOW) return
  lastMessage = text
  lastMessageTime = now
  Message({ message: text, type: 'error', duration: duration || 3 * 1000 })
}
