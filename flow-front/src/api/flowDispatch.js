import request from '@/utils/request-flow'

// 任务分页列表（含期次数、人员数）
export function getDispatchTaskList(params) {
  return request({
    url: '/flow-dispatch/list',
    method: 'get',
    params
  })
}

// 任务管理页统计卡
export function getDispatchStats() {
  return request({
    url: '/flow-dispatch/stats',
    method: 'get'
  })
}

// 任务详情（编辑回填）
export function getDispatchTask(id) {
  return request({
    url: `/flow-dispatch/${id}`,
    method: 'get'
  })
}

// 期次预览：按任务周期 + 是否立即下发，计算期次序号/默认期次名/开始截止时间
export function getPreviewPeriod(taskId, immediate) {
  return request({
    url: '/flow-dispatch/preview-period',
    method: 'get',
    params: { taskId, immediate }
  })
}

// 检索当前是否有任务的期次应下发
export function checkDueDispatches() {
  return request({
    url: '/flow-dispatch/check-due',
    method: 'get'
  })
}

// 自动下发所有到期待下发的期次
export function autoDispatchDuePeriods() {
  return request({
    url: '/flow-dispatch/auto-dispatch',
    method: 'post'
  })
}

// 创建任务（含下发周期配置、模板配置信息、人员）
export function saveDispatchPlan(data) {
  return request({
    url: '/flow-dispatch/save',
    method: 'post',
    data
  })
}

// 更新任务
export function updateDispatchPlan(data) {
  return request({
    url: '/flow-dispatch/update',
    method: 'put',
    data
  })
}

// 启用/停用任务
export function toggleDispatchPlanStatus(id) {
  return request({
    url: `/flow-dispatch/toggle-status/${id}`,
    method: 'put'
  })
}

// 设置/取消样例（仅超管）
export function toggleDispatchSample(id) {
  return request({
    url: `/flow-dispatch/sample/${id}`,
    method: 'put'
  })
}

// 删除任务（仅当任务下无任何期次时允许）
export function deleteDispatchPlan(id) {
  return request({
    url: `/flow-dispatch/delete/${id}`,
    method: 'delete'
  })
}

// 任务人员列表
export function getTaskMembers(taskId) {
  return request({
    url: `/flow-dispatch/${taskId}/members`,
    method: 'get'
  })
}

// 全量保存任务人员
export function saveTaskMembers(taskId, userIds) {
  return request({
    url: `/flow-dispatch/${taskId}/members`,
    method: 'put',
    data: userIds
  })
}

// 生成期次：manual=true 手动临时期次；否则按周期自动（immediate 立即/下一期次）
export function generatePeriod(taskId, data) {
  return request({
    url: `/flow-dispatch/${taskId}/periods`,
    method: 'post',
    data
  })
}

// ==================== 下发配置模板 ====================

// 配置模板列表（新建任务时可拉取复用）
export function getConfigTemplates(keyword) {
  return request({
    url: '/flow-dispatch/config-template/list',
    method: 'get',
    params: { keyword }
  })
}

// 下发配置模板页统计卡
export function getConfigTemplateStats() {
  return request({
    url: '/flow-dispatch/config-template/stats',
    method: 'get'
  })
}

// 配置模板详情（编辑回填）
export function getConfigTemplate(id) {
  return request({
    url: `/flow-dispatch/config-template/${id}`,
    method: 'get'
  })
}

// 新增/更新配置模板（有 id 则更新）
export function saveConfigTemplate(data) {
  return request({
    url: '/flow-dispatch/config-template/save',
    method: 'post',
    data
  })
}

// 删除下发配置模板
export function deleteConfigTemplate(id) {
  return request({
    url: `/flow-dispatch/config-template/delete/${id}`,
    method: 'delete'
  })
}

// 设置/取消下发配置模板样例（仅超管）
export function toggleConfigTemplateSample(id) {
  return request({
    url: `/flow-dispatch/config-template/sample/${id}`,
    method: 'put'
  })
}

// 期次新增人员：抄用期次配置为每位新人员创建独立提交任务
export function addPeriodMembers(dispatchId, userIds) {
  return request({
    url: `/flow-dispatch/${dispatchId}/period/add-members`,
    method: 'post',
    data: { userIds }
  })
}
