<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span>系统首页</span>
              <span>/</span>
              <span class="active">任务处理</span>
            </nav>
            <h3 class="page-heading">我的任务</h3>
          </div>
          <div class="header-actions">
            <button class="btn-refresh" @click="refresh"><i class="el-icon-refresh" /> 刷新</button>
          </div>
        </div>

        <!-- 统计卡 -->
        <section class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon icon-total"><i class="el-icon-s-order" /></div>
            <div class="stat-body">
              <div class="stat-label">我的任务</div>
              <div class="stat-value">{{ stats.taskCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-pending"><i class="el-icon-alarm-clock" /></div>
            <div class="stat-body">
              <div class="stat-label">待处理节点</div>
              <div class="stat-value">{{ stats.pendingNodeCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-done"><i class="el-icon-circle-check" /></div>
            <div class="stat-body">
              <div class="stat-label">已完成节点</div>
              <div class="stat-value">{{ stats.doneNodeCount || 0 }}</div>
            </div>
          </div>
        </section>

        <!-- 筛选区 -->
        <section class="filter-section">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="filter-label">任务名称</label>
              <input v-model="filters.taskName" class="filter-input" placeholder="请输入任务名称" @keyup.enter="handleSearch">
            </div>
            <div class="filter-item">
              <label class="filter-label">处理状态</label>
              <select v-model="filters.status" class="filter-select">
                <option value="">全部</option>
                <option :value="1">待处理</option>
                <option :value="0">已完成</option>
              </select>
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

        <!-- 提示条 -->
        <section class="tip-bar">
          <i class="el-icon-info" />
          展开任务可查看各期次待办节点；点「处理」进入办理，点「查看详情」回看历史提交。
        </section>

        <!-- 任务 → 期次 两级折叠面板（卡片式，同任务管理） -->
        <div v-if="!loading && list.length === 0" class="empty-state">
          <i class="el-icon-finished" />
          <p>暂无待办任务</p>
        </div>
        <div v-else class="task-collapse">
          <div v-if="loading" class="loading-bar"><i class="el-icon-loading" /> 加载中...</div>
          <div v-for="g in list" :key="g.taskId" class="task-panel" :class="{ 'is-open': isTaskOpen(g.taskId) }">
            <div class="tp-head" @click="toggleTask(g)">
              <div class="ct-icon"><i class="el-icon-s-order" /></div>
              <div class="ct-main">
                <div class="ct-name-row">
                  <span class="ct-name">{{ g.taskName }}</span>
                  <span v-if="g.pendingCount > 0" class="pending-tag">{{ g.pendingCount }} 待处理</span>
                </div>
                <div class="ct-sub">
                  <template v-if="g.templateName">{{ g.templateName }} · </template>
                  {{ g.periodCount || 0 }} 期次 · 创建时间 {{ g.taskCreateTime }}
                </div>
              </div>
              <i class="el-icon-arrow-down tp-arrow" />
            </div>

            <!-- 展开内容：期次（一行一期次，未完成可处理 / 已完成查看详情） -->
            <div v-show="isTaskOpen(g.taskId)" class="tp-body">
              <div class="task-detail">
                <div v-if="g.periods.length === 0" class="period-empty"><i class="el-icon-tickets" /> 暂无期次任务</div>
                <section v-else class="period-section">
                  <div class="sec-title"><i class="el-icon-tickets" /> 期次列表 <span class="sec-sub">共 {{ g.periods.length }} 期</span></div>
                  <table class="period-table">
                    <thead>
                      <tr>
                        <th>期次</th>
                        <th>起止时间</th>
                        <th class="text-center">状态</th>
                        <th>流程进度</th>
                        <th class="text-right">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="per in g.periods" :key="per.dispatchId || per.periodName || 'none'" class="hover-row">
                        <td class="font-bold">
                          {{ per.periodName || '无期次' }}
                          <span v-if="per.periodNo" class="period-no">第 {{ per.periodNo }} 期</span>
                        </td>
                        <td>{{ per.startTime ? per.startTime + ' ~ ' + (per.endTime || '—') : '—' }}</td>
                        <td class="text-center">
                          <span class="status-badge" :class="periodStatusClass(per)">{{ periodStatusText(per) }}</span>
                        </td>
                        <td>
                          <!-- 完整流程节点链（chip 样式，同期次人员查看） -->
                          <div class="node-chain">
                            <template v-for="(nd, i) in (per.nodes || [])">
                              <span
                                :key="'n' + i"
                                class="node-chip"
                                :class="nodeChipClass(nd.status)"
                                :title="nd.nodeName + '（' + (nd.status === 1 ? '已完成' : (nd.status === 2 ? '进行中' : '未开始')) + '）'"
                              >{{ i + 1 }}.{{ nd.nodeName }}</span>
                            </template>
                            <span v-if="!per.nodes || per.nodes.length === 0" class="text-muted">—</span>
                          </div>
                        </td>
                        <td class="text-right">
                          <button v-if="periodPending(per)" class="btn-process" @click="openProcess(periodPending(per))"><i class="el-icon-s-claim" /> 处理</button>
                          <button v-else-if="periodCurrentTodo(per)" class="btn-view" @click="openProcess(periodCurrentTodo(per))"><i class="el-icon-view" /> 查看详情</button>
                          <span v-else class="text-muted">—</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </section>
              </div>
            </div>
          </div>

          <!-- 分页（默认每页 5 条任务，展开查看任务下所有期次；可切换每页任务数，最小 5） -->
          <div class="pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="total"
              :current-page.sync="currentPage"
              :page-size="pageSize"
              :page-sizes="[5, 10, 20]"
              @size-change="handleSizeChange"
              @current-change="fetchData"
            />
          </div>
        </div>
      </section>
    </main>

    <!-- 处理弹窗（独立组件） -->
    <ProcessDialog
      :visible="processVisible"
      :todo="currentTodo"
      :detail="taskDetail"
      :submitting="submitting"
      :drafting="drafting"
      @close="onCloseProcess"
      @submit="handleSubmit"
      @draft="handleDraft"
    />
  </div>
</template>

<script>
import { getMyTodoGrouped, getMyTodoStats, getTaskDetail, submitTask, saveDraftTask } from '@/api/task'
import ProcessDialog from './components/ProcessDialog.vue'

export default {
  name: 'TaskProcess',
  components: { ProcessDialog },
  data() {
    return {
      loading: false,
      submitting: false,
      drafting: false,
      list: [],
      total: 0,
      currentPage: 1,
      // 每页任务数（最小 5）
      pageSize: 5,
      // 统计卡
      stats: {},
      // 筛选条件
      filters: { taskName: '', status: '' },
      // 展开的任务（可多个同时展开）
      openTaskIds: [],
      // 处理弹窗
      processVisible: false,
      currentTodo: null,
      taskDetail: null
    }
  },
  mounted() {
    this.fetchData()
    this.fetchStats()
  },
  methods: {
    nodeTypeText(t) { return { 1: '开始', 2: '', 3: '结束' }[t] || '' },
    nodeTypeClass(t) { return { 1: 'badge-start', 3: 'badge-end' }[t] || 'badge-mid' },
    /** 期次下待处理的节点（第一条未处理） */
    periodPending(per) { return (per.todos || []).find(t => t.todoStatus === 0) || null },
    /** 期次下展示的当前节点：待处理优先，否则取最后一条已处理记录 */
    periodCurrentTodo(per) {
      const pending = this.periodPending(per)
      if (pending) return pending
      const todos = per.todos || []
      return todos.length ? todos[todos.length - 1] : null
    },
    periodStatusText(per) { return this.periodPending(per) ? '待处理' : '已完成' },
    periodStatusClass(per) { return this.periodPending(per) ? 'st-todo' : 'st-done' },
    /** 节点状态→chip 样式：1已完成 / 2进行中(当前) / 0未开始 */
    nodeChipClass(s) {
      return { 1: 'chip-done', 2: 'chip-current', 0: 'chip-pending' }[s] || 'chip-pending'
    },
    isTaskOpen(id) { return this.openTaskIds.indexOf(id) >= 0 },
    toggleTask(g) {
      const idx = this.openTaskIds.indexOf(g.taskId)
      this.openTaskIds = idx >= 0
        ? this.openTaskIds.filter(id => id !== g.taskId)
        : this.openTaskIds.concat(g.taskId)
    },
    /** 每页任务数变化：回到第 1 页重新加载 */
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchData()
    },
    async fetchData() {
      this.loading = true
      try {
        const params = { page: this.currentPage, limit: this.pageSize }
        if (this.filters.taskName && this.filters.taskName.trim()) params.taskName = this.filters.taskName.trim()
        if (this.filters.status !== '') params.status = Number(this.filters.status)
        const res = await getMyTodoGrouped(params)
        this.list = res.data.records || []
        this.total = res.data.total || 0
        // 清理已不存在任务的展开状态
        const ids = this.list.map(g => g.taskId)
        this.openTaskIds = this.openTaskIds.filter(id => ids.indexOf(id) >= 0)
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async fetchStats() {
      try {
        const res = await getMyTodoStats()
        this.stats = res.data || {}
      } catch (e) {
        console.error(e)
      }
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchData()
    },
    resetFilters() {
      this.filters = { taskName: '', status: '' }
      this.currentPage = 1
      this.fetchData()
    },
    refresh() {
      this.fetchData()
      this.fetchStats()
    },
    async openProcess(todo) {
      this.currentTodo = todo
      this.processVisible = true
      this.taskDetail = null
      try {
        const res = await getTaskDetail(todo.taskId)
        this.taskDetail = res.data
      } catch (e) {
        console.error(e)
      }
    },
    onCloseProcess() {
      this.processVisible = false
      this.currentTodo = null
      this.taskDetail = null
    },
    async handleSubmit(payload) {
      this.submitting = true
      try {
        await submitTask(payload)
        const isReject = payload.action === 'reject'
        const isEnd = this.currentTodo && this.currentTodo.nodeType === 3
        this.$message.success(isReject ? '已退回到目标节点，表单已回填上次数据' : (isEnd ? '已提交，任务已完成' : '提交成功，已流转至下一节点'))
        this.processVisible = false
        this.fetchData()
      } catch (e) {
        console.error(e)
      } finally {
        this.submitting = false
      }
    },
    /** 暂存（保存草稿，不流转） */
    async handleDraft(payload) {
      this.drafting = true
      try {
        await saveDraftTask(payload)
        this.$message.success('已暂存，可随时继续填写')
      } catch (e) {
        console.error(e)
      } finally {
        this.drafting = false
      }
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
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
}
.page-heading { font-size: 30px; line-height: 38px; font-weight: 600; color: #1b1c1c; }
.header-actions { display: flex; align-items: center; gap: 10px; }

// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
  @media (max-width: 900px) { grid-template-columns: 1fr; }
}
.stat-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid $border; border-radius: 10px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon { width: 48px; height: 48px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; flex-shrink: 0;
  &.icon-total { background: $primary; }
  &.icon-pending { background: #b7791f; }
  &.icon-done { background: #266d00; }
}
.stat-body { flex: 1; }
.stat-label { font-size: 13px; color: #757575; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }

// 筛选区
.filter-section { background: #fff; border: 1px solid $border; border-radius: 10px; padding: 16px; }
.filter-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 13px; color: #757575; }
.filter-select, .filter-input { height: 36px; border: 1px solid $border; border-radius: 6px; padding: 0 10px; font-size: 13px; outline: none; background: #fff; transition: all .2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(197,48,48,0.12); }
}
.filter-actions { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(228,190,186,0.3); }
.filter-actions-right { display: flex; gap: 8px; }
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid $border; border-radius: 6px; font-size: 13px; color: #5b403d; background: #fff; cursor: pointer;
  &:hover { background: #f6f3f2; }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 6px; font-size: 13px; font-weight: bold; color: #fff; background: $primary; cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { opacity: 0.9; }
}
.btn-refresh { display: flex; align-items: center; gap: 5px; padding: 10px 16px; background: #fff; border: 1px solid $border; border-radius: 8px; color: #5b403d; font-weight: 600; font-size: 13px; cursor: pointer; transition: all .2s;
  i { color: $primary; }
  &:hover { border-color: $primary; color: $primary; background: #FFF9F9; }
}
.tip-bar { display: flex; align-items: center; gap: 8px; background: #FFF5F5; border: 1px solid $border; color: #8a4b46; font-size: 13px; border-radius: 10px; padding: 10px 14px;
  i { color: $primary; }
}
.empty-state { text-align: center; padding: 60px 20px; color: #bbb; background: #fff; border: 1px solid $border; border-radius: 10px;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
.task-collapse { position: relative; }
.loading-bar { display: flex; align-items: center; gap: 6px; justify-content: center; padding: 16px; color: $primary; font-size: 13px; }

// 折叠面板（卡片式，同任务管理）
.task-panel { background: #fff; border: 1px solid $border; border-radius: 10px; margin-bottom: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.03); }
.tp-head { display: flex; align-items: center; gap: 12px; padding: 14px 18px; cursor: pointer; transition: background .2s;
  &:hover { background: #FFF9F9; }
}
.tp-arrow { color: #909399; font-size: 14px; flex-shrink: 0; transition: transform .25s; }
.task-panel.is-open .tp-arrow { transform: rotate(-180deg); }
.tp-body { border-top: 1px solid $border; }
.ct-icon { width: 40px; height: 40px; border-radius: 10px; background: #FFF5F5; color: $primary; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.ct-main { flex: 1; min-width: 0; }
.ct-name-row { display: flex; align-items: center; gap: 8px; min-width: 0; }
.ct-name { font-size: 15px; font-weight: 700; color: #1b1c1c; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pending-tag { padding: 2px 10px; background: rgba(197,48,48,0.1); color: $primary; border-radius: 10px; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.ct-sub { font-size: 12px; color: #909399; margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.task-detail { padding: 16px; display: flex; flex-direction: column; gap: 16px; }
// 期次块（同任务管理期次列表样式）
.period-section { background: #fff; border: 1px solid $border; border-radius: 10px; padding: 14px 16px; }
.sec-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; color: #1b1c1c; margin-bottom: 12px;
  i { color: $primary; }
}
.sec-sub { font-size: 12px; color: #999; font-weight: 400; }
.period-no { font-size: 11px; color: $primary; background: rgba(197,48,48,0.1); border-radius: 8px; padding: 1px 8px; font-weight: 600; margin-left: 6px; }
.period-empty { text-align: center; padding: 24px; color: #bbb; font-size: 13px;
  i { margin-right: 4px; }
}
.font-bold { font-weight: 700; }
.text-muted { color: #bbb; }
.text-center { text-align: center; }
.period-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 10px 12px; font-weight: 700; color: #414755; background: #FAFAFA; border-bottom: 1px solid $border; font-size: 13px; }
  td { padding: 10px 12px; border-bottom: 1px solid $border; font-size: 13px; }
  tbody tr:last-child td { border-bottom: none; }
  .hover-row:hover { background: #FFF5F5; }
}
.text-center { text-align: center; }
.text-right { text-align: right; }
.node-badge { padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 600; margin-right: 4px; }
.badge-start { background: rgba(38,109,0,0.1); color: #266d00; }
.badge-mid { background: #f0f3ff; color: #545f72; }
.badge-end { background: rgba(197,48,48,0.1); color: $primary; }
.btn-process { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: $primary; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { opacity: 0.9; }
}
.btn-view { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: #fff; color: #545f72; border: 1px solid #d8dee9; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { background: #f0f3ff; border-color: #b7c3d8; }
}
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 600;
  &.st-todo { background: rgba(197,48,48,0.1); color: $primary; }
  &.st-done { background: rgba(38,109,0,0.1); color: #266d00; }
}

// 流程节点链（chip 样式，参考期次人员查看）
.node-chain { display: flex; align-items: center; gap: 4px; overflow-x: auto; max-width: 480px; padding-bottom: 2px;
  &::-webkit-scrollbar { height: 3px; }
  &::-webkit-scrollbar-thumb { background: #e0d2cf; border-radius: 3px; }
}
.node-chip { flex-shrink: 0; padding: 2px 9px; border-radius: 4px; font-size: 11px; font-weight: 600; line-height: 1.6; white-space: nowrap;
  &.chip-done { background: #266d00; color: #fff; }
  &.chip-current { background: rgba(197,48,48,0.14); color: $primary; border: 1px solid rgba(197,48,48,0.4); }
  &.chip-pending { background: #f0f0f0; color: #aaa; }
}

// 分页（同任务管理）
.pagination { display: flex; justify-content: flex-end; align-items: center; padding: 14px 16px; background: #fff; border: 1px solid $border; border-radius: 10px; margin-top: 12px; }
</style>
