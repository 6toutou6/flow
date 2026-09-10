<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span>管理员</span>
              <span>/</span>
              <span class="active">交接审批</span>
            </nav>
            <h3 class="page-heading">
              交接审批
              <span v-if="pendingCount > 0" class="head-badge">{{ pendingCount }}</span>
            </h3>
          </div>
          <div class="header-actions">
            <div class="head-filter">
              <label class="head-filter-label">任务</label>
              <el-select v-model="filterTask" class="head-select" filterable clearable placeholder="全部任务">
                <el-option v-for="t in taskOptions" :key="t.key" :label="t.name" :value="t.key" />
              </el-select>
            </div>
            <button class="btn-refresh" @click="refresh"><i class="el-icon-refresh" /> 刷新</button>
          </div>
        </div>

        <!-- 页签：待我审批 / 本部门交接记录 -->
        <div class="tab-bar">
          <button class="tab-item" :class="{ active: tab === 'pending' }" @click="switchTab('pending')">
            <i class="el-icon-s-check" /> 待我审批
            <span v-if="pendingCount > 0" class="tab-badge">{{ pendingCount }}</span>
          </button>
          <button class="tab-item" :class="{ active: tab === 'records' }" @click="switchTab('records')">
            <i class="el-icon-time" /> 本部门交接记录
          </button>
        </div>

        <!-- 待我审批（按任务折叠） -->
        <template v-if="tab === 'pending'">
          <section class="tip-bar">
            <i class="el-icon-info" />
            仅创建人同部门的部门管理员（含创建人本人）可审批。审批通过后，申请人名下的期次归属，以及其在这条任务里参与过的全部节点（含已提交的历史节点）处理人，将一并转给接手人（含多处理人节点）。是否同步任务配置名单由你在此确认（申请人不在名单中时该项不适用）。
          </section>

          <div v-if="!loading && pendingGroups.length === 0" class="empty-state">
            <i class="el-icon-s-check" />
            <p>{{ list.length === 0 ? '暂无待审批的交接申请' : '没有符合筛选条件的交接申请' }}</p>
          </div>
          <div v-else class="task-collapse">
            <div v-if="loading" class="loading-bar"><i class="el-icon-loading" /> 加载中...</div>
            <div
              v-for="g in pendingGroups"
              :key="g.key"
              class="task-panel"
              :class="{ 'is-open': isOpen('pending', g.key) }"
            >
              <div class="tp-head" @click="toggleGroup('pending', g.key)">
                <div class="ct-icon"><i class="el-icon-s-order" /></div>
                <div class="ct-main">
                  <span class="ct-name" :title="g.name">{{ g.name }}</span>
                  <span class="pending-tag">{{ g.list.length }} 条待审批</span>
                  <span class="ct-meta">{{ applicantText(g.list) }}</span>
                </div>
                <i class="el-icon-arrow-down tp-arrow" />
              </div>
              <div v-show="isOpen('pending', g.key)" class="tp-body">
                <el-table :data="g.list" style="width:100%">
                  <el-table-column label="申请人 → 接手人" min-width="235">
                    <template slot-scope="{ row }">
                      <span class="who">{{ row.fromUserName }}<span v-if="row.fromUserId" class="emp-no">{{ row.fromUserId }}</span></span>
                      <i class="el-icon-right arrow" />
                      <span class="who to">{{ row.toUserName }}<span v-if="row.toUserId" class="emp-no">{{ row.toUserId }}</span></span>
                    </template>
                  </el-table-column>
                  <el-table-column label="交接范围" width="130" align="center">
                    <template slot-scope="{ row }">{{ scopeText(row) }}</template>
                  </el-table-column>
                  <el-table-column prop="remark" label="交接说明" min-width="180" show-overflow-tooltip>
                    <template slot-scope="{ row }">{{ row.remark || '—' }}</template>
                  </el-table-column>
                  <el-table-column prop="applyTime" label="申请时间" width="170" />
                  <el-table-column label="操作" width="170" align="right" fixed="right">
                    <template slot-scope="{ row }">
                      <button class="btn-approve" @click="openApprove(row)"><i class="el-icon-check" /> 同意</button>
                      <button class="btn-reject" @click="openReject(row)"><i class="el-icon-close" /> 拒绝</button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </template>

        <!-- 本部门交接记录（含待审批与已处理，只读，表格，按任务折叠） -->
        <template v-else>
          <section class="tip-bar">
            <i class="el-icon-info" />
            以下为本部门的交接记录（您有审批权限的范围，含待审批与已处理，只读）：审批通过后归属与节点处理人即时迁移，是否同步任务配置名单以审批人当时的确认结果为准。
          </section>

          <div v-if="!recordsLoading && recordGroups.length === 0" class="empty-state">
            <i class="el-icon-time" />
            <p>{{ recordsList.length === 0 ? '本部门暂无交接记录' : '没有符合筛选条件的交接记录' }}</p>
          </div>
          <div v-else class="task-collapse">
            <div v-if="recordsLoading" class="loading-bar"><i class="el-icon-loading" /> 加载中...</div>
            <div
              v-for="g in recordGroups"
              :key="g.key"
              class="task-panel"
              :class="{ 'is-open': isOpen('records', g.key) }"
            >
              <div class="tp-head" @click="toggleGroup('records', g.key)">
                <div class="ct-icon"><i class="el-icon-s-order" /></div>
                <div class="ct-main">
                  <span class="ct-name" :title="g.name">{{ g.name }}</span>
                  <span class="pending-tag">{{ g.list.length }} 条</span>
                  <span class="ct-meta">{{ sumText(g.list) }}</span>
                </div>
                <i class="el-icon-arrow-down tp-arrow" />
              </div>
              <div v-show="isOpen('records', g.key)" class="tp-body">
                <el-table :data="g.list" style="width:100%">
                  <el-table-column label="申请人 → 接手人" min-width="235">
                    <template slot-scope="{ row }">
                      <span class="who">{{ row.fromUserName }}<span v-if="row.fromUserId" class="emp-no">{{ row.fromUserId }}</span></span>
                      <i class="el-icon-right arrow" />
                      <span class="who to">{{ row.toUserName }}<span v-if="row.toUserId" class="emp-no">{{ row.toUserId }}</span></span>
                    </template>
                  </el-table-column>
                  <el-table-column label="交接范围" width="130" align="center">
                    <template slot-scope="{ row }">{{ scopeText(row) }}</template>
                  </el-table-column>
                  <el-table-column label="同步名单" width="90" align="center">
                    <template slot-scope="{ row }">
                      <span class="sync-tag" :class="row.syncMember === 1 ? 'on' : 'off'">{{ syncText(row) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="85" align="center">
                    <template slot-scope="{ row }">
                      <span class="st-badge" :class="statusCls(row.status)">{{ statusText(row.status) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="审批人" width="128">
                    <template slot-scope="{ row }">
                      <template v-if="row.approverName">
                        <span class="who">{{ row.approverName }}</span>
                        <span v-if="row.approverId" class="emp-no">{{ row.approverId }}</span>
                      </template>
                      <span v-else>—</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="说明/意见" min-width="150" show-overflow-tooltip>
                    <template slot-scope="{ row }">{{ opinionText(row) }}</template>
                  </el-table-column>
                  <el-table-column prop="applyTime" label="申请时间" width="170" />
                  <el-table-column label="审批时间" width="170">
                    <template slot-scope="{ row }">{{ row.approveTime || '—' }}</template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </template>
      </section>
    </main>

    <!-- 同意确认（是否同步名单由审批人在此决定） -->
    <el-dialog title="确认交接" :visible.sync="approveVisible" width="480px" append-to-body :close-on-click-modal="false">
      <div v-if="cur" class="ap-tip">
        确认将 <b>{{ whoText(cur.fromUserName, cur.fromUserId) }}</b> 名下「{{ cur.dispatchName }}」的 <b>{{ scopeText(cur) }}</b>交接给 <b>{{ whoText(cur.toUserName, cur.toUserId) }}</b>？
        <div class="ap-sub">归属该申请人的期次会整期交出；其在这条任务里参与过的节点（含已提交的历史节点）仅换处理人，期次归属不变——接手人将能看到这些经办痕迹。</div>
      </div>
      <el-checkbox v-if="cur && cur.syncMemberApplicable" v-model="approveSync">同时把任务配置名单中的「{{ cur.fromUserName }}」改为「{{ cur.toUserName }}」（后续新期次下发给接手人）</el-checkbox>
      <div v-else-if="cur" class="ap-sub"><i class="el-icon-info" /> 该申请人不在任务配置名单中（仅作为节点处理人参与），无需同步配置名单。</div>
      <div v-if="cur && cur.remark" class="ap-remark">交接说明：{{ cur.remark }}</div>
      <div slot="footer">
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doApprove"><i class="el-icon-check" /> 确认通过</el-button>
      </div>
    </el-dialog>

    <!-- 拒绝原因 -->
    <el-dialog title="拒绝交接申请" :visible.sync="rejectVisible" width="480px" append-to-body :close-on-click-modal="false">
      <div v-if="cur" class="ap-tip">拒绝 <b>{{ whoText(cur.fromUserName, cur.fromUserId) }}</b> 向 <b>{{ whoText(cur.toUserName, cur.toUserId) }}</b> 的交接申请「{{ cur.dispatchName }}」。</div>
      <el-input v-model="rejectReason" type="textarea" :rows="3" maxlength="120" show-word-limit placeholder="选填：请说明拒绝原因" />
      <div slot="footer">
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doReject"><i class="el-icon-close" /> 确认拒绝</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getPendingHandovers, getPendingHandoverCount, getApproverHandoverRecords, approveHandover, rejectHandover } from '@/service/sys/TaskHandoverService'

export default {
  name: 'TaskHandoverApproval',
  data() {
    return {
      tab: 'pending',
      loading: false,
      acting: false,
      // 任务筛选（'' / undefined = 全部）
      filterTask: '',
      list: [],
      pendingCount: 0,
      // 本部门交接记录
      recordsLoading: false,
      recordsList: [],
      // 折叠面板展开的分组（按页签分别记录；null 表示尚未初始化，首次加载默认全部展开）
      openMap: { pending: null, records: null },
      // 审批弹窗
      approveVisible: false,
      approveSync: false,
      cur: null,
      // 拒绝弹窗
      rejectVisible: false,
      rejectReason: ''
    }
  },
  computed: {
    /** 当前页签数据里的任务选项（供筛选下拉） */
    taskOptions() {
      const list = this.tab === 'pending' ? this.list : this.recordsList
      const seen = {}
      const out = []
      ;(list || []).forEach(r => {
        const k = r.dispatchId || ''
        if (!seen[k]) {
          seen[k] = 1
          out.push({ key: k, name: r.dispatchName || '未知任务' })
        }
      })
      return out
    },
    filteredPending() { return this.filterByTask(this.list) },
    filteredRecords() { return this.filterByTask(this.recordsList) },
    /** 待我审批：按任务分组 */
    pendingGroups() { return this.groupByTask(this.filteredPending) },
    /** 本部门交接记录：按任务分组 */
    recordGroups() { return this.groupByTask(this.filteredRecords) },
    /** 当前页签生效的分组 */
    visibleGroups() { return this.tab === 'pending' ? this.pendingGroups : this.recordGroups }
  },
  watch: {
    /** 筛选变化后按新分组集合重算展开状态（新出现的分组默认展开） */
    filterTask() {
      this.$nextTick(() => this.syncOpenState(this.tab, this.visibleGroups))
    }
  },
  mounted() {
    this.fetchAll()
  },
  methods: {
    statusText(s) { return { 0: '待审批', 1: '已通过', 2: '已拒绝' }[s] || '—' },
    statusCls(s) { return { 0: 'st-pending', 1: 'st-approved', 2: 'st-rejected' }[s] || '' },
    /** 是否同步名单：仅审批通过后才有结论；交接人不在配置名单中时为「不适用」 */
    syncText(row) {
      if (!row || row.status !== 1) return '—'
      if (row.syncMemberApplicable === false) return '不适用'
      return row.syncMember === 1 ? '同步' : '不同步'
    },
    /** 交接范围文案：期次 / 节点席位按实际存在项组合展示 */
    scopeText(row) {
      if (!row) return '—'
      const parts = []
      if ((row.periodCount || 0) > 0) parts.push(row.periodCount + ' 期')
      if ((row.nodeCount || 0) > 0) parts.push(row.nodeCount + ' 条节点记录')
      return parts.length ? parts.join(' 与 ') : '—'
    },
    /** 姓名 + 员工号文案（审批确认弹窗用，避免重名歧义） */
    whoText(name, id) {
      if (!name) return '—'
      return id ? name + '（' + id + '）' : name
    },
    /** 说明/意见：拒绝优先展示拒绝原因，通过展示审批说明，待审批展示申请说明 */
    opinionText(row) {
      if (!row) return '—'
      if (row.status === 2) return row.rejectReason || '已拒绝'
      if (row.status === 1) return row.remark || '已通过'
      return row.remark || '—'
    },
    /** 任务筛选（空表示不过滤） */
    filterByTask(list) {
      if (!this.filterTask) return list || []
      return (list || []).filter(r => (r.dispatchId || '') === this.filterTask)
    },
    /** 按任务分组（同一任务的申请收在同一个面板里，保持后端时间倒序下的首现顺序） */
    groupByTask(list) {
      const groups = []
      const index = {}
      ;(list || []).forEach(r => {
        const key = r.dispatchId || 'unknown'
        if (index[key] === undefined) {
          index[key] = groups.length
          groups.push({ key, name: r.dispatchName || '未知任务', list: [] })
        }
        groups[index[key]].list.push(r)
      })
      return groups
    },
    /** 分组汇总文案：待审批 x · 已通过 y · 已拒绝 z */
    sumText(list) {
      const cnt = { 0: 0, 1: 0, 2: 0 }
      ;(list || []).forEach(r => { if (cnt[r.status] !== undefined) cnt[r.status]++ })
      const parts = []
      if (cnt[0]) parts.push('待审批 ' + cnt[0])
      if (cnt[1]) parts.push('已通过 ' + cnt[1])
      if (cnt[2]) parts.push('已拒绝 ' + cnt[2])
      return parts.join(' · ') || '—'
    },
    /** 待审批分组的申请人概览（去重，最多列 3 人） */
    applicantText(list) {
      const names = []
      ;(list || []).forEach(r => {
        if (r.fromUserName && names.indexOf(r.fromUserName) < 0) names.push(r.fromUserName)
      })
      if (!names.length) return '—'
      const head = names.slice(0, 3).join('、')
      return '申请人：' + head + (names.length > 3 ? ' 等 ' + names.length + ' 人' : '')
    },
    isOpen(tabKey, key) {
      const arr = this.openMap[tabKey] || []
      return arr.indexOf(key) >= 0
    },
    toggleGroup(tabKey, key) {
      const cur = this.openMap[tabKey] || []
      const next = cur.indexOf(key) >= 0 ? cur.filter(k => k !== key) : cur.concat(key)
      this.$set(this.openMap, tabKey, next)
    },
    /** 数据加载后同步展开状态：首次默认全部展开，之后保留用户收起的分组；新出现的分组默认展开（筛选后结果不被折叠隐藏） */
    syncOpenState(tabKey, groups) {
      const keys = groups.map(g => g.key)
      const prev = this.openMap[tabKey]
      if (prev === null) {
        this.$set(this.openMap, tabKey, keys)
        return
      }
      const next = prev.filter(k => keys.indexOf(k) >= 0)
      keys.forEach(k => { if (prev.indexOf(k) < 0) next.push(k) })
      this.$set(this.openMap, tabKey, next)
    },
    /** 数据变化后，若筛选的任务已不存在则清空筛选 */
    ensureFilterValid() {
      if (!this.filterTask) return
      if (!this.taskOptions.some(o => o.key === this.filterTask)) this.filterTask = ''
    },
    switchTab(t) {
      if (this.tab === t) return
      this.tab = t
      // 两个页签的任务集合不同，切换时重置筛选，避免筛出空列表
      this.filterTask = ''
      if (t === 'pending') this.fetchPending()
      else this.fetchRecords()
    },
    refresh() {
      if (this.tab === 'pending') this.fetchAll()
      else this.fetchRecords()
    },
    async fetchAll() {
      await Promise.all([this.fetchPending(), this.fetchRecords()])
    },
    async fetchPending() {
      this.loading = true
      try {
        const [res, cnt] = await Promise.all([getPendingHandovers(), getPendingHandoverCount()])
        this.list = (res && res.data) || []
        this.pendingCount = (cnt && cnt.data) || 0
        this.ensureFilterValid()
        this.syncOpenState('pending', this.pendingGroups)
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    /** 本部门交接记录（审批权限范围内，含全部状态） */
    async fetchRecords() {
      this.recordsLoading = true
      try {
        const res = await getApproverHandoverRecords()
        this.recordsList = (res && res.data) || []
        this.ensureFilterValid()
        this.syncOpenState('records', this.recordGroups)
      } catch (e) {
        console.error(e)
      } finally {
        this.recordsLoading = false
      }
    },
    openApprove(row) {
      this.cur = row
      // 是否同步名单由审批人决定，默认不同步
      this.approveSync = false
      this.approveVisible = true
    },
    openReject(row) {
      this.cur = row
      this.rejectReason = ''
      this.rejectVisible = true
    },
    async doApprove() {
      if (!this.cur) return
      this.acting = true
      try {
        const res = await approveHandover(this.cur.id, this.approveSync ? 1 : 0)
        this.$message.success((res && res.message) || '已通过，交接完成')
        this.approveVisible = false
        this.fetchAll()
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '操作失败')
      } finally {
        this.acting = false
      }
    },
    async doReject() {
      if (!this.cur) return
      this.acting = true
      try {
        const res = await rejectHandover(this.cur.id, (this.rejectReason && this.rejectReason.trim()) || null)
        this.$message.success((res && res.message) || '已拒绝')
        this.rejectVisible = false
        this.fetchAll()
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '操作失败')
      } finally {
        this.acting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.dashboard-container { display: flex; min-height: 100vh; background: var(--color-primary-surface); color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
}
.page-heading { display: flex; align-items: center; gap: 8px; font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.head-badge { min-width: 20px; height: 20px; padding: 0 6px; border-radius: 10px; background: #EF4444; color: #fff; font-size: 12px; font-weight: 700; line-height: 20px; text-align: center; }
.header-actions { display: flex; align-items: center; gap: 10px; }

// 页头筛选条件
.head-filter { display: flex; align-items: center; gap: 8px; }
.head-filter-label { font-size: 13px; color: #757575; white-space: nowrap; }
.head-select { width: 200px;
  ::v-deep .el-input__inner { height: 36px; line-height: 36px; border-color: $border; border-radius: 2px; font-size: 13px;
    &:focus { border-color: $primary; }
  }
  ::v-deep .el-input__icon { line-height: 36px; }
}
.btn-refresh { display: flex; align-items: center; gap: 5px; height: 36px; padding: 0 16px; background: #fff; border: 1px solid $border; border-radius: 2px; color: var(--color-primary); font-weight: 600; font-size: 13px; cursor: pointer; transition: all .2s;
  i { color: $primary; }
  &:hover { border-color: $primary; color: $primary; background: var(--color-primary-surface); }
}

// 页签
.tab-bar { display: flex; gap: 8px; }
.tab-item { display: flex; align-items: center; gap: 6px; padding: 10px 20px; background: #fff; border: 1px solid $border; border-radius: 3px; color: #545f72; font-size: 14px; font-weight: 600; cursor: pointer; transition: all .2s;
  &:hover { border-color: $primary; color: $primary; }
  &.active { background: $primary; border-color: $primary; color: #fff; }
}
.tab-badge { min-width: 18px; height: 18px; padding: 0 5px; border-radius: 9px; background: #EF4444; color: #fff; font-size: 11px; font-weight: 700; line-height: 18px; text-align: center; }

.tip-bar { display: flex; align-items: center; gap: 8px; background: var(--color-primary-light); border: 1px solid $border; color: var(--color-primary-hover); font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: $primary; }
  b { color: $primary; }
}

.empty-state { text-align: center; padding: 60px 20px; color: #bbb; background: #fff; border: 1px solid $border; border-radius: 3px;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}

// 折叠面板（卡片式，同「我的任务」任务组）
.task-collapse { position: relative; }
.loading-bar { display: flex; align-items: center; gap: 6px; justify-content: center; padding: 16px; color: $primary; font-size: 13px; }
.task-panel { background: #fff; border: 1px solid $border; border-radius: 3px; margin-bottom: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  &:hover .tp-arrow { color: $primary; }
  &.is-open .tp-arrow { transform: rotate(180deg); }
}
.tp-head { display: flex; align-items: center; gap: 10px; padding: 10px 14px; cursor: pointer; transition: background .2s;
  &:hover { background: var(--color-primary-surface); }
}
.ct-icon { width: 40px; height: 40px; border-radius: 3px; background: var(--color-primary-light); color: $primary; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.ct-main { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; white-space: nowrap; }
.ct-name { font-size: 15px; font-weight: 700; color: #1b1c1c; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pending-tag { padding: 2px 10px; background: rgba(var(--color-primary-rgb),0.1); color: $primary; border-radius: 3px; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.ct-meta { font-size: 12px; color: #909399; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-left: 4px; flex-shrink: 1; }
.tp-arrow { color: #94A3B8; font-size: 14px; flex-shrink: 0; transition: transform .25s; }
.tp-body { border-top: 1px solid $border; padding: 12px 14px; }

.who { font-weight: 600; color: #1b1c1c; }
.who.to { color: $primary; }
// 员工号（跟在姓名后的小字，表格与卡片通用）
.emp-no { margin-left: 5px; font-size: 11px; font-weight: 400; color: #A8B0BF; }
.arrow { color: #b7c3d8; margin: 0 6px; }
.sync-tag { display: inline-block; padding: 1px 8px; border-radius: 3px; font-size: 12px; font-weight: 600;
  &.on { background: rgba(21,128,61,0.1); color: #15803D; }
  &.off { background: #f0f0f0; color: #909399; }
}
.st-badge { display: inline-block; padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 600;
  &.st-pending { background: rgba(217,119,6,0.12); color: #B45309; }
  &.st-approved { background: rgba(21,128,61,0.1); color: #15803D; }
  &.st-rejected { background: rgba(239,68,68,0.1); color: #DC2626; }
}
.btn-approve { display: inline-flex; align-items: center; gap: 3px; margin-right: 8px; padding: 5px 12px; background: $primary; color: #fff; border: none; border-radius: 2px; cursor: pointer; font-size: 12px; font-weight: 600;
  &:hover { opacity: 0.9; }
}
.btn-reject { display: inline-flex; align-items: center; gap: 3px; padding: 5px 12px; background: #fff; color: #DC2626; border: 1px solid #FCA5A5; border-radius: 2px; cursor: pointer; font-size: 12px; font-weight: 600;
  &:hover { background: #FEF2F2; }
}
.ap-tip { font-size: 13px; color: #414755; line-height: 1.7; margin-bottom: 14px;
  b { color: $primary; }
}
.ap-remark { font-size: 12px; color: #909399; margin-top: 8px; }
.ap-sub { font-size: 12px; color: #909399; line-height: 1.7; margin-top: 6px;
  i { color: #b7c3d8; }
}
</style>
