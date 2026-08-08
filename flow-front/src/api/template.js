import request from '@/utils/request-flow'

// 模板管理：分页列表
export function getTemplateList(params) {
  return request({
    url: '/flow-template/list',
    method: 'post',
    data: params
  })
}

// 模板详情（模板元数据 + 字段列表）
export function getTemplateDetail(id) {
  return request({
    url: `/flow-template/${id}`,
    method: 'get'
  })
}

// 新建模板
export function addTemplate(data) {
  return request({
    url: '/flow-template/save',
    method: 'post',
    data
  })
}

// 更新模板元数据
export function updateTemplate(data) {
  return request({
    url: '/flow-template/update',
    method: 'put',
    data
  })
}

// 启用/停用切换
export function toggleTemplateStatus(id) {
  return request({
    url: `/flow-template/toggle-status/${id}`,
    method: 'put'
  })
}

// 设置/取消样例（仅超管）
export function toggleTemplateSample(id) {
  return request({
    url: `/flow-template/sample/${id}`,
    method: 'put'
  })
}

// 复制模板
export function copyTemplate(id) {
  return request({
    url: `/flow-template/copy/${id}`,
    method: 'post'
  })
}

// 删除模板
export function deleteTemplate(id) {
  return request({
    url: `/flow-template/delete/${id}`,
    method: 'delete'
  })
}

// 统计卡数据（总数/活跃/本月更新率）
export function getTemplateStats() {
  return request({
    url: '/flow-template/stats',
    method: 'get'
  })
}

// 模板被使用情况（任务数/期次数），保存流程设计前提示用户
export function getTemplateUsage(id) {
  return request({
    url: `/flow-template/usage/${id}`,
    method: 'get'
  })
}

// 启用模板列表（任务下发选模板用）
export function getEnabledTemplates() {
  return request({
    url: '/flow-template/enabled-list',
    method: 'get'
  })
}

// 保存流程设计（节点链 + 每个节点的字段）
export function saveTemplateFlow(data) {
  return request({
    url: '/flow-template/save-flow',
    method: 'put',
    data
  })
}

// 单独保存某节点说明文件（上传/删除后即时持久化，避免刷新丢失）
export function saveNodeGuideFiles(nodeId, guideFiles) {
  return request({
    url: '/flow-template/node-guide-files',
    method: 'put',
    data: { nodeId, guideFiles }
  })
}

// 模板版本记录列表
export function getTemplateVersions(id) {
  return request({
    url: `/flow-template/versions/${id}`,
    method: 'get'
  })
}
