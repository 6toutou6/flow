<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span class="link" @click="goBackTaskManage">任务管理</span>
              <span>/</span>
              <span class="active">期次任务关联</span>
            </nav>
            <h3 class="page-heading">期次任务关联</h3>
          </div>
          <div class="header-actions">
            <button class="btn-refresh" @click="refresh"><i class="el-icon-refresh" /> 刷新</button>
          </div>
        </div>

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
                <option :value="1">待我处理</option>
                <option :value="0">已完成</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">创建开始</label>
              <input v-model="filters.createStart" type="date" class="filter-input">
            </div>
            <div class="filter-item">
              <label class="filter-label">创建结束</label>
              <input v-model="filters.createEnd" type="date" class="filter-input">
            </div>
          </div>
          <div class="filter-actions">
            <div class="filter-actions-left">
              <button class="btn-create" @click="addVisible = true"><i class="el-icon-plus" /> 新建关联</button>
            </div>
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
          展开任务可查看各期次待办节点；点「处理」进入办理页，点「查看详情」回看历史提交与完整流程。
        </section>

        <!-- 任务 → 期次 两级折叠面板（卡片式，同任务管理） -->
        <div v-if="!loading && list.length === 0" class="empty-state">
          <i class="el-icon-finished" />
          <p>暂无关联任务</p>
        </div>
        <div v-else class="task-collapse">
          <div v-if="loading" class="loading-bar"><i class="el-icon-loading" /> 加载中...</div>
          <div v-for="g in list" :key="g.taskId" class="task-panel" :class="{ 'is-open': isTaskOpen(g.taskId) }">
            <div class="tp-head" @click="toggleTask(g)">
              <div class="ct-icon"><i class="el-icon-s-order" /></div>
              <div class="ct-main">
                <span class="ct-name" :title="g.taskName">{{ g.taskName }}</span>
                <span v-if="g.pendingCount > 0" class="pending-tag">{{ g.pendingCount }} 待处理</span>
                <span class="ct-meta" :title="ctMetaText(g)">{{ ctMetaText(g) }}</span>
              </div>
              <i class="el-icon-arrow-down tp-arrow" />
            </div>

            <!-- 展开内容：期次 → 待办（沿用原表格样式；同一期次多条不同子任务待办各自独立一行，流程进度各按自己任务展示） -->
            <div v-show="isTaskOpen(g.taskId)" class="tp-body">
              <div class="task-detail">
                <div v-if="g.periods.length === 0" class="period-empty"><i class="el-icon-tickets" /> 暂无期次任务</div>
                <section v-else class="period-section">
                  <div class="sec-title"><i class="el-icon-tickets" /> 期次列表 <span class="sec-sub">共 {{ g.periods.length }} 期 · {{ g.pendingCount }} 待处理</span></div>
                  <table class="period-table">
                    <thead>
                      <tr>
                        <th>期次</th>
                        <th>任务名称</th>
                        <th>起止时间</th>
                        <th class="text-center">状态</th>
                        <th>流程进度</th>
                        <th class="text-right">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <template v-for="per in g.periods">
                        <!-- 无筛选时：无待办的期次给一行占位提示；有筛选时该期次无匹配行则不展示 -->
                        <tr v-if="!statActive('pending') && !statActive('done') && per.todos && per.todos.length === 0" :key="(per.dispatchId || per.periodName || 'none') + '-empty'" class="hover-row">
                          <td class="font-bold">{{ per.periodName || '无期次' }}</td>
                          <td class="text-muted">—</td>
                          <td>{{ per.startTime ? per.startTime + ' ~ ' + (per.endTime || '—') : '—' }}</td>
                          <td colspan="4" class="text-muted">该期次无待办任务</td>
                        </tr>
                        <!-- 每条待办 = 完完整整独立一行（不合并单元格），各自进度链与操作；
                             点「待我处理/已完成」卡后仅保留匹配行（其余期次/行不再展示） -->
                        <tr
                          v-for="(td, tdi) in visibleTodos(per)"
                          :key="td.taskNodeId || ((per.dispatchId || per.periodName || 'none') + '-' + tdi)"
                          class="hover-row"
                        >
                          <td class="font-bold">
                            {{ per.periodName || '无期次' }}
                            <span v-if="per.periodNo" class="period-no">第 {{ per.periodNo }} 期</span>
                            <span class="ptag" :class="perTagCls(per)">{{ perTagText(per) }}</span>
                          </td>
                          <td>
                            <!-- 员工任务名称（该待办所属成员任务实例名，如「技术部-张三的问题整改处理」） -->
                            <span class="emp-task-name" :title="td.taskName">{{ td.taskName || '—' }}</span>
                          </td>
                          <td>
                            {{ per.startTime ? per.startTime + ' ~ ' + (per.endTime || '—') : '—' }}
                          </td>
                          <td class="text-center">
                            <span class="status-badge" :class="td.todoStatus === 0 ? 'st-todo' : 'st-done'">{{ td.todoStatus === 0 ? '待我处理' : '已完成' }}</span>
                            <span v-if="rowOverdueTag(td, per)" class="overdue-tag"><i class="el-icon-alarm-clock" /> {{ rowOverdueTag(td, per) }}</span>
                          </td>
                          <td>
                            <!-- 该待办所属任务实例的流程进度（不同子任务各自展示） -->
                            <div class="node-chain">
                              <template v-for="(nd, i) in (td.chains || per.nodes || [])">
                                <span
                                  :key="'n' + i"
                                  class="node-chip"
                                  :class="nodeChipClass(nd.status)"
                                  :title="nd.nodeName + '（' + (nd.status === 1 ? '已完成' : (nd.status === 2 ? '进行中' : '未开始')) + '）'"
                                >{{ i + 1 }}.{{ nd.nodeName }}</span>
                              </template>
                              <span v-if="!(td.chains || []).length && !(per.nodes || []).length" class="text-muted">—</span>
                            </div>
                          </td>
                          <td class="text-right">
                            <button v-if="td.todoStatus === 0" class="btn-process" @click="openProcess(td, per, g)"><i class="el-icon-s-claim" /> 处理</button>
                            <button v-else class="btn-view" @click="openProcess(td, per, g)"><i class="el-icon-view" /> 查看详情</button>
                          </td>
                        </tr>
                      </template>
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
              @current-change="applyPage"
            />
          </div>
        </div>
      </section>
    </main>

    <!-- 新建关联弹窗 -->
    <AddTaskLinkModal
      :visible="addVisible"
      :source-dispatch-name="sourceTaskName"
      :source-period-id="sourcePeriodId"
      :source-period-name="sourcePeriodName"
      @success="onLinkAdded"
      @close="addVisible = false"
    />
  </div>
</template>

<script>
import { getMyTodoGrouped, getMyTodoStats } from '@/service/sys/TaskService'
import { getTaskLinksBySource } from '@/service/sys/FlowDispatchService'
import { NODE_TYPE } from '@/constants/dict'
import AddTaskLinkModal from '@/components/AddTaskLinkModal.vue'

export default {
  name: 'TaskProcess',
  components: { AddTaskLinkModal },
  data() {
    return {
      loading: false,
      list: [],
      // 过滤后的全部关联任务组（分页切片前）
      allList: [],
      total: 0,
      currentPage: 1,
      // 每页任务数（最小 5）
      pageSize: 5,
      // 统计卡
      stats: {},
      // 筛选条件
      filters: { taskName: '', status: '', taskType: '', templateName: '', createStart: '', createEnd: '' },
      // 统计卡筛选（待处理/已完成同维互斥；点「我的任务」恢复全部）
      activeStats: [],
      // 展开的任务（可多个同时展开）
      openTaskIds: [],
      // 新建关联弹窗
      addVisible: false
    }
  },
  computed: {
    /** 来源任务配置ID（路由带参） */
    sourceDispatchId() {
      return this.$route.query.dispatchId || ''
    },
    /** 来源任务配置名（路由带参） */
    sourceTaskName() {
      return this.$route.query.taskName || ''
    },
    /** 来源期次ID（路由带参） */
    sourcePeriodId() {
      return this.$route.query.periodId || ''
    },
    /** 来源期次名（路由带参） */
    sourcePeriodName() {
      return this.$route.query.periodName || ''
    }
  },
  mounted() {
    this.fetchData()
    this.fetchStats()
  },
  methods: {
    /** 返回任务管理 */
    goBackTaskManage() {
      this.$router.push('/flow-dispatch/index')
    },
    /** 新建关联成功：关闭弹窗并刷新 */
    onLinkAdded() {
      this.addVisible = false
      this.fetchData()
    },
    nodeTypeText(t) { return { [NODE_TYPE.START]: '开始', [NODE_TYPE.END]: '结束' }[t] || '' },
    nodeTypeClass(t) { return { [NODE_TYPE.START]: 'badge-start', [NODE_TYPE.END]: 'badge-end' }[t] || 'badge-mid' },
    /** 节点状态→chip 样式：1已完成 / 2进行中(当前) / 0未开始 */
    nodeChipClass(s) {
      return { 1: 'chip-done', 2: 'chip-current', 0: 'chip-pending' }[s] || 'chip-pending'
    },
    /** 统计卡是否选中：全部卡 = 无任何分类筛选时高亮 */
    statActive(k) {
      if (k === 'all') return this.activeStats.length === 0
      return this.activeStats.indexOf(k) >= 0
    },
    /** 点击统计卡：待处理/已完成同维互斥，再点取消；点「我的任务」恢复全部 */
    toggleStat(k) {
      if (k === 'all') {
        this.activeStats = []
      } else {
        if (k === 'pending' || k === 'done') {
          this.activeStats = this.activeStats.filter(x => x !== 'pending' && x !== 'done')
        }
        const i = this.activeStats.indexOf(k)
        if (i >= 0) this.activeStats.splice(i, 1)
        else this.activeStats.push(k)
      }
      // 与状态下拉同源（1待处理 / 0已完成 / 空=全部）
      this.filters.status = this.activeStats.indexOf('pending') >= 0 ? 1 : (this.activeStats.indexOf('done') >= 0 ? 0 : '')
      this.currentPage = 1
      this.fetchData()
    },
    /**
     * 行级超期标：待处理行 → 当前已超截止显示「已超期」；已处理行 → 处理时间晚于截止显示「超期完成」；否则不显示
     */
    rowOverdueTag(td, per) {
      if (!per || !per.endTime) return ''
      const end = new Date(String(per.endTime).replace(/-/g, '/'))
      if (isNaN(end.getTime())) return ''
      if (td.todoStatus === 0) {
        // 待处理：仍可正常处理，仅标识超期
        return new Date() > end ? '已超期' : ''
      }
      // 已处理：与截止时间对比是否为超期完成
      if (!td.handleTime) return ''
      const fin = new Date(String(td.handleTime).replace(/-/g, '/'))
      if (isNaN(fin.getTime())) return ''
      return fin.getTime() > end.getTime() ? '超期完成' : ''
    },
    /** 任务组头单行摘要（模板 · N 期次 · 创建时间） */
    ctMetaText(g) {
      const tpl = g.templateName ? g.templateName + ' · ' : ''
      return `${tpl}${g.periodCount || 0} 期次 · 创建 ${g.taskCreateTime || '—'}`
    },
    /** 期次级本人状态标签：该期次下我的节点是否还有待我处理 */
    perTagText(per) {
      return this.perHasTodo(per) ? '待我处理' : '已完成'
    },
    perTagCls(per) {
      return this.perHasTodo(per) ? 'pt-todo' : 'pt-done'
    },
    perHasTodo(per) {
      return !!(per && per.todos && per.todos.some(t => t.todoStatus === 0))
    },
    /** 展开行过滤：点「待我处理」只保留待处理行、点「已完成」只保留已完成行（全部时返回全部） */
    visibleTodos(per) {
      const todos = (per && per.todos) || []
      if (this.statActive('pending')) return todos.filter(t => t.todoStatus === 0)
      if (this.statActive('done')) return todos.filter(t => t.todoStatus !== 0)
      return todos
    },
    isTaskOpen(id) { return this.openTaskIds.indexOf(id) >= 0 },
    toggleTask(g) {
      const idx = this.openTaskIds.indexOf(g.taskId)
      this.openTaskIds = idx >= 0
        ? this.openTaskIds.filter(id => id !== g.taskId)
        : this.openTaskIds.concat(g.taskId)
    },
    /** 每页任务数变化：回到第 1 页重新切片 */
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.applyPage()
    },
    /** 按当前页码对已过滤的关联列表做前端分页切片 */
    applyPage() {
      const total = this.allList.length
      this.total = total
      const maxPage = Math.max(1, Math.ceil(total / this.pageSize))
      if (this.currentPage > maxPage) this.currentPage = maxPage
      const start = (this.currentPage - 1) * this.pageSize
      this.list = this.allList.slice(start, start + this.pageSize)
      // 清理已不存在任务的展开状态
      const ids = this.list.map(g => g.taskId)
      this.openTaskIds = this.openTaskIds.filter(id => ids.indexOf(id) >= 0)
    },
    /** 仅保留与当前来源期次关联的待办行（连带其所属期次、任务组；无关联则不展示） */
    keepLinked(groups, linkedTaskIds) {
      if (!linkedTaskIds.length) return []
      const out = []
      groups.forEach(g => {
        const periods = []
        ;(g.periods || []).forEach(per => {
          const todos = (per.todos || []).filter(t => linkedTaskIds.indexOf(t.taskId) >= 0)
          if (todos.length) periods.push(Object.assign({}, per, { todos }))
        })
        if (periods.length) {
          const pendingCount = periods.reduce((s, p) => s + (p.todos || []).filter(t => t.todoStatus === 0).length, 0)
          out.push(Object.assign({}, g, { periods, pendingCount, periodCount: periods.length }))
        }
      })
      return out
    },
    async fetchData() {
      this.loading = true
      try {
        // 1. 取当前来源期次关联的目标任务（我收到的任务）
        const linkRes = await getTaskLinksBySource('', this.sourceDispatchId, this.sourcePeriodId)
        const links = linkRes.data || []
        const linkedTaskIds = links.map(l => l.targetTaskId).filter(id => !!id)
        // 2. 我的任务（拉全量）→ 仅保留被关联的待办行
        const params = { page: 1, limit: 500 }
        if (this.filters.taskName && this.filters.taskName.trim()) params.taskName = this.filters.taskName.trim()
        if (this.filters.status !== '') params.status = Number(this.filters.status)
        if (this.filters.taskType !== '') params.taskType = Number(this.filters.taskType)
        if (this.filters.templateName && this.filters.templateName.trim()) params.templateName = this.filters.templateName.trim()
        if (this.filters.createStart) params.createStart = this.filters.createStart
        if (this.filters.createEnd) params.createEnd = this.filters.createEnd
        const res = await getMyTodoGrouped(params)
        this.allList = this.keepLinked(res.data.records || [], linkedTaskIds)
        this.applyPage()
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
      this.filters = { taskName: '', status: '', taskType: '', templateName: '', createStart: '', createEnd: '' }
      this.activeStats = []
      this.currentPage = 1
      this.fetchData()
    },
    refresh() {
      this.fetchData()
      this.fetchStats()
    },
    /** 打开办理/查看详情（整页）：携带待办节点与上下文参数 */
    openProcess(todo, per, g) {
      this.$router.push({
        path: '/task-process/detail',
        query: {
          taskId: todo.taskId,
          tn: todo.taskNodeId || '',
          mode: todo.todoStatus === 0 ? 'process' : 'view',
          taskName: todo.taskName || (g && g.taskName) || '',
          templateName: todo.templateName || (g && g.templateName) || '',
          periodName: (per && per.periodName) || ''
        }
      })
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
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .link { color: #909399; cursor: pointer;
    &:hover { color: $primary; }
  }
  .active { color: $primary; font-weight: 600; }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.header-actions { display: flex; align-items: center; gap: 10px; }

// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
  @media (max-width: 900px) { grid-template-columns: 1fr; }
}
.stat-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: all .2s;
  &.clickable { cursor: pointer;
    &:hover { border-color: $primary; box-shadow: 0 2px 8px rgba(var(--color-primary-rgb), 0.12); }
    &.active { border-color: $primary; background: var(--color-primary-light); box-shadow: inset 0 0 0 1px rgba(var(--color-primary-rgb), 0.5); }
  }
}
.stat-icon { width: 48px; height: 48px; border-radius: 3px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; flex-shrink: 0;
  &.icon-total { background: $primary; }
  &.icon-pending { background: #B45309; }
  &.icon-done { background: #15803D; }
}
.stat-body { flex: 1; display: flex; align-items: baseline; justify-content: space-between; gap: 10px; min-width: 0; }
.stat-label { font-size: 13px; color: #757575; white-space: nowrap; }
.stat-value { font-size: 24px; font-weight: 700; color: #1b1c1c; line-height: 1.1; white-space: nowrap; }

// 筛选区
.filter-section { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px; }
.filter-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 13px; color: #757575; }
.filter-select, .filter-input { height: 36px; border: 1px solid $border; border-radius: 2px; padding: 0 10px; font-size: 13px; outline: none; background: #fff; transition: all .2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.12); }
}
.filter-actions { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(var(--color-primary-rgb),0.08); }
.filter-actions-left, .filter-actions-right { display: flex; gap: 8px; }
.btn-create { display: flex; align-items: center; gap: 5px; padding: 10px 22px; background: $primary; color: #fff; border: none; border-radius: 3px; font-weight: 600; font-size: 13px; cursor: pointer; box-shadow: 0 2px 8px rgba(var(--color-primary-rgb),0.25); transition: all .2s;
  &:hover { opacity: 0.9; transform: translateY(-1px); }
}
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid $border; border-radius: 2px; font-size: 13px; color: var(--color-primary); background: #fff; cursor: pointer;
  &:hover { background: var(--color-primary-light); }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 2px; font-size: 13px; font-weight: bold; color: #fff; background: $primary; cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { opacity: 0.9; }
}
.btn-refresh { display: flex; align-items: center; gap: 5px; padding: 10px 16px; background: #fff; border: 1px solid $border; border-radius: 3px; color: var(--color-primary); font-weight: 600; font-size: 13px; cursor: pointer; transition: all .2s;
  i { color: $primary; }
  &:hover { border-color: $primary; color: $primary; background: var(--color-primary-surface); }
}
.tip-bar { display: flex; align-items: center; gap: 8px; background: var(--color-primary-light); border: 1px solid $border; color: var(--color-primary-hover); font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: $primary; }
}
.empty-state { text-align: center; padding: 60px 20px; color: #bbb; background: #fff; border: 1px solid $border; border-radius: 3px;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
.task-collapse { position: relative; }
.loading-bar { display: flex; align-items: center; gap: 6px; justify-content: center; padding: 16px; color: $primary; font-size: 13px; }

// 折叠面板（卡片式，同任务管理）
.task-panel { background: #fff; border: 1px solid $border; border-radius: 3px; margin-bottom: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.03); }
.tp-head { display: flex; align-items: center; gap: 10px; padding: 8px 14px; cursor: pointer; transition: background .2s;
  &:hover { background: var(--color-primary-surface); }
}
.tp-arrow { color: #94A3B8; font-size: 14px; flex-shrink: 0; transition: transform .25s; cursor: pointer; }
.task-panel:hover .tp-arrow { color: $primary; }
.task-panel.is-open .tp-arrow { transform: rotate(180deg); }
.tp-body { border-top: 1px solid $border; }
.ct-icon { width: 40px; height: 40px; border-radius: 3px; background: var(--color-primary-light); color: $primary; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.ct-main { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; white-space: nowrap; }
.ct-name { font-size: 15px; font-weight: 700; color: #1b1c1c; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex-shrink: 1; }
.pending-tag { padding: 2px 10px; background: rgba(var(--color-primary-rgb),0.1); color: $primary; border-radius: 3px; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.ct-meta { font-size: 12px; color: #909399; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-left: 10px; flex-shrink: 1; }

.task-detail { padding: 16px; display: flex; flex-direction: column; gap: 16px; }
// 期次块（同任务管理期次列表样式）
.period-section { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 14px 16px; }
.sec-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; color: #1b1c1c; margin-bottom: 12px;
  i { color: $primary; }
}
.sec-sub { font-size: 12px; color: #999; font-weight: 400; }
.period-no { font-size: 11px; color: $primary; background: rgba(var(--color-primary-rgb),0.1); border-radius: 3px; padding: 1px 8px; font-weight: 600; margin-left: 6px; }
// 期次级本人状态标签（按期次区分 待我处理/已完成）
.ptag { display: inline-block; padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 600; margin-left: 6px; vertical-align: 1px;
  &.pt-todo { background: rgba(var(--color-primary-rgb),0.1); color: $primary; }
  &.pt-done { background: rgba(21, 128, 61,0.1); color: #15803D; }
}
// 员工任务名称列：长名省略，hover 以 title 查看完整
.emp-task-name { display: inline-block; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; color: #1b1c1c; }
.period-empty { text-align: center; padding: 24px; color: #bbb; font-size: 13px;
  i { margin-right: 4px; }
}
.font-bold { font-weight: 700; }
.text-muted { color: #bbb; }
.text-center { text-align: center; }
.period-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 10px 12px; font-weight: 700; color: #414755; background: var(--color-primary-light); border-bottom: 1px solid $border; font-size: 13px; }
  td { padding: 10px 12px; border-bottom: 1px solid $border; font-size: 13px; }
  tbody tr:last-child td { border-bottom: none; }
  .hover-row:hover { background: var(--color-primary-light); }
}
.text-center { text-align: center; }
.text-right { text-align: right; }
.node-badge { padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 600; margin-right: 4px; }
.badge-start { background: rgba(21, 128, 61,0.1); color: #15803D; }
.badge-mid { background: #f0f3ff; color: #545f72; }
.badge-end { background: rgba(var(--color-primary-rgb),0.1); color: $primary; }
.btn-process { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: $primary; color: #fff; border: none; border-radius: 2px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { opacity: 0.9; }
}
.btn-view { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: #fff; color: #545f72; border: 1px solid #d8dee9; border-radius: 2px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { background: #f0f3ff; border-color: #b7c3d8; }
}
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 600;
  &.st-todo { background: rgba(var(--color-primary-rgb),0.1); color: $primary; }
  &.st-done { background: rgba(21, 128, 61,0.1); color: #15803D; }
}

// 流程节点链（chip 样式，参考期次人员查看）
.node-chain { display: flex; align-items: center; gap: 4px; overflow-x: auto; max-width: 480px; padding-bottom: 2px;
  &::-webkit-scrollbar { height: 3px; }
  &::-webkit-scrollbar-thumb { background: #e0d2cf; border-radius: 3px; }
}
.node-chip { flex-shrink: 0; padding: 2px 9px; border-radius: 4px; font-size: 11px; font-weight: 600; line-height: 1.6; white-space: nowrap;
  &.chip-done { background: #15803D; color: #fff; }
  // 进行中（当前待处理节点）：实底深色白字加粗，与「未开始」浅灰明确区分
  &.chip-current { background: var(--color-primary); color: #fff; font-weight: 700; box-shadow: 0 0 0 1px rgba(255,255,255,0.35) inset; }
  &.chip-pending { background: #f0f0f0; color: #aaa; }
}
// 已超期标识（软性标记：仍可提交，仅提示）
.overdue-tag { display: inline-flex; align-items: center; gap: 3px; margin-left: 6px; padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; color: #fff; background: #D97706; }

// 分页（同任务管理）
.pagination { display: flex; justify-content: flex-end; align-items: center; padding: 14px 16px; background: #fff; border: 1px solid $border; border-radius: 3px; margin-top: 12px; }
</style>
