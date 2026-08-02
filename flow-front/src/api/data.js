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
