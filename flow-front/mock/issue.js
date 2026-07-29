const Mock = require('mockjs')

const issues = Mock.mock({
  'items|20': [{
    id: '@id',
    issueNo: function() {
      return '#RECT-' + Mock.mock('@integer(2023, 2024)') + '-' + Mock.mock('@string("number", 3)')
    },
    title: function() {
      const titles = [
        '主变压器渗油隐患整改',
        '办公区消防通道杂物堆积',
        '网络安全防护系统升级',
        '配电房防汛物资储备检查',
        '员工餐厅卫生环境抽查整改',
        '配电室通风系统故障修复',
        '厂区围墙破损修复',
        '消防栓水压不足问题整改',
        '视频监控系统盲区排查',
        '停车场照明设施维护',
        '空调系统节能改造',
        '电梯年检过期整改',
        '废水处理设备维护',
        '安全出口标识更新',
        '门禁系统升级改造',
        '会议室设备更新',
        '绿化养护问题整改',
        '食堂排烟系统清洗',
        '仓库货架安全检查',
        '配电室绝缘检测'
      ]
      return titles[Mock.Random.integer(0, titles.length - 1)]
    },
    department: function() {
      const depts = ['生产技术部', '综合管理部', '信息技术部', '安全环保部', '人力资源部', '财务部']
      return depts[Mock.Random.integer(0, depts.length - 1)]
    },
    deadline: '@date("yyyy-MM-dd")',
    'status|1': ['pending', 'rectifying', 'overdue', 'completed'],
    'priority|1': ['high', 'medium', 'low'],
    description: '@paragraph(2, 4)',
    column1: '@word(3, 8)',
    column2: '@integer(100, 9999)',
    column3: '@date("yyyy-MM-dd")',
    column4: '@float(0, 100, 2, 2)',
    column5: '@cword(2, 4)',
    column6: '@boolean',
    createdAt: '@datetime("yyyy-MM-dd HH:mm:ss")',
    updatedAt: '@datetime("yyyy-MM-dd HH:mm:ss")'
  }]
}).items

const getStatusText = (status) => {
  const map = {
    pending: '待处理',
    rectifying: '整改中',
    overdue: '已逾期',
    completed: '已完成'
  }
  return map[status] || status
}

const getPriorityText = (priority) => {
  const map = {
    high: '紧急',
    medium: '中',
    low: '低'
  }
  return map[priority] || priority
}

const statusFilterMap = {
  '待处理': 'pending',
  '整改中': 'rectifying',
  '已逾期': 'overdue',
  '已完成': 'completed'
}

const priorityFilterMap = {
  '紧急': 'high',
  '中': 'medium',
  '低': 'low'
}

module.exports = [
  {
    url: '/vue-admin-template/issue/list',
    type: 'get',
    response: config => {
      const { status, department, priority, column1, column2, column3, column4, column5, column6, page = 1, limit = 10 } = config.query
      let filtered = issues

      if (status && status !== '全部状态') {
        const realStatus = statusFilterMap[status] || status
        filtered = filtered.filter(item => item.status === realStatus)
      }
      if (department && department !== '所有部门') {
        filtered = filtered.filter(item => item.department === department)
      }
      if (priority && priority !== '全部') {
        const realPriority = priorityFilterMap[priority] || priority
        filtered = filtered.filter(item => item.priority === realPriority)
      }
      if (column1) {
        filtered = filtered.filter(item => String(item.column1).includes(column1))
      }
      if (column2) {
        filtered = filtered.filter(item => String(item.column2).includes(column2))
      }
      if (column3) {
        filtered = filtered.filter(item => item.column3 && item.column3.includes(column3))
      }
      if (column4) {
        filtered = filtered.filter(item => String(item.column4).includes(column4))
      }
      if (column5) {
        filtered = filtered.filter(item => String(item.column5).includes(column5))
      }
      if (column6) {
        const val = column6 === 'true'
        filtered = filtered.filter(item => item.column6 === val)
      }

      const total = filtered.length
      const start = (page - 1) * limit
      const end = start + limit

      return {
        code: 20000,
        data: {
          total,
          items: filtered.slice(start, end).map(item => ({
            ...item,
            statusText: getStatusText(item.status),
            priorityText: getPriorityText(item.priority)
          }))
        }
      }
    }
  },
  {
    url: '/vue-admin-template/issue/:id',
    type: 'get',
    response: config => {
      const { id } = config.query
      const issue = issues.find(item => item.id === id)
      if (issue) {
        return {
          code: 20000,
          data: {
            ...issue,
            statusText: getStatusText(issue.status),
            priorityText: getPriorityText(issue.priority)
          }
        }
      }
      return {
        code: 50000,
        message: '问题不存在'
      }
    }
  },
  {
    url: '/vue-admin-template/issue',
    type: 'post',
    response: config => {
      const { body } = config
      const newIssue = {
        id: Mock.mock('@id'),
        issueNo: '#RECT-' + new Date().getFullYear() + '-' + String(issues.length + 1).padStart(3, '0'),
        ...body,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        statusText: getStatusText(body.status || 'pending'),
        priorityText: getPriorityText(body.priority || 'medium')
      }
      issues.unshift(newIssue)
      return {
        code: 20000,
        data: newIssue,
        message: '创建成功'
      }
    }
  },
  {
    url: '/vue-admin-template/issue/:id',
    type: 'put',
    response: config => {
      const { id } = config.query
      const { body } = config
      const index = issues.findIndex(item => item.id === id)
      if (index !== -1) {
        issues[index] = {
          ...issues[index],
          ...body,
          updatedAt: new Date().toISOString(),
          statusText: getStatusText(body.status),
          priorityText: getPriorityText(body.priority)
        }
        return {
          code: 20000,
          data: issues[index],
          message: '更新成功'
        }
      }
      return {
        code: 50000,
        message: '问题不存在'
      }
    }
  },
  {
    url: '/vue-admin-template/issue/:id',
    type: 'delete',
    response: config => {
      const { id } = config.query
      const index = issues.findIndex(item => item.id === id)
      if (index !== -1) {
        issues.splice(index, 1)
        return {
          code: 20000,
          message: '删除成功'
        }
      }
      return {
        code: 50000,
        message: '问题不存在'
      }
    }
  },
  {
    url: '/vue-admin-template/issue/stats',
    type: 'get',
    response: () => {
      const stats = {
        total: issues.length,
        pending: issues.filter(i => i.status === 'pending').length,
        rectifying: issues.filter(i => i.status === 'rectifying').length,
        overdue: issues.filter(i => i.status === 'overdue').length,
        completed: issues.filter(i => i.status === 'completed').length
      }
      return {
        code: 20000,
        data: stats
      }
    }
  }
]
