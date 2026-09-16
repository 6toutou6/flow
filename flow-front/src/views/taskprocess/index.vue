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

        <!-- 统计卡（点击筛选：待我处理 / 已完成，点「全部」恢复） -->
        <section class="stats-grid">
          <div class="stat-card clickable" :class="{ active: statActive('all') }" title="点击显示全部任务" @click="toggleStat('all')">
            <div class="stat-icon icon-total"><i class="el-icon-s-order" /></div>
            <div class="stat-body">
              <div class="stat-label">全部</div>
              <div class="stat-value">{{ stats.taskCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card clickable" :class="{ active: statActive('pending') }" title="点击筛选还有待我处理的节点，再点取消" @click="toggleStat('pending')">
            <div class="stat-icon icon-pending"><i class="el-icon-alarm-clock" /></div>
            <div class="stat-body">
              <div class="stat-label">待我处理</div>
              <div class="stat-value">{{ stats.pendingNodeCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card clickable" :class="{ active: statActive('done') }" title="点击筛选我已处理完成的节点，再点取消" @click="toggleStat('done')">
            <div class="stat-icon icon-done"><i class="el-icon-circle-check" /></div>
            <div class="stat-body">
              <div class="stat-label">已完成</div>
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
                <option :value="1">待我处理</option>
                <option :value="0">已完成</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">任务类型</label>
              <select v-model="filters.taskType" class="filter-select">
                <option value="">全部</option>
                <option :value="1">样例任务</option>
                <option :value="0">普通任务</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">模板名称</label>
              <input v-model="filters.templateName" class="filter-input" placeholder="请输入模板名称" @keyup.enter="handleSearch">
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
          <p>暂无待办任务</p>
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
              <button
                v-if="canHandover(g)"
                class="btn-handover"
                title="把该任务下我名下的期次与待处理节点交接给他人"
                @click.stop="openHandover(g)"
              ><i class="el-icon-s-promotion" /> 交接任务</button>
              <i class="el-icon-arrow-down tp-arrow" />
            </div>

            <!-- 展开内容：期次 → 待办（沿用原表格样式；同一期次多条不同子任务待办各自独立一行，流程进度各按自己任务展示） -->
            <div v-show="isTaskOpen(g.taskId)" class="tp-body">
              <div class="task-detail">
                <div v-if="g.periods.length === 0" class="period-empty"><i class="el-icon-tickets" /> 暂无期次任务</div>
                <section v-else class="period-section">
                  <div class="sec-title">
                    <i class="el-icon-tickets" /> 期次列表 <span class="sec-sub">共 {{ g.periods.length }} 期 · {{ g.pendingCount }} 待处理</span>
                    <button class="btn-toggle-all" @click.stop="toggleAllPeriods(g)">
                      {{ allPeriodsOpen(g) ? '收起全部' : '展开全部' }}
                    </button>
                  </div>
                  <table class="period-table">
                    <thead>
                      <tr>
                        <th>期次</th>
                        <th>待办情况</th>
                        <th>起止时间</th>
                        <th class="text-center">状态</th>
                        <th class="text-right">操作</th>
                      </tr>
                    </thead>
                    <!-- 每个期次一个折叠块（<tbody>）：期次行给汇总信息，点开后才是该期次下与我有关的待办 -->
                    <template v-for="per in g.periods">
                      <tbody
                        v-if="showPeriod(per)"
                        :key="periodKey(g, per)"
                        :class="{ 'is-open': isPeriodOpen(g, per) }"
                      >
                        <!-- 期次汇总行（点击整行折叠/展开） -->
                        <tr class="period-head" @click="togglePeriod(g, per)">
                          <td class="font-bold">
                            <i class="el-icon-caret-right per-arrow" />
                            {{ per.periodName || '无期次' }}
                          </td>
                          <td class="per-summary">
                            {{ periodTodoText(per) }}
                            <span v-if="periodPending(per) > 0" class="pending-tag">{{ periodPending(per) }} 待处理</span>
                          </td>
                          <td>{{ periodRange(per) }}</td>
                          <td class="text-center">
                            <span class="ptag" :class="perTagCls(per)">{{ perTagText(per) }}</span>
                          </td>
                          <td class="text-right">
                            <button class="btn-period-toggle" @click.stop="togglePeriod(g, per)">
                              {{ isPeriodOpen(g, per) ? '收起' : '展开' }}
                              <i class="el-icon-arrow-down per-toggle-arrow" />
                            </button>
                          </td>
                        </tr>

                        <!-- 展开后的待办行：每条待办独立一行，各自进度链与操作 -->
                        <template v-if="isPeriodOpen(g, per)">
                          <tr
                            v-if="!statActive('pending') && !statActive('done') && (!per.todos || per.todos.length === 0)"
                            :key="(per.dispatchId || per.periodName || 'none') + '-empty'"
                            class="period-body"
                          >
                            <td colspan="5" class="text-muted period-empty-row"><i class="el-icon-tickets" /> 该期次无待办任务</td>
                          </tr>
                          <!-- 待办行：按列对齐期次表头（期次→任务名 / 待办情况→状态 / 起止时间→流程进度 / 状态→操作 / 操作→空） -->
                          <tr
                            v-for="(td, tdi) in visibleTodos(per)"
                            :key="td.taskNodeId || ((per.dispatchId || per.periodName || 'none') + '-' + tdi)"
                            class="hover-row period-body"
                          >
                            <td class="pb-td pb-td-name">
                              <span class="pb-lead">└</span>
                              <span class="pb-name" :title="td.taskName">{{ td.taskName || '—' }}</span>
                            </td>
                            <td class="pb-td">
                              <span class="status-badge" :class="td.todoStatus === 0 ? 'st-todo' : 'st-done'">{{ td.todoStatus === 0 ? '待我处理' : '已完成' }}</span>
                              <span v-if="rowOverdueTag(td, per)" class="overdue-tag"><i class="el-icon-alarm-clock" /> {{ rowOverdueTag(td, per) }}</span>
                            </td>
                            <td class="pb-td pb-td-chain">
                              <span class="node-chain">
                                <template v-for="(nd, i) in (td.chains || [])">
                                  <span
                                    :key="'n' + i"
                                    class="node-chip"
                                    :class="nodeChipClass(nd.status)"
                                    :title="nd.nodeName + '（' + (nd.status === 1 ? '已完成' : (nd.status === 2 ? '进行中' : '未开始')) + '）'"
                                  >{{ i + 1 }}.{{ nd.nodeName }}</span>
                                </template>
                                <span v-if="!(td.chains || []).length" class="text-muted">—</span>
                              </span>
                            </td>
                            <td class="pb-td" style="text-align: center;">
                              <button v-if="td.todoStatus === 0" class="btn-process" @click="openProcess(td, per, g)"><i class="el-icon-s-claim" /> 处理</button>
                              <button v-else class="btn-view" @click="openProcess(td, per, g)"><i class="el-icon-view" /> 查看详情</button>
                            </td>
                            <td class="pb-td"></td>
                          </tr>
                        </template>
                      </tbody>
                    </template></table>
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

    <!-- 任务交接申请弹窗 -->
    <HandoverModal
      :visible="handoverVisible"
      :dispatch-id="handoverCtx.dispatchId"
      :dispatch-name="handoverCtx.dispatchName"
      :period-count="handoverCtx.periodCount"
      :node-count="handoverCtx.nodeCount"
      @success="onHandoverSuccess"
      @close="handoverVisible = false"
    />
  </div>
</template>

<script>
import { getMyTodoGrouped, getMyTodoStats } from '@/service/sys/TaskService'
import HandoverModal from '@/components/HandoverModal'
import { NODE_TYPE } from '@/constants/dict'

export default {
  name: 'TaskProcess',
  components: { HandoverModal },
  data() {
    return {
      loading: false,
      list: [],
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
      // 已展开的期次折叠块（key = 任务 id + 期次 id），默认全部折叠
      openPeriodKeys: [],
      // 交接弹窗
      handoverVisible: false,
      handoverCtx: { dispatchId: '', dispatchName: '', periodCount: 0, nodeCount: 0 }
    }
  },
  mounted() {
    this.fetchData()
    this.fetchStats()
  },
  methods: {
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
    /** 期次折叠块的唯一 key（跨任务下可能有同名期次，需带任务 id） */
    periodKey(g, per) {
      return (g ? g.taskId : '') + '|' + (per.dispatchId || per.periodName || 'none')
    },
    isPeriodOpen(g, per) { return this.openPeriodKeys.indexOf(this.periodKey(g, per)) >= 0 },
    togglePeriod(g, per) {
      const k = this.periodKey(g, per)
      const idx = this.openPeriodKeys.indexOf(k)
      if (idx >= 0) {
        this.openPeriodKeys = this.openPeriodKeys.filter(x => x !== k)
      } else {
        this.openPeriodKeys = this.openPeriodKeys.concat(k)
      }
    },
    /** 该期次是否整块展示：无筛选时全展示；有筛选时只保留有命中的期次 */
    showPeriod(per) {
      if (!this.statActive('pending') && !this.statActive('done')) return true
      return this.visibleTodos(per).length > 0
    },
    /** 期次下待我处理的条数 */
    periodPending(per) {
      return (per.todos || []).filter(t => t.todoStatus === 0).length
    },
    /** 期次起止时间（显示到时分，完整呈现） */
    periodRange(per) {
      if (!per.startTime) return '—'
      const s = String(per.startTime).slice(0, 16)
      if (!per.endTime) return s
      return s + ' ~ ' + String(per.endTime).slice(0, 16)
    },
    /** 期次汇总行的待办摘要（待我处理的条数由状态列角标负责，这里只说总数，避免重复） */
    periodTodoText(per) {
      const all = (per.todos || []).length
      return all === 0 ? '无待办任务' : `共 ${all} 条待办`
    },
    /** 该任务下所有期次是否都已展开 */
    allPeriodsOpen(g) {
      const keys = (g.periods || []).filter(p => this.showPeriod(p)).map(p => this.periodKey(g, p))
      return keys.length > 0 && keys.every(k => this.openPeriodKeys.indexOf(k) >= 0)
    },
    /** 展开/收起该任务下的全部期次 */
    toggleAllPeriods(g) {
      const keys = (g.periods || []).filter(p => this.showPeriod(p)).map(p => this.periodKey(g, p))
      if (this.allPeriodsOpen(g)) {
        const set = {}
        keys.forEach(k => { set[k] = true })
        this.openPeriodKeys = this.openPeriodKeys.filter(k => !set[k])
      } else {
        const merged = this.openPeriodKeys.slice()
        keys.forEach(k => { if (merged.indexOf(k) < 0) merged.push(k) })
        this.openPeriodKeys = merged
      }
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
        if (this.filters.taskType !== '') params.taskType = Number(this.filters.taskType)
        if (this.filters.templateName && this.filters.templateName.trim()) params.templateName = this.filters.templateName.trim()
        if (this.filters.createStart) params.createStart = this.filters.createStart
        if (this.filters.createEnd) params.createEnd = this.filters.createEnd
        const res = await getMyTodoGrouped(params)
        if (!res || res.code !== 200) return
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
    },
    /**
     * 是否展示交接入口（入口常显）。
     * 只要该任务存在任务配置（有期次）就展示按钮；我名下是否真的可交接
     * 由弹窗内按 myPeriodCount / myNodeCount 提示，避免用户找不到入口。
     */
    canHandover(g) {
      if (!g) return false
      return !!(g.periods && g.periods.some(p => p && p.dispatchId))
    },
    /** 打开交接弹窗：交接范围 = 该任务配置下我名下的期次 + 我在该任务下的全部节点席位 */
    openHandover(g) {
      this.handoverCtx = {
        dispatchId: g.taskId,
        dispatchName: g.taskName,
        periodCount: g.myPeriodCount || 0,
        nodeCount: g.myNodeCount || 0
      }
      this.handoverVisible = true
    },
    /** 交接申请提交成功：关闭弹窗即可，提示由弹窗自己发出（避免重复弹两次） */
    onHandoverSuccess() {
      this.handoverVisible = false
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
.filter-actions { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(var(--color-primary-rgb),0.08); }
.filter-actions-right { display: flex; gap: 8px; }
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
// 期次列表右上角的「展开全部 / 收起全部」（期次较多时省得逐期点）
.btn-toggle-all { margin-left: auto; padding: 2px 10px; background: #fff; color: $primary; border: 1px solid $border; border-radius: 2px; cursor: pointer; font-size: 12px; font-weight: 600;
  &:hover { background: var(--color-primary-light); border-color: $primary; }
}
// ========== 期次折叠块：期次行是「主」、待办行是「次」，全部展开时也要一眼分得清 ==========
// 选择器带 .period-table 是为了盖过下面的 `.period-table td { padding: 10px 12px }`
.period-table .period-head {
  cursor: pointer;
  background: #E4EBF5;                                    // 主行底色明显重于待办行的纯白
  &:hover { background: #D9E3F2; }
  .per-arrow { margin-right: 6px; color: #5A6472; font-size: 12px; transition: transform .2s; }
  // 每块上方一条 2px 粗线 + 首列色条，块与块之间不会糊在一起
  td { border-top: 2px solid #C9D4E2; border-bottom: 1px solid #C9D4E2; font-weight: 700; font-size: 13px; padding: 7px 12px; }
  td:first-child { border-left: 3px solid #93A2B8; white-space: nowrap; }
}
tbody.is-open > .period-head {
  background: #DCE7F8;                                    // 展开时更重，仍稳在「主」的位置
  .per-arrow { transform: rotate(90deg); color: $primary; }
  td { border-top-color: $primary; }
  td:first-child { border-left-color: $primary; }
}
.per-summary { color: #64748B; font-size: 12px; font-weight: 400;
  // 「N 待处理」角标跟在这列的总数后面，与文字留出间隔
  .pending-tag { margin-left: 6px; padding: 1px 7px; font-size: 11px; border-radius: 3px; vertical-align: 1px; }
}
// 待办行（次）：按列对齐表头，整行淡蓝底，左侧带虚线从属线
// 注意：选择器带 .period-table 提高特异性，否则会被 `.period-table td { padding }` 覆盖
.period-table .period-body {
  background: #FAFCFE;
  td { border-bottom: 1px solid #EDF2F8; }
  &.hover-row:hover { background: #F1F6FC; }
}
// 待办行各单元格：统一缩小 padding
.pb-td { padding: 6px 12px !important; vertical-align: middle; font-size: 12.5px; }
// 第一列任务名：左侧留出虚线从属线 + └ 引导符的空间
.pb-td-name { position: relative; padding-left: 30px !important;
  &::before { content: ''; position: absolute; left: 15px; top: 0; bottom: 0; border-left: 1px dashed #BFD0E4; }
}
.pb-lead { color: #A9B8CC; font-size: 12px; margin-right: 4px; }
.pb-name { display: inline-block; max-width: calc(100% - 20px); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #45505F; font-weight: 600; font-size: 12.5px; vertical-align: middle; }
// 第三列流程进度：链宽跟随列宽，溢出可横滚
.pb-td-chain .node-chain { max-width: 100%; }
.period-empty-row { padding: 16px 12px 16px 34px !important; color: #9AA5B4 !important; font-size: 12px; }
.btn-period-toggle { display: inline-flex; align-items: center; gap: 4px; padding: 2px 9px; background: #fff; color: $primary; border: 1px solid $border; border-radius: 2px; cursor: pointer; font-size: 12px; font-weight: 600; white-space: nowrap; line-height: 18px;
  &:hover { background: var(--color-primary-light); border-color: $primary; }
  .per-toggle-arrow { font-size: 12px; transition: transform .2s; }
}
tbody.is-open .btn-period-toggle .per-toggle-arrow { transform: rotate(180deg); }
// 期次级本人状态标签（按期次区分 待我处理/已完成）；与相邻标签的间距由各自容器的 gap 控制
.ptag { display: inline-block; padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 600; vertical-align: 1px;
  &.pt-todo { background: rgba(var(--color-primary-rgb),0.1); color: $primary; }
  &.pt-done { background: rgba(21, 128, 61,0.1); color: #15803D; }
}
.period-empty { text-align: center; padding: 24px; color: #bbb; font-size: 13px;
  i { margin-right: 4px; }
}
.font-bold { font-weight: 700; }
.text-muted { color: #bbb; }
.text-center { text-align: center; }
.period-table { width: 100%; text-align: left; border-collapse: collapse; table-layout: fixed;
  th { padding: 10px 12px; font-weight: 700; color: #414755; background: var(--color-primary-light); border-bottom: 1px solid $border; font-size: 13px; }
  td { padding: 10px 12px; border-bottom: 1px solid $border; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  // 固定列宽（合计 100%）：期次 / 待办情况 / 起止时间 / 状态 / 操作
  // 起止时间给了 28%，因为要完整显示「2026-05-05 09:00 ~ 2026-05-12 09:00」
  th:nth-child(1) { width: 20%; }
  th:nth-child(2) { width: 18%; }
  th:nth-child(3) { width: 30%; }
  th:nth-child(4) { width: 16%; }
  th:nth-child(5) { width: 16%; }
  tbody tr:last-child td { border-bottom: none; }
  .hover-row:hover { background: var(--color-primary-light); }
  // 起止时间列窄，单独缩小字号省空间（任务名列靠 .emp-task-name 的 ellipsis 控制）
  td:nth-child(3) { font-size: 12px; }
}
.text-center { text-align: center; }
.text-right { text-align: right; }
.node-badge { padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 600; margin-right: 4px; }
.badge-start { background: rgba(21, 128, 61,0.1); color: #15803D; }
.badge-mid { background: #f0f3ff; color: #545f72; }
.badge-end { background: rgba(var(--color-primary-rgb),0.1); color: $primary; }
// 「处理 / 查看详情」等宽，右对齐时两个按钮的左右边缘都能对齐
.btn-process { display: inline-flex; align-items: center; justify-content: center; gap: 4px; min-width: 80px; padding: 4px 12px; line-height: 18px; background: $primary; color: #fff; border: none; border-radius: 2px; cursor: pointer; font-size: 12px; font-weight: 600; white-space: nowrap; flex-shrink: 0;
  &:hover { opacity: 0.9; }
}
.btn-view { display: inline-flex; align-items: center; justify-content: center; gap: 4px; min-width: 80px; padding: 4px 12px; line-height: 18px; background: #fff; color: #545f72; border: 1px solid #d8dee9; border-radius: 2px; cursor: pointer; font-size: 12px; font-weight: 600; white-space: nowrap; flex-shrink: 0;
  &:hover { background: #f0f3ff; border-color: #b7c3d8; }
}
// 交接任务（任务组头右侧）
.btn-handover { display: flex; align-items: center; gap: 4px; flex-shrink: 0; padding: 5px 12px; background: #fff; color: #B45309; border: 1px solid #F0B775; border-radius: 2px; cursor: pointer; font-size: 12px; font-weight: 600; transition: all .2s;
  &:hover { background: #FFF7ED; border-color: #D97706; }
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
.node-chip { flex-shrink: 0; padding: 1px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; line-height: 17px; white-space: nowrap;
  &.chip-done { background: #15803D; color: #fff; }
  // 进行中（当前待处理节点）：实底深色白字加粗，与「未开始」浅灰明确区分
  &.chip-current { background: var(--color-primary); color: #fff; font-weight: 700; box-shadow: 0 0 0 1px rgba(255,255,255,0.35) inset; }
  &.chip-pending { background: #f0f0f0; color: #aaa; }
}
// 已超期标识（软性标记：仍可提交，仅提示）
.overdue-tag { display: inline-flex; align-items: center; gap: 3px; padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; color: #fff; background: #D97706; }

// 分页（同任务管理）
.pagination { display: flex; justify-content: flex-end; align-items: center; padding: 14px 16px; background: #fff; border: 1px solid $border; border-radius: 3px; margin-top: 12px; }
</style>
