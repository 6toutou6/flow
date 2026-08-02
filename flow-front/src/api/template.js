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
