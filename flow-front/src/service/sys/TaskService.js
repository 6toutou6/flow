import { getDg, postDg } from '@/service/BaseAxios'

// 后端控制器
const controller = 'flow-task'

/**
 * @desc: 任务列表（POST /flow-task/list；param: 分页与筛选）
 */
export function getTaskList(params) {
  return postDg(controller, 'list', params)
}

/**
 * @desc: 任务详情（GET /flow-task/detail/{id}；任务 + 填报人员列表）
 */
export function getTaskDetail(id) {
  return getDg(controller, 'detail', id)
}

/**
 * @desc: 任务组详情（GET /flow-task/dispatch/{dispatchId}；数据后台 level 2：组头 + 全部成员）
 */
export function getTaskGroupDetail(dispatchId) {
  return getDg(controller, 'dispatch', dispatchId)
}

/**
 * @desc: 任务组成员分页查询（POST /flow-task/dispatch-members/{dispatchId}；param: 姓名/部门/状态过滤）
 */
export function getTaskMembers(dispatchId, params) {
  return postDg(controller, `dispatch-members/${dispatchId}`, params)
}

/**
 * @desc: 删除主任务（POST /flow-task/dispatch/delete/{dispatchId}；仅当组内无人员时才允许）
 */
export function deleteTaskGroup(dispatchId) {
  return postDg(controller, `dispatch/delete/${dispatchId}`)
}

/**
 * @desc: 更新任务（POST /flow-task/update）
 */
export function updateTask(data) {
  return postDg(controller, 'update', data)
}

/**
 * @desc: 结束任务（POST /flow-task/end/{id}）
 */
export function endTask(id) {
  return postDg(controller, `end/${id}`)
}

/**
 * @desc: 作废任务（POST /flow-task/cancel/{id}）
 */
export function cancelTask(id) {
  return postDg(controller, `cancel/${id}`)
}

/**
 * @desc: 删除任务（POST /flow-task/delete/{id}）
 */
export function deleteTask(id) {
  return postDg(controller, `delete/${id}`)
}

/**
 * @desc: 节点提交并流转（POST /flow-task/submit；处理人填写当前节点表单后提交，指定下一节点处理人）
 */
export function submitTask(data) {
  return postDg(controller, 'submit', data)
}

/**
 * @desc: 暂存（POST /flow-task/save-draft；保存草稿，不校验必填、不流转，下次打开回填）
 */
export function saveDraftTask(data) {
  return postDg(controller, 'save-draft', data)
}

/**
 * @desc: 转办（POST /flow-task/transfer；处理人把待办转给他人）
 */
export function transferTask(data) {
  return postDg(controller, 'transfer', data)
}

/**
 * @desc: 我的待办（POST /flow-task/my-todo；当前登录用户作为处理人且未处理的任务节点）
 */
export function getMyTodoList(params) {
  return postDg(controller, 'my-todo', params)
}

/**
 * @desc: 我的任务（POST /flow-task/my-todo-grouped；任务→期次 两级展示，任务级分页）
 */
export function getMyTodoGrouped(params) {
  return postDg(controller, 'my-todo-grouped', params)
}

/**
 * @desc: 我的任务统计（POST /flow-task/my-todo-stats；统计卡）
 */
export function getMyTodoStats() {
  return postDg(controller, 'my-todo-stats')
}

/**
 * @desc: 任务流转进度（GET /flow-task/progress/{taskId}；全部节点）
 */
export function getTaskProgress(taskId) {
  return getDg(controller, 'progress', taskId)
}

/**
 * @desc: 临时增加处理人（POST /flow-task/add-handlers；加入当前节点并行处理）
 */
export function addTaskHandlers(data) {
  return postDg(controller, 'add-handlers', data)
}

/**
 * @desc: 删除任务中的某个处理人（POST /flow-task/remove-handler；连同其所有提交记录）
 */
export function removeTaskHandler(taskId, handlerUserId) {
  return postDg(controller, 'remove-handler', { taskId, handlerUserId })
}

/**
 * @desc: 批量催办（POST /flow-task/urge-batch；对多个成员任务发送催办通知，记录到流转日志）
 */
export function urgeTaskBatch(taskIds) {
  return postDg(controller, 'urge-batch', { taskIds })
}

/**
 * @desc: 批量删除成员任务（POST /flow-task/delete-batch；级联清理节点/表单/附件）
 */
export function deleteTaskBatch(taskIds) {
  return postDg(controller, 'delete-batch', { taskIds })
}

/**
 * @desc: 任务流转/催办日志列表（GET /flow-task/logs/{taskId}）
 */
export function getTaskLogs(taskId) {
  return getDg(controller, 'logs', taskId)
}
