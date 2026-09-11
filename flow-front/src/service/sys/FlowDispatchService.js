import { getDg, postDg } from '@/service/BaseAxios'

// 后端控制器
const dispatchCtl = 'flow-dispatch'
const linkCtl = 'flow-task-link'

/**
 * @desc: 任务分页列表（POST /flow-dispatch/list；含期次数、人员数）
 */
export function getDispatchTaskList(params) {
  return postDg(dispatchCtl, 'list', params)
}

/**
 * @desc: 任务管理页统计卡（POST /flow-dispatch/stats）
 */
export function getDispatchStats() {
  return postDg(dispatchCtl, 'stats')
}

/**
 * @desc: 任务详情（GET /flow-dispatch/detail/{id}；编辑回填）
 */
export function getDispatchTask(id) {
  return getDg(dispatchCtl, 'detail', id)
}

/**
 * @desc: 期次预览（POST /flow-dispatch/preview-period）
 *        按任务周期 + 是否立即下发，计算期次序号/默认期次名/开始截止时间
 */
export function getPreviewPeriod(taskId, immediate) {
  return postDg(dispatchCtl, 'preview-period', { taskId, immediate })
}

/**
 * @desc: 检索当前是否有任务的期次应下发（POST /flow-dispatch/check-due）
 */
export function checkDueDispatches() {
  return postDg(dispatchCtl, 'check-due')
}

/**
 * @desc: 自动下发所有到期待下发的期次（POST /flow-dispatch/auto-dispatch）
 */
export function autoDispatchDuePeriods() {
  return postDg(dispatchCtl, 'auto-dispatch')
}

/**
 * @desc: 创建任务（POST /flow-dispatch/save；含下发周期配置、模板配置信息、人员）
 */
export function saveDispatchPlan(data) {
  return postDg(dispatchCtl, 'save', data)
}

/**
 * @desc: 更新任务（POST /flow-dispatch/update）
 */
export function updateDispatchPlan(data) {
  return postDg(dispatchCtl, 'update', data)
}

/**
 * @desc: 启用/停用任务（POST /flow-dispatch/toggle-status/{id}）
 */
export function toggleDispatchPlanStatus(id) {
  return postDg(dispatchCtl, `toggle-status/${id}`)
}

/**
 * @desc: 设置/取消样例（POST /flow-dispatch/sample/{id}；仅超管）
 */
export function toggleDispatchSample(id) {
  return postDg(dispatchCtl, `sample/${id}`)
}

/**
 * @desc: 删除任务（POST /flow-dispatch/delete/{id}；仅当任务下无任何期次时允许）
 */
export function deleteDispatchPlan(id) {
  return postDg(dispatchCtl, `delete/${id}`)
}

/**
 * @desc: 任务人员列表（GET /flow-dispatch/members/{taskId}）
 */
export function getTaskMembers(taskId) {
  return getDg(dispatchCtl, 'members', taskId)
}

/**
 * @desc: 全量保存任务人员（POST /flow-dispatch/members/save/{taskId}；body: userIds 数组）
 */
export function saveTaskMembers(taskId, userIds) {
  return postDg(dispatchCtl, `members/save/${taskId}`, userIds)
}

/**
 * @desc: 生成期次（POST /flow-dispatch/periods/{taskId}；manual=true 手动临时期次，否则按周期自动）
 */
export function generatePeriod(taskId, data) {
  return postDg(dispatchCtl, `periods/${taskId}`, data)
}

// ==================== 下发配置模板 ====================

/**
 * @desc: 配置模板列表（POST /flow-dispatch/config-template/list；新建任务时可拉取复用）
 */
export function getConfigTemplates(keyword, sample, mine, cycleType, creatorName, updateStart, updateEnd) {
  return postDg(dispatchCtl, 'config-template/list', {
    keyword: keyword || undefined,
    sample: sample === '' || sample == null ? undefined : sample,
    mine: mine ? true : undefined,
    cycleType: cycleType === '' || cycleType == null ? undefined : cycleType,
    creatorName: creatorName || undefined,
    updateStart: updateStart || undefined,
    updateEnd: updateEnd || undefined
  })
}

/**
 * @desc: 下发配置模板页统计卡（POST /flow-dispatch/config-template/stats）
 */
export function getConfigTemplateStats() {
  return postDg(dispatchCtl, 'config-template/stats')
}

/**
 * @desc: 配置模板详情（GET /flow-dispatch/config-template/detail/{id}；编辑回填）
 */
export function getConfigTemplate(id) {
  return getDg(dispatchCtl, 'config-template/detail', id)
}

/**
 * @desc: 新增/更新配置模板（POST /flow-dispatch/config-template/save；有 id 则更新）
 */
export function saveConfigTemplate(data) {
  return postDg(dispatchCtl, 'config-template/save', data)
}

/**
 * @desc: 删除下发配置模板（POST /flow-dispatch/config-template/delete/{id}）
 */
export function deleteConfigTemplate(id) {
  return postDg(dispatchCtl, `config-template/delete/${id}`)
}

/**
 * @desc: 设置/取消下发配置模板样例（POST /flow-dispatch/config-template/sample/{id}；仅超管）
 */
export function toggleConfigTemplateSample(id) {
  return postDg(dispatchCtl, `config-template/sample/${id}`)
}

/**
 * @desc: 复制下发配置模板（POST /flow-dispatch/config-template/copy/{id}）
 *        样例可复制，产物归属当前部门管理员并转为普通配置
 */
export function copyConfigTemplate(id) {
  return postDg(dispatchCtl, `config-template/copy/${id}`)
}

/**
 * @desc: 期次新增人员（POST /flow-dispatch/period/add-members/{dispatchId}；抄用期次配置为每位新人员创建独立提交任务）
 */
export function addPeriodMembers(dispatchId, userIds) {
  return postDg(dispatchCtl, `period/add-members/${dispatchId}`, { userIds })
}

/**
 * @desc: 修改期次截止时间（POST /flow-dispatch/period/end-time/{dispatchId}；同步更新该期次下所有成员任务的截止时间）
 */
export function updatePeriodEndTime(dispatchId, endTime) {
  return postDg(dispatchCtl, `period/end-time/${dispatchId}`, { endTime })
}

/**
 * @desc: 期次详情（GET /flow-dispatch/period/detail/{dispatchId}；含截止时间、催办时间，期次人员页展示用）
 */
export function getPeriodInfo(dispatchId) {
  return getDg(dispatchCtl, 'period/detail', dispatchId)
}

// ===== 任务关联（汇总 ← 收集） =====

/**
 * @desc: 建立关联（POST /flow-task-link/create；配置级/成员级）
 */
export function createTaskLink(data) {
  return postDg(linkCtl, 'create', data)
}

/**
 * @desc: 按来源查询（POST /flow-task-link/by-source）
 *        dispatchId=任务配置取全部期次关联；periodId=精确到某期次
 */
export function getTaskLinksBySource(taskId, dispatchId, periodId) {
  return postDg(linkCtl, 'by-source', { taskId, dispatchId, periodId })
}

/**
 * @desc: 按目标反查（POST /flow-task-link/by-target；taskId=当前收到的成员任务）
 */
export function getTaskLinksByTarget(taskId, dispatchId, periodId) {
  return postDg(linkCtl, 'by-target', { taskId, dispatchId, periodId })
}

/**
 * @desc: 解除关联（POST /flow-task-link/delete/{id}）
 */
export function removeTaskLink(id) {
  return postDg(linkCtl, `delete/${id}`)
}
