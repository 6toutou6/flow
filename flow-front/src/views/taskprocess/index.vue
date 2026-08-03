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
          <button class="btn-refresh" @click="fetchData"><i class="el-icon-refresh" /> 刷新</button>
        </div>

        <!-- 待办表格 -->
        <div class="table-card table-loading-wrapper">
          <div v-if="loading" class="loading-overlay">
            <div class="loading-spinner"><i class="el-icon-loading spinning" /><p>加载中...</p></div>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>任务名称</th>
                <th>所属模板</th>
                <th>当前节点</th>
                <th>状态</th>
                <th>下发时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in list" :key="row.taskNodeId" class="hover-row">
                <td class="font-bold">{{ row.taskName }}</td>
                <td>{{ row.templateName || '—' }}</td>
                <td>
                  <span class="node-badge" :class="nodeTypeClass(row.nodeType)">{{ nodeTypeText(row.nodeType) }}</span>
                  {{ row.nodeName }}
                </td>
                <td>
                  <span class="status-badge" :class="row.todoStatus === 1 ? 'st-done' : 'st-todo'">
                    {{ row.todoStatus === 1 ? '已处理' : '待处理' }}
                  </span>
                </td>
                <td>{{ row.taskCreateTime }}</td>
                <td class="text-right">
                  <button v-if="row.todoStatus !== 1" class="btn-process" @click="openProcess(row)"><i class="el-icon-s-claim" /> 处理</button>
                  <button v-else class="btn-view" @click="openProcess(row)"><i class="el-icon-view" /> 查看详情</button>
                </td>
              </tr>
              <tr v-if="!loading && list.length === 0">
                <td colspan="6" class="text-center" style="padding: 40px; color: #999;">
                  <i class="el-icon-finished" style="font-size: 40px; display: block; margin-bottom: 8px;" />
                  暂无任务
                </td>
              </tr>
            </tbody>
          </table>
          <!-- 分页 -->
          <div v-if="total > 0" class="pagination">
            <span class="pagination-info">共计 {{ total }} 条任务</span>
            <div class="pagination-controls">
              <button class="page-btn" :disabled="currentPage === 1" @click="prevPage"><i class="el-icon-arrow-left" /></button>
              <span class="page-current">{{ currentPage }} / {{ totalPages }}</span>
              <button class="page-btn" :disabled="currentPage === totalPages" @click="nextPage"><i class="el-icon-arrow-right" /></button>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 处理弹窗（独立组件） -->
    <ProcessDialog
      :visible="processVisible"
      :todo="currentTodo"
      :detail="taskDetail"
      :submitting="submitting"
      @close="onCloseProcess"
      @submit="handleSubmit"
    />
  </div>
</template>

<script>
import { getMyTodoList, getTaskDetail, submitTask } from '@/api/task'
import ProcessDialog from './components/ProcessDialog.vue'

export default {
  name: 'TaskProcess',
  components: { ProcessDialog },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      // 处理弹窗
      processVisible: false,
      currentTodo: null,
      taskDetail: null,
      submitting: false
    }
  },
  computed: {
    totalPages() { return Math.ceil(this.total / this.pageSize) || 1 }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    nodeTypeText(t) { return { 1: '开始', 2: '', 3: '结束' }[t] || '' },
    nodeTypeClass(t) { return { 1: 'badge-start', 3: 'badge-end' }[t] || 'badge-mid' },
    async fetchData() {
      this.loading = true
      try {
        const res = await getMyTodoList({ page: this.currentPage, limit: this.pageSize })
        this.list = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    prevPage() { if (this.currentPage > 1) { this.currentPage--; this.fetchData() } },
    nextPage() { if (this.currentPage < this.totalPages) { this.currentPage++; this.fetchData() } },
    async openProcess(todo) {
      this.currentTodo = todo
      this.processVisible = true
      this.taskDetail = null
      try {
        const res = await getTaskDetail(todo.taskId)
        this.taskDetail = res.data
      } catch (e) {
        console.error(e)
      }
    },
    onCloseProcess() {
      this.processVisible = false
      this.currentTodo = null
      this.taskDetail = null
    },
    async handleSubmit(payload) {
      this.submitting = true
      try {
        await submitTask(payload)
        const isReject = payload.action === 'reject'
        const isEnd = this.currentTodo && this.currentTodo.nodeType === 3
        this.$message.success(isReject ? '已退回到目标节点，表单已回填上次数据' : (isEnd ? '已提交，任务已完成' : '提交成功，已流转至下一节点'))
        this.processVisible = false
        this.fetchData()
      } catch (e) {
        console.error(e)
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
$border: #e4beba;
.dashboard-container { display: flex; min-height: 100vh; background: #F5F7FA; font-family: 'Inter', sans-serif; color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
}
.page-heading { font-size: 30px; line-height: 38px; font-weight: 600; color: #1b1c1c; }
.btn-refresh { display: flex; align-items: center; gap: 4px; padding: 8px 16px; background: #fff; border: 1px solid $border; border-radius: 6px; color: #5b403d; cursor: pointer; font-size: 13px;
  &:hover { background: #f6f3f2; }
}

// 表格
.table-card { background: #fff; border: 1px solid $border; border-radius: 8px; overflow: hidden; position: relative; }
.loading-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(255,255,255,0.9); display: flex; align-items: center; justify-content: center; z-index: 10; }
.loading-spinner { text-align: center; color: $primary;
  .el-icon-loading { font-size: 40px; display: block; margin-bottom: 8px; }
  p { font-size: 14px; color: #606266; margin: 0; }
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.spinning { animation: spin 1s linear infinite; }
.data-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 12px 16px; font-weight: 700; color: #414755; background: #FAFAFA; border-bottom: 1px solid $border; }
  td { padding: 12px 16px; border-bottom: 1px solid $border; }
  .hover-row:hover { background: #FFF5F5; }
}
.font-bold { font-weight: 700; }
.text-muted { color: #999; font-size: 13px; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.node-badge { padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 600; margin-right: 4px; }
.badge-start { background: rgba(38,109,0,0.1); color: #266d00; }
.badge-mid { background: #f0f3ff; color: #545f72; }
.badge-end { background: rgba(197,48,48,0.1); color: $primary; }
.btn-process { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: $primary; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { opacity: 0.9; }
}
.btn-view { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: #fff; color: #545f72; border: 1px solid #d8dee9; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { background: #f0f3ff; border-color: #b7c3d8; }
}
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 600;
  &.st-todo { background: rgba(197,48,48,0.1); color: $primary; }
  &.st-done { background: rgba(38,109,0,0.1); color: #266d00; }
}
.pagination { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #faf9f9; border-top: 1px solid $border; }
.pagination-info { font-size: 12px; color: #414755; }
.pagination-controls { display: flex; align-items: center; gap: 8px; }
.page-btn { width: 32px; height: 32px; border-radius: 4px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; cursor: pointer; font-size: 14px;
  &:hover:not(:disabled) { background: #efeded; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.page-current { font-size: 13px; color: #414755; }
</style>
