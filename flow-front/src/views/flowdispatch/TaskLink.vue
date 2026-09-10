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
                <span class="active">期次任务关联</span>
              </nav>
            </div>
            <h3 class="page-heading">期次任务关联</h3>
          </div>
        </div>

        <!-- 来源期次信息 -->
        <section class="tip-bar">
          <i class="el-icon-tickets" />
          <template v-if="sourceTaskName">任务：{{ sourceTaskName }}</template>
          <template v-if="sourcePeriodName"> · 期次：{{ sourcePeriodName }}</template>
          <template v-if="!sourceTaskName && !sourcePeriodName">期次任务关联</template>
          <span class="tip-sub">· 本期次可关联到「我收到的任务」，双方均可查看与解除；点击行可展开目标任务流程链</span>
        </section>

        <!-- 关联列表 -->
        <section class="link-card">
          <div class="lc-head">
            <span class="lc-title"><i class="el-icon-link" /> 已关联任务 <span class="lc-count">共 {{ existingLinks.length }} 条</span></span>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="addVisible = true">新增关联</el-button>
          </div>

          <div v-if="existingLoading" class="lc-loading"><i class="el-icon-loading" /> 加载关联中...</div>
          <div v-else-if="existingLinks.length === 0" class="lc-empty">
            <i class="el-icon-link" />
            <p>本期次还没有关联任务</p>
            <el-button type="primary" size="small" @click="addVisible = true">新增关联</el-button>
          </div>
          <table v-else class="rel-table">
            <thead>
              <tr>
                <th>任务名称</th>
                <th class="text-center">状态</th>
                <th>流程进度</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="lk in existingLinks">
                <tr :key="lk.id" class="hover-row" :class="{ 'rel-row-open': isOpen(lk) }" @click="toggleExpand(lk)">
                  <td>
                    <i class="el-icon-arrow-right rel-arrow" :class="{ open: isOpen(lk) }" />
                    <span class="emp-task-name" :title="lk.targetTaskName || lk.targetDispatchName || ''">{{ lk.targetTaskName || lk.targetDispatchName || '—' }}</span>
                    <span v-if="lk.targetPeriodName" class="rel-period-tag" :title="lk.targetPeriodName">{{ lk.targetPeriodName }}</span>
                  </td>
                  <td class="text-center">
                    <span class="status-badge" :class="isDone(lk) ? 'st-done' : 'st-todo'">{{ isDone(lk) ? '已完成' : '进行中' }}</span>
                  </td>
                  <td>
                    <div class="node-chain">
                      <template v-for="(nd, ni) in (lk.targetChain || [])">
                        <span
                          :key="'n' + ni"
                          class="node-chip"
                          :class="chipCls(nd.status)"
                          :title="nd.nodeName + '（' + (nd.status === 1 ? '已完成' : (nd.status === 2 ? '进行中' : '未开始')) + '）'"
                        >{{ ni + 1 }}.{{ nd.nodeName }}</span>
                      </template>
                      <span v-if="!(lk.targetChain || []).length" class="text-muted">—</span>
                    </div>
                  </td>
                  <td class="text-right">
                    <button class="action-link" @click.stop="toggleExpand(lk)"><i class="el-icon-view" /> {{ isOpen(lk) ? '收起' : '查看流程' }}</button>
                    <button class="action-link text-error" @click.stop="handleRemove(lk)"><i class="el-icon-delete" /> 解除</button>
                  </td>
                </tr>
                <!-- 折叠展开：流程链组件（横/竖） -->
                <tr v-if="isOpen(lk)" :key="'d' + lk.id" class="rel-detail-tr">
                  <td colspan="4">
                    <div class="rel-detail">
                      <div v-if="loadingDetailOf(lk)" class="detail-loading"><i class="el-icon-loading" /> 加载流程链...</div>
                      <FlowChain
                        v-else-if="detailOf(lk)"
                        :key="'fc' + lk.id"
                        :task-detail="detailOf(lk)"
                        title="流程链"
                      />
                      <div v-else-if="chainFallback(lk)" class="detail-fallback">
                        <div class="node-chain">
                          <template v-for="(nd, ni) in (lk.targetChain || [])">
                            <span :key="'f' + ni" class="node-chip" :class="chipCls(nd.status)">{{ ni + 1 }}.{{ nd.nodeName }}</span>
                          </template>
                        </div>
                        <div class="detail-fallback-tip">流程详情加载失败，以上为目标任务节点概览</div>
                      </div>
                      <div v-else class="detail-loading"><i class="el-icon-warning-outline" /> 流程详情加载失败，请刷新后重试</div>

                      <!-- 关联信息条 -->
                      <div class="rel-extra">
                        <span v-if="lk.remark" class="rel-extra-item"><i class="el-icon-chat-line-square" /> 关联说明：{{ lk.remark }}</span>
                        <span class="rel-extra-item"><i class="el-icon-time" /> 关联时间：{{ lk.createTime || '—' }}</span>
                        <button class="btn-danger" @click="handleRemove(lk)"><i class="el-icon-delete" /> 解除关联</button>
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </section>
      </section>
    </main>

    <!-- 新增关联弹窗 -->
    <AddTaskLinkModal
      :visible="addVisible"
      :source-dispatch-name="sourceTaskName"
      :source-period-id="sourcePeriodId"
      :source-period-name="sourcePeriodName"
      :linked-task-ids="linkedTaskIds"
      @success="onAdded"
      @close="addVisible = false"
    />
  </div>
</template>

<script>
import FlowChain from '@/components/FlowChain.vue'
import AddTaskLinkModal from '@/components/AddTaskLinkModal.vue'
import { getTaskLinksBySource, removeTaskLink } from '@/service/sys/FlowDispatchService'
import { getTaskDetail } from '@/service/sys/TaskService'

export default {
  name: 'FlowDispatchTaskLink',
  components: { FlowChain, AddTaskLinkModal },
  data() {
    return {
      existingLinks: [],
      existingLoading: false,
      expandedIds: [],
      linkDetails: {},
      detailLoadingIds: [],
      detailFailedIds: [],
      addVisible: false
    }
  },
  computed: {
    /** 来源任务配置名（路由带参） */
    sourceTaskName() {
      return this.$route.query.taskName || ''
    },
    /** 来源期次ID */
    sourcePeriodId() {
      return this.$route.query.periodId || ''
    },
    /** 来源期次名 */
    sourcePeriodName() {
      return this.$route.query.periodName || ''
    },
    /** 已关联目标任务ID集合（新增弹窗排除） */
    linkedTaskIds() {
      return this.existingLinks.map(l => l.targetTaskId).filter(Boolean)
    }
  },
  created() {
    this.loadExisting()
  },
  methods: {
    goBack() {
      this.$router.push('/flow-dispatch/index')
    },
    /** 本来源期次已建立的关联 */
    async loadExisting() {
      if (!this.sourcePeriodId) return
      this.existingLoading = true
      try {
        const res = await getTaskLinksBySource(null, null, this.sourcePeriodId)
        this.existingLinks = (res && res.data) || []
      } catch (e) {
        console.error(e)
        this.existingLinks = []
      } finally {
        this.existingLoading = false
      }
    },
    onAdded() {
      this.addVisible = false
      this.loadExisting()
    },
    /** 目标任务是否已完成 */
    isDone(lk) {
      return (lk && (lk.doneCount || 0)) > 0 && (lk.doneCount || 0) >= (lk.memberCount || 1)
    },
    /** 节点 chip 样式映射 */
    chipCls(s) {
      return { 1: 'chip-done', 2: 'chip-current', 0: 'chip-pending' }[s] || 'chip-pending'
    },
    isOpen(lk) {
      return this.expandedIds.indexOf(lk.id) >= 0
    },
    loadingDetailOf(lk) {
      return lk && this.detailLoadingIds.indexOf(lk.id) >= 0
    },
    detailOf(lk) {
      return (lk && this.linkDetails[lk.id]) || null
    },
    /** 展开切换：打开且无详情缓存时异步加载目标任务完整详情供流程链渲染 */
    toggleExpand(lk) {
      const i = this.expandedIds.indexOf(lk.id)
      if (i >= 0) {
        this.expandedIds.splice(i, 1)
        return
      }
      this.expandedIds.push(lk.id)
      if (!this.linkDetails[lk.id] && this.detailFailedIds.indexOf(lk.id) < 0) {
        this.fetchDetail(lk)
      }
    },
    async fetchDetail(lk) {
      if (!lk || !lk.targetTaskId) return
      if (this.detailLoadingIds.indexOf(lk.id) >= 0) return
      this.detailLoadingIds.push(lk.id)
      try {
        const res = await getTaskDetail(lk.targetTaskId)
        this.$set(this.linkDetails, lk.id, (res && res.data) || null)
      } catch (e) {
        console.error(e)
        this.detailFailedIds.push(lk.id)
      } finally {
        const li = this.detailLoadingIds.indexOf(lk.id)
        if (li >= 0) this.detailLoadingIds.splice(li, 1)
      }
    },
    chainFallback(lk) {
      return (lk && lk.targetChain && lk.targetChain.length > 0) || false
    },
    /** 解除一条关联 */
    async handleRemove(lk) {
      if (!lk || !lk.id) return
      try {
        await this.$confirm(`解除与「${lk.targetTaskName || lk.targetDispatchName || ''}」的关联？`, '解除关联', { type: 'warning' })
      } catch (e) {
        return
      }
      try {
        await removeTaskLink(lk.id)
        this.$message.success('已解除关联')
        this.loadExisting()
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '解除失败')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #e4e7ed;

.dashboard-container { display: flex; min-height: 100vh; background-color: var(--color-primary-surface); color: #1b1c1c; }
.main-content { flex: 1; min-width: 0; padding: 16px; }
.page-content { display: flex; flex-direction: column; gap: 12px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.header-left { display: flex; flex-direction: column; gap: 6px; }
.hl-row1 { display: flex; align-items: center; gap: 12px; }
.btn-back { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; background: transparent; border: none; color: $primary; cursor: pointer; font-size: 13px;
  &:hover { background: var(--color-primary-light); border-radius: 3px; }
}
.breadcrumb { font-size: 12px; color: #909399; display: flex; align-items: center; gap: 6px;
  .link { color: $primary; cursor: pointer; }
  .active { color: #414755; }
}
.page-heading { margin: 0; font-size: 17px; font-weight: 700; color: #1b1c1c; }
.tip-bar { display: flex; align-items: center; gap: 8px; background: var(--color-primary-light); border: 1px solid $border; color: var(--color-primary-hover); font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: $primary; }
  .tip-sub { color: #8a93a5; margin-left: 6px; }
}
.link-card { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 14px 16px; }
.lc-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.lc-title { font-size: 14px; font-weight: 700; color: #1b1c1c;
  i { color: $primary; margin-right: 4px; }
}
.lc-count { font-size: 12px; font-weight: 400; color: #909399; margin-left: 6px; }
.lc-loading { font-size: 13px; color: #999; padding: 28px 0; text-align: center; }
.lc-empty { text-align: center; color: #999; padding: 36px 0;
  i { font-size: 34px; color: #d4d9e0; display: block; margin-bottom: 8px; }
  p { margin: 0 0 14px; font-size: 13px; }
}
// 表格（对齐任务处理「期次列表」）
.rel-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 10px 12px; font-weight: 700; color: #414755; background: var(--color-primary-light); border-bottom: 1px solid $border; font-size: 13px; white-space: nowrap; }
  td { padding: 10px 12px; border-bottom: 1px solid $border; font-size: 13px; }
  tbody tr:last-child td { border-bottom: none; }
  .hover-row { cursor: pointer;
    &:hover { background: var(--color-primary-light); }
  }
  .rel-row-open td { background: #F4F7FB; }
}
.rel-arrow { color: #94a3b8; font-size: 12px; margin-right: 6px; transition: transform 0.2s;
  &.open { transform: rotate(90deg); color: $primary; }
}
.text-center { text-align: center; }
.text-right { text-align: right; }
.text-muted { color: #bbb; }
.emp-task-name { display: inline-block; max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; color: #1b1c1c; }
.rel-period-tag { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; background: rgba(var(--color-primary-rgb), 0.08); color: $primary; margin-left: 6px; vertical-align: 1px; white-space: nowrap; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 600; white-space: nowrap;
  &.st-todo { background: rgba(var(--color-primary-rgb), 0.1); color: $primary; }
  &.st-done { background: rgba(21, 128, 61, 0.1); color: #15803D; }
}
.node-chain { display: flex; align-items: center; gap: 4px; overflow-x: auto; padding-bottom: 2px; }
.node-chip { flex-shrink: 0; padding: 2px 9px; border-radius: 4px; font-size: 11px; font-weight: 600; line-height: 1.6; white-space: nowrap;
  &.chip-done { background: #15803D; color: #fff; }
  &.chip-current { background: $primary; color: #fff; font-weight: 700; box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.35) inset; }
  &.chip-pending { background: #f0f0f0; color: #aaa; }
}
.action-link { color: $primary; background: none; border: none; cursor: pointer; font-size: 13px;
  &:hover { text-decoration: underline; }
  &.text-error { color: #B45309; }
}
.rel-detail-tr td { background: #FBFCFE; padding: 14px 18px; }
.rel-detail { display: flex; flex-direction: column; gap: 12px; }
.detail-loading { font-size: 13px; color: #999; text-align: center; padding: 30px 0; }
.detail-fallback { display: flex; flex-direction: column; gap: 8px; }
.detail-fallback-tip { font-size: 12px; color: #bbb; }
.rel-extra { display: flex; align-items: center; flex-wrap: wrap; gap: 16px; border-top: 1px dashed #e8edf3; padding-top: 10px; font-size: 12px; }
.rel-extra-item { color: #8a93a5;
  i { color: $primary; margin-right: 3px; }
}
.btn-danger { display: inline-flex; align-items: center; gap: 4px; margin-left: auto; background: none; border: none; color: #B45309; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { text-decoration: underline; }
}
</style>
