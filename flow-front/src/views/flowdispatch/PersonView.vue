<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div class="header-left">
            <div class="hl-row1">
              <button class="btn-back" @click="$router.push('/flow-dispatch/index')"><i class="el-icon-back" /> 返回任务管理</button>
              <nav class="breadcrumb">
                <span>任务管理</span>
                <span>/</span>
                <span class="active">按人员查看</span>
              </nav>
            </div>
            <h3 class="page-heading">按人员查看</h3>
          </div>
          <div class="header-actions">
            <button class="btn-refresh" :disabled="loading" @click="fetchPersons"><i class="el-icon-refresh" /> 刷新</button>
          </div>
        </div>

        <!-- 任务概览条 -->
        <section class="tip-bar">
          <i class="el-icon-s-order" />
          <span class="tip-task">{{ taskName || '任务' }}</span>
          <span class="tip-sep">·</span>
          <span>参与人员 <b>{{ total }}</b> 人</span>
          <span class="tip-sep">·</span>
          <span>累计提交 <b>{{ submitTotal }}</b> 次</span>
          <span class="tip-sep">·</span>
          <span>展示结构：<b>人员 → 任务链 → 流程链</b></span>
        </section>

        <!-- 搜索 -->
        <section class="filter-section">
          <div class="filter-item">
            <label class="filter-label">姓名 / 工号</label>
            <input v-model="filters.name" class="filter-input" placeholder="输入姓名或工号搜索" @keyup.enter="handleSearch">
          </div>
          <div class="filter-actions">
            <button class="btn-reset" :disabled="loading" @click="resetFilters">重置</button>
            <button class="btn-search" :disabled="loading" @click="handleSearch"><i v-if="loading" class="el-icon-loading" /><span v-else>查询</span></button>
          </div>
        </section>

        <!-- 人员折叠列表 -->
        <div v-if="!loading && persons.length === 0" class="empty-state">
          <i class="el-icon-user" />
          <p>该任务下暂无人员提交记录</p>
        </div>
        <template v-else>
          <div v-for="p in persons" :key="p.userId" class="person-panel" :class="{ 'is-open': isPersonOpen(p.userId) }">
            <!-- 一级：人员 -->
            <div class="pp-head" @click="togglePerson(p)">
              <span class="pp-avatar" :style="{ background: avatarColor(p.userId) }">{{ (p.userName || 'U').charAt(0) }}</span>
              <div class="pp-main">
                <div class="pp-name-row">
                  <span class="pp-name">{{ p.userName || '—' }}</span>
                  <span v-if="p.userEmpNo" class="pp-emp">{{ p.userEmpNo }}</span>
                  <span class="pp-dept"><i class="el-icon-office-building" /> {{ p.userDept || '未分配部门' }}</span>
                </div>
                <div class="pp-sub">累计提交 {{ p.submitCount || 0 }} 次 · 参与期次 {{ p.periodCount || 0 }} 个</div>
              </div>
              <div class="pp-meta">
                <span class="pp-last"><i class="el-icon-time" /> 最近提交 {{ p.lastSubmitTime || '—' }}</span>
              </div>
              <i class="el-icon-arrow-down pp-arrow" />
            </div>

            <!-- 展开：任务链列表（每条链 = 该人员参与的一次完整流转） -->
            <div v-show="isPersonOpen(p.userId)" class="pp-body">
              <div v-if="personData[p.userId] && personData[p.userId].loading" class="pp-loading"><i class="el-icon-loading" /> 正在加载任务链...</div>
              <template v-else-if="personData[p.userId]">
                <template v-if="personData[p.userId].chains.length > 0">
                  <div v-for="chain in personData[p.userId].chains" :key="chain.taskId" class="period-panel" :class="{ 'is-open': isChainOpen(p.userId, chain.taskId) }">
                    <!-- 二级：任务链 -->
                    <div class="pdp-head" @click="toggleChain(p.userId, chain.taskId)">
                      <span class="pdp-icon"><i class="el-icon-s-operation" /></span>
                      <div class="pdp-main">
                        <span class="pdp-name">{{ chain.taskName }}</span>
                        <span v-if="chain.periodName" class="pdp-no" :title="'期次：' + chain.periodName">{{ chain.periodName }}<template v-if="chain.periodNo"> · 第{{ chain.periodNo }}期</template></span>
                        <span :class="taskStatusClass(chain.taskStatus)">{{ chain.taskStatus || '—' }}</span>
                      </div>
                      <span class="pdp-count">节点 {{ chainDoneCount(chain) }}/{{ chain.nodes.length }} 已处理</span>
                      <i class="el-icon-arrow-down pdp-arrow" />
                    </div>

                    <!-- 展开：该任务链完整流程（统一 FlowChain 组件，横/竖可切换） -->
                    <div v-show="isChainOpen(p.userId, chain.taskId)" class="pdp-body">
                      <div v-if="chain.detailLoading" class="pp-loading"><i class="el-icon-loading" /> 流程加载中...</div>
                      <FlowChain
                        v-else-if="chain.detail"
                        :task-detail="chain.detail"
                        :selected-handler="{ id: p.userId }"
                        :card="false"
                        title="流程链"
                      />
                      <div v-else class="pp-empty">流程链加载失败，请刷新重试</div>
                    </div>
                  </div>
                </template>
                <div v-else class="pp-empty">该人员在该任务下暂无参与的任务链</div>
              </template>
              <div v-else class="pp-loading"><i class="el-icon-loading" /> 加载中...</div>
            </div>
          </div>

          <!-- 人员分页（最小 5 条/页） -->
          <div class="pagination">
            <div class="pagination-left">
              <span class="pagination-info">共计 {{ total }} 名人员</span>
              <select v-model="pageSize" class="page-size-select" @change="onPageSizeChange">
                <option :value="5">5 条/页</option>
                <option :value="10">10 条/页</option>
                <option :value="20">20 条/页</option>
                <option :value="50">50 条/页</option>
              </select>
            </div>
            <div class="pagination-controls">
              <button class="page-btn" :disabled="page === 1" @click="prevPage"><i class="el-icon-arrow-left" /></button>
              <span class="page-current">{{ page }} / {{ totalPages }}</span>
              <button class="page-btn" :disabled="page === totalPages" @click="nextPage"><i class="el-icon-arrow-right" /></button>
            </div>
          </div>
        </template>
      </section>
    </main>
  </div>
</template>

<script>
import { getPersonList, getPersonRecords } from '@/service/sys/DataService'
import { getTaskDetail } from '@/service/sys/TaskService'
import FlowChain from '@/components/FlowChain.vue'

const AVATAR_COLORS = ['var(--color-primary)', '#B45309', '#15803D', '#2B6CB0', '#6B46C1', '#6366F1', '#047857', '#D97706']

export default {
  name: 'FlowDispatchPersonView',
  components: { FlowChain },
  data() {
    return {
      loading: false,
      taskId: null,
      taskName: '',
      filters: { name: '' },
      persons: [],
      total: 0,
      page: 1,
      pageSize: 5,
      // 展开状态
      openPersonIds: [],
      openChainKeys: [],
      // 每人任务链数据（含各自 taskDetail，供 FlowChain 渲染）
      personData: {}
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    },
    submitTotal() {
      return this.persons.reduce((s, p) => s + (p.submitCount || 0), 0)
    }
  },
  created() {
    this.taskId = this.$route.query.taskId || null
    this.taskName = this.$route.query.taskName || ''
    this.fetchPersons()
  },
  methods: {
    avatarColor(id) {
      if (!id) return AVATAR_COLORS[0]
      // 32 位主键为字符串：改用字符串 hash 取色（Number() 会变 NaN）
      const str = String(id)
      let hash = 0
      for (let i = 0; i < str.length; i++) hash = (hash * 31 + str.charCodeAt(i)) >>> 0
      return AVATAR_COLORS[hash % AVATAR_COLORS.length]
    },
    isPersonOpen(id) { return this.openPersonIds.indexOf(id) >= 0 },
    chainKey(personId, taskId) { return personId + '|' + taskId },
    isChainOpen(personId, taskId) { return this.openChainKeys.indexOf(this.chainKey(personId, taskId)) >= 0 },
    async fetchPersons() {
      this.loading = true
      try {
        const res = await getPersonList({
          name: this.filters.name || undefined,
          taskId: this.taskId || undefined,
          page: this.page,
          limit: this.pageSize
        })
        const d = res.data || {}
        this.persons = d.records || []
        this.total = d.total || 0
        // 清理已不存在人员的展开状态与数据
        const ids = this.persons.map(p => p.userId)
        this.openPersonIds = this.openPersonIds.filter(id => ids.indexOf(id) >= 0)
        Object.keys(this.personData).forEach(id => {
          if (ids.indexOf(id) < 0) this.$delete(this.personData, id)
        })
        this.openChainKeys = this.openChainKeys.filter(k => ids.indexOf(k.split('|')[0]) >= 0)
      } catch (e) {
        console.error(e)
        this.$notifyError(e, '人员列表加载失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() { this.page = 1; this.fetchPersons() },
    resetFilters() { this.filters.name = ''; this.page = 1; this.fetchPersons() },
    onPageSizeChange() { this.page = 1; this.fetchPersons() },
    prevPage() { if (this.page > 1) { this.page--; this.fetchPersons() } },
    nextPage() { if (this.page < this.totalPages) { this.page++; this.fetchPersons() } },
    /** 展开/收起人员：展开时懒加载该人员参与的任务链（每条链再加载 taskDetail 供流程链展示） */
    togglePerson(p) {
      const idx = this.openPersonIds.indexOf(p.userId)
      if (idx >= 0) {
        this.openPersonIds = this.openPersonIds.filter(id => id !== p.userId)
      } else {
        this.openPersonIds = this.openPersonIds.concat(p.userId)
        if (!this.personData[p.userId]) this.loadPersonChains(p)
      }
    },
    async loadPersonChains(p) {
      this.$set(this.personData, p.userId, { loading: true, chains: [] })
      try {
        const res = await getPersonRecords({
          userId: p.userId,
          taskId: this.taskId || undefined,
          page: 1,
          limit: 500
        })
        const rows = (res.data && res.data.records) || []
        const chains = this.groupChains(rows)
        // 默认展开全部任务链（链头可再折叠），并逐链拉取完整任务详情
        chains.forEach(c => {
          const key = this.chainKey(p.userId, c.taskId)
          if (this.openChainKeys.indexOf(key) < 0) this.openChainKeys.push(key)
          this.$set(c, 'detail', null)
          this.$set(c, 'detailLoading', false)
        })
        this.$set(this.personData, p.userId, { loading: false, chains })
        chains.forEach(c => this.loadChainDetail(c))
      } catch (e) {
        console.error(e)
        this.$set(this.personData, p.userId, { loading: false, chains: [] })
        this.$notifyError(e, '任务链加载失败')
      }
    },
    /** 节点行按 taskId 归组成任务链（后端已按 期次倒序 + 链创建顺序 + 链内 sort 升序 返回） */
    groupChains(rows) {
      const chains = []
      const map = {}
      rows.forEach(nd => {
        const key = nd.taskId
        if (!map[key]) {
          map[key] = {
            taskId: key,
            taskName: nd.taskName || '未命名任务',
            taskStatus: nd.taskStatus || '',
            dispatchId: nd.dispatchId,
            periodName: nd.periodName,
            periodNo: nd.periodNo,
            nodes: []
          }
          chains.push(map[key])
        }
        map[key].nodes.push(nd)
      })
      return chains
    },
    /** 拉取单条任务链完整详情（含 templateNodes + taskNodes，供统一流程链渲染） */
    async loadChainDetail(chain) {
      this.$set(chain, 'detailLoading', true)
      try {
        const res = await getTaskDetail(chain.taskId)
        this.$set(chain, 'detail', res.data || null)
      } catch (e) {
        console.error(e)
        this.$set(chain, 'detail', null)
      } finally {
        this.$set(chain, 'detailLoading', false)
      }
    },
    chainDoneCount(chain) {
      return chain.nodes.reduce((s, nd) => s + (nd.submitStatus === 1 ? 1 : 0), 0)
    },
    toggleChain(personId, taskId) {
      const key = this.chainKey(personId, taskId)
      const idx = this.openChainKeys.indexOf(key)
      this.openChainKeys = idx >= 0
        ? this.openChainKeys.filter(k => k !== key)
        : this.openChainKeys.concat(key)
    },
    taskStatusClass(status) {
      if (status === '进行中') return 'tstatus run'
      if (status === '已结束') return 'tstatus done'
      return 'tstatus'
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
.header-left { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; }
.hl-row1 { display: flex; align-items: center; gap: 14px; }
.hl-row1 .breadcrumb { margin-bottom: 0; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.btn-back, .btn-refresh { display: flex; align-items: center; gap: 4px; padding: 8px 16px; background: #fff; border: 1px solid $border; border-radius: 2px; color: var(--color-primary); cursor: pointer; font-size: 13px;
  i { color: $primary; }
  &:hover { background: var(--color-primary-light); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
// 概览条
.tip-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; background: var(--color-primary-light); border: 1px solid $border; color: var(--color-primary-hover); font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: $primary; }
  b { color: $primary; font-weight: 700; }
}
.tip-task { font-weight: 700; color: #1b1c1c; max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tip-sep { color: #94A3B8; }
// 搜索
.filter-section { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px; flex-wrap: wrap; }
.filter-item { display: flex; flex-direction: column; gap: 4px; min-width: 260px; }
.filter-label { font-size: 13px; color: #757575; }
.filter-input { height: 36px; border: 1px solid #dcdfe6; border-radius: 2px; padding: 0 10px; font-size: 13px; outline: none; transition: all .2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.12); }
}
.filter-actions { display: flex; gap: 8px; }
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid $border; border-radius: 2px; font-size: 13px; color: var(--color-primary); background: #fff; cursor: pointer;
  &:hover { background: var(--color-primary-light); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 2px; font-size: 13px; font-weight: bold; color: #fff; background: $primary; cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.empty-state { text-align: center; padding: 60px 20px; color: #bbb; background: #fff; border: 1px solid $border; border-radius: 3px;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
// 一级：人员
.person-panel { background: #fff; border: 1px solid $border; border-radius: 3px; margin-bottom: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.03); }
.pp-head { display: flex; align-items: center; gap: 12px; padding: 14px 18px; cursor: pointer; transition: background .2s;
  &:hover { background: var(--color-primary-surface); }
}
.pp-avatar { width: 42px; height: 42px; border-radius: 50%; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700; flex-shrink: 0; }
.pp-main { flex: 1; min-width: 0; }
.pp-name-row { display: flex; align-items: center; gap: 8px; min-width: 0; flex-wrap: wrap; }
.pp-name { font-size: 15px; font-weight: 700; color: #1b1c1c; }
.pp-emp { font-size: 12px; color: #909399; font-family: monospace; }
.pp-dept { font-size: 12px; color: var(--color-primary-hover); display: inline-flex; align-items: center; gap: 3px; background: var(--color-primary-light); padding: 1px 8px; border-radius: 3px; }
.pp-sub { font-size: 12px; color: #909399; margin-top: 3px; }
.pp-meta { flex-shrink: 0; }
.pp-last { font-size: 12px; color: #909399; display: inline-flex; align-items: center; gap: 3px;
  i { color: #B45309; }
}
.pp-arrow { color: #94A3B8; font-size: 14px; flex-shrink: 0; transition: transform .25s; cursor: pointer; }
.person-panel.is-open .pp-arrow { transform: rotate(180deg); }
.pp-body { border-top: 1px solid $border; padding: 12px 14px 14px; }
.pp-loading { display: flex; align-items: center; gap: 6px; padding: 18px 4px; color: $primary; font-size: 13px; justify-content: center; }
.pp-empty { text-align: center; padding: 16px; color: #bbb; font-size: 13px; }
// 二级：任务链
.period-panel { border: 1px solid rgba(228,190,186,0.6); border-radius: 3px; margin-bottom: 10px; overflow: hidden;
  &:last-child { margin-bottom: 0; }
}
.pdp-head { display: flex; align-items: center; gap: 10px; padding: 11px 14px; background: var(--color-primary-light); cursor: pointer; transition: background .2s;
  &:hover { background: var(--color-primary-light); }
}
.pdp-icon { color: $primary; font-size: 15px; flex-shrink: 0; }
.pdp-main { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
.pdp-name { font-size: 14px; font-weight: 700; color: #1b1c1c; }
.pdp-no { font-size: 11px; color: $primary; background: rgba(var(--color-primary-rgb),0.1); border-radius: 3px; padding: 1px 8px; font-weight: 600; flex-shrink: 0; }
.pdp-count { font-size: 12px; color: #909399; flex-shrink: 0; }
.pdp-arrow { color: #909399; font-size: 13px; flex-shrink: 0; transition: transform .25s; }
.period-panel.is-open .pdp-arrow { transform: rotate(180deg); }
.pdp-body { padding: 12px; border-top: 1px solid #E2E8F0; background: #fff; }
// 任务链状态徽标
.tstatus { font-size: 11px; border-radius: 3px; padding: 1px 8px; font-weight: 600; flex-shrink: 0; background: #F0F0F0; color: #909399;
  &.run { background: rgba(#15803D,0.1); color: #15803D; }
  &.done { background: rgba(#2B6CB0,0.1); color: #2B6CB0; }
}
// 分页
.pagination { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; background: #fff; border: 1px solid $border; border-radius: 3px; }
.pagination-left { display: flex; align-items: center; gap: 12px; }
.pagination-info { font-size: 12px; color: #414755; }
.page-size-select { height: 32px; border: 1px solid $border; border-radius: 4px; padding: 0 6px; font-size: 12px; color: #414755; outline: none; background: #fff; cursor: pointer;
  &:focus { border-color: $primary; }
}
.pagination-controls { display: flex; align-items: center; gap: 8px; }
.page-btn { width: 32px; height: 32px; border-radius: 4px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; cursor: pointer; font-size: 14px;
  &:hover:not(:disabled) { background: #efeded; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.page-current { font-size: 13px; color: #414755; }
</style>
