import { getDg, postDg } from '@/service/BaseAxios'

// 后端控制器
const controller = 'flow-task-handover'

// 任务交接：处理人发起，创建人同部门部门管理员审批

/** 发起交接申请（POST /flow-task-handover/apply） */
export function applyHandover(data) {
  return postDg(controller, 'apply', data)
}

/** 我发起的交接申请（POST /flow-task-handover/mine） */
export function getMyHandovers() {
  return postDg(controller, 'mine')
}

/** 待我审批（POST /flow-task-handover/pending） */
export function getPendingHandovers() {
  return postDg(controller, 'pending')
}

/** 待我审批数量（POST /flow-task-handover/pending-count；页签角标） */
export function getPendingHandoverCount() {
  return postDg(controller, 'pending-count')
}

/** 我相关的交接记录（POST /flow-task-handover/records；仅我是交接人或接手人，只读查看） */
export function getHandoverRecords() {
  return postDg(controller, 'records')
}

/** 本部门交接记录（POST /flow-task-handover/approver-records；部门管理员审批范围内，含全部状态） */
export function getApproverHandoverRecords() {
  return postDg(controller, 'approver-records')
}

/** 某任务配置的交接记录（GET /flow-task-handover/by-dispatch/{dispatchId}） */
export function getHandoversByDispatch(dispatchId) {
  return getDg(controller, 'by-dispatch', dispatchId)
}

/** 审批通过（POST /flow-task-handover/approve/{id}；可覆盖是否同步任务配置名单） */
export function approveHandover(id, syncMember) {
  return postDg(controller, `approve/${id}`, { syncMember })
}

/** 审批拒绝（POST /flow-task-handover/reject/{id}） */
export function rejectHandover(id, reason) {
  return postDg(controller, `reject/${id}`, { reason })
}
