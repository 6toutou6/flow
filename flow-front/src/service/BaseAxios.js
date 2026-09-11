// 本地开发兜底：注册内网框架风格的全局请求函数 get/post
// （内网框架已提供同名全局函数时不覆盖，搬回内网无需改动本文件）
import '@/utils/request'

// 后端项目名称（内网网关路由前缀，与后端项目名保持一致）
const flow_service = 'flow-service'

/**
 * @desc: get方式，/号(Flow)
 */
export function getDg(controller, method, getParam) {
  return get(flow_service, controller, method, getParam)
}

/**
 * @desc: post方式（Flow）
 */
export function postDg(controller, method, postParam) {
  return post(flow_service, controller, method, postParam)
}

export default { getDg, postDg }
