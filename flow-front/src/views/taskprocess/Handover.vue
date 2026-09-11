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
              <span class="active">任务交接</span>
            </nav>
            <h3 class="page-heading">任务交接</h3>
          </div>
          <div class="header-actions">
            <div class="head-filter">
              <label class="head-filter-label">状态</label>
              <el-select v-model="filterStatus" class="head-select" placeholder="全部">
                <el-option label="全部" value="" />
                <el-option label="待审批" :value="0" />
                <el-option label="已通过" :value="1" />
                <el-option label="已拒绝" :value="2" />
              </el-select>
            </div>
            <button class="btn-refresh" @click="refresh"><i class="el-icon-refresh" /> 刷新</button>
          </div>
        </div>

        <!-- 页签：申请人视角；审批入口在「管理员 / 交接审批」 -->
        <div class="tab-bar">
          <button class="tab-item" :class="{ active: tab === 'mine' }" @click="switchTab('mine')">
            <i class="el-icon-s-promotion" /> 我的交接申请
            <span v-if="minePending > 0" class="tab-badge">{{ minePending }}</span>
          </button>
          <button class="tab-item" :class="{ active: tab === 'records' }" @click="switchTab('records')">
            <i class="el-icon-time" /> 交接记录
          </button>
        </div>

        <!-- 我的交接申请（按创建部门折叠） -->
        <template v-if="tab === 'mine'">
          <section class="tip-bar">
            <i class="el-icon-info" />
            提交后需创建人同部门的部门管理员审批；审批通过后，你名下的期次归属将整期转给接手人，你在这条任务里参与过的节点（含已提交的历史节点）也一并转给接手人，期次归属不受影响。
          </section>

          <div v-if="!loading && mineGroups.length === 0" class="empty-state">
            <i class="el-icon-s-promotion" />
            <p>{{ mineList.length === 0 ? '暂无交接申请' : '没有符合筛选条件的交接申请' }}</p>
          </div>
          <div v-else class="task-collapse">
            <div v-if="loading" class="loading-bar"><i class="el-icon-loading" /> 加载中...</div>
            <div
              v-for="g in mineGroups"
              :key="g.key"
              class="task-panel"
              :class="{ 'is-open': isOpen('mine', g.key) }"
            >
              <div class="tp-head" @click="toggleGroup('mine', g.key)">
                <div class="ct-icon"><i class="el-icon-office-building" /></div>
                <div class="ct-main">
                  <span class="ct-name" :title="g.name">{{ g.name }}</span>
                  <span class="pending-tag">{{ g.list.length }} 条</span>
                  <span class="ct-meta">{{ sumText(g.list) }}</span>
                </div>
                <i class="el-icon-arrow-down tp-arrow" />
              </div>
              <div v-show="isOpen('mine', g.key)" class="tp-body">
                <el-table :data="g.list" style="width:100%">
                  <el-table-column prop="dispatchName" label="任务名称" min-width="200" show-overflow-tooltip />
                  <el-table-column label="接手人" width="132">
                    <template slot-scope="{ row }">
                      <span class="who">{{ row.toUserName }}</span>
                      <span v-if="row.toUserId" class="emp-no">{{ row.toUserId }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="85" align="center">
                    <template slot-scope="{ row }">
                      <span class="st-badge" :class="statusCls(row.status)">{{ statusText(row.status) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="审批人" width="132">
                    <template slot-scope="{ row }">
                      <template v-if="row.approverName">
                        <span class="who">{{ row.approverName }}</span>
                        <span v-if="row.approverId" class="emp-no">{{ row.approverId }}</span>
                      </template>
                      <span v-else>—</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="审批意见" min-width="150" show-overflow-tooltip>
                    <template slot-scope="{ row }">{{ row.status === 2 ? (row.rejectReason || '已拒绝') : (row.status === 1 ? '已通过' : '—') }}</template>
                  </el-table-column>
                  <el-table-column prop="applyTime" label="申请时间" width="170" />
                  <el-table-column label="审批时间" width="170">
                    <template slot-scope="{ row }">{{ row.approveTime || '—' }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="150" align="right" fixed="right">
                    <template slot-scope="{ row }">
                      <button class="btn-link" @click="openDetail(row)"><i class="el-icon-document" /> 详情</button>
                      <button class="btn-link" @click="openDispatchRecords(row)"><i class="el-icon-time" /> 记录</button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </template>

        <!-- 交接记录（只读，表格，按创建部门折叠） -->
        <template v-else>
          <section class="tip-bar">
            <i class="el-icon-info" />
            以下仅展示与您相关的交接记录（您是交接人或接手人），他人之间的交接不予展示；审批通过后归属与节点处理人即时迁移，是否同步任务配置名单以审批人确认为准。
          </section>

          <div v-if="!loading && recordGroups.length === 0" class="empty-state">
            <i class="el-icon-time" />
            <p>{{ recordsList.length === 0 ? '暂无交接记录' : '没有符合筛选条件的交接记录' }}</p>
          </div>
          <div v-else class="task-collapse">
            <div v-if="loading" class="loading-bar"><i class="el-icon-loading" /> 加载中...</div>
            <div
              v-for="g in recordGroups"
              :key="g.key"
              class="task-panel"
              :class="{ 'is-open': isOpen('records', g.key) }"
            >
              <div class="tp-head" @click="toggleGroup('records', g.key)">
                <div class="ct-icon"><i class="el-icon-office-building" /></div>
                <div class="ct-main">
                  <span class="ct-name" :title="g.name">{{ g.name }}</span>
                  <span class="pending-tag">{{ g.list.length }} 条</span>
                  <span class="ct-meta">{{ sumText(g.list) }}</span>
                </div>
                <i class="el-icon-arrow-down tp-arrow" />
              </div>
              <div v-show="isOpen('records', g.key)" class="tp-body">
                <el-table :data="g.list" style="width:100%">
                  <el-table-column prop="dispatchName" label="任务名称" min-width="200" show-overflow-tooltip />
                  <el-table-column label="交接人" width="128">
                    <template slot-scope="{ row }">
                      <span class="who">{{ row.fromUserName }}</span>
                      <span v-if="row.fromUserId" class="emp-no">{{ row.fromUserId }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="接手人" width="128">
                    <template slot-scope="{ row }">
                      <span class="who to">{{ row.toUserName }}</span>
                      <span v-if="row.toUserId" class="emp-no">{{ row.toUserId }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="我的角色" width="90" align="center">
                    <template slot-scope="{ row }">
                      <span v-if="row.myRole" class="role-tag" :class="row.myRole === '接手人' ? 'to' : 'from'">{{ row.myRole }}</span>
                      <span v-else>—</span>
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
                  <el-table-column prop="applyTime" label="申请时间" width="170" />
                  <el-table-column label="审批时间" width="170">
                    <template slot-scope="{ row }">{{ row.approveTime || '—' }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="95" align="right" fixed="right">
                    <template slot-scope="{ row }">
                      <button class="btn-link" @click="openDetail(row)"><i class="el-icon-document" /> 详情</button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </template>
      </section>
    </main>

    <!-- 单条交接申请详情（首页省略的交接范围 / 同步名单在此查看，表单卡片） -->
    <el-dialog :title="detailTitle" :visible.sync="detailVisible" width="660px" append-to-body>
      <div v-if="detailRow" class="rec-list">
        <div class="rec-card">
          <div class="rc-head">
            <div class="rc-who">
              <span class="who">{{ detailRow.fromUserName }}<span v-if="detailRow.fromUserId" class="emp-no">{{ detailRow.fromUserId }}</span></span>
              <i class="el-icon-right arrow" />
              <span class="who to">{{ detailRow.toUserName }}<span v-if="detailRow.toUserId" class="emp-no">{{ detailRow.toUserId }}</span></span>
              <span v-if="detailRow.myRole" class="role-tag" :class="detailRow.myRole === '接手人' ? 'to' : 'from'">{{ detailRow.myRole }}</span>
            </div>
            <div class="rc-right">
              <span class="rc-time"><i class="el-icon-time" /> {{ detailRow.applyTime || '—' }}</span>
              <span class="st-badge" :class="statusCls(detailRow.status)">{{ statusText(detailRow.status) }}</span>
            </div>
          </div>
          <div class="rc-body">
            <div class="rc-item span-2">
              <span class="rc-label">任务名称</span>
              <span class="rc-value">{{ detailRow.dispatchName || '—' }}</span>
            </div>
            <div class="rc-item">
              <span class="rc-label">交接人</span>
              <span class="rc-value">{{ whoText(detailRow.fromUserName, detailRow.fromUserId) }}</span>
            </div>
            <div class="rc-item">
              <span class="rc-label">接手人</span>
              <span class="rc-value">{{ whoText(detailRow.toUserName, detailRow.toUserId) }}</span>
            </div>
            <div class="rc-item">
              <span class="rc-label">创建部门</span>
              <span class="rc-value">{{ detailRow.deptName || '—' }}</span>
            </div>
            <div class="rc-item">
              <span class="rc-label">交接范围</span>
              <span class="rc-value">{{ scopeText(detailRow) }}</span>
            </div>
            <div class="rc-item">
              <span class="rc-label">同步名单</span>
              <span class="rc-value"><span class="sync-tag" :class="detailRow.syncMember === 1 ? 'on' : 'off'">{{ syncText(detailRow) }}</span></span>
            </div>
            <div class="rc-item">
              <span class="rc-label">审批人</span>
              <span class="rc-value">{{ whoText(detailRow.approverName, detailRow.approverId) }}</span>
            </div>
            <div class="rc-item">
              <span class="rc-label">申请时间</span>
              <span class="rc-value">{{ detailRow.applyTime || '—' }}</span>
            </div>
            <div class="rc-item">
              <span class="rc-label">审批时间</span>
              <span class="rc-value">{{ detailRow.approveTime || '—' }}</span>
            </div>
            <div class="rc-item span-2">
              <span class="rc-label">说明/意见</span>
              <span class="rc-value">{{ opinionText(detailRow) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 某任务的交接记录（完整审计，表单卡片） -->
    <el-dialog :title="dispatchRecordsTitle" :visible.sync="dispatchRecordsVisible" width="820px" append-to-body>
      <div v-loading="dispatchRecordsLoading" class="dialog-body">
        <div v-if="!dispatchRecordsLoading && dispatchRecordsList.length === 0" class="dialog-empty">暂无交接记录</div>
        <div v-else class="rec-list">
          <div v-for="row in dispatchRecordsList" :key="row.id" class="rec-card">
            <div class="rc-head">
              <div class="rc-who">
                <span class="who">{{ row.fromUserName }}<span v-if="row.fromUserId" class="emp-no">{{ row.fromUserId }}</span></span>
                <i class="el-icon-right arrow" />
                <span class="who to">{{ row.toUserName }}<span v-if="row.toUserId" class="emp-no">{{ row.toUserId }}</span></span>
              </div>
              <div class="rc-right">
                <span class="rc-time"><i class="el-icon-time" /> {{ row.applyTime || '—' }}</span>
                <span class="st-badge" :class="statusCls(row.status)">{{ statusText(row.status) }}</span>
              </div>
            </div>
            <div class="rc-body">
              <div class="rc-item span-2">
                <span class="rc-label">任务名称</span>
                <span class="rc-value">{{ row.dispatchName || '—' }}</span>
              </div>
              <div class="rc-item">
                <span class="rc-label">交接范围</span>
                <span class="rc-value">{{ scopeText(row) }}</span>
              </div>
              <div class="rc-item">
                <span class="rc-label">同步名单</span>
                <span class="rc-value"><span class="sync-tag" :class="row.syncMember === 1 ? 'on' : 'off'">{{ syncText(row) }}</span></span>
              </div>
              <div class="rc-item">
                <span class="rc-label">审批人</span>
                <span class="rc-value">{{ row.approverName || '—' }}</span>
              </div>
              <div class="rc-item">
                <span class="rc-label">审批时间</span>
                <span class="rc-value">{{ row.approveTime || '—' }}</span>
              </div>
              <div class="rc-item span-2">
                <span class="rc-label">说明/意见</span>
                <span class="rc-value">{{ opinionText(row) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getMyHandovers, getHandoversByDispatch, getHandoverRecords } from '@/service/sys/TaskHandoverService'

export default {
  name: 'TaskHandover',
  data() {
    return {
      tab: 'mine',
      loading: false,
      // 状态筛选（'' = 全部；0 待审批 / 1 已通过 / 2 已拒绝）
      filterStatus: '',
      mineList: [],
      recordsList: [],
      // 折叠面板展开的分组（按页签分别记录；null 表示尚未初始化，首次加载默认全部展开）
      openMap: { mine: null, records: null },
      // 单条交接申请详情（弹窗）
      detailVisible: false,
      detailRow: null,
      detailTitle: '交接详情',
      // 某任务的交接记录（弹窗）
      dispatchRecordsVisible: false,
      dispatchRecordsLoading: false,
      dispatchRecordsList: [],
      dispatchRecordsTitle: '交接记录'
    }
  },
  computed: {
    /** 我发起的申请中待审批条数（页签角标，不受筛选影响） */
    minePending() { return (this.mineList || []).filter(r => r.status === 0).length },
    filteredMine() { return this.filterByStatus(this.mineList) },
    filteredRecords() { return this.filterByStatus(this.recordsList) },
    /** 我的交接申请：按创建部门分组 */
    mineGroups() { return this.groupByDept(this.filteredMine) },
    /** 交接记录：按创建部门分组 */
    recordGroups() { return this.groupByDept(this.filteredRecords) },
    /** 当前页签生效的分组 */
    visibleGroups() { return this.tab === 'mine' ? this.mineGroups : this.recordGroups }
  },
  watch: {
    /** 筛选变化后按新分组集合重算展开状态（新出现的分组默认展开） */
    filterStatus() {
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
    /** 说明/意见：拒绝优先展示拒绝原因，通过展示审批说明，待审批展示申请说明 */
    opinionText(row) {
      if (!row) return '—'
      if (row.status === 2) return row.rejectReason || '已拒绝'
      if (row.status === 1) return row.remark || '已通过'
      return row.remark || '—'
    },
    /** 状态筛选（'' 表示不过滤） */
    filterByStatus(list) {
      if (this.filterStatus === '' || this.filterStatus === null || this.filterStatus === undefined) return list || []
      return (list || []).filter(r => r.status === this.filterStatus)
    },
    /** 按创建部门分组（保持后端时间倒序下的首现顺序） */
    groupByDept(list) {
      const groups = []
      const index = {}
      ;(list || []).forEach(r => {
        const key = r.deptId === null || r.deptId === undefined ? 'unknown' : String(r.deptId)
        if (index[key] === undefined) {
          index[key] = groups.length
          groups.push({ key, name: r.deptName || '未知部门', list: [] })
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
    switchTab(t) {
      this.tab = t
      if (t === 'mine') this.fetchMine()
      else this.fetchRecords()
    },
    async fetchAll() {
      await Promise.all([this.fetchMine(), this.fetchRecords()])
    },
    async fetchMine() {
      this.loading = true
      try {
        const res = await getMyHandovers()
        this.mineList = (res && res.data) || []
        this.syncOpenState('mine', this.mineGroups)
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async fetchRecords() {
      this.loading = true
      try {
        const res = await getHandoverRecords()
        this.recordsList = (res && res.data) || []
        this.syncOpenState('records', this.recordGroups)
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    refresh() {
      this.fetchAll()
    },
    /** 姓名 + 员工号文案（弹窗表单用） */
    whoText(name, id) {
      if (!name) return '—'
      return id ? name + '（' + id + '）' : name
    },
    /** 查看单条交接申请的完整详情（交接范围 / 同步名单等，首页表格不展示的字段在此查看） */
    openDetail(row) {
      if (!row) return
      this.detailRow = row
      this.detailTitle = '交接详情 · ' + (row.dispatchName || '')
      this.detailVisible = true
    },
    /** 查看该任务配置的完整交接记录（全部状态） */
    async openDispatchRecords(row) {
      this.dispatchRecordsTitle = '交接记录 · ' + (row.dispatchName || '')
      this.dispatchRecordsList = []
      this.dispatchRecordsVisible = true
      this.dispatchRecordsLoading = true
      try {
        const res = await getHandoversByDispatch(row.dispatchId)
        this.dispatchRecordsList = (res && res.data) || []
      } catch (e) {
        console.error(e)
        this.$notifyError(e, '交接记录加载失败')
      } finally {
        this.dispatchRecordsLoading = false
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
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.header-actions { display: flex; align-items: center; gap: 10px; }

// 页头筛选条件
.head-filter { display: flex; align-items: center; gap: 8px; }
.head-filter-label { font-size: 13px; color: #757575; white-space: nowrap; }
.head-select { width: 150px;
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

// 交接记录：表单式卡片
.rec-list { display: flex; flex-direction: column; gap: 10px; }
.rec-card { border: 1px solid #E2E8F0; border-radius: 3px; background: #fff; transition: box-shadow .2s;
  &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
}
.rc-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 9px 14px; background: var(--color-primary-surface); border-bottom: 1px solid #E2E8F0; }
.rc-who { display: flex; align-items: center; gap: 6px; min-width: 0; }
.rc-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.rc-time { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399;
  i { color: #b7c3d8; }
}
.rc-body { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 30px; padding: 12px 14px; }
.rc-item { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.rc-label { flex: 0 0 70px; font-size: 12px; color: #909399; }
.rc-value { flex: 1; min-width: 0; font-size: 13px; color: #1b1c1c; word-break: break-word; }
// 一行两个字段；跨列占满整行
.span-2 { grid-column: span 2; }

.dialog-body { min-height: 80px; }
.dialog-empty { text-align: center; padding: 32px 20px; color: #bbb; font-size: 13px; }

.who { font-weight: 600; color: #1b1c1c; }
.who.to { color: $primary; }
// 员工号（跟在姓名后的小字，表格与卡片通用）
.emp-no { margin-left: 5px; font-size: 11px; font-weight: 400; color: #A8B0BF; }
.arrow { color: #b7c3d8; margin: 0 6px; }
.sync-tag { display: inline-block; padding: 1px 8px; border-radius: 3px; font-size: 12px; font-weight: 600;
  &.on { background: rgba(21,128,61,0.1); color: #15803D; }
  &.off { background: #f0f0f0; color: #909399; }
}
.role-tag { display: inline-block; padding: 1px 8px; border-radius: 3px; font-size: 12px; font-weight: 600; margin-left: 4px;
  &.from { background: rgba(100,116,139,0.12); color: #475569; }
  &.to { background: var(--color-primary-light); color: $primary; }
}
.st-badge { display: inline-block; padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 600;
  &.st-pending { background: rgba(217,119,6,0.12); color: #B45309; }
  &.st-approved { background: rgba(21,128,61,0.1); color: #15803D; }
  &.st-rejected { background: rgba(239,68,68,0.1); color: #DC2626; }
}
.btn-link { display: inline-flex; align-items: center; gap: 3px; padding: 5px 8px; background: transparent; color: $primary; border: none; cursor: pointer; font-size: 12px; font-weight: 600;
  &:hover { text-decoration: underline; }
}
</style>
