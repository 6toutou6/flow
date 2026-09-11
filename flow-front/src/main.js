import Vue from 'vue'

import 'normalize.css/normalize.css' // A modern alternative to CSS resets

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import zhCN from 'element-ui/lib/locale/lang/zh-CN' // lang i18n（中文：分页器等组件文案）

// 应用已保存的主题（在渲染前设置，避免主题闪烁）
;(function applySavedTheme() {
  try {
    const saved = localStorage.getItem('flow-theme')
    if (saved === 'red' || saved === 'slate') {
      document.documentElement.setAttribute('data-theme', saved)
    }
  } catch (e) {
    // ignore
  }
})()

import '@/styles/index.scss' // global css

import App from './App'
import store from './store'
import router from './router'

import '@/icons' // icon
import '@/permission' // permission control

import { notifyError } from '@/utils/notify'

/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online ! ! !
 */
if (process.env.NODE_ENV === 'production') {
  const { mockXHR } = require('../mock')
  mockXHR()
}

// set ElementUI lang to ZH-CN（分页器上一页/下一页、日期选择器等均为中文）
Vue.use(ElementUI, { locale: zhCN })

// 业务侧 catch 的统一错误提示入口。
// 接口错误（业务码非 200 / HTTP 异常）已由请求层 BaseAxios 弹过一次，这里只兜住本地异常
// （数据解析、本地校验等），同一个错误不会弹两次；相同文案 800ms 内还会去重。
// 用法：this.$notifyError(e, '保存失败')
Vue.prototype.$notifyError = (e, fallback) => {
  notifyError((e && e.message) || fallback)
}

Vue.config.productionTip = false

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
