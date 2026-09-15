<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span class="active">数据展示</span>
              <span class="hint">本部门的任务与人员情况</span>
            </nav>
            <h3 class="page-heading">部门数据</h3>
          </div>
          <div class="header-actions">
            <button class="btn-refresh" :disabled="loading" @click="fetchData">
              <i :class="loading ? 'el-icon-loading' : 'el-icon-refresh'" /> 刷新
            </button>
          </div>
        </div>

        <!-- 无部门归属提示（dept_admin 未登记） -->
        <div v-if="!loading && noDept" class="tip-bar">
          <i class="el-icon-warning-outline" />
          <span>当前账号未在部门管理员中登记部门，暂无部门数据可展示。</span>
        </div>

        <!-- 统计卡 -->
        <div v-loading="loading" class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon icon-task"><i class="el-icon-s-order" /></div>
            <div class="stat-body">
              <div class="stat-label">部门任务</div>
              <div class="stat-value">{{ num(stats.totalTasks) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-period"><i class="el-icon-tickets" /></div>
            <div class="stat-body">
              <div class="stat-label">涉及期次</div>
              <div class="stat-value">{{ num(stats.totalPeriods) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-running"><i class="el-icon-loading" /></div>
            <div class="stat-body">
              <div class="stat-label">进行中期次</div>
              <div class="stat-value">{{ num(stats.runningPeriods) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-done"><i class="el-icon-circle-check" /></div>
            <div class="stat-body">
              <div class="stat-label">已完成期次</div>
              <div class="stat-value">{{ num(stats.finishedPeriods) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-person"><i class="el-icon-user" /></div>
            <div class="stat-body">
              <div class="stat-label">部门人数</div>
              <div class="stat-value">{{ num(stats.memberCount) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-todo"><i class="el-icon-bell" /></div>
            <div class="stat-body">
              <div class="stat-label">部门待办</div>
              <div class="stat-value">{{ num(stats.myTodoCount) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-already"><i class="el-icon-finished" /></div>
            <div class="stat-body">
              <div class="stat-label">部门已办</div>
              <div class="stat-value">{{ num(stats.doneCount) }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-submit"><i class="el-icon-upload2" /></div>
            <div class="stat-body">
              <div class="stat-label">部门提交</div>
              <div class="stat-value">{{ num(stats.submitCount) }}</div>
            </div>
          </div>
        </div>

        <!-- 部门提交趋势 -->
        <BarChart
          :data="trend"
          title="部门提交趋势"
          icon="el-icon-data-line"
          total-label="累计提交"
          empty-text="暂无提交记录"
        />

        <!-- 分布（一行三个） -->
        <section class="donut-grid">
          <DonutChart :data="taskStatus" title="部门任务状态分布" unit="任务" empty-text="暂无任务" />
          <DonutChart :data="periodStatus" title="部门期次状态分布" unit="期次" empty-text="暂无期次" />
          <DonutChart :data="nodeStatus" title="部门节点进度" unit="节点" empty-text="暂无节点" />
        </section>

        <!-- 下发排行 + 下发期次中未处理的任务 -->
        <section class="rank-grid">
          <RankList :data="dispatchRank" title="部门下发排行" icon="el-icon-s-promotion" unit="期" empty-text="本部门暂无下发记录" />
          <RankList :data="periodPendingRank" title="下发期次中未处理的任务" icon="el-icon-alarm-clock" unit="个" name-width="240px" empty-text="没有未处理的任务" />
        </section>

        <!-- 字段类型使用排行 + 模板节点数分布 -->
        <section class="rank-grid">
          <RankList :data="fieldTypeRank" title="字段类型使用排行" icon="el-icon-s-grid" unit="个字段" empty-text="暂无字段数据" />
          <RankList :data="templateNodeDist" title="模板节点数分布" icon="el-icon-s-operation" unit="个模板" empty-text="暂无模板数据" />
        </section>
      </section>
    </main>
  </div>
</template>

<script>
import BarChart from '@/components/charts/BarChart.vue'
import DonutChart from '@/components/charts/DonutChart.vue'
import RankList from '@/components/charts/RankList.vue'
import { getDeptDashboard } from '@/service/sys/DataService'
import { withFieldTypeNames } from './fieldTypes'

/** 数据展示（部门维度）：部门由后端按 dept_admin 登记决定，前端不传身份参数 */
export default {
  name: 'DataViewDept',
  components: { BarChart, DonutChart, RankList },
  data() {
    return {
      loading: false,
      noDept: false,
      stats: {},
      taskStatus: [],
      periodStatus: [],
      nodeStatus: [],
      dispatchRank: [],
      periodPendingRank: [],
      fieldTypeRank: [],
      templateNodeDist: [],
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
        const res = await getDeptDashboard({ trendType: 'month' })
        const d = (res && res.data) || {}
        this.stats = d.stats || {}
        this.taskStatus = d.taskStatus || []
        this.periodStatus = d.periodStatus || []
        this.nodeStatus = d.nodeStatus || []
        this.dispatchRank = d.dispatchRank || []
        this.periodPendingRank = d.periodPendingRank || []
        this.fieldTypeRank = withFieldTypeNames(d.fieldTypeRank)
        this.templateNodeDist = d.templateNodeDist || []
        this.trend = d.trend || []
        // 没有部门归属时后端返回全空壳：以此提示用户
        this.noDept = !this.num(this.stats.memberCount) && !this.taskStatus.length && !this.trend.length
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
.tip-bar { display: flex; align-items: center; gap: 8px; background: #FEF3C7; border: 1px solid #FDE68A; color: #B45309; font-size: 13px; border-radius: 3px; padding: 10px 14px; }
// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  @media (max-width: 1200px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 700px) { grid-template-columns: repeat(2, 1fr); }
}
.stat-card { display: flex; align-items: center; gap: 14px; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px 18px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon { width: 44px; height: 44px; border-radius: 3px; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; flex-shrink: 0;
  &.icon-task { background: $primary; }
  &.icon-period { background: var(--color-primary-hover); }
  &.icon-running { background: #B45309; }
  &.icon-done { background: #15803D; }
  &.icon-person { background: #2B6CB0; }
  &.icon-todo { background: #6B46C1; }
  &.icon-already { background: #6366F1; }
  &.icon-submit { background: #0F766E; }
}
.stat-body { flex: 1; min-width: 0; }
.stat-label { font-size: 12px; color: #757575; margin-bottom: 4px; white-space: nowrap; }
.stat-value { font-size: 26px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }
// 分布区
.donut-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
  @media (max-width: 1500px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 900px) { grid-template-columns: 1fr; }
}
// 排行区
.rank-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;
  @media (max-width: 900px) { grid-template-columns: 1fr; }
}
</style>
