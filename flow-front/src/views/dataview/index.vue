<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 + 面包屑 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span>管理员</span>
              <span>/</span>
              <span class="active">数据展示</span>
            </nav>
            <h3 class="page-heading">数据展示</h3>
          </div>
          <div class="header-actions">
            <button class="btn-refresh" :disabled="loading" @click="fetchDashboard"><i class="el-icon-refresh" /> 刷新</button>
          </div>
        </div>

        <!-- 统计卡 -->
        <section class="stats-grid">
          <div v-for="c in statCards" :key="c.key" class="stat-card">
            <div class="stat-icon" :class="'icon-' + c.key"><i :class="c.icon" /></div>
            <div class="stat-body">
              <div class="stat-label">{{ c.label }}</div>
              <div class="stat-value">{{ c.value }}</div>
            </div>
          </div>
        </section>

        <!-- 提交趋势（粒度 + 时间范围联动） -->
        <section class="chart-card">
          <div class="chart-head">
            <div class="chart-title"><i class="el-icon-data-line" /> 提交趋势</div>
            <div class="trend-controls">
              <div class="trend-tabs">
                <span
                  v-for="tp in trendTypes"
                  :key="tp.value"
                  class="trend-tab"
                  :class="{ active: trendType === tp.value }"
                  @click="setTrendType(tp.value)"
                >{{ tp.label }}</span>
              </div>
              <!-- 日期范围：按粒度联动（日→选到日 / 月→选到月 / 季度→选季度 / 年→选年） -->
              <div class="range-picker">
                <el-date-picker
                  v-if="trendType === 'day'"
                  v-model="dayRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="yyyy-MM-dd"
                  size="small"
                  :picker-options="datePickerOptions"
                  @change="onRangeChange"
                />
                <el-date-picker
                  v-else-if="trendType === 'month'"
                  v-model="monthRange"
                  type="monthrange"
                  range-separator="至"
                  start-placeholder="开始月份"
                  end-placeholder="结束月份"
                  value-format="yyyy-MM"
                  size="small"
                  :picker-options="datePickerOptions"
                  @change="onRangeChange"
                />
                <template v-else-if="trendType === 'quarter'">
                  <el-select v-model="quarterStart" placeholder="起始季度" size="small" class="range-select" @change="onRangeChange">
                    <el-option v-for="q in quarterOptions" :key="q" :label="q" :value="q" />
                  </el-select>
                  <span class="range-sep">至</span>
                  <el-select v-model="quarterEnd" placeholder="结束季度" size="small" class="range-select" @change="onRangeChange">
                    <el-option v-for="q in quarterOptions" :key="q" :label="q" :value="q" />
                  </el-select>
                </template>
                <template v-else>
                  <el-select v-model="yearStart" placeholder="起始年份" size="small" class="range-select" @change="onRangeChange">
                    <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
                  </el-select>
                  <span class="range-sep">至</span>
                  <el-select v-model="yearEnd" placeholder="结束年份" size="small" class="range-select" @change="onRangeChange">
                    <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
                  </el-select>
                </template>
                <button v-if="hasRange" class="range-clear" @click="clearRange"><i class="el-icon-close" /> 清除范围</button>
              </div>
            </div>
            <div class="chart-meta">
              <span class="meta-item"><b>{{ trendMax }}</b> 峰值</span>
              <span class="meta-item"><b>{{ trendAvg }}</b> 平均</span>
              <span class="meta-item"><b>{{ trendTotal }}</b> 累计提交</span>
            </div>
          </div>
          <div class="bar-chart">
            <div v-for="(d, i) in trendData" :key="i" class="bar-col" :title="`${d.date}：${d.count} 次`">
              <div class="bar-track">
                <div class="bar-fill" :class="{ hot: d.count === trendMax && d.count > 0 }" :style="{ height: barHeight(d.count) }" />
              </div>
              <div class="bar-val" v-show="d.count > 0">{{ d.count }}</div>
              <div class="bar-date">{{ d.shortDate }}</div>
            </div>
          </div>
        </section>

        <!-- 环形图（一行） -->
        <section class="donut-grid">
          <div class="chart-card">
            <div class="chart-title"><i class="el-icon-pie-chart" /> 任务状态分布</div>
            <div class="donut-wrap">
              <div class="donut" :style="donutStyle(taskStatus)">
                <div class="donut-hole">
                  <b>{{ taskStatusTotal }}</b>
                  <span>任务</span>
                </div>
              </div>
              <div class="donut-legend">
                <div v-for="(item, i) in taskStatus" :key="i" class="legend-item">
                  <span class="legend-dot" :style="{ background: STATUS_COLORS[i % STATUS_COLORS.length] }" />
                  <span class="legend-name">{{ item.name }}</span>
                  <span class="legend-value">{{ item.value }}</span>
                </div>
                <div v-if="taskStatus.length === 0" class="legend-empty">暂无数据</div>
              </div>
            </div>
          </div>

          <div class="chart-card">
            <div class="chart-title"><i class="el-icon-pie-chart" /> 期次状态分布</div>
            <div class="donut-wrap">
              <div class="donut" :style="donutStyle(periodStatus)">
                <div class="donut-hole">
                  <b>{{ periodStatusTotal }}</b>
                  <span>期次</span>
                </div>
              </div>
              <div class="donut-legend">
                <div v-for="(item, i) in periodStatus" :key="i" class="legend-item">
                  <span class="legend-dot" :style="{ background: STATUS_COLORS[i % STATUS_COLORS.length] }" />
                  <span class="legend-name">{{ item.name }}</span>
                  <span class="legend-value">{{ item.value }}</span>
                </div>
                <div v-if="periodStatus.length === 0" class="legend-empty">暂无数据</div>
              </div>
            </div>
          </div>

          <div class="chart-card">
            <div class="chart-title"><i class="el-icon-s-check" /> 节点状态分布</div>
            <div class="donut-wrap">
              <div class="donut" :style="donutStyle(nodeStatus)">
                <div class="donut-hole">
                  <b>{{ nodeStatusTotal }}</b>
                  <span>节点</span>
                </div>
              </div>
              <div class="donut-legend">
                <div v-for="(item, i) in nodeStatus" :key="i" class="legend-item">
                  <span class="legend-dot" :style="{ background: STATUS_COLORS[i % STATUS_COLORS.length] }" />
                  <span class="legend-name">{{ item.name }}</span>
                  <span class="legend-value">{{ item.value }}</span>
                </div>
                <div v-if="nodeStatus.length === 0" class="legend-empty">暂无数据</div>
              </div>
            </div>
          </div>

          <div class="chart-card">
            <div class="chart-title"><i class="el-icon-refresh" /> 下发周期类型</div>
            <div class="donut-wrap">
              <div class="donut" :style="donutStyle(periodCycle)">
                <div class="donut-hole">
                  <b>{{ periodCycleTotal }}</b>
                  <span>任务</span>
                </div>
              </div>
              <div class="donut-legend">
                <div v-for="(item, i) in periodCycle" :key="i" class="legend-item">
                  <span class="legend-dot" :style="{ background: STATUS_COLORS[i % STATUS_COLORS.length] }" />
                  <span class="legend-name">{{ item.name }}</span>
                  <span class="legend-value">{{ item.value }}</span>
                </div>
                <div v-if="periodCycle.length === 0" class="legend-empty">暂无数据</div>
              </div>
            </div>
          </div>
        </section>

        <div class="charts-row">
          <!-- 下发部门排行 -->
          <section class="chart-card">
            <div class="chart-title"><i class="el-icon-office-building" /> 下发部门排行</div>
            <div v-if="dispatchDeptRank.length > 0" class="rank-list">
              <div v-for="(item, i) in sliceRank(dispatchDeptRank, rankExpand.dept)" :key="i" class="rank-item">
                <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
                <span class="rank-name" :title="item.name">{{ item.name }}</span>
                <div class="rank-bar-track">
                  <div class="rank-bar-fill" :style="{ width: rankWidth(item.value, dispatchDeptRankMax), background: STATUS_COLORS[i % STATUS_COLORS.length] }" />
                </div>
                <span class="rank-value">{{ item.value }}</span>
              </div>
              <div v-if="dispatchDeptRank.length > 5" class="rank-toggle" @click="rankExpand.dept = !rankExpand.dept">
                {{ rankExpand.dept ? '收起' : '展开全部（' + dispatchDeptRank.length + '）' }}
              </div>
            </div>
            <div v-else class="chart-empty">暂无下发数据</div>
          </section>

          <!-- 处理人部门排行 -->
          <section class="chart-card">
            <div class="chart-title"><i class="el-icon-user" /> 处理人部门排行</div>
            <div v-if="handlerDeptRank.length > 0" class="rank-list">
              <div v-for="(item, i) in sliceRank(handlerDeptRank, rankExpand.handler)" :key="i" class="rank-item">
                <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
                <span class="rank-name" :title="item.name">{{ item.name }}</span>
                <div class="rank-bar-track">
                  <div class="rank-bar-fill" :style="{ width: rankWidth(item.value, handlerDeptRankMax), background: STATUS_COLORS[(i + 2) % STATUS_COLORS.length] }" />
                </div>
                <span class="rank-value">{{ item.value }}</span>
              </div>
              <div v-if="handlerDeptRank.length > 5" class="rank-toggle" @click="rankExpand.handler = !rankExpand.handler">
                {{ rankExpand.handler ? '收起' : '展开全部（' + handlerDeptRank.length + '）' }}
              </div>
            </div>
            <div v-else class="chart-empty">暂无提交数据</div>
          </section>
        </div>

        <div class="charts-row">
          <!-- 字段类型使用排行 -->
          <section class="chart-card">
            <div class="chart-title"><i class="el-icon-s-grid" /> 字段类型使用排行</div>
            <div v-if="fieldTypeRank.length > 0" class="rank-list">
              <div v-for="(item, i) in sliceRank(fieldTypeRank, rankExpand.field)" :key="i" class="rank-item">
                <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
                <span class="rank-name" :title="item.name">{{ item.name }}</span>
                <div class="rank-bar-track">
                  <div class="rank-bar-fill" :style="{ width: rankWidth(item.value, fieldTypeRankMax), background: STATUS_COLORS[(i + 3) % STATUS_COLORS.length] }" />
                </div>
                <span class="rank-value">{{ item.value }}<span class="rank-unit"> 个</span></span>
              </div>
              <div v-if="fieldTypeRank.length > 5" class="rank-toggle" @click="rankExpand.field = !rankExpand.field">
                {{ rankExpand.field ? '收起' : '展开全部（' + fieldTypeRank.length + '）' }}
              </div>
            </div>
            <div v-else class="chart-empty">暂无字段数据</div>
          </section>

          <!-- 模板节点数分布 -->
          <section class="chart-card">
            <div class="chart-title"><i class="el-icon-s-operation" /> 模板节点数分布 <span class="chart-sub">按节点个数统计模板数</span></div>
            <div v-if="templateNodeDist.length > 0" class="rank-list">
              <div v-for="(item, i) in sliceRank(templateNodeDist, rankExpand.node)" :key="i" class="rank-item">
                <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
                <span class="rank-name" :title="item.name">{{ item.name }}</span>
                <div class="rank-bar-track">
                  <div class="rank-bar-fill" :style="{ width: rankWidth(item.value, templateNodeDistMax), background: STATUS_COLORS[(i + 4) % STATUS_COLORS.length] }" />
                </div>
                <span class="rank-value">{{ item.value }}<span class="rank-unit"> 个模板</span></span>
              </div>
              <div v-if="templateNodeDist.length > 5" class="rank-toggle" @click="rankExpand.node = !rankExpand.node">
                {{ rankExpand.node ? '收起' : '展开全部（' + templateNodeDist.length + '）' }}
              </div>
            </div>
            <div v-else class="chart-empty">暂无模板数据</div>
          </section>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import { getDashboard } from '@/api/data'

const STATUS_COLORS = ['#334155', '#15803D', '#B45309', '#2B6CB0', '#6B46C1', '#6366F1']
// 字段类型 → 中文名
const FIELD_TYPE_NAMES = {
  text: '单行文本',
  textarea: '多行文本',
  number: '数字',
  date: '日期',
  datetime: '日期时间',
  select: '下拉选择',
  radio: '单选',
  checkbox: '多选',
  upload: '附件上传',
  file: '文件',
  image: '图片',
  person: '人员选择',
  rich: '富文本'
}

export default {
  name: 'DataView',
  data() {
    return {
      loading: false,
      data: null,
      STATUS_COLORS,
      // 提交趋势粒度：day / month / quarter / year
      trendType: 'day',
      trendTypes: [
        { value: 'day', label: '日' },
        { value: 'month', label: '月' },
        { value: 'quarter', label: '季度' },
        { value: 'year', label: '年' }
      ],
      // 按粒度各自独立的时间范围（与粒度联动）
      dayRange: null,
      monthRange: null,
      quarterStart: '',
      quarterEnd: '',
      yearStart: '',
      yearEnd: '',
      // 排行卡片展开状态（默认仅展示前 5 条，可展开查看全部）
      rankExpand: { dept: false, handler: false, field: false, node: false },
      // 时间范围最小值：统一对齐 2026-09-01（日/月/季度/年 均不可早于此）
      datePickerOptions: {
        disabledDate(date) {
          return date.getTime() < new Date(2026, 8, 1).getTime()
        }
      }
    }
  },
  computed: {
    stats() {
      return (this.data && this.data.stats) || {}
    },
    statCards() {
      const s = this.stats
      return [
        { key: 'task', label: '任务总数', value: s.totalTasks || 0, icon: 'el-icon-files' },
        { key: 'period', label: '期次总数', value: s.totalPeriods || 0, icon: 'el-icon-tickets' },
        { key: 'running', label: '进行中期次', value: s.runningPeriods || 0, icon: 'el-icon-loading' },
        { key: 'person', label: '参与人员', value: (this.data && this.data.personCount) || 0, icon: 'el-icon-user' },
        { key: 'submit', label: '累计提交', value: (this.data && this.data.submitTotal) || 0, icon: 'el-icon-upload2' },
        { key: 'todo', label: '我的待办', value: s.myTodoCount || 0, icon: 'el-icon-bell' }
      ]
    },
    taskStatus() {
      return (this.data && this.data.taskStatus) || []
    },
    periodStatus() {
      return (this.data && this.data.periodStatus) || []
    },
    nodeStatus() {
      return (this.data && this.data.nodeStatus) || []
    },
    periodCycle() {
      return (this.data && this.data.periodCycle) || []
    },
    dispatchDeptRank() {
      return (this.data && this.data.dispatchDeptRank) || []
    },
    handlerDeptRank() {
      return (this.data && this.data.handlerDeptRank) || []
    },
    templateNodeDist() {
      return (this.data && this.data.templateNodeDist) || []
    },
    fieldTypeRank() {
      const list = (this.data && this.data.fieldTypeRank) || []
      return list.map(i => Object.assign({}, i, { name: FIELD_TYPE_NAMES[i.name] || i.name }))
    },
    taskStatusTotal() {
      return this.taskStatus.reduce((s, i) => s + (Number(i.value) || 0), 0)
    },
    periodStatusTotal() {
      return this.periodStatus.reduce((s, i) => s + (Number(i.value) || 0), 0)
    },
    nodeStatusTotal() {
      return this.nodeStatus.reduce((s, i) => s + (Number(i.value) || 0), 0)
    },
    periodCycleTotal() {
      return this.periodCycle.reduce((s, i) => s + (Number(i.value) || 0), 0)
    },
    dispatchDeptRankMax() {
      return Math.max(1, ...this.dispatchDeptRank.map(i => Number(i.value) || 0))
    },
    handlerDeptRankMax() {
      return Math.max(1, ...this.handlerDeptRank.map(i => Number(i.value) || 0))
    },
    templateNodeDistMax() {
      return Math.max(1, ...this.templateNodeDist.map(i => Number(i.value) || 0))
    },
    fieldTypeRankMax() {
      return Math.max(1, ...this.fieldTypeRank.map(i => Number(i.value) || 0))
    },
    // 季度选项（最小 2026-Q3，对齐 2026-09-01 最小值）
    quarterOptions() {
      const list = []
      const nowY = new Date().getFullYear()
      for (let y = 2026; y <= nowY + 1; y++) {
        const fromQ = y === 2026 ? 3 : 1
        for (let q = fromQ; q <= 4; q++) list.push(`${y}-Q${q}`)
      }
      return list
    },
    // 年份选项（最小 2026）
    yearOptions() {
      const list = []
      const nowY = new Date().getFullYear()
      for (let y = 2026; y <= nowY + 1; y++) list.push(String(y))
      return list
    },
    // 当前粒度是否已设置时间范围
    hasRange() {
      if (this.trendType === 'day') return !!(this.dayRange && this.dayRange.length === 2)
      if (this.trendType === 'month') return !!(this.monthRange && this.monthRange.length === 2)
      if (this.trendType === 'quarter') return !!(this.quarterStart && this.quarterEnd)
      if (this.trendType === 'year') return !!(this.yearStart && this.yearEnd)
      return false
    },
    // 当前粒度的起止边界（Date），无范围返回 null
    rangeBoundaries() {
      if (this.trendType === 'day' && this.dayRange && this.dayRange.length === 2) {
        return { start: this.parseYmd(this.dayRange[0]), end: this.parseYmd(this.dayRange[1]) }
      }
      if (this.trendType === 'month' && this.monthRange && this.monthRange.length === 2) {
        const s = this.monthRange[0].split('-').map(Number)
        const e = this.monthRange[1].split('-').map(Number)
        return { start: new Date(s[0], s[1] - 1, 1), end: new Date(e[0], e[1], 0) }
      }
      if (this.trendType === 'quarter' && this.quarterStart && this.quarterEnd) {
        const s = this.parseQuarterKey(this.quarterStart)
        const e = this.parseQuarterKey(this.quarterEnd)
        return { start: new Date(s.y, (s.q - 1) * 3, 1), end: new Date(e.y, e.q * 3, 0) }
      }
      if (this.trendType === 'year' && this.yearStart && this.yearEnd) {
        return { start: new Date(Number(this.yearStart), 0, 1), end: new Date(Number(this.yearEnd), 11, 31) }
      }
      return null
    },
    /** 提交趋势（按粒度 + 时间范围补齐空周期） */
    trendData() {
      const raw = this.data && this.data.trend ? this.data.trend : []
      const map = {}
      raw.forEach(t => { map[t.date] = Number(t.count) || 0 })
      const list = []
      const now = new Date()
      // 自定义时间范围（起止），无则用粒度默认窗口
      const range = this.rangeBoundaries
      if (this.trendType === 'month') {
        const start = range ? range.start : new Date(2026, 8, 1)
        const end = range ? range.end : now
        for (let d = new Date(start.getFullYear(), start.getMonth(), 1); d <= end; d.setMonth(d.getMonth() + 1)) {
          const key = `${d.getFullYear()}-${this.pad(d.getMonth() + 1)}`
          list.push({ date: key, shortDate: `${d.getMonth() + 1}月`, count: map[key] || 0 })
        }
      } else if (this.trendType === 'quarter') {
        const start = range ? range.start : new Date(2026, 8, 1)
        const end = range ? range.end : now
        let sy = start.getFullYear()
        let sq = Math.floor(start.getMonth() / 3) + 1
        const ey = end.getFullYear()
        const eq = Math.floor(end.getMonth() / 3) + 1
        while (sy < ey || (sy === ey && sq <= eq)) {
          const key = `${sy}-Q${sq}`
          list.push({ date: key, shortDate: `${sy}年Q${sq}`, count: map[key] || 0 })
          sq += 1
          if (sq > 4) { sq = 1; sy += 1 }
        }
      } else if (this.trendType === 'year') {
        const startY = range ? range.start.getFullYear() : 2026
        const endY = range ? range.end.getFullYear() : now.getFullYear()
        for (let y = startY; y <= endY; y++) {
          list.push({ date: String(y), shortDate: `${y}年`, count: map[String(y)] || 0 })
        }
      } else {
        const start = range ? range.start : new Date(2026, 8, 1)
        const end = range ? range.end : now
        for (let d = new Date(start.getFullYear(), start.getMonth(), start.getDate()); d <= end; d.setDate(d.getDate() + 1)) {
          const key = `${d.getFullYear()}-${this.pad(d.getMonth() + 1)}-${this.pad(d.getDate())}`
          list.push({ date: key, shortDate: `${d.getMonth() + 1}/${d.getDate()}`, count: map[key] || 0 })
        }
      }
      return list
    },
    trendMax() {
      return Math.max(0, ...this.trendData.map(d => d.count))
    },
    trendTotal() {
      return this.trendData.reduce((s, d) => s + d.count, 0)
    },
    trendAvg() {
      return this.trendData.length ? (this.trendTotal / this.trendData.length).toFixed(1) : '0.0'
    }
  },
  methods: {
    pad(n) {
      return n < 10 ? '0' + n : '' + n
    },
    /** 'yyyy-MM-dd' → Date */
    parseYmd(s) {
      const parts = String(s).split('-').map(Number)
      return new Date(parts[0], (parts[1] || 1) - 1, parts[2] || 1)
    },
    /** '2026-Q2' → { y, q } */
    parseQuarterKey(k) {
      const parts = String(k).split('-Q')
      return { y: Number(parts[0]), q: Number(parts[1]) }
    },
    /** Date → 'yyyy-MM-dd' */
    fmtDate(d) {
      return `${d.getFullYear()}-${this.pad(d.getMonth() + 1)}-${this.pad(d.getDate())}`
    },
    barHeight(count) {
      if (!this.trendMax) return '0%'
      return Math.max(4, Math.round((count / this.trendMax) * 100)) + '%'
    },
    rankWidth(value, max) {
      return Math.max(6, Math.round((Number(value) / max) * 100)) + '%'
    },
    /** 排行默认展示前 5 条，展开后展示全部 */
    sliceRank(list, expanded) {
      return expanded ? list : list.slice(0, 5)
    },
    /** 环形图样式对象（conic-gradient） */
    donutStyle(list) {
      const total = list.reduce((s, i) => s + (Number(i.value) || 0), 0)
      if (total <= 0) return { background: '#f0f0f0' }
      let start = 0
      const stops = list.map((item, i) => {
        const pct = (Number(item.value) / total) * 100
        const from = start
        start += pct
        return `${STATUS_COLORS[i % STATUS_COLORS.length]} ${from}% ${start}%`
      })
      return { background: `conic-gradient(${stops.join(', ')})` }
    },
    setTrendType(type) {
      if (this.trendType === type) return
      this.trendType = type
      this.fetchDashboard()
    },
    onRangeChange() {
      this.fetchDashboard()
    },
    clearRange() {
      this.dayRange = null
      this.monthRange = null
      this.quarterStart = ''
      this.quarterEnd = ''
      this.yearStart = ''
      this.yearEnd = ''
      this.fetchDashboard()
    },
    async fetchDashboard() {
      this.loading = true
      try {
        const params = { trendType: this.trendType }
        const range = this.rangeBoundaries
        if (range) {
          params.startDate = this.fmtDate(range.start)
          params.endDate = this.fmtDate(range.end)
        }
        const res = await getDashboard(params)
        this.data = res.data || null
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '数据加载失败')
      } finally {
        this.loading = false
      }
    }
  },
  mounted() {
    this.fetchDashboard()
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
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.btn-refresh { display: flex; align-items: center; gap: 4px; padding: 8px 16px; background: #fff; border: 1px solid $border; border-radius: 2px; color: var(--color-primary); cursor: pointer; font-size: 13px;
  &:hover { background: var(--color-primary-light); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 16px;
  @media (max-width: 1200px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 700px) { grid-template-columns: repeat(2, 1fr); }
}
.stat-card { display: flex; align-items: center; gap: 14px; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px 18px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon { width: 44px; height: 44px; border-radius: 3px; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; flex-shrink: 0;
  &.icon-task { background: $primary; }
  &.icon-period { background: var(--color-primary-hover); }
  &.icon-running { background: #B45309; }
  &.icon-person { background: #2B6CB0; }
  &.icon-submit { background: #15803D; }
  &.icon-todo { background: #6B46C1; }
}
.stat-body { flex: 1; min-width: 0; }
.stat-label { font-size: 12px; color: #757575; margin-bottom: 4px; white-space: nowrap; }
.stat-value { font-size: 26px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }

// 图表卡
.chart-card { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.chart-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.chart-title { font-size: 15px; font-weight: 700; color: #414755; display: flex; align-items: center; gap: 6px;
  i { color: $primary; }
}
.chart-sub { font-size: 12px; font-weight: 400; color: #aaa; }
.chart-meta { display: flex; gap: 18px; font-size: 12px; color: #909399;
  .meta-item b { color: $primary; font-size: 15px; margin-right: 3px; }
}
.chart-empty { text-align: center; padding: 40px 0; color: #bbb; font-size: 13px; }

// 趋势控制区：粒度切换 + 时间范围
.trend-controls { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.trend-tabs { display: flex; background: var(--color-primary-light); border: 1px solid $border; border-radius: 3px; padding: 2px;
  .trend-tab { padding: 5px 14px; font-size: 12px; color: #64748B; cursor: pointer; border-radius: 2px; user-select: none; transition: all .2s;
    &:hover { color: $primary; }
    &.active { background: $primary; color: #fff; font-weight: 600; }
  }
}
.range-picker { display: flex; align-items: center; gap: 6px;
  ::v-deep .el-range-editor { height: 30px; border-color: $border; border-radius: 2px; }
  ::v-deep .el-select .el-input__inner { border-color: $border; border-radius: 2px; }
}
.range-select { width: 120px; }
.range-sep { font-size: 12px; color: #909399; }
.range-clear { display: flex; align-items: center; gap: 3px; padding: 5px 10px; font-size: 12px; color: #64748B; background: #fff; border: 1px solid $border; border-radius: 2px; cursor: pointer;
  &:hover { color: $primary; border-color: $primary; }
}

// 柱状趋势图
.bar-chart { display: flex; align-items: flex-end; gap: 3px; height: 220px; padding: 8px 4px 0; border-bottom: 1px solid #E2E8F0; overflow-x: auto; }
.bar-col { flex: 1; min-width: 18px; display: flex; flex-direction: column; align-items: center; height: 100%; }
.bar-track { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.bar-fill { width: 60%; max-width: 22px; min-height: 2px; background: rgba(var(--color-primary-rgb),0.35); border-radius: 3px 3px 0 0; transition: height .4s;
  &:hover { background: $primary; }
  &.hot { background: $primary; box-shadow: 0 0 6px rgba(var(--color-primary-rgb),0.5); }
}
.bar-val { font-size: 10px; color: $primary; font-weight: 700; height: 14px; line-height: 14px; }
.bar-date { font-size: 9px; color: #bbb; height: 16px; line-height: 16px; transform: scale(0.92); white-space: nowrap; }

// 环形图（一行 4 个）
.donut-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  @media (max-width: 1500px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 700px) { grid-template-columns: 1fr; }
}
.donut-wrap { display: flex; align-items: center; gap: 14px; padding: 10px 0 6px; }
.donut { width: 118px; height: 118px; border-radius: 50%; flex-shrink: 0; position: relative; display: flex; align-items: center; justify-content: center; }
.donut-hole { width: 68px; height: 68px; border-radius: 50%; background: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center;
  b { font-size: 19px; color: #1b1c1c; line-height: 1.1; }
  span { font-size: 10px; color: #909399; }
}
.donut-legend { flex: 1; display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.legend-dot { width: 9px; height: 9px; border-radius: 3px; flex-shrink: 0; }
.legend-name { color: #414755; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.legend-value { font-weight: 700; color: #1b1c1c; }
.legend-empty { color: #bbb; font-size: 12px; }

// 横向排行
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px;
  @media (max-width: 900px) { grid-template-columns: 1fr; }
}
.rank-list { display: flex; flex-direction: column; gap: 12px; padding: 6px 0; }
.rank-item { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.rank-no { width: 22px; height: 22px; border-radius: 50%; background: #f0f0f0; color: #909399; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0;
  &.top { background: $primary; color: #fff; }
}
.rank-name { width: 140px; color: #414755; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex-shrink: 0; }
.rank-bar-track { flex: 1; height: 10px; background: #f5f5f5; border-radius: 2px; overflow: hidden; min-width: 40px; }
.rank-bar-fill { height: 100%; border-radius: 2px; transition: width .4s; }
.rank-value { width: 62px; text-align: right; font-weight: 700; color: #1b1c1c; flex-shrink: 0; }
.rank-unit { font-size: 10px; color: #aaa; font-weight: 400; }
.rank-toggle { text-align: center; padding: 4px 0; margin-top: 2px; font-size: 12px; color: $primary; cursor: pointer; user-select: none; border-top: 1px dashed #E2E8F0;
  &:hover { color: var(--color-primary-hover); }
}
</style>
