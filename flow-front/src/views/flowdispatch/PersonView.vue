<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span>任务管理</span>
              <span>/</span>
              <span class="active">按人员查看</span>
            </nav>
            <h3 class="page-heading">按人员查看</h3>
          </div>
          <div class="header-actions">
            <button class="btn-back" @click="$router.push('/flow-dispatch/index')"><i class="el-icon-back" /> 返回任务管理</button>
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
          <span>展示结构：<b>人员 → 期次 → 节点</b></span>
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

            <!-- 展开：期次折叠列表 -->
            <div v-show="isPersonOpen(p.userId)" class="pp-body">
              <div v-if="personData[p.userId] && personData[p.userId].loading" class="pp-loading"><i class="el-icon-loading" /> 正在加载提交记录...</div>
              <template v-else-if="personData[p.userId]">
                <template v-if="personData[p.userId].periods.length > 0">
                  <div v-for="(period, pi) in personData[p.userId].periods" :key="pi" class="period-panel" :class="{ 'is-open': isPeriodOpen(p.userId, period.key) }">
                    <!-- 二级：期次 -->
                    <div class="pdp-head" @click="togglePeriod(p.userId, period.key)">
                      <span class="pdp-icon"><i class="el-icon-tickets" /></span>
                      <div class="pdp-main">
                        <span class="pdp-name">{{ period.name }}</span>
                        <span v-if="period.periodNo" class="pdp-no">第 {{ period.periodNo }} 期</span>
                      </div>
                      <span class="pdp-count">提交 {{ period.list.length }} 次</span>
                      <i class="el-icon-arrow-down pdp-arrow" />
                    </div>

                    <!-- 展开：节点提交列表 -->
                    <div v-show="isPeriodOpen(p.userId, period.key)" class="pdp-body">
                      <div v-for="rec in period.list" :key="rec.id" class="rec-panel" :class="{ 'is-open': expandedRecIds.indexOf(rec.id) >= 0 }">
                        <!-- 三级：节点提交行 -->
                        <div class="rec-head" @click="toggleRec(rec)">
                          <span class="rec-icon"><i :class="recIcon(rec.nodeName)" /></span>
                          <span class="rec-node">{{ rec.nodeName || '未知节点' }}</span>
                          <span class="rec-time"><i class="el-icon-time" /> {{ rec.submitTime || '—' }}</span>
                          <span class="rec-toggle"><i class="el-icon-arrow-down" /></span>
                        </div>
                        <!-- 节点提交表单 -->
                        <div v-show="expandedRecIds.indexOf(rec.id) >= 0" class="rec-body" v-loading="recDetailLoading[rec.id]">
                          <template v-if="recDetailMap[rec.id]">
                            <template v-if="recDetailMap[rec.id].formDataItems && recDetailMap[rec.id].formDataItems.length > 0">
                              <div v-for="(item, fi) in recDetailMap[rec.id].formDataItems" :key="fi" class="rf-row">
                                <span class="rf-label">{{ item.fieldLabel }}</span>
                                <AttachField v-if="item.fieldType === 'file' || item.fieldType === 'image'" :value="item.fieldValue" :field-type="item.fieldType" readonly class="rf-value" />
                                <span v-else class="rf-value">{{ item.fieldValue || '—' }}</span>
                              </div>
                            </template>
                            <div v-else class="rf-empty">该次提交未填写表单数据</div>
                          </template>
                          <div v-else class="rf-empty"><i class="el-icon-loading" /> 表单加载中...</div>
                        </div>
                      </div>
                      <div v-if="period.list.length === 0" class="pp-empty">该期次下无提交记录</div>
                    </div>
                  </div>
                </template>
                <div v-else class="pp-empty">该人员在该任务下暂无提交记录</div>
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
import { getPersonList, getPersonRecords, getRecordDetail } from '@/api/data'
import AttachField from '@/components/AttachField.vue'

const AVATAR_COLORS = ['#334155', '#B45309', '#15803D', '#2B6CB0', '#6B46C1', '#6366F1', '#047857', '#D97706']
const NODE_ICONS = { 需求: 'el-icon-edit-outline', 设计: 'el-icon-magic-stick', 合同: 'el-icon-document-copy', 审核: 'el-icon-check', 审批: 'el-icon-circle-check', 签订: 'el-icon-postcard' }

export default {
  name: 'FlowDispatchPersonView',
  components: { AttachField },
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
      openPeriodKeys: [],
      expandedRecIds: [],
      // 每人提交记录（按期次分组）
      personData: {},
      recDetailMap: {},
      recDetailLoading: {}
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
    this.taskId = this.$route.query.taskId ? Number(this.$route.query.taskId) : null
    this.taskName = this.$route.query.taskName || ''
    this.fetchPersons()
  },
  methods: {
    avatarColor(id) {
      if (!id) return AVATAR_COLORS[0]
      return AVATAR_COLORS[Math.abs(Number(id)) % AVATAR_COLORS.length]
    },
    recIcon(nodeName) {
      const n = nodeName || ''
      for (const k in NODE_ICONS) {
        if (n.indexOf(k) >= 0) return NODE_ICONS[k]
      }
      return 'el-icon-position'
    },
    isPersonOpen(id) { return this.openPersonIds.indexOf(id) >= 0 },
    isPeriodOpen(personId, key) { return this.openPeriodKeys.indexOf(personId + '|' + key) >= 0 },
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
          if (ids.indexOf(Number(id)) < 0) this.$delete(this.personData, id)
        })
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '人员列表加载失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() { this.page = 1; this.fetchPersons() },
    resetFilters() { this.filters.name = ''; this.page = 1; this.fetchPersons() },
    onPageSizeChange() { this.page = 1; this.fetchPersons() },
    prevPage() { if (this.page > 1) { this.page--; this.fetchPersons() } },
    nextPage() { if (this.page < this.totalPages) { this.page++; this.fetchPersons() } },
    /** 展开/收起人员：展开时懒加载该人员记录并按期次分组 */
    togglePerson(p) {
      const idx = this.openPersonIds.indexOf(p.userId)
      if (idx >= 0) {
        this.openPersonIds = this.openPersonIds.filter(id => id !== p.userId)
      } else {
        this.openPersonIds = this.openPersonIds.concat(p.userId)
        if (!this.personData[p.userId]) this.loadPersonRecords(p)
      }
    },
    async loadPersonRecords(p) {
      this.$set(this.personData, p.userId, { loading: true, periods: [] })
      try {
        const res = await getPersonRecords({
          userId: p.userId,
          taskId: this.taskId || undefined,
          page: 1,
          limit: 500
        })
        const records = (res.data && res.data.records) || []
        this.$set(this.personData, p.userId, { loading: false, periods: this.groupPeriods(records) })
      } catch (e) {
        console.error(e)
        this.$set(this.personData, p.userId, { loading: false, periods: [] })
        this.$message.error((e && e.message) || '提交记录加载失败')
      }
    },
    /** 提交记录按「期次名称」分组（保持提交时间倒序） */
    groupPeriods(records) {
      const map = {}
      const first = {}
      records.forEach(r => {
        const key = r.periodName || '未知期次'
        if (!map[key]) {
          map[key] = []
          first[key] = r
        }
        map[key].push(r)
      })
      return Object.keys(map).map(key => ({
        key,
        name: key,
        periodNo: first[key].periodNo,
        list: map[key]
      }))
    },
    togglePeriod(personId, key) {
      const full = personId + '|' + key
      const idx = this.openPeriodKeys.indexOf(full)
      this.openPeriodKeys = idx >= 0
        ? this.openPeriodKeys.filter(k => k !== full)
        : this.openPeriodKeys.concat(full)
    },
    /** 展开节点提交：懒加载表单内容 */
    toggleRec(rec) {
      const idx = this.expandedRecIds.indexOf(rec.id)
      if (idx >= 0) {
        this.expandedRecIds = this.expandedRecIds.filter(id => id !== rec.id)
      } else {
        this.expandedRecIds = this.expandedRecIds.concat(rec.id)
        if (!this.recDetailMap[rec.id]) this.loadRecDetail(rec)
      }
    },
    async loadRecDetail(rec) {
      this.$set(this.recDetailLoading, rec.id, true)
      try {
        const res = await getRecordDetail(rec.id)
        this.$set(this.recDetailMap, rec.id, res.data || {})
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '表单内容加载失败')
        this.$set(this.recDetailMap, rec.id, {})
      } finally {
        this.$set(this.recDetailLoading, rec.id, false)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #334155;
$border: #CBD5E1;
.dashboard-container { display: flex; min-height: 100vh; background: #F8FAFC;  color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.btn-back, .btn-refresh { display: flex; align-items: center; gap: 4px; padding: 8px 16px; background: #fff; border: 1px solid $border; border-radius: 2px; color: #334155; cursor: pointer; font-size: 13px;
  i { color: $primary; }
  &:hover { background: #F1F5F9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
// 概览条
.tip-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; background: #F1F5F9; border: 1px solid $border; color: #475569; font-size: 13px; border-radius: 3px; padding: 10px 14px;
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
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(51,65,85,0.12); }
}
.filter-actions { display: flex; gap: 8px; }
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid $border; border-radius: 2px; font-size: 13px; color: #334155; background: #fff; cursor: pointer;
  &:hover { background: #F1F5F9; }
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
  &:hover { background: #F8FAFC; }
}
.pp-avatar { width: 42px; height: 42px; border-radius: 50%; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700; flex-shrink: 0; }
.pp-main { flex: 1; min-width: 0; }
.pp-name-row { display: flex; align-items: center; gap: 8px; min-width: 0; flex-wrap: wrap; }
.pp-name { font-size: 15px; font-weight: 700; color: #1b1c1c; }
.pp-emp { font-size: 12px; color: #909399; font-family: monospace; }
.pp-dept { font-size: 12px; color: #475569; display: inline-flex; align-items: center; gap: 3px; background: #F1F5F9; padding: 1px 8px; border-radius: 3px; }
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
// 二级：期次
.period-panel { border: 1px solid rgba(228,190,186,0.6); border-radius: 3px; margin-bottom: 10px; overflow: hidden;
  &:last-child { margin-bottom: 0; }
}
.pdp-head { display: flex; align-items: center; gap: 10px; padding: 11px 14px; background: #F1F5F9; cursor: pointer; transition: background .2s;
  &:hover { background: #F1F5F9; }
}
.pdp-icon { color: $primary; font-size: 15px; flex-shrink: 0; }
.pdp-main { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
.pdp-name { font-size: 14px; font-weight: 700; color: #1b1c1c; }
.pdp-no { font-size: 11px; color: $primary; background: rgba(51,65,85,0.1); border-radius: 3px; padding: 1px 8px; font-weight: 600; flex-shrink: 0; }
.pdp-count { font-size: 12px; color: #909399; flex-shrink: 0; }
.pdp-arrow { color: #909399; font-size: 13px; flex-shrink: 0; transition: transform .25s; }
.period-panel.is-open .pdp-arrow { transform: rotate(180deg); }
.pdp-body { padding: 10px 12px; border-top: 1px solid #E2E8F0; background: #fff; }
// 三级：节点提交
.rec-panel { border: 1px solid #E2E8F0; border-radius: 2px; margin-bottom: 8px; overflow: hidden;
  &:last-child { margin-bottom: 0; }
}
.rec-head { display: flex; align-items: center; gap: 10px; padding: 9px 12px; cursor: pointer; transition: background .2s;
  &:hover { background: #F8FAFC; }
}
.rec-icon { color: #15803D; font-size: 14px; flex-shrink: 0; }
.rec-node { font-size: 13px; font-weight: 600; color: #1b1c1c; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-time { font-size: 12px; color: #909399; display: inline-flex; align-items: center; gap: 3px; flex-shrink: 0;
  i { color: #B45309; }
}
.rec-toggle { color: #909399; font-size: 12px; flex-shrink: 0; transition: transform .25s; }
.rec-panel.is-open .rec-toggle { transform: rotate(180deg); }
.rec-body { padding: 10px 12px 12px; border-top: 1px dashed #E2E8F0; background: #FCFCFD; }
.rf-row { display: flex; gap: 10px; padding: 7px 10px; font-size: 13px; background: #fff; border: 1px solid #E2E8F0; border-radius: 4px; margin-bottom: 6px;
  &:last-child { margin-bottom: 0; }
}
.rf-label { width: 140px; color: #757575; flex-shrink: 0; font-weight: 600; word-break: break-all; }
.rf-value { color: #1b1c1c; flex: 1; word-break: break-all; white-space: pre-wrap; line-height: 1.5; }
.rf-empty { font-size: 12px; color: #bbb; text-align: center; padding: 10px 0; }
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
