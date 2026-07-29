import { createMockResponse } from '@/utils/request'

const Mock = require('mockjs')

const tableData = Mock.mock({
  'items|30': [{
    id: '@id',
    date: '@date("yyyy-MM-dd")',
    name: '@cname',
    address: '@county(true)',
    type: function() {
      const types = ['类型A', '类型B', '类型C']
      return types[Mock.Random.integer(0, types.length - 1)]
    },
    status: function() {
      const statuses = ['正常', '异常', '待处理']
      return statuses[Mock.Random.integer(0, statuses.length - 1)]
    }
  }]
}).items

export function getList(params) {
  const { page = 1, limit = 10, name, address, type, status } = params
  let filtered = tableData

  if (name) {
    filtered = filtered.filter(item => item.name.includes(name))
  }
  if (address) {
    filtered = filtered.filter(item => item.address.includes(address))
  }
  if (type && type !== '全部') {
    filtered = filtered.filter(item => item.type === type)
  }
  if (status && status !== '全部') {
    filtered = filtered.filter(item => item.status === status)
  }

  const total = filtered.length
  const start = (page - 1) * limit
  const end = start + limit

  return createMockResponse({
    total,
    items: filtered.slice(start, end)
  })
}
