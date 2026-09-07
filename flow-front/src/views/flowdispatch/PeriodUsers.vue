<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div class="header-left">
            <div class="hl-row1">
            <button class="btn-back" @click="goBack"><i class="el-icon-arrow-left" /> 返回</button>
            <nav class="breadcrumb">
              <span class="link" @click="goBack">任务管理</span>
              <span>/</span>
              <span class="active">期次人员</span>
            </nav>
          </div>
          <h3 class="page-heading">期次人员</h3>
          </div>
        </div>

        <!-- 期次信息 -->
        <section class="tip-bar">
          <i class="el-icon-tickets" />
          <template v-if="taskName">任务：{{ taskName }}</template>
          <template v-if="periodName"> · 期次：{{ periodName }}</template>
          <template v-if="periodInfo && periodInfo.endTime"> · 截止：{{ periodInfo.endTime }}</template>
          <template v-if="periodInfo && periodInfo.urgeTime"> · 催办：{{ periodInfo.urgeTime }}</template>
          <template v-if="!periodName && !taskName">期次人员查看</template>
          <span class="tip-sub">· 每张卡片为一个员工任务（首个节点处理人启动后仍会继续流转）；点击卡片可查看完整流程</span>
        </section>

        <!-- 成员筛选 -->
        <section class="filter-bar">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="filter-label">姓名</label>
              <input v-model="filters.name" class="filter-input" placeholder="输入姓名搜索" @keyup.enter="onSearch" />
            </div>
            <div class="filter-item">
              <label class="filter-label">部门</label>
              <input v-model="filters.dept" class="filter-input" placeholder="输入部门搜索" @keyup.enter="onSearch" />
            </div>
            <div class="filter-item">
              <label class="filter-label">状态</label>
              <select v-model="filters.status" class="filter-select">
                <option value="">全部</option>
                <option value="进行中">进行中</option>
                <option value="已完成">已完成</option>
                <option value="已作废">已作废</option>
              </select>
            </div>
          </div>
          <div class="filter-actions">
            <span class="member-count">共 {{ total }} 人</span>
            <div class="filter-actions-right">
              <button class="btn-reset" @click="onReset">重置</button>
              <button class="btn-search" @click="onSearch">查询</button>
            </div>
          </div>
        </section>

        <!-- 批量操作条 -->
        <section class="bulk-bar">
          <div class="bulk-left">
            <el-checkbox :value="allSelected" :indeterminate="indeterminate" @change="onToggleAll">全选</el-checkbox>
            <span class="bulk-count">已选 {{ selected.length }} 人</span>
          </div>
          <div class="bulk-right">
            <button class="btn-bulk add" @click="openAddModal">
              <i class="el-icon-plus" /> 新增人员
            </button>
            <button class="btn-bulk urge" :disabled="selected.length === 0" @click="onBatchUrge">
              <i class="el-icon-alarm-clock" /> 批量催办
            </button>
            <button class="btn-bulk danger" :disabled="selected.length === 0" @click="onBatchDelete">
              <i class="el-icon-delete" /> 批量删除
            </button>
          </div>
        </section>

        <!-- 成员列表 -->
        <div v-loading="loading" class="members-wrap">
          <div v-if="!loading && members.length === 0" class="empty-state">
            <i class="el-icon-user" />
            <p>该期次暂无人员</p>
          </div>
          <div v-else class="member-list">
            <div v-for="m in members" :key="m.taskId" class="member-item" @click="openFlow(m)">
              <div class="mi-check" @click.stop>
                <el-checkbox :value="selected.includes(m.taskId)" @change="v => toggleSelect(m.taskId, v)" />
              </div>
              <div class="mi-left">
                <div class="mi-badge" :class="{ 'is-done': isTaskDone(m) }">
                  <i v-if="isTaskDone(m)" class="el-icon-check" />
                  <span v-else-if="badgeNodeText(m)" class="badge-node-text">{{ badgeNodeText(m) }}</span>
                  <i v-else class="el-icon-s-operation" />
                </div>
                <div class="mi-info">
                  <!-- 主标题行：任务名称（左） + 状态徽标（右） -->
                  <div class="mi-name" :title="m.taskName">
                    <span class="mi-task-text">{{ m.taskName || '未命名任务' }}</span>
                    <span class="status-chip" :class="statusClass(m.status)">{{ statusText(m.status) }}</span>
                  </div>
                  <!-- 进行中的任务展示处理中信息（当前处理人/当前节点/进度）；已完成/已作废不显示 -->
                  <div v-if="!isTaskDone(m)" class="mi-meta">
                    <span><i class="el-icon-user" /> 当前处理人：{{ m.currentHandlerName || '—' }}</span>
                    <span><i class="el-icon-s-claim" /> 当前节点：{{ m.currentNodeName || '—' }}</span>
                    <span><i class="el-icon-odometer" /> 进度 {{ memberProgressText(m) }}</span>
                  </div>
                  <div class="progress-bar thin"><div class="progress-fill" :style="{ width: memberProgress(m) + '%' }" /></div>
                  <div class="mi-nodes">
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
              <div class="mi-right" @click.stop>
                <button class="mi-view" @click="openFlow(m)"><i class="el-icon-view" /> 查看流程</button>
              </div>
            </div>
          </div>
          <!-- 分页 -->
          <div v-if="total > 0" class="pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next"
              :total="total"
              :current-page.sync="page"
              :page-size="limit"
              :page-sizes="[10, 20, 50]"
              @size-change="onSizeChange"
              @current-change="onPageChange"
            />
          </div>
        </div>
      </section>
    </main>

    <!-- 新增人员：复用生成期次的 UserPicker 选人弹窗 -->
    <UserPicker
      :visible="pickerVisible"
      title="新增本期次人员（可多选，不影响任务配置）"
      :exclude-ids="pickerExcludeIds"
      @confirm="onAddMembers"
      @close="pickerVisible = false"
    />
  </div>
</template>

<script>
import { getTaskMembers, urgeTaskBatch, deleteTaskBatch } from '@/service/sys/TaskService'
import { addPeriodMembers, getPeriodInfo } from '@/service/sys/FlowDispatchService'
import UserPicker from '@/components/UserPicker/index.vue'

export default {
  name: 'PeriodUsers',
  components: { UserPicker },
  data() {
    return {
      loading: false,
      dispatchId: null,
      periodName: '',
      taskName: '',
      periodInfo: null,
      members: [],
      total: 0,
      page: 1,
      limit: 10,
      filters: { name: '', dept: '', status: '' },
      selected: [],
      pickerVisible: false,
      adding: false,
      memberUserIds: []
    }
  },
  computed: {
    allSelected() {
      return this.members.length > 0 && this.members.every(m => this.selected.includes(m.taskId))
    },
    indeterminate() {
      return this.selected.length > 0 && !this.allSelected
    },
    // 已在期次中的人员，UserPicker 中禁用勾选避免重复新增
    pickerExcludeIds() {
      return this.memberUserIds
    }
  },
  created() {
    this.dispatchId = this.$route.query.dispatchId || null
    this.periodName = this.$route.query.periodName || ''
    this.taskName = this.$route.query.taskName || ''
    this.fetchMembers()
    this.fetchPeriodInfo()
  },
  methods: {
    async fetchPeriodInfo() {
      if (!this.dispatchId) return
      try {
        const res = await getPeriodInfo(this.dispatchId)
        this.periodInfo = res.data || null
      } catch (e) {
        console.error(e)
      }
    },
    toggleSelect(taskId, checked) {
      if (checked) {
        if (!this.selected.includes(taskId)) this.selected.push(taskId)
      } else {
        this.selected = this.selected.filter(id => id !== taskId)
      }
    },
    onToggleAll(checked) {
      if (checked) {
        const ids = this.members.map(m => m.taskId).filter(id => !this.selected.includes(id))
        this.selected = this.selected.concat(ids)
      } else {
        const pageIds = this.members.map(m => m.taskId)
        this.selected = this.selected.filter(id => !pageIds.includes(id))
      }
    },
    openAddModal() {
      this.pickerVisible = true
    },
    async onAddMembers(users) {
      if (!users || users.length === 0 || this.adding) return
      this.adding = true
      try {
        const res = await addPeriodMembers(this.dispatchId, users.map(u => u.yyytId || u.id).filter(Boolean))
        const count = res.data != null ? res.data : users.length
        this.$message.success(`已新增 ${count} 位人员`)
        this.pickerVisible = false
        this.page = 1
        this.fetchMembers()
      } catch (e) {
        this.$message.error((e && e.message) || '新增失败')
      } finally {
        this.adding = false
      }
    },
    async onBatchUrge() {
      if (this.selected.length === 0) return
      try {
        await this.$confirm(`确定向已选的 ${this.selected.length} 位人员的当前处理人发送催办通知吗？`, '批量催办确认', {
          confirmButtonText: '发送催办',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--primary'
        })
        const res = await urgeTaskBatch(this.selected)
        const count = res.data != null ? res.data : this.selected.length
        this.$message.success(`已发送 ${count} 条催办通知（已完成/已作废人员自动跳过）`)
        this.fetchMembers()
      } catch (e) {
        if (e !== 'cancel') this.$message.error((e && e.message) || '催办失败')
      }
    },
    async onBatchDelete() {
      if (this.selected.length === 0) return
      try {
        await this.$confirm(`确定删除已选的 ${this.selected.length} 位人员吗？将级联清除其全部节点、表单与附件，删除后无法恢复。`, '批量删除确认', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--primary'
        })
        const res = await deleteTaskBatch(this.selected)
        const count = res.data != null ? res.data : this.selected.length
        this.$message.success(`已删除 ${count} 位人员`)
        this.selected = []
        this.fetchMembers()
      } catch (e) {
        if (e !== 'cancel') this.$message.error((e && e.message) || '删除失败')
      }
    },
    /** 卡片左标：任务已完成（状态码 2 / 展示词「已完成」/ 底层词「已结束」）时显示绿色勾 */
    isTaskDone(m) { return m.status === 2 || m.status === '已完成' || m.status === '已结束' },
    /** 卡片左标：未完成时取当前节点名的首字（如「整」=整改审核） */
    badgeNodeText(m) {
      const name = m.currentNodeName || ''
      return name ? name.charAt(0) : ''
    },
    /** 展示归一：底层任务状态「已结束」在成员卡片统一展示为「已完成」 */
    statusText(s) { return ({ 进行中: '进行中', 已完成: '已完成', 已结束: '已完成', 已作废: '已作废', 1: '进行中', 2: '已完成', 3: '已作废' })[s] || '—' },
    statusClass(s) { return ({ 进行中: 'status-running', 已完成: 'status-done', 已结束: 'status-done', 已作废: 'status-cancel', 1: 'status-running', 2: 'status-done', 3: 'status-cancel' })[s] || '' },
    memberProgress(m) {
      const total = m.totalNodeCount || 0
      if (!total) return 0
      return Math.round(Math.min(this.memberDoneCount(m), total) / total * 100)
    },
    /** 已完成节点数：nodeSteps 中真正完成(done)的节点数——正在办理(current)、被退回(rejected)、未开始(pending)均不计 */
    memberDoneCount(m) {
      const steps = m.nodeSteps || []
      return steps.filter(s => s.status === 'done').length
    },
    /** 进度文本：已完成节点数/总节点数（如 0/5=第一个节点尚未完成） */
    memberProgressText(m) {
      const total = m.totalNodeCount || 0
      if (!total) return '—'
      return `${Math.min(this.memberDoneCount(m), total)}/${total}`
    },
    async fetchMembers() {
      if (!this.dispatchId) return
      this.loading = true
      try {
        const params = { page: this.page, limit: this.limit }
        if (this.filters.name && this.filters.name.trim()) params.name = this.filters.name.trim()
        if (this.filters.dept && this.filters.dept.trim()) params.dept = this.filters.dept.trim()
        if (this.filters.status !== '') params.status = this.filters.status
        const res = await getTaskMembers(this.dispatchId, params)
        this.members = (res.data && res.data.records) || []
        this.total = (res.data && res.data.total) || 0
        this.memberUserIds = this.members.map(m => m.ownerUserId).filter(id => id != null)
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
        // 列表变化后清空勾选，避免选中已不在当前页的数据
        this.selected = []
      }
    },
    onSearch() { this.page = 1; this.fetchMembers() },
    onReset() {
      this.filters = { name: '', dept: '', status: '' }
      this.page = 1
      this.fetchMembers()
    },
    onSizeChange(size) { this.limit = size; this.page = 1; this.fetchMembers() },
    onPageChange(p) { this.page = p; this.fetchMembers() },
    openFlow(member) {
      if (!member || !member.taskId) return
      this.$router.push({
        path: '/flow-dispatch/period-flow',
        query: {
          dispatchId: this.dispatchId || '',
          taskId: member.taskId,
          handlerId: member.ownerUserId,
          realName: member.ownerName || '',
          empNo: member.ownerEmpNo || '',
          deptName: member.ownerDept || '',
          periodName: this.periodName || '',
          taskName: this.taskName || ''
        }
      })
    },
    goBack() {
      this.$router.push('/flow-dispatch/index')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.dashboard-container { display: flex; min-height: 100vh; background-color: var(--color-primary-surface);  color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; width: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.header-left { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; }
.hl-row1 { display: flex; align-items: center; gap: 14px; }
.hl-row1 .breadcrumb { margin-bottom: 0; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
  .link { color: $primary; cursor: pointer;
    &:hover { text-decoration: underline; }
  }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.btn-back { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; background: transparent; border: none; color: var(--color-primary); cursor: pointer; font-size: 13px; transition: background .2s;
  &:hover { background: rgba(var(--color-primary-rgb), 0.08); }
  &:hover { background: var(--color-primary-light); }
}
.tip-bar { display: flex; align-items: center; gap: 8px; background: var(--color-primary-light); border: 1px solid $border; color: var(--color-primary-hover); font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: $primary; }
  .tip-sub { color: #94A3B8; }
}
.filter-bar { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 14px; }
.filter-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px;
  @media (max-width: 700px) { grid-template-columns: 1fr; }
}
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 12px; color: #757575; }
.filter-select { height: 34px; border: 1px solid $border; border-radius: 4px; padding: 0 8px; font-size: 13px; outline: none; background: #fff;
  &:focus { border-color: $primary; }
}
.filter-input { height: 34px; border: 1px solid $border; border-radius: 4px; padding: 0 10px; font-size: 13px; outline: none;
  &:focus { border-color: $primary; }
}
.filter-actions { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(var(--color-primary-rgb),0.08); }
.filter-actions-right { display: flex; gap: 8px; margin-left: auto; }
.bulk-bar { display: flex; justify-content: space-between; align-items: center; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 10px 16px; }
.bulk-left { display: flex; align-items: center; gap: 12px; }
.bulk-count { font-size: 13px; color: $primary; font-weight: 600; }
.bulk-right { display: flex; gap: 8px; }
.btn-bulk { display: inline-flex; align-items: center; gap: 4px; padding: 6px 14px; border-radius: 2px; font-size: 12px; font-weight: 600; cursor: pointer; transition: all .2s; border: 1px solid transparent;
  &.add { background: $primary; color: #fff; border-color: $primary;
    &:hover { opacity: 0.9; }
  }
  &.urge { background: #EFF6FF; color: #B45309; border-color: rgba(180, 83, 9,0.5);
    &:hover { background: rgba(180, 83, 9,0.12); }
  }
  &.danger { background: #fff; color: #DC2626; border-color: rgba(220,38,38,0.3);
    &:hover { background: rgba(220,38,38,0.05); }
  }
  &:disabled { opacity: 0.45; cursor: not-allowed; }
}
.member-count { font-size: 12px; color: #757575; }
.btn-reset { padding: 0 14px; height: 32px; border: 1px solid $border; border-radius: 4px; font-size: 13px; color: var(--color-primary); background: #fff; cursor: pointer;
  &:hover { background: var(--color-primary-light); }
}
.btn-search { padding: 0 14px; height: 32px; border: none; border-radius: 4px; font-size: 13px; font-weight: bold; color: #fff; background: $primary; cursor: pointer;
  &:hover { opacity: 0.9; }
}
.members-wrap { min-height: 180px; }
.empty-state { text-align: center; padding: 48px 20px; color: #bbb; background: #fff; border: 1px solid $border; border-radius: 3px;
  i { font-size: 42px; display: block; margin-bottom: 10px; }
  p { font-size: 13px; margin: 0; }
}
.member-list { display: flex; flex-direction: column; gap: 10px; }
.member-item { display: flex; justify-content: space-between; align-items: center; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 14px 16px; transition: all .2s; cursor: pointer;
  &:hover { box-shadow: 0 3px 10px rgba(var(--color-primary-rgb),0.12); border-color: $primary; }
}
.mi-check { flex-shrink: 0; margin-right: 10px; display: flex; align-items: center; }
.mi-left { display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0; }
.mi-badge { width: 40px; height: 40px; border-radius: 8px; background: rgba(var(--color-primary-rgb), 0.1); color: $primary; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
// 当前节点首字 / 已完成绿勾
.mi-badge .badge-node-text { font-size: 16px; font-weight: 700; line-height: 1; }
.mi-badge.is-done { background: rgba(21, 128, 61, 0.12); color: #15803D; }
.mi-badge.is-done i { font-size: 20px; }
.mi-info { flex: 1; min-width: 0; }
// 主标题行：任务名称（左）+ 状态徽标（右）
.mi-name { display: flex; align-items: center; gap: 10px; min-width: 0; margin-top: 1px; }
.mi-task-text { font-size: 15px; font-weight: 700; color: #1b1c1c; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mi-name .status-chip { flex-shrink: 0; }
// 处理中信息（当前处理人/当前节点/进度），位于标题行下方左侧
.mi-meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12px; color: #757575; margin-top: 5px; line-height: 1.6;
  i { margin-right: 2px; }
}
.status-chip { padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; display: inline-flex; border: 1px solid transparent; }
.status-running { background: rgba(var(--color-primary-rgb),0.1); border-color: $primary; color: $primary; }
.status-done { background: rgba(21, 128, 61,0.1); border-color: #15803D; color: #15803D; }
.status-cancel { background: rgba(220,38,38,0.08); border-color: #DC2626; color: #DC2626; }
.status-empty { background: rgba(144,147,153,0.1); border-color: #909399; color: #909399; }
.progress-bar.thin { width: 100%; height: 5px; background: #f0f0f0; border-radius: 3px; margin-top: 6px; overflow: hidden; }
.progress-fill { height: 100%; background: $primary; border-radius: 3px; transition: width .3s; }
.mi-nodes { display: flex; align-items: center; gap: 4px; margin-top: 6px; overflow-x: auto; white-space: nowrap;
  &::-webkit-scrollbar { height: 4px; }
  &::-webkit-scrollbar-thumb { background: #ddd; border-radius: 4px; }
}
.node-chip { padding: 2px 9px; border-radius: 4px; font-size: 11px; font-weight: 600; flex-shrink: 0; line-height: 1.6; }
.chip-done { background: #15803D; color: #fff; }
// 进行中（当前待处理节点）：实底深色白字加粗，与「未开始」浅灰明确区分
.chip-current { background: var(--color-primary); color: #fff; font-weight: 700; box-shadow: 0 0 0 1px rgba(255,255,255,0.35) inset; }
.chip-rejected { background: rgba(180, 83, 9,0.16); color: #B45309; border: 1px solid rgba(180, 83, 9,0.45); }
.chip-pending { background: #f0f0f0; color: #aaa; }
.mi-right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; margin-left: 12px; }
.mi-view { display: inline-flex; align-items: center; gap: 3px; padding: 5px 10px; border: 1px solid $border; background: #fff; color: $primary; border-radius: 2px; font-size: 12px; font-weight: 600; cursor: pointer;
  &:hover { background: var(--color-primary-light); border-color: $primary; }
}
.pagination { display: flex; justify-content: flex-end; align-items: center; padding: 14px 16px; background: #fff; border: 1px solid $border; border-radius: 3px; margin-top: 12px; }
</style>
