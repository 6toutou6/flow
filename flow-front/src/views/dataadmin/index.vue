<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 + 面包屑 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span :class="{ active: level === 1, link: level > 1 }" @click="backToTasks">数据后台</span>
              <template v-if="level >= 2">
                <span>/</span>
                <span :class="{ active: level === 2, link: level > 2 }" @click="backToHandlers">{{ selectedTask && selectedTask.taskName }}</span>
              </template>
              <template v-if="level >= 3">
                <span>/</span>
                <span class="active">{{ selectedHandler && selectedHandler.realName }} 的流程</span>
              </template>
            </nav>
            <h3 class="page-heading">{{ headingText }}</h3>
          </div>
          <div v-if="level === 1" class="header-actions">
            <button class="btn-dispatch" @click="dispatchVisible = true"><i class="el-icon-s-promotion" /> 下发任务</button>
            <button class="btn-refresh" @click="fetchTaskList"><i class="el-icon-refresh" /> 刷新</button>
          </div>
          <div v-else class="header-actions">
            <button v-if="level === 2 && canAddHandler" class="btn-dispatch" @click="addHandlerVisible = true"><i class="el-icon-plus" /> 新增人员</button>
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
                <div class="stat-label">总任务数</div>
                <div class="stat-value">{{ stats.totalTasks || 0 }}</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon icon-running"><i class="el-icon-loading" /></div>
              <div class="stat-body">
                <div class="stat-label">进行中</div>
                <div class="stat-value">{{ stats.runningTasks || 0 }}</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon icon-done"><i class="el-icon-circle-check" /></div>
              <div class="stat-body">
                <div class="stat-label">已完成</div>
                <div class="stat-value">{{ stats.finishedTasks || 0 }}</div>
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
                  <option :value="1">进行中</option>
                  <option :value="2">已完成</option>
                  <option :value="3">已作废</option>
                </select>
              </div>
              <div class="filter-item">
                <label class="filter-label">任务名称</label>
                <input v-model="filters.taskName" class="filter-input" placeholder="输入任务名称搜索" @keyup.enter="handleSearch" />
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
                  <th>所属模板</th>
                  <th class="text-center">状态</th>
                  <th>当前处理人</th>
                  <th>进度</th>
                  <th>下发时间</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in taskList" :key="row.id" class="hover-row">
                  <td class="font-bold">{{ row.taskName }}</td>
                  <td>{{ tplName(row.templateId) }}</td>
                  <td class="text-center">
                    <span :class="taskStatusClass(row.status)">{{ taskStatusText(row.status) }}</span>
                  </td>
                  <td>{{ userName(row.currentHandlerId) }}</td>
                  <td>
                    <span class="progress-text">{{ row.finishedNodeCount || 0 }} / {{ row.totalNodeCount || 0 }}</span>
                    <div class="progress-bar"><div class="progress-fill" :style="{ width: progressPercent(row) + '%' }" /></div>
                  </td>
                  <td>{{ row.createTime }}</td>
                  <td class="text-right">
                    <button class="action-link" @click="openTaskHandlers(row)"><i class="el-icon-user" /> 查看人员</button>
                  </td>
                </tr>
                <tr v-if="!loading && taskList.length === 0">
                  <td colspan="7" class="text-center" style="padding: 32px; color: #999;">暂无任务数据</td>
                </tr>
              </tbody>
            </table>
            <!-- 分页 -->
            <div class="pagination">
              <span class="pagination-info">共计 {{ taskTotal }} 条任务</span>
              <div class="pagination-controls">
                <button class="page-btn" :disabled="currentPage === 1" @click="prevPage"><i class="el-icon-arrow-left" /></button>
                <span class="page-current">{{ currentPage }} / {{ totalPages }}</span>
                <button class="page-btn" :disabled="currentPage === totalPages" @click="nextPage"><i class="el-icon-arrow-right" /></button>
              </div>
            </div>
          </div>
        </template>

        <!-- ===== 第二级：任务的处理人员 ===== -->
        <template v-if="level === 2">
          <div v-loading="detailLoading" class="handlers-wrap">
            <div v-if="!detailLoading && handlers.length === 0" class="empty-state">
              <i class="el-icon-user" />
              <p>该任务暂无处理人记录</p>
            </div>
            <div v-else class="handler-grid">
              <div v-for="h in handlers" :key="h.id" class="handler-card">
                <div class="hc-left" @click="openHandlerFlow(h)">
                  <div class="hc-avatar">{{ h.realName ? h.realName.charAt(0) : 'U' }}</div>
                  <div class="hc-info">
                    <div class="hc-name">{{ h.realName }} <span class="hc-emp">{{ h.empNo }}</span></div>
                    <div class="hc-dept">{{ h.deptName || '—' }}</div>
                    <div class="hc-nodes">
                      <span v-for="(n, ni) in h.nodes" :key="ni" class="node-chip" :class="nodeChipClass(n)">{{ n.nodeName }}</span>
                    </div>
                  </div>
                </div>
                <div class="hc-right">
                  <button class="hc-delete" @click="onRemoveHandler(h)"><i class="el-icon-delete" /> 删除</button>
                  <div class="hc-view" @click="openHandlerFlow(h)">
                    <i class="el-icon-arrow-right" />
                    <span class="hc-action">查看流程</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ===== 第三级：流程详情 ===== -->
        <template v-if="level === 3">
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

    <!-- 任务下发弹窗（独立组件） -->
    <DispatchModal
      :visible="dispatchVisible"
      @success="onDispatchSuccess"
      @close="dispatchVisible = false"
    />

    <!-- 临时新增处理人弹窗（加入当前节点并行处理） -->
    <UserPicker
      :visible="addHandlerVisible"
      title="新增处理人（加入当前节点并行处理）"
      :exclude-ids="handlerIdsInTask"
      @confirm="onConfirmAddHandler"
      @close="addHandlerVisible = false"
    />
  </div>
</template>

<script>
import { getTaskList, getTaskDetail, addTaskHandlers, removeTaskHandler } from '@/api/task'
import { getUserList } from '@/api/sysuser'
import { getTemplateList } from '@/api/template'
import { getDataStats } from '@/api/data'
import UserPicker from '@/components/UserPicker'
import DispatchModal from './components/DispatchModal.vue'
import HandlerFlowDetail from './components/HandlerFlowDetail.vue'

export default {
  name: 'DataAdmin',
  components: { DispatchModal, HandlerFlowDetail, UserPicker },
  data() {
    return {
      level: 1,
      // 第一级
      loading: false,
      taskList: [],
      taskTotal: 0,
      currentPage: 1,
      pageSize: 10,
      totalPages: 1,
      stats: {},
      filters: { status: '', taskName: '' },
      // 任务下发弹窗
      dispatchVisible: false,
      // 新增处理人弹窗
      addHandlerVisible: false,
      // 映射
      userMap: {},
      tplMap: {},
      // 第二级
      detailLoading: false,
      selectedTask: null,
      taskDetail: null,
      handlers: [],
      // 第三级
      selectedHandler: null
    }
  },
  computed: {
    headingText() {
      if (this.level === 1) return '任务流转数据后台'
      if (this.level === 2) return '任务处理人员'
      return '流程处理详情'
    },
    /** 是否可新增处理人：进行中或已完成（作废不可加） */
    canAddHandler() {
      return this.selectedTask && (this.selectedTask.status === 1 || this.selectedTask.status === 2)
    },
    /** 当前任务已有处理人ID（新增时排除，避免重复） */
    handlerIdsInTask() {
      return this.handlers.map(h => h.id)
    }
  },
  mounted() {
    this.loadMaps()
    this.fetchTaskList()
    this.fetchStats()
  },
  methods: {
    userName(id) {
      const u = this.userMap[id]
      return u ? u.realName : (id ? '用户' + id : '—')
    },
    tplName(id) {
      const t = this.tplMap[id]
      return t ? t.templateName : '—'
    },
    taskStatusText(s) { return { 1: '进行中', 2: '已完成', 3: '已作废' }[s] || '—' },
    taskStatusClass(s) { return { 1: 'status-chip status-running', 2: 'status-chip status-done', 3: 'status-chip status-cancel' }[s] || '' },
    progressPercent(row) {
      if (!row.totalNodeCount) return 0
      return Math.round(((row.finishedNodeCount || 0) / row.totalNodeCount) * 100)
    },
    nodeChipClass(n) {
      if (n.submitStatus === 0) return 'chip-current'
      if (n.action === 1) return 'chip-rejected'
      return 'chip-done'
    },
    onDispatchSuccess() {
      this.fetchTaskList()
      this.fetchStats()
    },
    /** 新增处理人确认 */
    async onConfirmAddHandler(users) {
      if (!users || users.length === 0) return
      try {
        const handlerIds = users.map(u => u.id)
        const res = await addTaskHandlers({ taskId: this.selectedTask.id, handlerIds })
        this.$message.success(res.message || '新增成功')
        this.addHandlerVisible = false
        await this.refreshHandlers()
      } catch (e) {
        this.$message.error((e && e.message) || '新增失败')
      }
    },
    /** 刷新当前任务的处理人员列表 */
    async refreshHandlers() {
      if (!this.selectedTask) return
      this.detailLoading = true
      try {
        const res = await getTaskDetail(this.selectedTask.id)
        this.taskDetail = res.data
        this.handlers = this.extractHandlers(this.taskDetail.taskNodes || [])
      } catch (e) {
        console.error(e)
      } finally {
        this.detailLoading = false
      }
    },
    /** 删除任务中的某个处理人（连同其所有提交记录） */
    async onRemoveHandler(handler) {
      try {
        await this.$confirm('确定删除该人员？将一并删除其所有提交记录，且不可恢复', '删除确认', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        })
      } catch (e) {
        return // 用户取消
      }
      try {
        const res = await removeTaskHandler(this.selectedTask.id, handler.id)
        this.$message.success(res.message || '删除成功')
        await this.refreshHandlers()
        this.fetchStats()
      } catch (e) {
        this.$message.error((e && e.message) || '删除失败')
      }
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
        if (this.filters.status) params.status = Number(this.filters.status)
        if (this.filters.taskName) params.taskName = this.filters.taskName
        const res = await getTaskList(params)
        this.taskList = res.data.records || []
        this.taskTotal = res.data.total || 0
        this.totalPages = Math.ceil(this.taskTotal / this.pageSize) || 1
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async fetchStats() {
      try {
        const res = await getDataStats()
        this.stats = res.data || {}
      } catch (e) { console.error(e) }
    },
    prevPage() { if (this.currentPage > 1) { this.currentPage--; this.fetchTaskList() } },
    nextPage() { if (this.currentPage < this.totalPages) { this.currentPage++; this.fetchTaskList() } },
    handleSearch() { this.currentPage = 1; this.fetchTaskList() },
    resetFilters() {
      this.filters = { status: '', taskName: '' }
      this.currentPage = 1
      this.fetchTaskList()
    },
    /** 点击任务 → 查看处理人员 */
    async openTaskHandlers(task) {
      this.selectedTask = task
      this.level = 2
      this.detailLoading = true
      this.taskDetail = null
      this.handlers = []
      try {
        const res = await getTaskDetail(task.id)
        this.taskDetail = res.data
        this.handlers = this.extractHandlers(this.taskDetail.taskNodes || [])
      } catch (e) {
        console.error(e)
      } finally {
        this.detailLoading = false
      }
    },
    /** 从 taskNodes 提取处理人列表（按 handlerUserId 去重） */
    extractHandlers(taskNodes) {
      const map = {}
      const order = []
      taskNodes.forEach(tn => {
        if (!tn.handlerUserId) return
        if (!map[tn.handlerUserId]) {
          const u = this.userMap[tn.handlerUserId] || {}
          map[tn.handlerUserId] = {
            id: tn.handlerUserId,
            realName: tn.handlerName || u.realName || '—',
            empNo: u.empNo || '—',
            deptName: u.deptName || '—',
            nodes: []
          }
          order.push(tn.handlerUserId)
        }
        map[tn.handlerUserId].nodes.push({
          nodeName: tn.nodeName,
          nodeType: tn.nodeType,
          submitStatus: tn.submitStatus,
          action: tn.action,
          handleTime: tn.handleTime
        })
      })
      return order.map(id => map[id])
    },
    /** 点击人员 → 查看流程详情 */
    openHandlerFlow(handler) {
      this.selectedHandler = handler
      this.level = 3
    },
    goBack() {
      if (this.level === 3) this.backToHandlers()
      else if (this.level === 2) this.backToTasks()
    },
    backToTasks() {
      this.level = 1
      this.selectedTask = null
      this.taskDetail = null
      this.handlers = []
      this.selectedHandler = null
    },
    backToHandlers() {
      this.level = 2
      this.selectedHandler = null
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
$border: #e4beba;
.dashboard-container { display: flex; min-height: 100vh; background: #F5F7FA; font-family: 'Inter', sans-serif; color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; color: #414755; margin-bottom: 8px; align-items: center;
  .active { color: $primary; font-weight: 600; }
  .link { color: $primary; cursor: pointer;
    &:hover { text-decoration: underline; }
  }
}
.page-heading { font-size: 30px; line-height: 38px; font-weight: 600; color: #1b1c1c; }
.btn-refresh { display: flex; align-items: center; gap: 4px; padding: 8px 16px; background: #fff; border: 1px solid $border; border-radius: 6px; color: #5b403d; cursor: pointer; font-size: 13px;
  &:hover { background: #f6f3f2; }
}
.header-actions { display: flex; gap: 8px; align-items: center; }
.btn-dispatch { display: flex; align-items: center; gap: 4px; padding: 8px 18px; background: $primary; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { opacity: 0.9; }
}

// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  @media (max-width: 1100px) { grid-template-columns: repeat(2, 1fr); }
}
.stat-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid $border; border-radius: 8px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; flex-shrink: 0;
  &.icon-total { background: $primary; }
  &.icon-running { background: #b7791f; }
  &.icon-done { background: #266d00; }
  &.icon-todo { background: #00596f; }
}
.stat-body { flex: 1; }
.stat-label { font-size: 13px; color: #757575; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }

// 筛选
.filter-section { background: #fff; border: 1px solid $border; border-radius: 8px; padding: 16px; }
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
.filter-actions { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(228,190,186,0.3); }
.filter-actions-right { display: flex; gap: 8px; margin-left: auto; }
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid $border; border-radius: 4px; font-size: 13px; color: #5b403d; background: #fff; cursor: pointer;
  &:hover { background: #f6f3f2; }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 4px; font-size: 13px; font-weight: bold; color: #fff; background: $primary; cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { opacity: 0.9; }
}

// 表格
.table-card { background: #fff; border: 1px solid $border; border-radius: 8px; overflow: hidden; position: relative; }
.loading-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(255,255,255,0.9); display: flex; align-items: center; justify-content: center; z-index: 10; }
.loading-spinner { text-align: center; color: $primary;
  .el-icon-loading { font-size: 40px; display: block; margin-bottom: 8px; }
  p { font-size: 14px; color: #606266; margin: 0; }
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.spinning { animation: spin 1s linear infinite; }
.data-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 12px 16px; font-weight: 700; color: #414755; background: #FAFAFA; border-bottom: 1px solid $border; }
  td { padding: 12px 16px; border-bottom: 1px solid $border; }
  .hover-row:hover { background: #FFF5F5; }
}
.font-bold { font-weight: 700; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.status-chip { padding: 2px 8px; border-radius: 2px; font-size: 12px; font-weight: 600; display: inline-flex; border: 1px solid transparent; }
.status-running { background: rgba(197,48,48,0.1); border-color: $primary; color: $primary; }
.status-done { background: rgba(38,109,0,0.1); border-color: #266d00; color: #266d00; }
.status-cancel { background: rgba(186,26,26,0.1); border-color: #ba1a1a; color: #ba1a1a; }
.progress-text { font-size: 12px; color: #757575; }
.progress-bar { width: 80px; height: 6px; background: #f0f0f0; border-radius: 3px; margin-top: 4px; overflow: hidden; }
.progress-fill { height: 100%; background: $primary; border-radius: 3px; transition: width .3s; }
.action-link { color: $primary; background: none; border: none; cursor: pointer; font-size: 14px; display: inline-flex; align-items: center; gap: 3px;
  &:hover { text-decoration: underline; }
}
.pagination { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #faf9f9; border-top: 1px solid $border; }
.pagination-info { font-size: 12px; color: #414755; }
.pagination-controls { display: flex; align-items: center; gap: 8px; }
.page-btn { width: 32px; height: 32px; border-radius: 4px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; cursor: pointer; font-size: 14px;
  &:hover:not(:disabled) { background: #efeded; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.page-current { font-size: 13px; color: #414755; }

// 第二级：处理人员
.handlers-wrap { min-height: 200px; }
.empty-state { text-align: center; padding: 60px 20px; color: #bbb;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
.handler-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;
  @media (max-width: 900px) { grid-template-columns: 1fr; }
}
.handler-card { display: flex; justify-content: space-between; align-items: center; background: #fff; border: 1px solid $border; border-radius: 8px; padding: 16px 18px; cursor: pointer; transition: all .2s;
  &:hover { box-shadow: 0 3px 10px rgba(197,48,48,0.12); border-color: $primary; }
}
.hc-left { display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0; }
.hc-avatar { width: 44px; height: 44px; border-radius: 50%; background: $primary; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 600; flex-shrink: 0; }
.hc-info { flex: 1; min-width: 0; }
.hc-name { font-size: 15px; font-weight: 700; color: #1b1c1c; }
.hc-emp { font-size: 12px; color: #757575; font-weight: 400; margin-left: 6px; font-family: monospace; }
.hc-dept { font-size: 12px; color: #757575; margin-top: 2px; }
.hc-nodes { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
.node-chip { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 600; }
.chip-done { background: rgba(38,109,0,0.1); color: #266d00; }
.chip-current { background: rgba(197,48,48,0.12); color: $primary; }
.chip-rejected { background: rgba(183,121,31,0.15); color: #b7791f; }
.hc-right { display: flex; flex-direction: column; align-items: center; gap: 6px; color: $primary; flex-shrink: 0; margin-left: 12px; }
.hc-view { display: flex; flex-direction: column; align-items: center; cursor: pointer;
  i { font-size: 18px; }
}
.hc-action { font-size: 11px; margin-top: 2px; }
.hc-delete { display: flex; align-items: center; gap: 2px; padding: 2px 8px; background: none; border: 1px solid #e4beba; border-radius: 4px; color: #ba1a1a; cursor: pointer; font-size: 11px;
  &:hover { background: #FFF5F5; border-color: #ba1a1a; }
  i { font-size: 12px; }
}

// 第三级：流程详情
.flow-detail-wrap { display: flex; flex-direction: column; gap: 16px; }
.handler-bar { display: flex; align-items: center; gap: 14px; background: #fff; border: 1px solid $border; border-radius: 8px; padding: 16px 20px; }
.hb-avatar { width: 48px; height: 48px; border-radius: 50%; background: $primary; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 600; flex-shrink: 0; }
.hb-info { flex: 1; }
.hb-name { font-size: 16px; font-weight: 700; color: #1b1c1c; }
.hb-emp { font-size: 12px; color: #757575; font-weight: 400; margin-left: 6px; font-family: monospace; }
.hb-dept { font-size: 12px; color: #757575; margin-top: 2px; }
.hb-tip { font-size: 12px; color: #999; }

// 流程链
.chain-section { background: #fff; border: 1px solid $border; border-radius: 8px; padding: 20px; }
.section-title { font-size: 15px; font-weight: 700; color: $primary; margin-bottom: 16px; }
.chain-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 8px; }
.chain-track { display: flex; flex-direction: column; gap: 10px; }
.chain-step { border: 1px solid #ebeef5; border-radius: 8px; padding: 12px 14px; background: #fff; transition: all .2s;
  &.st-done { border-color: rgba(38,109,0,0.3); background: rgba(38,109,0,0.03); }
  &.st-current { border-color: $primary; background: #FFF5F5; box-shadow: 0 0 0 2px rgba(197,48,48,0.1); }
  &.st-rejected { border-color: rgba(183,121,31,0.4); background: rgba(183,121,31,0.05); }
  &.st-pending { opacity: 0.55; background: #f7f7f7; }
  &.mine { box-shadow: 0 0 0 2px rgba(197,48,48,0.25); }
  &.clickable { cursor: pointer;
    &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
  }
  &.expanded { box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
}
.step-head { display: flex; align-items: center; gap: 10px; }
.step-no { width: 22px; height: 22px; border-radius: 50%; background: #e4beba; color: #5b403d; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.st-current .step-no { background: $primary; color: #fff; }
.st-done .step-no { background: #266d00; color: #fff; }
.st-rejected .step-no { background: #b7791f; color: #fff; }
.step-name { font-size: 14px; font-weight: 600; color: #1b1c1c; flex: 1; }
.step-badge { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; }
.badge-done { background: rgba(38,109,0,0.12); color: #266d00; }
.badge-current { background: rgba(197,48,48,0.12); color: $primary; }
.badge-rejected { background: rgba(183,121,31,0.15); color: #b7791f; }
.badge-pending { background: #e8e8e8; color: #999; }
.mine-tag { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 600; background: $primary; color: #fff; }
.step-meta { display: flex; gap: 16px; margin-top: 6px; padding-left: 32px; font-size: 12px; color: #757575;
  i { margin-right: 3px; }
  .meta-pending { color: #bbb; font-style: italic; }
  .meta-reject { color: #b7791f; font-weight: 600; }
}
.step-form { margin-top: 10px; padding: 10px 12px; background: #fff; border-radius: 6px; border: 1px dashed #e4beba; }
.form-row { display: flex; padding: 5px 0; font-size: 13px; border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.fr-label { width: 130px; color: #757575; flex-shrink: 0; }
.fr-value { color: #1b1c1c; flex: 1; word-break: break-all; }
.form-empty { font-size: 12px; color: #bbb; text-align: center; padding: 8px; }
</style>
