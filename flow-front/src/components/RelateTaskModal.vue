<template>
  <!-- 期次任务关联管理弹窗：主表对齐「任务处理-期次列表」，展开区任务名表头 + 流程链组件 -->
  <el-dialog
    :title="'期次任务关联'"
    :visible.sync="dialogVisible"
    width="1200px"
    custom-class="rel-dialog"
    :close-on-click-modal="false"
    append-to-body
    @close="handleClose"
  >
    <div class="rel-hd">
      <span class="rel-src"><i class="el-icon-tickets" /> 来源期次：<b>{{ sourceDispatchName || '—' }}</b> / {{ sourcePeriodName || '—' }}</span>
      <el-button type="primary" size="small" icon="el-icon-plus" @click="openAdd">新增关联</el-button>
    </div>

    <div v-if="existingLoading" class="rel-loading"><i class="el-icon-loading" /> 加载关联中...</div>
    <div v-else-if="existingLinks.length === 0" class="rel-empty">
      <i class="el-icon-link" />
      <p>本期次还没有关联任务</p>
      <el-button type="primary" size="small" @click="openAdd">新增关联</el-button>
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
          <!-- 折叠展开：任务名称表头 + 流程链组件（横/竖） -->
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

    <div class="rel-foot">
      <span class="rel-tip"><i class="el-icon-info" /> 点击行或「查看流程」展开目标任务流程链；被关联的任务也会展示这一关联</span>
      <el-button @click="handleClose">关闭</el-button>
    </div>

    <!-- 新增关联二级弹窗 -->
    <el-dialog
      :title="'新增关联'"
      :visible.sync="addVisible"
      width="560px"
      append-to-body
      :close-on-click-modal="false"
      @close="resetAdd"
    >
      <div class="rtm-tip"><i class="el-icon-link" /> 来源期次：<b>{{ sourceDispatchName || '—' }}</b> / {{ sourcePeriodName || '—' }}</div>
      <div class="rtm-form-row">
        <span class="rtm-form-label"><span class="req">*</span> 我收到的任务</span>
        <el-select v-model="selMyTask" filterable placeholder="搜索你收到的任务（关联对象）" style="flex:1" value-key="taskId">
          <el-option v-for="m in availMyTasks" :key="m.taskId" :label="m.taskName" :value="m">
            <span>{{ m.taskName }}</span>
            <span v-if="m.periodName" class="rtm-opt-sub">{{ m.periodName }}</span>
            <span v-if="m.taskId" class="rtm-opt-sub"> · {{ m.taskId }}</span>
          </el-option>
        </el-select>
      </div>
      <div class="rtm-form-row rtm-form-top">
        <span class="rtm-form-label">关联说明</span>
        <el-input
          v-model="linkRemark"
          type="textarea"
          :rows="3"
          maxlength="120"
          show-word-limit
          placeholder="选填：说明为何关联（如：需引用该任务的收集结果）"
        />
      </div>
      <div class="rtm-add-hint">已关联过的任务不会出现在选择列表中。</div>
      <div class="rtm-add-footer">
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selMyTask" @click="handleSubmit">
          <i class="el-icon-link" /> 建立关联
        </el-button>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { createTaskLink, getTaskLinksBySource, removeTaskLink } from '@/service/sys/FlowDispatchService'
import { getMyTodoList, getTaskDetail } from '@/service/sys/TaskService'
import FlowChain from '@/components/FlowChain.vue'

export default {
  name: 'RelateTaskModal',
  components: { FlowChain },
  props: {
    visible: { type: Boolean, default: false },
    /** 来源任务配置ID（我创建的某任务，期次所属） */
    sourceDispatchId: { type: String, default: '' },
    /** 来源任务配置名 */
    sourceDispatchName: { type: String, default: '' },
    /** 来源期次ID（flow_task_dispatch.id，必填） */
    sourcePeriodId: { type: String, default: '' },
    /** 来源期次名 */
    sourcePeriodName: { type: String, default: '' }
  },
  data() {
    return {
      myTasks: [],
      existingLinks: [],
      existingLoading: false,
      expandedIds: [],
      // 展开行目标任务的完整详情缓存（供 FlowChain 渲染）
      linkDetails: {},
      detailLoadingIds: [],
      detailFailedIds: [],
      addVisible: false,
      selMyTask: null,
      linkRemark: '',
      saving: false
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.$emit('close') }
    },
    /** 已关联过的目标任务从下拉中排除 */
    availMyTasks() {
      const linked = this.existingLinks.map(l => l.targetTaskId).filter(Boolean)
      return this.myTasks.filter(m => linked.indexOf(m.taskId) < 0)
    }
  },
  watch: {
    visible(val) {
      if (!val) return
      this.loadMyTasks()
      this.loadExisting()
    }
  },
  methods: {
    /** 我收到的任务（成员任务，含已完成，作为关联对象） */
    async loadMyTasks() {
      if (this.myTasks.length > 0) return
      try {
        const res = await getMyTodoList({ page: 1, limit: 200 })
        const rows = ((res && res.data && res.data.records) || []).filter(m => m && m.taskId)
        // 同一成员任务去重（可能多个待办节点）
        const seen = {}
        this.myTasks = rows.filter(m => (seen[m.taskId] ? false : (seen[m.taskId] = true)))
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '我的任务加载失败')
      }
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
    openAdd() {
      this.addVisible = true
    },
    resetAdd() {
      this.selMyTask = null
      this.linkRemark = ''
    },
    handleClose() {
      this.$emit('close')
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
    },
    async handleSubmit() {
      if (!this.selMyTask) {
        this.$message.warning('请选择要关联的任务')
        return
      }
      this.saving = true
      try {
        const res = await createTaskLink({
          sourcePeriodId: this.sourcePeriodId,
          targetTaskId: this.selMyTask.taskId,
          remark: (this.linkRemark && this.linkRemark.trim()) || null
        })
        this.$message.success(res.message || '关联成功')
        this.resetAdd()
        this.addVisible = false
        this.loadExisting()
        this.$emit('success', (res && res.data) || {})
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '关联失败')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #e4e7ed;

.rel-hd { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.rel-src { font-size: 13px; color: #8a93a5;
  b { color: #414755; }
  i { color: $primary; margin-right: 4px; }
}
.rel-loading { font-size: 13px; color: #999; padding: 28px 0; text-align: center; }
// 弹窗主体限高：内容超高时在主体内滚动，不撑破屏幕
::v-deep .rel-dialog .el-dialog__body { max-height: 72vh; overflow-y: auto; }
::v-deep .rel-dialog .el-dialog__header { padding-bottom: 12px; }
.rel-empty { text-align: center; color: #999; padding: 36px 0;
  i { font-size: 34px; color: #d4d9e0; display: block; margin-bottom: 8px; }
  p { margin: 0 0 14px; font-size: 13px; }
}
// ===== 表格：对齐任务处理「期次列表」=====
.rel-table { width: 100%; text-align: left; border-collapse: collapse; margin-bottom: 6px;
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
// ===== 展开详情行 =====
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
.rel-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.rel-tip { font-size: 12px; color: #999;
  i { color: $primary; margin-right: 3px; }
}
.rtm-tip { font-size: 12px; color: #8a93a5; background: var(--color-primary-light); border: 1px dashed rgba(var(--color-primary-rgb), 0.35); border-radius: 3px; padding: 8px 10px; margin-bottom: 14px; line-height: 1.6;
  i { color: $primary; }
  b { color: #414755; }
}
.rtm-form-row { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.rtm-form-top { align-items: flex-start; }
.rtm-form-label { width: 110px; flex-shrink: 0; font-size: 13px; font-weight: 600; color: #414755; }
.req { color: $primary; }
.rtm-opt-sub { color: #999; font-size: 11px; margin-left: 8px; }
.rtm-add-hint { font-size: 12px; color: #999; padding-left: 122px; }
.rtm-add-footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
</style>
