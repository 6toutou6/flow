import { getDg, postDg } from '@/service/BaseAxios'

// 后端控制器
const controller = 'flow-data'

/**
 * @desc: 填报记录列表（POST /flow-data/list；param: 查询条件）
 */
export function getRecordList(params) {
  return postDg(controller, 'list', params)
}

/**
 * @desc: 填报记录详情（GET /flow-data/record/{id}）
 */
export function getRecordDetail(id) {
  return getDg(controller, 'record', id)
}

/**
 * @desc: 数据后台统计卡（POST /flow-data/stats）
 */
export function getDataStats() {
  return postDg(controller, 'stats')
}

/**
 * @desc: 提交量趋势（GET /flow-data/trend/{days}）
 */
export function getDataTrend(days = 30) {
  return getDg(controller, 'trend', days)
}

/**
 * @desc: 按人员展示：人员提交汇总列表（POST /flow-data/person-list；param: 分页与筛选）
 */
export function getPersonList(params) {
  return postDg(controller, 'person-list', params)
}

/**
 * @desc: 按人员展示：某人全部历史提交记录（POST /flow-data/person-records；param: 分页与筛选）
 */
export function getPersonRecords(params) {
  return postDg(controller, 'person-records', params)
}

/**
 * @desc: 数据展示页聚合数据（POST /flow-data/dashboard）
 *        trendType: day/month/quarter/year，可选时间范围 startDate/endDate
 */
export function getDashboard(params = {}) {
  return postDg(controller, 'dashboard', Object.assign({ trendType: 'day' }, params))
}

/**
 * @desc: 数据展示（个人维度）POST /flow-data/dashboard-user
 *        普通用户只看与自己相关的数据；范围由后端按登录身份决定，前端不传身份参数
 */
export function getUserDashboard(params = {}) {
  return postDg(controller, 'dashboard-user', Object.assign({ trendType: 'day' }, params))
}

/**
 * @desc: 数据展示（部门维度）POST /flow-data/dashboard-dept
 *        部门管理员只看本部门数据；部门由后端按 dept_admin 登记决定，前端不传身份参数
 */
export function getDeptDashboard(params = {}) {
  return postDg(controller, 'dashboard-dept', Object.assign({ trendType: 'day' }, params))
}
