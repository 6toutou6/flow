import { createMockResponse } from '@/utils/request'

const Mock = require('mockjs')

const persons = Mock.mock({
  'items|15': [{
    id: '@id',
    name: '@cname',
    department: function() {
      const depts = ['安全生产部', '行政管理部', '技术研发中心', '质量管控部', '人力资源部']
      return depts[Mock.Random.integer(0, depts.length - 1)]
    },
    role: function() {
      const roles = ['安全主管', '行政专员', '技术支持', '运维工程师', '安全员', '质检员', '项目经理']
      return roles[Mock.Random.integer(0, roles.length - 1)]
    },
    phone: function() {
      return '1' + Mock.mock('@string("number", 10)')
    },
    email: '@email'
  }]
}).items

const approvers = Mock.mock({
  'items|20': [{
    id: '@id',
    name: '@cname',
    department: function() {
      const depts = ['安全管理部', '质量管控部', '技术研发中心', '高层管理']
      return depts[Mock.Random.integer(0, depts.length - 1)]
    },
    role: function() {
      const roles = ['审批组长', '质量经理', '安全总监', '技术总监', '部门经理']
      return roles[Mock.Random.integer(0, roles.length - 1)]
    }
  }]
}).items

const correctRecords = Mock.mock({
  'items|10': [{
    id: '@id',
    issueNo: function() {
      return '#RECT-' + Mock.mock('@integer(2023, 2024)') + '-' + Mock.mock('@string("number", 3)')
    },
    title: function() {
      const titles = [
        '消防通道杂物堆积问题',
        '灭火器过期未更换',
        '配电房漏水隐患',
        '办公区照明故障',
        '网络安全漏洞整改',
        '食堂卫生不达标',
        '电梯年检过期',
        '消防栓水压不足',
        '视频监控盲区',
        '停车场照明不足'
      ]
      return titles[Mock.Random.integer(0, titles.length - 1)]
    },
    status: function() {
      const statuses = ['pending', 'confirmed', 'rejected', 'processing', 'completed']
      return statuses[Mock.Random.integer(0, statuses.length - 1)]
    },
    isTrue: function() {
      const vals = [true, true, true, false]
      return vals[Mock.Random.integer(0, vals.length - 1)]
    },
    personName: '@cname',
    personDepartment: function() {
      const depts = ['安全生产部', '行政管理部', '技术研发中心']
      return depts[Mock.Random.integer(0, depts.length - 1)]
    },
    reason: '@paragraph(2, 3)',
    plan: '@paragraph(2, 4)',
    description: '@paragraph(2, 3)',
    attachments: function() {
      if (Math.random() < 0.5) {
        const count = Mock.Random.integer(1, 3)
        const names = ['整改照片.jpg', '整改报告.pdf', '现场记录.docx', '验收证明.pdf']
        const result = []
        for (let i = 0; i < count; i++) {
          result.push({
            name: names[i % names.length],
            size: (Math.random() * 5 + 0.5).toFixed(2) + 'MB',
            url: '#'
          })
        }
        return result
      }
      return []
    },
    approvers: function() {
      if (Math.random() < 0.6) {
        const count = Mock.Random.integer(1, 2)
        const result = []
        for (let i = 0; i < count; i++) {
          result.push({
            id: Mock.mock('@id'),
            name: '@cname',
            department: function() {
              const depts = ['安全管理部', '质量管控部']
              return depts[Mock.Random.integer(0, depts.length - 1)]
            }
          })
        }
        return result
      }
      return []
    },
    createdAt: '@datetime("yyyy-MM-dd HH:mm:ss")',
    updatedAt: '@datetime("yyyy-MM-dd HH:mm:ss")'
  }]
}).items

const getStatusText = (status) => {
  const map = {
    pending: '待确认',
    confirmed: '已确认属实',
    rejected: '不属实',
    processing: '整改中',
    completed: '已完成'
  }
  return map[status] || status
}

export function getPersonList(params) {
  const { page = 1, limit = 10, name, department } = params
  let filtered = persons

  if (name) {
    filtered = filtered.filter(p => p.name.includes(name))
  }
  if (department && department !== '全部') {
    filtered = filtered.filter(p => p.department === department)
  }

  const total = filtered.length
  const start = (page - 1) * limit
  const end = start + limit

  return createMockResponse({
    total,
    items: filtered.slice(start, end)
  })
}

export function getApproverList(params) {
  const { page = 1, limit = 10, name, department } = params
  let filtered = approvers

  if (name) {
    filtered = filtered.filter(p => p.name.includes(name))
  }
  if (department && department !== '全部') {
    filtered = filtered.filter(p => p.department === department)
  }

  const total = filtered.length
  const start = (page - 1) * limit
  const end = start + limit

  return createMockResponse({
    total,
    items: filtered.slice(start, end)
  })
}

export function getCorrectRecords(params) {
  const { page = 1, limit = 10, status, isTrue, personName } = params
  let filtered = correctRecords

  if (status && status !== '全部') {
    filtered = filtered.filter(r => r.status === status)
  }
  if (isTrue !== undefined) {
    filtered = filtered.filter(r => r.isTrue === isTrue)
  }
  if (personName) {
    filtered = filtered.filter(r => r.personName.includes(personName))
  }

  const total = filtered.length
  const start = (page - 1) * limit
  const end = start + limit

  return createMockResponse({
    total,
    items: filtered.slice(start, end).map(item => ({
      ...item,
      statusText: getStatusText(item.status),
      isTrueText: item.isTrue ? '属实' : '不属实'
    }))
  })
}

export function getCorrectRecord(id) {
  const record = correctRecords.find(r => r.id === id)
  if (record) {
    return createMockResponse({
      ...record,
      statusText: getStatusText(record.status),
      isTrueText: record.isTrue ? '属实' : '不属实'
    })
  }
  return Promise.reject({ code: 50000, message: '记录不存在' })
}

export function createCorrectRecord(data) {
  const newRecord = {
    id: Mock.mock('@id'),
    issueNo: '#RECT-' + new Date().getFullYear() + '-' + String(correctRecords.length + 1).padStart(3, '0'),
    ...data,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    statusText: getStatusText(data.status || 'pending'),
    isTrueText: data.isTrue ? '属实' : '不属实'
  }
  correctRecords.unshift(newRecord)
  return createMockResponse(newRecord, '提交成功')
}

export function updateCorrectRecord(id, data) {
  const index = correctRecords.findIndex(r => r.id === id)
  if (index !== -1) {
    correctRecords[index] = {
      ...correctRecords[index],
      ...data,
      updatedAt: new Date().toISOString(),
      statusText: getStatusText(data.status),
      isTrueText: data.isTrue ? '属实' : '不属实'
    }
    return createMockResponse(correctRecords[index], '更新成功')
  }
  return Promise.reject({ code: 50000, message: '记录不存在' })
}

export function deleteCorrectRecord(id) {
  const index = correctRecords.findIndex(r => r.id === id)
  if (index !== -1) {
    correctRecords.splice(index, 1)
    return createMockResponse(null, '删除成功')
  }
  return Promise.reject({ code: 50000, message: '记录不存在' })
}