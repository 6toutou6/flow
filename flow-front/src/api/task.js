import request from '@/utils/request-flow'

// 任务列表
export function getTaskList(params) {
  return request({
    url: '/flow-task/list',
    method: 'post',
    data: params
  })
}

// 任务详情（任务 + 填报人员列表）
export function getTaskDetail(id) {
  return request({
    url: `/flow-task/${id}`,
    method: 'get'
  })
}

// 任务组详情（数据后台 level 2：组头 + 全部成员）
export function getTaskGroupDetail(dispatchId) {
  return request({
    url: `/flow-task/dispatch/${dispatchId}`,
    method: 'get'
  })
}

// 任务组成员分页查询（姓名/部门/状态过滤）
export function getTaskMembers(dispatchId, params) {
  return request({
    url: `/flow-task/dispatch/${dispatchId}/members`,
    method: 'get',
    params
  })
}

// 删除主任务（任务组）：仅当组内无人员时才允许
export function deleteTaskGroup(dispatchId) {
  return request({
    url: `/flow-task/dispatch/${dispatchId}`,
    method: 'delete'
  })
}

// 更新任务
export function updateTask(data) {
  return request({
    url: '/flow-task/update',
    method: 'put',
    data
  })
}

// 结束任务
export function endTask(id) {
  return request({
    url: `/flow-task/end/${id}`,
    method: 'put'
  })
}

// 作废任务
export function cancelTask(id) {
  return request({
    url: `/flow-task/cancel/${id}`,
    method: 'put'
  })
}

// 删除任务
export function deleteTask(id) {
  return request({
    url: `/flow-task/delete/${id}`,
    method: 'delete'
  })
}

// 节点提交并流转（处理人填写当前节点表单后提交，指定下一节点处理人）
export function submitTask(data) {
  return request({
    url: '/flow-task/submit',
    method: 'post',
    data
  })
}

// 暂存（保存草稿，不校验必填、不流转；下次打开回填）
export function saveDraftTask(data) {
  return request({
    url: '/flow-task/save-draft',
    method: 'post',
    data
  })
}

// 我的待办（当前登录用户作为处理人且未处理的任务节点）
export function getMyTodoList(params) {
  return request({
    url: '/flow-task/my-todo',
    method: 'post',
    data: params
  })
}

// 我的任务（任务→期次 两级展示）：任务级分页，任务下按期次分组待办节点
export function getMyTodoGrouped(params) {
  return request({
    url: '/flow-task/my-todo-grouped',
    method: 'post',
    data: params
  })
}

// 我的任务统计（统计卡）
export function getMyTodoStats() {
  return request({
    url: '/flow-task/my-todo-stats',
    method: 'get'
  })
}

// 任务流转进度（全部节点）
export function getTaskProgress(taskId) {
  return request({
    url: `/flow-task/progress/${taskId}`,
    method: 'get'
  })
}

// 临时增加处理人（加入当前节点并行处理）
export function addTaskHandlers(data) {
  return request({
    url: '/flow-task/add-handlers',
    method: 'post',
    data
  })
}

// 删除任务中的某个处理人（连同其所有提交记录）
export function removeTaskHandler(taskId, handlerUserId) {
  return request({
    url: '/flow-task/remove-handler',
    method: 'delete',
    params: { taskId, handlerUserId }
  })
}

// 催办：给任务当前节点处理人发送催办通知（记录到流转日志）
export function urgeTask(taskId) {
  return request({
    url: `/flow-task/urge/${taskId}`,
    method: 'post'
  })
}

// 批量催办：对多个成员任务发送催办通知（记录到流转日志）
export function urgeTaskBatch(taskIds) {
  return request({
    url: '/flow-task/urge-batch',
    method: 'post',
    data: { taskIds }
  })
}

// 批量删除成员任务（级联清理节点/表单/附件）
export function deleteTaskBatch(taskIds) {
  return request({
    url: '/flow-task/delete-batch',
    method: 'post',
    data: { taskIds }
  })
}

// 任务流转/催办日志列表
export function getTaskLogs(taskId) {
  return request({
    url: `/flow-task/logs/${taskId}`,
    method: 'get'
  })
}
