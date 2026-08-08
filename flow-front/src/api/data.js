import request from '@/utils/request-flow'

// 填报记录列表
export function getRecordList(params) {
  return request({
    url: '/flow-data/list',
    method: 'post',
    data: params
  })
}

// 填报记录详情
export function getRecordDetail(id) {
  return request({
    url: `/flow-data/record/${id}`,
    method: 'get'
  })
}

// 数据后台统计卡
export function getDataStats() {
  return request({
    url: '/flow-data/stats',
    method: 'get'
  })
}

// 提交量趋势
export function getDataTrend(days = 30) {
  return request({
    url: '/flow-data/trend',
    method: 'get',
    params: { days }
  })
}

// 按人员展示：人员提交汇总列表（分页）
export function getPersonList(params) {
  return request({
    url: '/flow-data/person-list',
    method: 'get',
    params
  })
}

// 按人员展示：某人全部历史提交记录（分页）
export function getPersonRecords(params) {
  return request({
    url: '/flow-data/person-records',
    method: 'get',
    params
  })
}

// 数据展示页聚合数据（统计卡 + 各图表；trendType: day/month/quarter/year，可选时间范围 startDate/endDate）
export function getDashboard(params = {}) {
  return request({
    url: '/flow-data/dashboard',
    method: 'get',
    params: Object.assign({ trendType: 'day' }, params)
  })
}
