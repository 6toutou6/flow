import { createMockResponse } from '@/utils/request'

const Mock = require('mockjs')

// 数据池
const titlePool = [
  '2023年度安全生产隐患排查整改任务',
  '办公区数字化设备能耗优化',
  '员工食堂卫生满意度随访',
  '厂区消防设施季度巡检整改',
  '网络安全漏洞专项排查整改',
  '配电房防汛物资储备检查整改',
  '电梯年检过期专项整改',
  '废水处理设备维护整改任务',
  '视频监控系统盲区排查整改',
  '停车场照明设施维护整改',
  '空调系统节能改造任务',
  '安全出口标识更新整改'
]

const departmentPool = ['安全生产部', '行政管理部', '技术研发中心']

const cyclePool = [
  { cycle: '每月整改', cycleInfo: '每月整改 - 每月25日截止' },
  { cycle: '每周整改', cycleInfo: '每周整改 - 每周五截止' },
  { cycle: '单次整改', cycleInfo: '单次整改 - 2023年11月30日截止' },
  { cycle: '季度整改', cycleInfo: '季度整改 - 每季度末截止' }
]

const priorityPool = [
  { priority: 'high', priorityText: '高优先级' },
  { priority: 'medium', priorityText: '中优先级' },
  { priority: 'low', priorityText: '低优先级' }
]

const statusPool = [
  { status: 'progress', statusText: '进行中' },
  { status: 'closed', statusText: '已闭环' },
  { status: 'pending', statusText: '待启动' }
]

const namePool = ['张三', '李思', '王五', '赵六', '孙七', '周八', '吴九', '郑十', '钱十一', '冯十二', '陈十三', '褚十四']
const rolePool = ['安全主管', '行政专员', '技术支持', '运维工程师', '安全员', '检查员', '技术主管', '开发工程师', '测试工程师']

const submittedRemarks = [
  '消防通道已清理完毕，杂物已搬离',
  '灭火器更换供应商对接中，预计下周到货',
  '已完成本月自查表提交并归档',
  '消防报警系统线路排查进行中，预计3个工作日内完成',
  '应急照明设备检测中，部分灯具需更换',
  '完成仓库区灭火器临期检查并上报更换清单',
  '正在核对本期整改进度，已提交阶段性报告',
  '完成设备能耗基线数据采集与分析',
  '完成监控数据接口开发与联调',
  '完成上线前全量验收测试'
]

const pendingRemarks = [
  '尚未开始整改',
  '等待上一期完成后启动',
  '等待期次启动',
  '待分配整改资源'
]

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatDateTime(d) {
  return formatDate(d) + ' ' + String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
}

function pick(arr) {
  return arr[Mock.Random.integer(0, arr.length - 1)]
}

// 生成一个任务的期次列表
function genRounds(count) {
  const rounds = []
  const baseDate = new Date(2023, 9, 1)
  for (let i = 0; i < count; i++) {
    const totalPeople = Mock.Random.integer(3, 9)
    let status, statusText, completedPeople
    const r = Math.random()
    if (r < 0.4) {
      status = 'completed'; statusText = '已完成'; completedPeople = totalPeople
    } else if (r < 0.8) {
      status = 'progress'; statusText = '进行中'; completedPeople = Mock.Random.integer(1, totalPeople - 1)
    } else {
      status = 'pending'; statusText = '待启动'; completedPeople = 0
    }
    const publishDate = new Date(baseDate.getTime() + i * 30 * 24 * 60 * 60 * 1000)
    const deadlineDate = new Date(publishDate.getTime() + 30 * 24 * 60 * 60 * 1000)
    rounds.push({
      roundNo: i + 1,
      publishTime: formatDate(publishDate),
      deadline: formatDate(deadlineDate),
      totalPeople,
      completedPeople,
      completionRate: Math.round(completedPeople / totalPeople * 100),
      status,
      statusText
    })
  }
  return rounds
}

// 生成附件列表
function genAttachments() {
  const fileNames = [
    '整改照片.jpg',
    '整改报告.pdf',
    '现场记录.docx',
    '验收证明.pdf',
    '整改前后对比图.zip',
    '安全检查表.xlsx',
    '整改说明.pdf'
  ]
  const count = Mock.Random.integer(1, 3)
  const attachments = []
  const used = new Set()
  for (let i = 0; i < count; i++) {
    let name = pick(fileNames)
    while (used.has(name)) name = pick(fileNames)
    used.add(name)
    const size = (Math.random() * 5 + 0.5).toFixed(2)
    attachments.push({
      name,
      size: `${size}MB`,
      url: '#',
      type: name.split('.').pop().toLowerCase()
    })
  }
  return attachments
}

// 生成审批流程记录
function genApprovalFlow(tagClass) {
  const flows = []
  const submitTime = new Date()
  
  // 提交记录
  flows.push({
    step: 1,
    action: '提交整改',
    actor: '本人',
    time: formatDateTime(submitTime),
    status: 'completed',
    comment: '已提交整改材料，请审核'
  })
  
  if (tagClass === 'verified') {
    // 审核通过
    const reviewTime = new Date(submitTime.getTime() + Mock.Random.integer(1, 3) * 24 * 60 * 60 * 1000)
    flows.push({
      step: 2,
      action: '部门审核',
      actor: pick(['张三', '李四', '王五', '赵六']),
      time: formatDateTime(reviewTime),
      status: 'completed',
      comment: '整改内容符合要求，审核通过'
    })
    const verifyTime = new Date(reviewTime.getTime() + Mock.Random.integer(1, 2) * 24 * 60 * 60 * 1000)
    flows.push({
      step: 3,
      action: '质量核验',
      actor: pick(['质量部', '安全组', '技术科']),
      time: formatDateTime(verifyTime),
      status: 'completed',
      comment: '现场核验通过，整改有效'
    })
  } else if (tagClass === 'processing') {
    // 审核中
    const reviewTime = new Date(submitTime.getTime() + Mock.Random.integer(1, 2) * 24 * 60 * 60 * 1000)
    flows.push({
      step: 2,
      action: '部门审核',
      actor: pick(['张三', '李四', '王五', '赵六']),
      time: formatDateTime(reviewTime),
      status: 'completed',
      comment: '初步审核通过，待质量核验'
    })
    flows.push({
      step: 3,
      action: '质量核验',
      actor: '-',
      time: '-',
      status: 'pending',
      comment: '待安排现场核验'
    })
  }
  
  return flows
}

function genPersonRounds(taskRounds, submitRate) {
  return taskRounds.map(round => {
    // 期次已完成 → 该人员必已提交；期次进行中 → 按 submitRate 概率提交；期次待启动 → 未开始
    let submitted
    if (round.status === 'completed') {
      submitted = Math.random() < 0.9
    } else if (round.status === 'progress') {
      submitted = Math.random() < submitRate
    } else {
      submitted = false
    }

    let statusText, tagClass, submitTime, remark, attachments, approvalFlow
    if (submitted) {
      if (Math.random() < 0.6) {
        statusText = '已核验'; tagClass = 'verified'
      } else {
        statusText = '处理中'; tagClass = 'processing'
      }
      const t = new Date(round.publishTime)
      t.setDate(t.getDate() + Mock.Random.integer(5, 20))
      t.setHours(Mock.Random.integer(8, 18), Mock.Random.integer(0, 59))
      submitTime = formatDateTime(t)
      remark = pick(submittedRemarks)
      attachments = Math.random() < 0.3 ? genAttachments() : []
      approvalFlow = genApprovalFlow(tagClass)
    } else {
      statusText = '未开始'; tagClass = 'pending'
      submitTime = '-'
      remark = pick(pendingRemarks)
      attachments = []
      approvalFlow = []
    }
    return { roundNo: round.roundNo, statusText, tagClass, submitTime, remark, attachments, approvalFlow }
  })
}

// 生成一个人员
function genPerson(taskRounds, name) {
  const personRounds = genPersonRounds(taskRounds, Math.random())
  const anySubmitted = personRounds.some(r => r.tagClass !== 'pending')
  const allVerified = personRounds.every(r => r.tagClass === 'verified')
  let status, tag, tagClass
  if (!anySubmitted) {
    status = '待提交'; tag = '未开始'; tagClass = 'pending'
  } else if (allVerified) {
    status = '已提交'; tag = '已核验'; tagClass = 'verified'
  } else {
    status = '已提交'; tag = '处理中'; tagClass = 'processing'
  }
  return {
    name,
    role: pick(rolePool),
    department: pick(departmentPool),
    status,
    tag,
    tagClass,
    rounds: personRounds
  }
}

// 生成任务列表
function genTasks(count) {
  const tasks = []
  for (let i = 0; i < count; i++) {
    const titleSuffix = i >= titlePool.length ? `（第${Math.floor(i / titlePool.length) + 1}批）` : ''
    const title = titlePool[i % titlePool.length] + titleSuffix
    const cyc = pick(cyclePool)
    const pri = pick(priorityPool)
    const sta = pick(statusPool)
    const roundCount = Mock.Random.integer(1, 4)
    const rounds = genRounds(roundCount)
    // 生成不重名的人员
    const personCount = Mock.Random.integer(3, 7)
    const persons = []
    const usedNames = new Set()
    let guard = 0
    while (persons.length < personCount && guard < 100) {
      guard++
      const name = pick(namePool)
      if (usedNames.has(name)) continue
      usedNames.add(name)
      persons.push(genPerson(rounds, name))
    }
    // 更新期次的总人数和完成率，使其与实际人员数据一致
    rounds.forEach(round => {
      round.totalPeople = persons.length
      const completedPersons = persons.filter(p => {
        const pr = (p.rounds || []).find(r => r.roundNo === round.roundNo)
        return pr && pr.tagClass !== 'pending'
      }).length
      round.completedPeople = completedPersons
      round.completionRate = Math.round((completedPersons / persons.length) * 100)
    })
    const createdDate = new Date(2023, Mock.Random.integer(8, 10), Mock.Random.integer(1, 28), Mock.Random.integer(8, 18), Mock.Random.integer(0, 59))
    const completedRounds = rounds.filter(r => r.status === 'completed').length
    const nextPublishDate = rounds.length > 0 ? new Date(rounds[rounds.length - 1].deadline) : new Date()
    nextPublishDate.setDate(nextPublishDate.getDate() + 1)
    tasks.push({
      id: Mock.mock('@id'),
      active: false,
      priority: pri.priority,
      priorityText: pri.priorityText,
      title,
      department: pick(departmentPool),
      cycle: cyc.cycle,
      status: sta.status,
      statusText: sta.statusText,
      description: Mock.Random.cparagraph(2, 3),
      cycleInfo: cyc.cycleInfo,
      createdTime: formatDateTime(createdDate),
      progress: `${completedRounds}/${roundCount}`,
      rounds,
      persons,
      attachments: Math.random() < 0.6 ? genAttachments() : [],
      basis: Mock.Random.cparagraph(1, 2),
      nextPublishTime: formatDate(nextPublishDate),
      projectType: Mock.Random.pick(['安全隐患', '设备维护', '环境卫生', '制度执行', '质量管控']),
      projectSource: Mock.Random.pick(['日常巡检', '专项检查', '群众举报', '上级督查', '系统预警']),
      request: Mock.Random.cparagraph(1, 2),
      needApproval: Math.random() < 0.6,
      urgeDays: Mock.Random.integer(1, 7),
      approvalTip: '请及时审核整改内容',
      finishTime: formatDate(new Date(createdDate.getTime() + 30 * 24 * 60 * 60 * 1000))
    })
  }
  return tasks
}

// 模块加载时生成一次，之后操作内存数据
const taskData = genTasks(24)

// 任务列表查询（带筛选 + 分页）
export function getTaskList(params) {
  const { department, responsible, cycle, status, date, page = 1, limit = 5 } = params
  let filtered = taskData

  if (department && department !== '全部部门') {
    filtered = filtered.filter(item => item.department === department)
  }
  if (responsible) {
    filtered = filtered.filter(item => item.persons.some(p => p.name.includes(responsible)))
  }
  // cycle 选项为 '单次'/'每周'/'每月'/'季度'，item.cycle 为 '单次整改' 等，用 includes 匹配
  if (cycle && cycle !== '全部') {
    filtered = filtered.filter(item => item.cycle.includes(cycle))
  }
  if (status && status !== '全部') {
    filtered = filtered.filter(item => item.statusText === status)
  }
  if (date) {
    filtered = filtered.filter(item => item.createdTime.indexOf(date) > -1)
  }

  const total = filtered.length
  const start = (page - 1) * limit
  const end = start + limit

  return createMockResponse({
    total,
    items: filtered.slice(start, end)
  })
}

// 任务统计
export function getTaskStats() {
  const stats = {
    total: taskData.length,
    progress: taskData.filter(i => i.status === 'progress').length,
    closed: taskData.filter(i => i.status === 'closed').length,
    pending: taskData.filter(i => i.status === 'pending').length
  }
  return createMockResponse(stats)
}

// 获取筛选选项
export function getTaskOptions() {
  return createMockResponse({
    departments: ['安全生产部', '行政管理部', '技术研发中心'],
    cycles: ['单次', '每周', '每月', '季度'],
    statuses: ['待启动', '进行中', '已闭环']
  })
}
