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
      <span class="atl-label"><span class="req">*</span> 我收到的任务</span>
      <el-select v-model="selMyTask" filterable placeholder="搜索你收到的任务（关联对象）" style="flex:1" value-key="taskId">
        <el-option v-for="m in availMyTasks" :key="m.taskId" :label="m.taskName" :value="m">
          <span>{{ m.taskName }}</span>
          <span v-if="m.periodName" class="atl-opt-sub">{{ m.periodName }}</span>
          <span v-if="m.taskId" class="atl-opt-sub"> · {{ m.taskId }}</span>
        </el-option>
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
    <div class="atl-hint">已关联过的任务不会出现在选择列表中。</div>
    <div class="atl-footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="!selMyTask" @click="handleSubmit">
        <i class="el-icon-link" /> 建立关联
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createTaskLink } from '@/service/sys/FlowDispatchService'
import { getMyTodoList } from '@/service/sys/TaskService'

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
      myTasks: [],
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
    /** 排除已关联过的目标任务 */
    availMyTasks() {
      const linked = this.linkedTaskIds || []
      return this.myTasks.filter(m => linked.indexOf(m.taskId) < 0)
    }
  },
  watch: {
    visible(val) {
      if (!val) return
      this.selMyTask = null
      this.linkRemark = ''
      this.loadMyTasks()
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
    reset() {
      this.selMyTask = null
      this.linkRemark = ''
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
        this.selMyTask = null
        this.linkRemark = ''
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
