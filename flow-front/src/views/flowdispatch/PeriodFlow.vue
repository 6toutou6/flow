<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div class="header-left">
            <div class="hl-row1">
            <button class="btn-back" @click="goBackUsers"><i class="el-icon-arrow-left" /> 返回</button>
            <nav class="breadcrumb">
              <span class="link" @click="goBack">任务管理</span>
              <span>/</span>
              <span class="link" @click="goBackUsers">期次人员</span>
              <span>/</span>
              <span class="active">流程详情</span>
            </nav>
          </div>
          <h3 class="page-heading">{{ flowTitle }}</h3>
          </div>
        </div>

        <!-- 期次上下文 -->
        <section class="tip-bar">
          <i class="el-icon-tickets" />
          <template v-if="taskName">任务：{{ taskName }}</template>
          <template v-if="periodName"> · 期次：{{ periodName }}</template>
        </section>

        <!-- 流程详情 -->
        <div v-loading="loading" class="flow-wrap">
          <template v-if="!loading && taskDetail">
            <HandlerFlowDetail :task-detail="taskDetail" :selected-handler="selectedHandler" />
          </template>
          <div v-else-if="!loading" class="empty-state">
            <i class="el-icon-view" />
            <p>暂无流程数据</p>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import { getTaskDetail } from '@/service/sys/TaskService'
import HandlerFlowDetail from '@/views/dataadmin/components/HandlerFlowDetail.vue'

export default {
  name: 'PeriodFlow',
  components: { HandlerFlowDetail },
  data() {
    return {
      loading: false,
      taskId: null,
      taskDetail: null,
      selectedHandler: null,
      taskName: '',
      periodName: ''
    }
  },
  computed: {
    flowTitle() {
      return this.selectedHandler && this.selectedHandler.userName
        ? this.selectedHandler.userName + ' 的流程'
        : '流程详情'
    }
  },
  created() {
    this.taskId = this.$route.query.taskId || null
    this.taskName = this.$route.query.taskName || ''
    this.periodName = this.$route.query.periodName || ''
    this.selectedHandler = {
      id: this.$route.query.handlerId || null,
      userName: this.$route.query.userName || '',
      yyytId: this.$route.query.yyytId || '',
      deptName: this.$route.query.deptName || ''
    }
    this.fetchDetail()
  },
  methods: {
    async fetchDetail() {
      if (!this.taskId) return
      this.loading = true
      try {
        const res = await getTaskDetail(this.taskId)
        this.taskDetail = res.data
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    goBack() {
      this.$router.push('/flow-dispatch/index')
    },
    goBackUsers() {
      this.$router.push({
        path: '/flow-dispatch/period-users',
        query: {
          dispatchId: this.$route.query.dispatchId || '',
          periodName: this.periodName,
          taskName: this.taskName
        }
      })
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
}
.flow-wrap { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 20px; min-height: 300px; }
.empty-state { text-align: center; padding: 60px 20px; color: #bbb;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
</style>
