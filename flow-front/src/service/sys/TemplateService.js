import { getDg, postDg } from '@/service/BaseAxios'

// 后端控制器
const controller = 'flow-template'

/**
 * @desc: 模板管理：分页列表（POST /flow-template/list；param: 分页与筛选）
 */
export function getTemplateList(params) {
  return postDg(controller, 'list', params)
}

/**
 * @desc: 模板详情（GET /flow-template/detail/{id}；模板元数据 + 字段列表）
 */
export function getTemplateDetail(id) {
  return getDg(controller, 'detail', id)
}

/**
 * @desc: 新建模板（POST /flow-template/save）
 */
export function addTemplate(data) {
  return postDg(controller, 'save', data)
}

/**
 * @desc: 更新模板元数据（POST /flow-template/update）
 */
export function updateTemplate(data) {
  return postDg(controller, 'update', data)
}

/**
 * @desc: 启用/停用切换（POST /flow-template/toggle-status/{id}）
 */
export function toggleTemplateStatus(id) {
  return postDg(controller, `toggle-status/${id}`)
}

/**
 * @desc: 设置/取消样例（POST /flow-template/sample/{id}；仅超管）
 */
export function toggleTemplateSample(id) {
  return postDg(controller, `sample/${id}`)
}

/**
 * @desc: 复制模板（POST /flow-template/copy/{id}）
 */
export function copyTemplate(id) {
  return postDg(controller, `copy/${id}`)
}

/**
 * @desc: 删除模板（POST /flow-template/delete/{id}）
 */
export function deleteTemplate(id) {
  return postDg(controller, `delete/${id}`)
}

/**
 * @desc: 统计卡数据（POST /flow-template/stats；总数/活跃/本月更新率）
 */
export function getTemplateStats() {
  return postDg(controller, 'stats')
}

/**
 * @desc: 模板被使用情况（GET /flow-template/usage/{id}；任务数/期次数，保存流程设计前提示用户）
 */
export function getTemplateUsage(id) {
  return getDg(controller, 'usage', id)
}

/**
 * @desc: 启用模板列表（POST /flow-template/enabled-list；任务下发选模板用）
 */
export function getEnabledTemplates() {
  return postDg(controller, 'enabled-list')
}

/**
 * @desc: 保存流程设计（POST /flow-template/save-flow；节点链 + 每个节点的字段）
 */
export function saveTemplateFlow(data) {
  return postDg(controller, 'save-flow', data)
}

/**
 * @desc: 单独保存某节点说明文件（POST /flow-template/node-guide-files）
 *        上传/删除后即时持久化，避免刷新丢失
 */
export function saveNodeGuideFiles(nodeId, guideFiles) {
  return postDg(controller, 'node-guide-files', { nodeId, guideFiles })
}

/**
 * @desc: 模板版本记录列表（GET /flow-template/versions/{id}）
 */
export function getTemplateVersions(id) {
  return getDg(controller, 'versions', id)
}
