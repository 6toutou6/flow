<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 + 面包屑 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <template v-if="fromDispatch">
                <span class="link" @click="backToFlowDispatch">任务管理</span>
                <span>/</span>
                <span class="active">{{ selectedGroup ? (selectedGroup.periodName || selectedGroup.taskName) : '期次人员' }}</span>
                <template v-if="level === 4">
                  <span>/</span>
                  <span class="active">{{ selectedHandler && selectedHandler.realName }} 的流程</span>
                </template>
              </template>
              <template v-else>
                <span :class="{ active: level === 1, link: level > 1 }" @click="backToTasks">数据后台</span>
                <template v-if="level >= 2">
                  <span>/</span>
                  <span :class="{ active: level === 2, link: level > 2 }" @click="backToPeriods">{{ selectedTask ? selectedTask.taskName : '任务' }}</span>
                </template>
                <template v-if="level >= 3">
                  <span>/</span>
                  <span :class="{ active: level === 3, link: level > 3 }" @click="backToMembers">{{ selectedGroup ? (selectedGroup.periodName || selectedGroup.taskName) : '期次' }}</span>
                </template>
                <template v-if="level >= 4">
                  <span>/</span>
                  <span class="active">{{ selectedHandler && selectedHandler.realName }} 的流程</span>
                </template>
              </template>
            </nav>
            <h3 class="page-heading">{{ headingText }}</h3>
          </div>
          <div v-if="level === 1" class="header-actions">
            <button class="btn-refresh" @click="fetchTaskList"><i class="el-icon-refresh" /> 刷新</button>
          </div>
          <div v-else class="header-actions">
            <button v-if="level === 3 && canAddHandler" class="btn-dispatch" @click="addHandlerVisible = true"><i class="el-icon-plus" /> 新增人员</button>
            <button class="btn-refresh" @click="goBack"><i class="el-icon-arrow-left" /> 返回</button>
          </div>
        </div>

        <!-- ===== 第一级：任务列表 ===== -->
        <template v-if="level === 1">
          <!-- 统计卡 -->
          <section class="stats-grid">
            <div class="stat-card">
              <div class="stat-icon icon-total"><i class="el-icon-files" /></div>
              <div class="stat-body">
                <div class="stat-label">任务总数</div>
                <div class="stat-value">{{ stats.totalTasks || 0 }}</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon icon-period"><i class="el-icon-tickets" /></div>
              <div class="stat-body">
                <div class="stat-label">期次总数</div>
                <div class="stat-value">{{ stats.totalPeriods || 0 }}</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon icon-running"><i class="el-icon-loading" /></div>
              <div class="stat-body">
                <div class="stat-label">进行中期次</div>
                <div class="stat-value">{{ stats.runningPeriods || 0 }}</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon icon-todo"><i class="el-icon-bell" /></div>
              <div class="stat-body">
                <div class="stat-label">我的待办</div>
                <div class="stat-value">{{ stats.myTodoCount || 0 }}</div>
              </div>
            </div>
          </section>

          <!-- 筛选区 -->
          <section class="filter-section">
            <div class="filter-grid">
              <div class="filter-item">
                <label class="filter-label">任务状态</label>
                <select v-model="filters.status" class="filter-select">
                  <option value="">全部</option>
                  <option :value="1">启用</option>
                  <option :value="0">停用</option>
                </select>
              </div>
              <div class="filter-item">
                <label class="filter-label">任务名称</label>
                <input v-model="filters.taskName" class="filter-input" placeholder="输入任务名称搜索" @keyup.enter="handleSearch">
              </div>
            </div>
            <div class="filter-actions">
              <div class="filter-actions-right">
                <button class="btn-reset" :disabled="loading" @click="resetFilters">重置</button>
                <button class="btn-search" :disabled="loading" @click="handleSearch">
                  <i v-if="loading" class="el-icon-loading" /><span v-else>查询</span>
                </button>
              </div>
            </div>
          </section>

          <!-- 任务表格 -->
          <div class="table-card table-loading-wrapper">
            <div v-if="loading" class="loading-overlay">
              <div class="loading-spinner"><i class="el-icon-loading spinning" /><p>加载中...</p></div>
            </div>
            <table class="data-table">
              <thead>
                <tr>
                  <th>任务名称</th>
                  <th>流程模板</th>
                  <th>周期</th>
                  <th>触发日</th>
                  <th class="text-center">期次数</th>
                  <th class="text-center">人员数</th>
                  <th class="text-center">状态</th>
                  <th>创建时间</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in taskList" :key="row.id" class="hover-row">
                  <td class="font-bold">{{ row.taskName }}
                    <span v-if="row.taskDesc" class="sub-text" :title="row.taskDesc">{{ row.taskDesc }}</span>
                  </td>
                  <td>{{ tplName(row.templateId) }}</td>
                  <td>{{ cycleText(row) }}</td>
                  <td>{{ cycleDayText(row) }}</td>
                  <td class="text-center">{{ row.periodCount || 0 }} 期</td>
                  <td class="text-center">{{ row.memberCount || 0 }} 人</td>
                  <td class="text-center">
                    <span :class="row.status === 1 ? 'tag-active' : 'tag-inactive'">{{ row.status === 1 ? '启用' : '停用' }}</span>
                  </td>
                  <td>{{ row.createTime }}</td>
                  <td class="text-right">
                    <button class="action-link" @click="openPeriods(row)"><i class="el-icon-s-order" /> 查看期次</button>
                  </td>
                </tr>
                <tr v-if="!loading && taskList.length === 0">
                  <td colspan="9" class="text-center" style="padding: 32px; color: #999;">暂无任务</td>
                </tr>
              </tbody>
            </table>
            <!-- 分页 -->
            <div class="pagination">
              <div class="pagination-left">
                <span class="pagination-info">共计 {{ taskTotal }} 个任务</span>
                <select v-model="pageSize" class="page-size-select" @change="onPageSizeChange">
                  <option :value="5">5 条/页</option>
                  <option :value="10">10 条/页</option>
                  <option :value="20">20 条/页</option>
                  <option :value="50">50 条/页</option>
                </select>
              </div>
              <div class="pagination-controls">
                <button class="page-btn" :disabled="currentPage === 1" @click="prevPage"><i class="el-icon-arrow-left" /></button>
                <span class="page-current">{{ currentPage }} / {{ totalPages }}</span>
                <button class="page-btn" :disabled="currentPage === totalPages" @click="nextPage"><i class="el-icon-arrow-right" /></button>
              </div>
            </div>
          </div>
        </template>

        <!-- ===== 第二级：期次列表 ===== -->
        <template v-if="level === 2">
          <!-- 筛选区 -->
          <section class="filter-section">
            <div class="filter-grid">
              <div class="filter-item">
                <label class="filter-label">期次状态</label>
                <select v-model="periodFilters.status" class="filter-select">
                  <option value="">全部</option>
                  <option :value="0">暂无人员</option>
                  <option :value="1">进行中</option>
                  <option :value="2">已完成</option>
                  <option :value="3">已作废</option>
                </select>
              </div>
              <div class="filter-item">
                <label class="filter-label">期次名称</label>
                <input v-model="periodFilters.taskName" class="filter-input" placeholder="输入期次名称搜索" @keyup.enter="handlePeriodSearch">
              </div>
            </div>
            <div class="filter-actions">
              <div class="filter-actions-right">
                <button class="btn-reset" :disabled="periodLoading" @click="resetPeriodFilters">重置</button>
                <button class="btn-search" :disabled="periodLoading" @click="handlePeriodSearch">
                  <i v-if="periodLoading" class="el-icon-loading" /><span v-else>查询</span>
                </button>
              </div>
            </div>
          </section>

          <!-- 期次表格 -->
          <div class="table-card table-loading-wrapper">
            <div v-if="periodLoading" class="loading-overlay">
              <div class="loading-spinner"><i class="el-icon-loading spinning" /><p>加载中...</p></div>
            </div>
            <table class="data-table">
              <thead>
                <tr>
                  <th>期次名称</th>
                  <th>所属任务</th>
                  <th>所属模板</th>
                  <th>截止时间</th>
                  <th class="text-center">状态</th>
                  <th class="text-center">成员数</th>
                  <th>成员状态概览</th>
                  <th>下发时间</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in periodList" :key="row.dispatchId" class="hover-row">
                  <td class="font-bold">
                    {{ row.periodName || row.taskName }}
                    <span v-if="row.manualFlag === 1" class="tag-manual">临时</span>
                  </td>
                  <td>{{ row.planName || '—' }}</td>
                  <td>{{ tplName(row.templateId) }}</td>
                  <td>{{ row.endTime || '—' }}</td>
                  <td class="text-center">
                    <span :class="taskStatusClass(row.status)">{{ taskStatusText(row.status) }}</span>
                  </td>
                  <td class="text-center">{{ row.memberCount || 0 }} 人</td>
                  <td>
                    <div class="member-overview">
                      <span v-if="row.runningCount" class="ov-running">{{ row.runningCount }} 进行中</span>
                      <span v-if="row.finishedCount" class="ov-done">{{ row.finishedCount }} 已完成</span>
                      <span v-if="row.cancelledCount" class="ov-cancel">{{ row.cancelledCount }} 作废</span>
                      <span v-if="!row.runningCount && !row.finishedCount && !row.cancelledCount" class="ov-none">—</span>
                    </div>
                  </td>
                  <td>{{ row.dispatchTime }}</td>
                  <td class="text-right">
                    <button class="action-link" @click="openGroup(row)"><i class="el-icon-user" /> 查看人员</button>
                    <button class="action-link text-error" :disabled="row.memberCount > 0" :title="row.memberCount > 0 ? '请先删除所有人员' : '删除期次'" @click="onDeleteGroup(row)"><i class="el-icon-delete" /> 删除</button>
                  </td>
                </tr>
                <tr v-if="!periodLoading && periodList.length === 0">
                  <td colspan="9" class="text-center" style="padding: 32px; color: #999;">暂无期次</td>
                </tr>
              </tbody>
            </table>
            <!-- 分页 -->
            <div class="pagination">
              <div class="pagination-left">
                <span class="pagination-info">共计 {{ periodTotal }} 个期次</span>
                <select v-model="pageSize" class="page-size-select" @change="onPageSizeChange">
                  <option :value="5">5 条/页</option>
                  <option :value="10">10 条/页</option>
                  <option :value="20">20 条/页</option>
                  <option :value="50">50 条/页</option>
                </select>
              </div>
              <div class="pagination-controls">
                <button class="page-btn" :disabled="periodPage === 1" @click="periodPrevPage"><i class="el-icon-arrow-left" /></button>
                <span class="page-current">{{ periodPage }} / {{ periodTotalPages }}</span>
                <button class="page-btn" :disabled="periodPage === periodTotalPages" @click="periodNextPage"><i class="el-icon-arrow-right" /></button>
              </div>
            </div>
          </div>
        </template>

        <!-- ===== 第三级：期次人员（每个成员 = 一条独立人员任务） ===== -->
        <template v-if="level === 3">
          <div v-loading="detailLoading" class="handlers-wrap">
            <div v-if="!detailLoading && members.length === 0" class="empty-state">
              <i class="el-icon-user" />
              <p>该期次暂无人员</p>
            </div>
            <template v-else>
              <!-- 成员筛选（姓名/部门/状态，点击查询才查询） -->
              <div class="filter-section member-filter">
                <div class="filter-grid">
                  <div class="filter-item">
                    <label class="filter-label">姓名</label>
                    <input v-model="memberFilters.name" class="filter-input" placeholder="输入姓名搜索" @keyup.enter="memberFilterSearch">
                  </div>
                  <div class="filter-item">
                    <label class="filter-label">部门</label>
                    <input v-model="memberFilters.dept" class="filter-input" placeholder="输入部门搜索" @keyup.enter="memberFilterSearch">
                  </div>
                  <div class="filter-item">
                    <label class="filter-label">状态</label>
                    <select v-model="memberFilters.status" class="filter-select">
                      <option value="">全部</option>
                      <option :value="1">进行中</option>
                      <option :value="2">已完成</option>
                      <option :value="3">已作废</option>
                    </select>
                  </div>
                </div>
                <div class="filter-actions">
                  <span class="member-count">共 {{ memberTotal }} 人<span v-if="selectedMemberIds.length" class="selected-count">已选 {{ selectedMemberIds.length }} 人</span></span>
                  <div class="filter-actions-right">
                    <button class="btn-batch-delete" :disabled="batchDeleting || selectedMemberIds.length === 0" @click="onBatchDeleteMembers">
                      <i v-if="batchDeleting" class="el-icon-loading" /><i v-else class="el-icon-delete" /> 批量删除 ({{ selectedMemberIds.length }})
                    </button>
                    <button class="btn-reset" @click="resetMemberFilters">重置</button>
                    <button class="btn-search" @click="memberFilterSearch">查询</button>
                  </div>
                </div>
              </div>
              <div v-if="members.length === 0" class="empty-state">
                <i class="el-icon-search" />
                <p>没有符合条件的人员</p>
              </div>
              <div v-else class="handler-grid">
                <div v-for="m in members" :key="m.taskId" class="handler-card" :class="{ selected: isMemberSelected(m) }">
                  <div class="hc-check" @click.stop>
                    <el-checkbox :value="isMemberSelected(m)" @change="val => onMemberCheck(m, val)" />
                  </div>
                  <div class="hc-left" @click="openMemberFlow(m)">
                    <div class="hc-avatar">{{ memberName(m).charAt(0) }}</div>
                    <div class="hc-info">
                      <div class="hc-name">{{ memberName(m) }}
                        <span class="hc-emp">{{ m.ownerEmpNo || '—' }}</span>
                        <span class="status-chip" :class="taskStatusClass(m.status)">{{ taskStatusText(m.status) }}</span>
                      </div>
                      <div class="hc-dept">{{ m.ownerDept || '—' }}</div>
                      <div class="hc-meta">
                        <span><i class="el-icon-user" /> 当前处理人：{{ m.currentHandlerName || '—' }}</span>
                        <span><i class="el-icon-s-claim" /> 当前节点：{{ m.currentNodeName || '—' }}</span>
                        <span><i class="el-icon-odometer" /> 进度 {{ m.finishedNodeCount || 0 }}/{{ m.totalNodeCount || 0 }}</span>
                      </div>
                      <div class="progress-bar thin"><div class="progress-fill" :style="{ width: memberProgress(m) + '%' }" /></div>
                      <!-- 流程节点横向展示：已通过绿 / 处理中红 / 未到灰 -->
                      <div class="hc-nodes">
                        <span
                          v-for="(st, si) in (m.nodeSteps || [])"
                          :key="si"
                          class="node-chip"
                          :class="'chip-' + st.status"
                          :title="st.nodeName"
                        >{{ st.stepNo }}.{{ st.nodeName }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="hc-right">
                    <button v-if="m.status === 1" class="hc-urge" :disabled="urgingId === m.taskId" :title="'催办「' + (m.currentHandlerName || m.ownerName || '') + '」尽快处理'" @click.stop="onUrgeTask(m)">
                      <i v-if="urgingId === m.taskId" class="el-icon-loading" />
                      <i v-else class="el-icon-alarm-clock" /> 催办
                    </button>
                    <div class="hc-view" @click="openMemberFlow(m)">
                      <i class="el-icon-arrow-right" />
                      <span class="hc-action">查看流程</span>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 成员分页（可切换每页条数） -->
              <div v-if="memberTotal > 0" class="pagination member-pagination">
                <div class="pagination-left">
                  <span class="pagination-info">共计 {{ memberTotal }} 人</span>
                  <select v-model="memberPageSize" class="page-size-select" @change="onMemberPageSizeChange">
                    <option :value="5">5 条/页</option>
                    <option :value="10">10 条/页</option>
                    <option :value="20">20 条/页</option>
                    <option :value="50">50 条/页</option>
                  </select>
                </div>
                <div class="pagination-controls">
                  <button class="page-btn" :disabled="memberPage === 1" @click="memberPrevPage"><i class="el-icon-arrow-left" /></button>
                  <span class="page-current">{{ memberPage }} / {{ memberTotalPages }}</span>
                  <button class="page-btn" :disabled="memberPage === memberTotalPages" @click="memberNextPage"><i class="el-icon-arrow-right" /></button>
                </div>
              </div>
            </template>
          </div>
        </template>

        <!-- ===== 第四级：流程详情 ===== -->
        <template v-if="level === 4">
          <div class="flow-detail-wrap">
            <div class="handler-bar">
              <div class="hb-avatar">{{ selectedHandler && selectedHandler.realName ? selectedHandler.realName.charAt(0) : 'U' }}</div>
              <div class="hb-info">
                <div class="hb-name">{{ selectedHandler && selectedHandler.realName }} <span class="hb-emp">{{ selectedHandler && selectedHandler.empNo }}</span></div>
                <div class="hb-dept">{{ selectedHandler && selectedHandler.deptName || '—' }}</div>
              </div>
              <div class="hb-tip">下方流程链中，该人员处理的节点已高亮标记</div>
            </div>

            <!-- 流程详情（去重链 + 多展开 + 操作记录，独立组件） -->
            <HandlerFlowDetail :task-detail="taskDetail" :selected-handler="selectedHandler" />
          </div>
        </template>
      </section>
    </main>

    <!-- 临时新增处理人弹窗（在期次内为新增人员创建独立任务） -->
    <UserPicker
      :visible="addHandlerVisible"
      :title="addHandlerTitle"
      :exclude-ids="handlerIdsInTask"
      @confirm="onConfirmAddHandler"
      @close="addHandlerVisible = false"
    />
  </div>
</template>

<script>
import { getTaskList, getTaskDetail, getTaskGroupDetail, getTaskMembers, addTaskHandlers, deleteTask, deleteTaskGroup, urgeTask } from '@/api/task'
import { getDispatchTaskList } from '@/api/flowDispatch'
import { getUserList } from '@/api/sysuser'
import { getTemplateList } from '@/api/template'
import { getDataStats } from '@/api/data'
import UserPicker from '@/components/UserPicker'
import HandlerFlowDetail from './components/HandlerFlowDetail.vue'

export default {
  name: 'DataAdmin',
  components: { HandlerFlowDetail, UserPicker },
  data() {
    return {
      level: 1,
      // 从任务管理页携带 dispatchId 直达期次人员（不走层级导航）
      fromDispatch: false,
      // 第一级：任务
      loading: false,
      taskList: [],
      taskTotal: 0,
      currentPage: 1,
      pageSize: 10,
      totalPages: 1,
      stats: {},
      filters: { status: '', taskName: '' },
      selectedTask: null,
      // 第二级：期次
      periodLoading: false,
      periodList: [],
      periodTotal: 0,
      periodPage: 1,
      periodFilters: { status: '', taskName: '' },
      periodTotalPages: 1,
      selectedGroup: null,
      // 新增人员弹窗
      addHandlerVisible: false,
      // 映射
      userMap: {},
      tplMap: {},
      // 第三级（成员，分页）
      detailLoading: false,
      members: [],
      memberFilters: { name: '', dept: '', status: '' },
      memberPage: 1,
      memberPageSize: 10,
      memberTotal: 0,
      memberTotalPages: 1,
      // 成员批量删除
      selectedMemberIds: [],
      batchDeleting: false,
      // 催办中成员任务ID
      urgingId: null,
      // 第四级
      taskDetail: null,
      selectedHandler: null
    }
  },
  computed: {
    headingText() {
      if (this.level === 1) return '任务数据后台'
      if (this.level === 2) {
        const t = this.selectedTask ? this.selectedTask.taskName : ''
        return '期次列表' + (t ? ` · ${t}` : '')
      }
      if (this.level === 3) {
        const g = this.selectedGroup
        if (g) {
          const period = g.periodName || (g.periodNo ? `第${g.periodNo}期` : '')
          const plan = g.planName ? g.planName : ''
          const extra = [plan, period].filter(Boolean).join(' / ')
          return '期次人员' + (extra ? ` · ${extra}` : '')
        }
        return '期次人员'
      }
      return '流程处理详情'
    },
    /** 是否可新增人员：选中期次即可（空期次也能补员） */
    canAddHandler() {
      return !!this.selectedGroup
    },
    /** 组内已有成员（新增时排除，避免重复） */
    handlerIdsInTask() {
      return this.members.map(m => m.ownerUserId).filter(Boolean)
    },
    /** 新增人员弹窗标题：为新增人员创建独立任务归入本期次 */
    addHandlerTitle() {
      return '新增人员（为该人员创建独立任务，归入本期次）'
    }
  },
  async mounted() {
    this.loadMaps()
    this.fetchStats()
    await this.fetchTaskList()
    this.handleRouteQuery()
  },
  methods: {
    /** 从任务管理页「查看人员」跳转进来时，携带 dispatchId 直达期次人员；否则支持 taskId 打开期次列表 */
    handleRouteQuery() {
      const dispatchId = this.$route.query && this.$route.query.dispatchId
      if (dispatchId) {
        this.fromDispatch = true
        this.openGroupById(Number(dispatchId))
        return
      }
      const taskId = this.$route.query && this.$route.query.taskId
      if (taskId) {
        this.$nextTick(() => {
          const found = this.taskList.find(t => String(t.id) === String(taskId))
          if (found) {
            this.openPeriods(found)
          } else {
            this.openPeriods({ id: Number(taskId), taskName: '任务 #' + taskId })
          }
        })
      }
    },
    /** 按 dispatchId 直接打开期次人员层（任务管理页「查看人员」入口） */
    async openGroupById(dispatchId) {
      this.level = 3
      this.members = []
      this.memberFilters = { name: '', dept: '', status: '' }
      this.memberPage = 1
      this.memberTotal = 0
      this.selectedMemberIds = []
      try {
        const res = await getTaskGroupDetail(dispatchId)
        this.selectedGroup = res.data || { dispatchId }
      } catch (e) {
        console.error(e)
        this.selectedGroup = { dispatchId }
      }
      await this.fetchMembers()
    },
    /** 从期次人员直接返回任务管理页 */
    backToFlowDispatch() {
      this.$router.push('/flow-dispatch/index')
    },
    userName(id) {
      const u = this.userMap[id]
      return u ? u.realName : (id ? '用户' + id : '—')
    },
    tplName(id) {
      const t = this.tplMap[id]
      return t ? t.templateName : '—'
    },
    taskStatusText(s) { return { 0: '空', 1: '进行中', 2: '已完成', 3: '已作废' }[s] || '—' },
    taskStatusClass(s) { return { 1: 'status-chip status-running', 2: 'status-chip status-done', 3: 'status-chip status-cancel', 0: 'status-chip status-empty' }[s] || '' },
    cycleText(row) {
      return { 1: '每周', 2: '每月', 3: '每季度', 4: '单次下发' }[row.cycleType] || '单次下发'
    },
    cycleDayText(row) {
      if (row.cycleType === 4) return '—'
      if (row.cycleType === 1) return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][row.cycleDay] || '—'
      return `每月 ${row.cycleDay} 号`
    },
    memberName(m) { return m.ownerName || '—' },
    memberProgress(m) {
      if (!m.totalNodeCount) return 0
      return Math.round(((m.finishedNodeCount || 0) / m.totalNodeCount) * 100)
    },
    /** 成员筛选查询（点击查询/回车才触发请求） */
    memberFilterSearch() {
      this.memberPage = 1
      this.fetchMembers()
    },
    /** 重置成员筛选并重新查询 */
    resetMemberFilters() {
      this.memberFilters = { name: '', dept: '', status: '' }
      this.memberPage = 1
      this.fetchMembers()
    },
    /** 成员分页查询（当前页 + 过滤条件） */
    async fetchMembers() {
      if (!this.selectedGroup) return
      this.detailLoading = true
      try {
        const params = { page: this.memberPage, limit: this.memberPageSize }
        if (this.memberFilters.name && this.memberFilters.name.trim()) params.name = this.memberFilters.name.trim()
        if (this.memberFilters.dept && this.memberFilters.dept.trim()) params.dept = this.memberFilters.dept.trim()
        if (this.memberFilters.status !== '') params.status = Number(this.memberFilters.status)
        const res = await getTaskMembers(this.selectedGroup.dispatchId, params)
        this.members = (res.data && res.data.records) || []
        this.memberTotal = (res.data && res.data.total) || 0
        this.memberTotalPages = Math.ceil(this.memberTotal / this.memberPageSize) || 1
        // 当前页超出范围（如删除本页最后一条后），回退到最后一页
        if (this.memberPage > this.memberTotalPages && this.memberTotalPages > 0) {
          this.memberPage = this.memberTotalPages
          await this.fetchMembers()
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.detailLoading = false
      }
    },
    memberPrevPage() {
      if (this.memberPage > 1) {
        this.memberPage--
        this.fetchMembers()
      }
    },
    memberNextPage() {
      if (this.memberPage < this.memberTotalPages) {
        this.memberPage++
        this.fetchMembers()
      }
    },
    /** 切换每页条数：回到第 1 页重新查询 */
    onMemberPageSizeChange() {
      this.memberPage = 1
      this.fetchMembers()
    },
    /** 催办：给任务当前节点处理人发送催办通知（写流转日志），防误点需二次确认 */
    onUrgeTask(m) {
      if (!m || !m.taskId) return
      const target = m.currentHandlerName || m.ownerName || '该处理人'
      this.$confirm(`确定向「${target}」发送催办通知吗？\n将提醒其在当前节点（${m.currentNodeName || '—'}）尽快处理。`, '催办确认', {
        confirmButtonText: '发送催办',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }).then(async() => {
        this.urgingId = m.taskId
        try {
          const res = await urgeTask(m.taskId)
          this.$message.success(res.message || '催办通知已发送')
          await this.fetchMembers()
        } catch (e) {
          this.$message.error((e && e.message) || '催办失败')
        } finally {
          this.urgingId = null
        }
      }).catch(() => {})
    },
    /** 新增人员确认（在本期次下为每个人创建独立成员任务，空期次也能补员） */
    async onConfirmAddHandler(users) {
      if (!users || users.length === 0) return
      try {
        const handlerIds = users.map(u => u.id)
        const res = await addTaskHandlers({ dispatchId: this.selectedGroup.dispatchId, handlerIds })
        this.$message.success(res.message || '新增成功')
        this.addHandlerVisible = false
        await this.refreshGroup()
        this.fetchStats()
      } catch (e) {
        this.$message.error((e && e.message) || '新增失败')
      }
    },
    /** 刷新当前期次（组头 + 成员分页） */
    async refreshGroup() {
      if (!this.selectedGroup) return
      try {
        const res = await getTaskGroupDetail(this.selectedGroup.dispatchId)
        if (res.data) this.selectedGroup = res.data
      } catch (e) {
        console.error(e)
      }
      await this.fetchMembers()
    },
    async loadMaps() {
      try {
        const [userRes, tplRes] = await Promise.all([
          getUserList({ page: 1, limit: 9999 }),
          getTemplateList({ page: 1, limit: 9999 })
        ])
        this.userMap = {}
        ;(userRes.data.records || []).forEach(u => { this.userMap[u.id] = u })
        this.tplMap = {}
        ;(tplRes.data.records || []).forEach(t => { this.tplMap[t.id] = t })
      } catch (e) { console.error(e) }
    },
    async fetchTaskList() {
      this.loading = true
      try {
        const params = { page: this.currentPage, limit: this.pageSize }
        if (this.filters.status !== '') params.status = Number(this.filters.status)
        if (this.filters.taskName) params.taskName = this.filters.taskName
        const res = await getDispatchTaskList(params)
        this.taskList = res.data.records || []
        this.taskTotal = res.data.total || 0
        this.totalPages = Math.ceil(this.taskTotal / this.pageSize) || 1
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async fetchPeriods() {
      if (!this.selectedTask) return
      this.periodLoading = true
      try {
        const params = { page: this.periodPage, limit: this.pageSize, taskId: this.selectedTask.id }
        if (this.periodFilters.status !== '') params.status = Number(this.periodFilters.status)
        if (this.periodFilters.taskName) params.taskName = this.periodFilters.taskName
        const res = await getTaskList(params)
        this.periodList = res.data.records || []
        this.periodTotal = res.data.total || 0
        this.periodTotalPages = Math.ceil(this.periodTotal / this.pageSize) || 1
      } catch (e) {
        console.error(e)
      } finally {
        this.periodLoading = false
      }
    },
    handlePeriodSearch() {
      this.periodPage = 1
      this.fetchPeriods()
    },
    resetPeriodFilters() {
      this.periodFilters = { status: '', taskName: '' }
      this.periodPage = 1
      this.fetchPeriods()
    },
    periodPrevPage() {
      if (this.periodPage > 1) { this.periodPage--; this.fetchPeriods() }
    },
    periodNextPage() {
      if (this.periodPage < this.periodTotalPages) { this.periodPage++; this.fetchPeriods() }
    },
    async fetchStats() {
      try {
        const res = await getDataStats()
        this.stats = res.data || {}
      } catch (e) { console.error(e) }
    },
    prevPage() { if (this.currentPage > 1) { this.currentPage--; this.fetchTaskList() } },
    nextPage() { if (this.currentPage < this.totalPages) { this.currentPage++; this.fetchTaskList() } },
    /** 第一/二级分页：切换每页条数（最小 5 条/页），回到第 1 页重新查询 */
    onPageSizeChange() {
      this.currentPage = 1
      this.periodPage = 1
      if (this.level === 1) this.fetchTaskList()
      else if (this.level === 2) this.fetchPeriods()
    },
    handleSearch() { this.currentPage = 1; this.fetchTaskList() },
    resetFilters() {
      this.filters = { status: '', taskName: '' }
      this.currentPage = 1
      this.fetchTaskList()
    },
    /** 点击任务 → 查看该任务下的期次列表 */
    openPeriods(task) {
      this.selectedTask = task
      this.level = 2
      this.periodPage = 1
      this.periodFilters = { status: '', taskName: '' }
      this.periodList = []
      this.periodTotal = 0
      this.fetchPeriods()
    },
    /** 点击期次 → 查看成员（组头 + 成员分页第一页） */
    async openGroup(group) {
      this.selectedGroup = group
      this.level = 3
      this.members = []
      this.memberFilters = { name: '', dept: '', status: '' }
      this.memberPage = 1
      this.memberTotal = 0
      this.selectedMemberIds = []
      try {
        const res = await getTaskGroupDetail(group.dispatchId)
        this.selectedGroup = res.data || group
      } catch (e) {
        console.error(e)
      }
      await this.fetchMembers()
    },
    /** 删除任务成员（删除该成员任务全部提交数据；期次保留，删空后仍可另行删除期次） */
    async onBatchDeleteMembers() {
      const ids = this.selectedMemberIds
      if (ids.length === 0) return
      const names = this.members.filter(m => ids.includes(m.taskId)).map(m => m.ownerName || m.taskId).join('、')
      try {
        await this.$confirm(`确定删除已选的 ${ids.length} 名成员（${names}）吗？将删除各成员任务及其全部提交记录，不可恢复。`, '批量删除成员确认', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        })
      } catch (e) {
        return // 用户取消
      }
      this.batchDeleting = true
      try {
        for (const id of ids) {
          await deleteTask(id)
        }
        this.$message.success(`已删除 ${ids.length} 名成员`)
        this.selectedMemberIds = []
        // 期次保留：无论删空与否都刷新成员（删空后显示空态，可在期次列表删除该期次）
        await this.refreshGroup()
        this.fetchStats()
      } catch (e) {
        this.$message.error((e && e.message) || '删除失败')
      } finally {
        this.batchDeleting = false
      }
    },
    /** 成员是否已勾选（用于跨页保留选中状态） */
    isMemberSelected(m) {
      return this.selectedMemberIds.includes(m.taskId)
    },
    onMemberCheck(m, val) {
      if (val) {
        if (!this.selectedMemberIds.includes(m.taskId)) this.selectedMemberIds.push(m.taskId)
      } else {
        this.selectedMemberIds = this.selectedMemberIds.filter(id => id !== m.taskId)
      }
    },
    /** 删除期次（任务组）：仅当期次内无人员时才允许（后端再次校验） */
    async onDeleteGroup(group) {
      try {
        await this.$confirm(`确定删除期次「${group.periodName || group.taskName}」吗？删除后不可恢复。`, '删除期次确认', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        })
      } catch (e) {
        return // 用户取消
      }
      try {
        const res = await deleteTaskGroup(group.dispatchId)
        this.$message.success(res.message || '删除成功')
        this.fetchPeriods()
        this.fetchStats()
      } catch (e) {
        this.$message.error((e && e.message) || '删除失败')
      }
    },
    /** 点击成员 → 查看该人员的流程详情 */
    async openMemberFlow(member) {
      this.selectedHandler = {
        id: member.ownerUserId,
        realName: member.ownerName,
        empNo: member.ownerEmpNo,
        deptName: member.ownerDept
      }
      this.taskDetail = null
      this.level = 4
      try {
        const res = await getTaskDetail(member.taskId)
        this.taskDetail = res.data
      } catch (e) {
        console.error(e)
      }
    },
    goBack() {
      // 从任务管理页直达：流程详情返回期次人员，期次人员返回任务管理页
      if (this.fromDispatch) {
        if (this.level === 4) this.backToMembers()
        else this.backToFlowDispatch()
        return
      }
      if (this.level === 4) this.backToMembers()
      else if (this.level === 3) this.backToPeriods()
      else if (this.level === 2) this.backToTasks()
    },
    backToTasks() {
      this.level = 1
      this.selectedTask = null
      this.selectedGroup = null
      this.members = []
      this.taskDetail = null
      this.selectedHandler = null
      this.selectedMemberIds = []
    },
    backToPeriods() {
      this.level = 2
      this.selectedGroup = null
      this.members = []
      this.taskDetail = null
      this.selectedHandler = null
      this.selectedMemberIds = []
      this.fetchPeriods()
    },
    backToMembers() {
      this.level = 3
      this.selectedHandler = null
      this.taskDetail = null
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.dashboard-container { display: flex; min-height: 100vh; background: var(--color-primary-surface);  color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; color: #414755; margin-bottom: 8px; align-items: center;
  .active { color: $primary; font-weight: 600; }
  .link { color: $primary; cursor: pointer;
    &:hover { text-decoration: underline; }
  }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.btn-refresh { display: flex; align-items: center; gap: 4px; padding: 8px 16px; background: #fff; border: 1px solid $border; border-radius: 2px; color: var(--color-primary); cursor: pointer; font-size: 13px;
  &:hover { background: var(--color-primary-light); }
}
.header-actions { display: flex; gap: 8px; align-items: center; }
.btn-dispatch { display: flex; align-items: center; gap: 4px; padding: 8px 18px; background: $primary; color: #fff; border: none; border-radius: 2px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { opacity: 0.9; }
}

// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  @media (max-width: 1100px) { grid-template-columns: repeat(2, 1fr); }
}
.stat-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon { width: 48px; height: 48px; border-radius: 3px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; flex-shrink: 0;
  &.icon-total { background: $primary; }
  &.icon-period { background: var(--color-primary-hover); }
  &.icon-running { background: #B45309; }
  &.icon-todo { background: #15803D; }
}
.stat-body { flex: 1; }
.stat-label { font-size: 13px; color: #757575; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }

// 筛选
.filter-section { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px; }
.filter-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 13px; color: #757575; }
.filter-select { height: 36px; border: 1px solid $border; border-radius: 4px; padding: 0 8px; font-size: 13px; outline: none;
  &:focus { border-color: $primary; }
}
.filter-input { height: 36px; border: 1px solid $border; border-radius: 4px; padding: 0 10px; font-size: 13px; outline: none;
  &:focus { border-color: $primary; }
}
.filter-actions { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(var(--color-primary-rgb),0.08); }
.filter-actions-right { display: flex; gap: 8px; margin-left: auto; }
.member-filter { margin-bottom: 16px; }
.member-count { font-size: 12px; color: #757575; }
.selected-count { color: $primary; font-weight: 600; margin-left: 8px; }
.btn-batch-delete { display: flex; align-items: center; gap: 4px; padding: 0 16px; height: 36px; border: none; border-radius: 4px; font-size: 13px; font-weight: bold; color: #fff; background: #DC2626; cursor: pointer;
  &:hover { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.member-pagination { margin-top: 16px; border: 1px solid $border; border-radius: 3px; justify-content: flex-end; }
.pagination-left { display: flex; align-items: center; gap: 12px; }
.page-size-select { height: 32px; border: 1px solid $border; border-radius: 4px; padding: 0 6px; font-size: 12px; color: #414755; outline: none; background: #fff; cursor: pointer;
  &:focus { border-color: $primary; }
}
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid $border; border-radius: 4px; font-size: 13px; color: var(--color-primary); background: #fff; cursor: pointer;
  &:hover { background: var(--color-primary-light); }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 4px; font-size: 13px; font-weight: bold; color: #fff; background: $primary; cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { opacity: 0.9; }
}

// 表格
.table-card { background: #fff; border: 1px solid $border; border-radius: 3px; overflow: hidden; position: relative; }
.loading-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(255,255,255,0.9); display: flex; align-items: center; justify-content: center; z-index: 10; }
.loading-spinner { text-align: center; color: $primary;
  .el-icon-loading { font-size: 40px; display: block; margin-bottom: 8px; }
  p { font-size: 14px; color: #606266; margin: 0; }
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.spinning { animation: spin 1s linear infinite; }
.data-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 12px 16px; font-weight: 700; color: #414755; background: var(--color-primary-light); border-bottom: 1px solid $border; }
  td { padding: 12px 16px; border-bottom: 1px solid $border; }
  .hover-row:hover { background: var(--color-primary-light); }
}
.font-bold { font-weight: 700; }
.sub-text { display: block; font-size: 12px; color: #909399; font-weight: 400; margin-top: 2px; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.tag-active { padding: 2px 10px; background: rgba(46,160,67,0.12); color: #2ea043; border-radius: 3px; font-size: 12px; }
.tag-inactive { padding: 2px 10px; background: #f0f0f0; color: #909399; border-radius: 3px; font-size: 12px; }
.tag-manual { padding: 1px 8px; background: rgba(180, 83, 9,0.14); color: #B45309; border-radius: 3px; font-size: 11px; margin-left: 6px; font-weight: 600; }
.status-chip { padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; display: inline-flex; border: 1px solid transparent; }
.status-running { background: rgba(var(--color-primary-rgb),0.1); border-color: $primary; color: $primary; }
.status-done { background: rgba(21, 128, 61,0.1); border-color: #15803D; color: #15803D; }
.status-cancel { background: rgba(220,38,38,0.08); border-color: #DC2626; color: #DC2626; }
.status-empty { background: rgba(144,147,153,0.1); border-color: #909399; color: #909399; }
.progress-text { font-size: 12px; color: #757575; }
.progress-bar { width: 80px; height: 6px; background: #f0f0f0; border-radius: 3px; margin-top: 4px; overflow: hidden; }
.progress-fill { height: 100%; background: $primary; border-radius: 3px; transition: width .3s; }
.member-overview { display: flex; gap: 10px; flex-wrap: wrap; font-size: 12px; font-weight: 600; }
.ov-running { color: $primary; }
.ov-done { color: #15803D; }
.ov-cancel { color: #DC2626; }
.ov-none { color: #bbb; font-weight: 400; }
.action-link { color: $primary; background: none; border: none; cursor: pointer; font-size: 14px; display: inline-flex; align-items: center; gap: 3px;
  &:hover { text-decoration: underline; }
  &:disabled { color: #bbb; cursor: not-allowed; text-decoration: none; }
}
.text-error { color: #DC2626; }
.pagination { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #faf9f9; border-top: 1px solid $border; }
.pagination-info { font-size: 12px; color: #414755; }
.pagination-controls { display: flex; align-items: center; gap: 8px; }
.member-pagination { justify-content: flex-end; }
.page-btn { width: 32px; height: 32px; border-radius: 4px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; cursor: pointer; font-size: 14px;
  &:hover:not(:disabled) { background: #efeded; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.page-current { font-size: 13px; color: #414755; }

// 第三级：处理人员
.handlers-wrap { min-height: 200px; }
.empty-state { text-align: center; padding: 60px 20px; color: #bbb;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
.handler-grid { display: grid; grid-template-columns: 1fr; gap: 12px; }
.handler-card { display: flex; justify-content: space-between; align-items: center; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px 18px; cursor: pointer; transition: all .2s;
  &:hover { box-shadow: 0 3px 10px rgba(var(--color-primary-rgb),0.12); border-color: $primary; }
  &.selected { border-color: $primary; background: var(--color-primary-light); box-shadow: 0 0 0 1px $primary; }
}
.hc-check { display: flex; align-items: center; flex-shrink: 0; margin-right: 12px; cursor: pointer; }
.hc-left { display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0; }
.hc-avatar { width: 44px; height: 44px; border-radius: 50%; background: $primary; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 600; flex-shrink: 0; }
.hc-info { flex: 1; min-width: 0; }
.hc-name { font-size: 15px; font-weight: 700; color: #1b1c1c; }
.hc-emp { font-size: 12px; color: #757575; font-weight: 400; margin-left: 6px; font-family: monospace; }
.hc-dept { font-size: 12px; color: #757575; margin-top: 2px; }
.hc-name .status-chip { margin-left: 8px; }
.hc-meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12px; color: #757575; margin-top: 6px;
  i { margin-right: 2px; }
}
.progress-bar.thin { width: 100%; margin-top: 8px; }
.hc-nodes { display: flex; align-items: center; gap: 4px; margin-top: 6px; overflow-x: auto; white-space: nowrap;
  &::-webkit-scrollbar { height: 4px; }
  &::-webkit-scrollbar-thumb { background: #ddd; border-radius: 4px; }
}
.node-chip { padding: 2px 9px; border-radius: 4px; font-size: 11px; font-weight: 600; flex-shrink: 0; line-height: 1.6; }
.chip-done { background: #15803D; color: #fff; }
.chip-current { background: rgba(var(--color-primary-rgb),0.14); color: $primary; border: 1px solid rgba(var(--color-primary-rgb),0.4); }
.chip-rejected { background: rgba(180, 83, 9,0.16); color: #B45309; border: 1px solid rgba(180, 83, 9,0.45); }
.chip-pending { background: #f0f0f0; color: #aaa; }
.chip-pending-count { margin-left: 3px; font-style: normal; color: $primary; font-weight: 700; }
.hc-right { display: flex; flex-direction: column; align-items: center; gap: 6px; color: $primary; flex-shrink: 0; margin-left: 12px; }
.hc-urge { display: inline-flex; align-items: center; gap: 4px; padding: 5px 10px; border: 1px solid rgba(180, 83, 9,0.5); background: #EFF6FF; color: #B45309; border-radius: 2px; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.2s;
  &:hover { background: rgba(180, 83, 9,0.12); border-color: #B45309; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.hc-view { display: flex; flex-direction: column; align-items: center; cursor: pointer;
  i { font-size: 18px; }
}
.hc-action { font-size: 11px; margin-top: 2px; }

// 第四级：流程详情
.flow-detail-wrap { display: flex; flex-direction: column; gap: 16px; }
.handler-bar { display: flex; align-items: center; gap: 14px; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px 20px; }
.hb-avatar { width: 48px; height: 48px; border-radius: 50%; background: $primary; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 600; flex-shrink: 0; }
.hb-info { flex: 1; }
.hb-name { font-size: 16px; font-weight: 700; color: #1b1c1c; }
.hb-emp { font-size: 12px; color: #757575; font-weight: 400; margin-left: 6px; font-family: monospace; }
.hb-dept { font-size: 12px; color: #757575; margin-top: 2px; }
.hb-tip { font-size: 12px; color: #999; }

// 流程链
.chain-section { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 20px; }
.section-title { font-size: 15px; font-weight: 700; color: $primary; margin-bottom: 16px; }
.chain-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 8px; }
.chain-track { display: flex; flex-direction: column; gap: 10px; }
.chain-step { border: 1px solid #ebeef5; border-radius: 3px; padding: 12px 14px; background: #fff; transition: all .2s;
  &.st-done { border-color: rgba(21, 128, 61,0.3); background: rgba(21, 128, 61,0.03); }
  &.st-current { border-color: $primary; background: var(--color-primary-light); box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.1); }
  &.st-rejected { border-color: rgba(180, 83, 9,0.4); background: rgba(180, 83, 9,0.05); }
  &.st-pending { opacity: 0.55; background: #f7f7f7; }
  &.mine { box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.25); }
  &.clickable { cursor: pointer;
    &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
  }
  &.expanded { box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
}
.step-head { display: flex; align-items: center; gap: 10px; }
.step-no { width: 22px; height: 22px; border-radius: 50%; background: #CBD5E1; color: var(--color-primary); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.st-current .step-no { background: $primary; color: #fff; }
.st-done .step-no { background: #15803D; color: #fff; }
.st-rejected .step-no { background: #B45309; color: #fff; }
.step-name { font-size: 14px; font-weight: 600; color: #1b1c1c; flex: 1; }
.step-badge { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; }
.badge-done { background: rgba(21, 128, 61,0.12); color: #15803D; }
.badge-current { background: rgba(var(--color-primary-rgb),0.12); color: $primary; }
.badge-rejected { background: rgba(180, 83, 9,0.15); color: #B45309; }
.badge-pending { background: #e8e8e8; color: #999; }
.mine-tag { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 600; background: $primary; color: #fff; }
.step-meta { display: flex; gap: 16px; margin-top: 6px; padding-left: 32px; font-size: 12px; color: #757575;
  i { margin-right: 3px; }
  .meta-pending { color: #bbb; font-style: italic; }
  .meta-reject { color: #B45309; font-weight: 600; }
}
.step-form { margin-top: 10px; padding: 10px 12px; background: #fff; border-radius: 2px; border: 1px dashed #CBD5E1; }
.form-row { display: flex; padding: 5px 0; font-size: 13px; border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.fr-label { width: 130px; color: #757575; flex-shrink: 0; }
.fr-value { color: #1b1c1c; flex: 1; word-break: break-all; }
.form-empty { font-size: 12px; color: #bbb; text-align: center; padding: 8px; }
</style>
