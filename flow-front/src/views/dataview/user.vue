<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span class="active">数据展示</span>
              <span class="hint">我自己的任务与提交情况</span>
            </nav>
            <h3 class="page-heading">我的数据</h3>
          </div>
          <div class="header-actions">
            <button class="btn-refresh" :disabled="loading" @click="fetchData">
              <i :class="loading ? 'el-icon-loading' : 'el-icon-refresh'" /> 刷新
            </button>
          </div>
        </div>

        <!-- 统计卡 -->
        <div v-loading="loading" class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon icon-task"><i class="el-icon-s-order" /></div>
            <div class="stat-body">
              <div class="stat-label">我的任务</div>
              <div class="stat-value">{{ num(stats.totalTasks) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-period"><i class="el-icon-tickets" /></div>
            <div class="stat-body">
              <div class="stat-label">我参与的期次</div>
              <div class="stat-value">{{ num(stats.totalPeriods) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-todo"><i class="el-icon-bell" /></div>
            <div class="stat-body">
              <div class="stat-label">我的待办</div>
              <div class="stat-value">{{ num(stats.myTodoCount) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-running"><i class="el-icon-finished" /></div>
            <div class="stat-body">
              <div class="stat-label">我的已办</div>
              <div class="stat-value">{{ num(stats.doneCount) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-submit"><i class="el-icon-upload2" /></div>
            <div class="stat-body">
              <div class="stat-label">我的提交</div>
              <div class="stat-value">{{ num(stats.submitCount) }}</div>
            </div>
          </div>
        </div>

        <!-- 提交趋势 -->
        <BarChart
          :data="trend"
          title="我的提交趋势"
          icon="el-icon-data-line"
          total-label="累计提交"
          empty-text="暂无提交记录"
        />

        <!-- 分布（一行两个） -->
        <section class="donut-grid">
          <DonutChart :data="taskStatus" title="我的任务状态分布" unit="任务" empty-text="暂无任务" />
          <DonutChart :data="nodeStatus" title="我的节点进度" unit="节点" empty-text="暂无节点" />
        </section>
      </section>
    </main>
  </div>
</template>

<script>
import BarChart from '@/components/charts/BarChart.vue'
import DonutChart from '@/components/charts/DonutChart.vue'
import { getUserDashboard } from '@/service/sys/DataService'

/** 数据展示（个人维度）：范围由后端按登录身份决定，前端不传身份参数 */
export default {
  name: 'DataViewUser',
  components: { BarChart, DonutChart },
  data() {
    return {
      loading: false,
      stats: {},
      taskStatus: [],
      nodeStatus: [],
      trend: []
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    num(v) {
      return Number(v) || 0
    },
    async fetchData() {
      this.loading = true
      try {
        const res = await getUserDashboard({ trendType: 'month' })
        const d = (res && res.data) || {}
        this.stats = d.stats || {}
        this.taskStatus = d.taskStatus || []
        this.nodeStatus = d.nodeStatus || []
        this.trend = d.trend || []
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
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
.breadcrumb { display: flex; gap: 8px; font-size: 12px; color: #414755; margin-bottom: 8px; align-items: center;
  .active { color: $primary; font-weight: 600; }
  .hint { color: #94A3B8; }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.btn-refresh { display: flex; align-items: center; gap: 4px; padding: 8px 16px; background: #fff; border: 1px solid $border; border-radius: 2px; color: var(--color-primary); cursor: pointer; font-size: 13px;
  &:hover { background: var(--color-primary-light); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px;
  @media (max-width: 1200px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 700px) { grid-template-columns: repeat(2, 1fr); }
}
.stat-card { display: flex; align-items: center; gap: 14px; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px 18px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon { width: 44px; height: 44px; border-radius: 3px; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; flex-shrink: 0;
  &.icon-task { background: $primary; }
  &.icon-period { background: var(--color-primary-hover); }
  &.icon-todo { background: #6B46C1; }
  &.icon-running { background: #B45309; }
  &.icon-submit { background: #15803D; }
}
.stat-body { flex: 1; min-width: 0; }
.stat-label { font-size: 12px; color: #757575; margin-bottom: 4px; white-space: nowrap; }
.stat-value { font-size: 26px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }
// 分布区
.donut-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;
  @media (max-width: 900px) { grid-template-columns: 1fr; }
}
</style>
