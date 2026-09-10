import request from '@/service/BaseAxios'

// 任务交接：处理人发起，创建人同部门部门管理员审批

// 发起交接申请
export function applyHandover(data) {
  return request({
    url: '/flow-task-handover',
    method: 'post',
    data
  })
}

// 我发起的交接申请
export function getMyHandovers() {
  return request({
    url: '/flow-task-handover/mine',
    method: 'get'
  })
}

// 待我审批
export function getPendingHandovers() {
  return request({
    url: '/flow-task-handover/pending',
    method: 'get'
  })
}

// 待我审批数量（角标）
export function getPendingHandoverCount() {
  return request({
    url: '/flow-task-handover/pending-count',
    method: 'get'
  })
}

// 我相关的交接记录（仅我是交接人或接手人，只读查看）
export function getHandoverRecords() {
  return request({
    url: '/flow-task-handover/records',
    method: 'get'
  })
}

// 本部门交接记录（部门管理员审批范围内，含全部状态）
export function getApproverHandoverRecords() {
  return request({
    url: '/flow-task-handover/approver-records',
    method: 'get'
  })
}

// 某任务配置的交接记录
export function getHandoversByDispatch(dispatchId) {
  return request({
    url: '/flow-task-handover/by-dispatch',
    method: 'get',
    params: { dispatchId }
  })
}

// 审批通过（可覆盖是否同步任务配置名单）
export function approveHandover(id, syncMember) {
  return request({
    url: `/flow-task-handover/${id}/approve`,
    method: 'post',
    data: { syncMember }
  })
}

// 审批拒绝
export function rejectHandover(id, reason) {
  return request({
    url: `/flow-task-handover/${id}/reject`,
    method: 'post',
    data: { reason }
  })
}
