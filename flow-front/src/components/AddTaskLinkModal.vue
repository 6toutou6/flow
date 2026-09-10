<template>
  <el-dialog
    :title="'新增关联'"
    :visible.sync="dialogVisible"
    width="560px"
    append-to-body
    :close-on-click-modal="false"
    @close="reset"
  >
    <div class="atl-tip"><i class="el-icon-link" /> 来源期次：<b>{{ sourceDispatchName || '—' }}</b> / {{ sourcePeriodName || '—' }}</div>
    <div class="atl-row">
      <span class="atl-label"><span class="req">*</span> 任务</span>
      <el-select
        v-model="selPlanId"
        filterable
        :loading="loading"
        placeholder="选择你收到的任务"
        style="flex:1"
        @change="onPlanChange"
      >
        <el-option v-for="g in groups" :key="g.taskId" :label="g.taskName" :value="g.taskId" />
      </el-select>
    </div>
    <div class="atl-row">
      <span class="atl-label"><span class="req">*</span> 期次</span>
      <el-select
        v-model="selPeriodKey"
        filterable
        :disabled="!selGroup"
        placeholder="选择期次"
        style="flex:1"
        @change="onPeriodChange"
      >
        <el-option v-for="per in periodOptions" :key="periodKey(per)" :label="periodLabel(per)" :value="periodKey(per)" />
      </el-select>
    </div>
    <div class="atl-row">
      <span class="atl-label"><span class="req">*</span> 员工任务</span>
      <el-select
        v-model="selTaskId"
        filterable
        :disabled="!selPeriod"
        placeholder="选择员工任务"
        style="flex:1"
      >
        <el-option v-for="t in empTaskOptions" :key="t.taskId" :label="t.taskName" :value="t.taskId" />
      </el-select>
    </div>
    <div class="atl-row atl-row-top">
      <span class="atl-label">关联说明</span>
      <el-input
        v-model="linkRemark"
        type="textarea"
        :rows="3"
        maxlength="120"
        show-word-limit
        placeholder="选填：说明为何关联（如：需引用该任务的收集结果）"
      />
    </div>
    <div class="atl-hint">已关联过的员工任务不会出现在选择列表中。</div>
    <div class="atl-footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="!selEmpTask" @click="handleSubmit">
        <i class="el-icon-link" /> 建立关联
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createTaskLink } from '@/service/sys/FlowDispatchService'
import { getMyTodoGrouped } from '@/service/sys/TaskService'

export default {
  name: 'AddTaskLinkModal',
  props: {
    visible: { type: Boolean, default: false },
    /** 来源任务配置名 */
    sourceDispatchName: { type: String, default: '' },
    /** 来源期次ID（flow_task_dispatch.id，必填） */
    sourcePeriodId: { type: String, default: '' },
    /** 来源期次名 */
    sourcePeriodName: { type: String, default: '' },
    /** 已关联的目标成员任务ID集合（下拉中排除） */
    linkedTaskIds: { type: Array, default: () => [] }
  },
  data() {
    return {
      loading: false,
      groups: [],
      // 级联选择值：任务(planId) → 期次(dispatchId) → 员工任务(flow_task.id)
      selPlanId: '',
      selPeriodKey: '',
      selTaskId: '',
      linkRemark: '',
      saving: false
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.$emit('close') }
    },
    /** 当前选中任务 */
    selGroup() {
      return this.groups.find(g => g.taskId === this.selPlanId) || null
    },
    /** 当前任务下的期次 */
    periodOptions() {
      return this.selGroup ? (this.selGroup.periods || []) : []
    },
    /** 当前选中期次 */
    selPeriod() {
      return this.periodOptions.find(p => this.periodKey(p) === this.selPeriodKey) || null
    },
    /** 当前期次下「我收到的员工任务」（按 taskId 去重，排除已关联） */
    empTaskOptions() {
      const per = this.selPeriod
      if (!per) return []
      const linked = this.linkedTaskIds || []
      const seen = {}
      const out = []
      ;(per.todos || []).forEach(t => {
        if (!t || !t.taskId || seen[t.taskId]) return
        seen[t.taskId] = true
        if (linked.indexOf(t.taskId) >= 0) return
        out.push(t)
      })
      return out
    },
    /** 当前选中员工任务 */
    selEmpTask() {
      return this.empTaskOptions.find(t => t.taskId === this.selTaskId) || null
    }
  },
  watch: {
    visible(val) {
      if (!val) return
      this.reset()
      this.loadGroups()
    }
  },
  methods: {
    /** 期次唯一键（存量无期次任务 dispatchId 为空，用固定键兜底） */
    periodKey(per) {
      return per.dispatchId || '__none__'
    },
    periodLabel(per) {
      const name = per.periodName || '无期次'
      return per.periodNo ? `${name}（第 ${per.periodNo} 期）` : name
    },
    onPlanChange() {
      this.selPeriodKey = ''
      this.selTaskId = ''
    },
    onPeriodChange() {
      this.selTaskId = ''
    },
    /** 我收到的任务（任务 → 期次 → 员工任务 三层，含已完成） */
    async loadGroups() {
      this.loading = true
      try {
        const res = await getMyTodoGrouped({ page: 1, limit: 500 })
        this.groups = ((res && res.data && res.data.records) || []).filter(g => g && g.taskId)
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '我的任务加载失败')
      } finally {
        this.loading = false
      }
    },
    reset() {
      this.selPlanId = ''
      this.selPeriodKey = ''
      this.selTaskId = ''
      this.linkRemark = ''
    },
    async handleSubmit() {
      if (!this.selEmpTask) {
        this.$message.warning('请选择要关联的员工任务')
        return
      }
      this.saving = true
      try {
        const res = await createTaskLink({
          sourcePeriodId: this.sourcePeriodId,
          targetTaskId: this.selEmpTask.taskId,
          remark: (this.linkRemark && this.linkRemark.trim()) || null
        })
        this.$message.success(res.message || '关联成功')
        this.reset()
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

.atl-tip { font-size: 12px; color: #8a93a5; background: var(--color-primary-light); border: 1px dashed rgba(var(--color-primary-rgb), 0.35); border-radius: 3px; padding: 8px 10px; margin-bottom: 14px; line-height: 1.6;
  i { color: $primary; }
  b { color: #414755; }
}
.atl-row { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.atl-row-top { align-items: flex-start; }
.atl-label { width: 110px; flex-shrink: 0; font-size: 13px; font-weight: 600; color: #414755; }
.req { color: $primary; }
.atl-opt-sub { color: #999; font-size: 11px; margin-left: 8px; }
.atl-hint { font-size: 12px; color: #999; padding-left: 122px; }
.atl-footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
</style>
