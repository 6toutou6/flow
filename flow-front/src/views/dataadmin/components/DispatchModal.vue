<template>
  <el-dialog title="下发流程任务" :visible.sync="dialogVisible" width="900px" :close-on-click-modal="false" append-to-body @close="handleClose">
    <!-- 步骤条 -->
    <div class="steps-card">
      <el-steps :active="step" align-center finish-status="success">
        <el-step title="选择模板" description="选择流程模板" />
        <el-step title="任务详情" description="填写任务信息" />
        <el-step title="指定首节点处理人" description="可多选，每人一个独立任务" />
      </el-steps>
    </div>

    <!-- 步骤1：选择模板 -->
    <div v-if="step === 0" class="step-panel">
      <div class="panel-hint">请选择一个启用的流程模板</div>
      <div v-loading="tplLoading" class="tpl-grid">
        <div
          v-for="tpl in templates"
          :key="tpl.id"
          class="tpl-card"
          :class="{ selected: form.templateId === tpl.id }"
          @click="pickTemplate(tpl)"
        >
          <div class="tpl-card-header">
            <i class="el-icon-document" />
            <span class="tpl-card-name">{{ tpl.templateName }}</span>
          </div>
          <div class="tpl-card-meta">
            <span v-if="tpl.category" class="meta-tag">{{ tpl.category }}</span>
            <span class="meta-ver">v{{ tpl.version }}</span>
          </div>
        </div>
        <div v-if="!tplLoading && templates.length === 0" class="empty-state">
          <i class="el-icon-warning-outline" /> 暂无启用的模板，请先在模板管理中创建并启用
        </div>
      </div>
      <div class="step-actions">
        <button class="btn-next" :disabled="!form.templateId" @click="goStep2">下一步 <i class="el-icon-arrow-right" /></button>
      </div>
    </div>

    <!-- 步骤2：任务详情 -->
    <div v-if="step === 1" class="step-panel">
      <div class="panel-hint">填写本次任务信息（每个处理人会收到一个同配置的独立任务）</div>
      <div class="form-card">
        <div class="form-row">
          <label class="form-label"><span class="req">*</span> 任务名称</label>
          <input v-model="form.taskName" class="form-input" placeholder="请输入本次任务名称" maxlength="100">
        </div>
        <div class="form-row form-row-2">
          <div>
            <label class="form-label"><span class="req">*</span> 填报开始时间</label>
            <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%" />
          </div>
          <div>
            <label class="form-label"><span class="req">*</span> 填报截止时间</label>
            <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择截止时间" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%" />
          </div>
        </div>
        <div class="form-row">
          <label class="form-label">任务说明</label>
          <textarea v-model="form.taskDesc" class="form-textarea" rows="3" placeholder="任务公告/填报要求说明（选填）" maxlength="500" />
        </div>
      </div>
      <div class="step-actions">
        <button class="btn-prev" @click="step = 0"><i class="el-icon-arrow-left" /> 上一步</button>
        <button class="btn-next" :disabled="!form.taskName || !form.startTime || !form.endTime" @click="step = 2">下一步 <i class="el-icon-arrow-right" /></button>
      </div>
    </div>

    <!-- 步骤3：指定首节点处理人（多选） -->
    <div v-if="step === 2" class="step-panel">
      <div class="panel-hint">
        指定首个节点（开始节点）的处理人，可多选；每个处理人将收到一个独立任务，各自按流程流转
        <button class="btn-add-user" @click="openUserPicker"><i class="el-icon-plus" /> 添加处理人</button>
      </div>
      <div class="selected-summary">
        <i class="el-icon-user" />
        <span>已选 <b>{{ firstHandlers.length }}</b> 个处理人</span>
        <span v-if="firstHandlers.length === 0" class="sum-tip">· 点击右上方"添加处理人"选择</span>
      </div>
      <div v-if="firstHandlers.length > 0" class="handler-list">
        <div v-for="(h, i) in firstHandlers" :key="h.id" class="handler-card">
          <div class="handler-info">
            <div class="handler-avatar">{{ h.realName ? h.realName.charAt(0) : 'U' }}</div>
            <div class="handler-detail">
              <div class="handler-name">{{ h.realName }} <span class="handler-emp">{{ h.empNo }}</span></div>
              <div class="handler-dept">{{ h.deptName || '—' }} · {{ h.phone || '—' }}</div>
            </div>
          </div>
          <div class="handler-actions">
            <span class="handler-idx">#{{ i + 1 }}</span>
            <button class="action-link text-error" @click="removeHandler(h.id)"><i class="el-icon-close" /> 移除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty-handler">
        <i class="el-icon-user" />
        <p>尚未指定首节点处理人</p>
        <p class="empty-tip">点击上方「添加处理人」按钮选择（支持多选）</p>
      </div>
      <div class="step-actions">
        <button class="btn-prev" @click="step = 1"><i class="el-icon-arrow-left" /> 上一步</button>
        <button class="btn-submit" :disabled="submitting || firstHandlers.length === 0" @click="handleSubmit">
          <i v-if="submitting" class="el-icon-loading" />
          <i v-else class="el-icon-s-promotion" /> 确认下发（{{ firstHandlers.length }} 个任务）
        </button>
      </div>
    </div>

    <!-- 选人弹窗（多选） -->
    <el-dialog title="选择首节点处理人（可多选）" :visible.sync="pickerVisible" width="760px" :close-on-click-modal="false" append-to-body>
      <div class="picker-filter">
        <input v-model="pickerFilter.realName" class="filter-input" placeholder="姓名" @keyup.enter="loadUsers">
        <input v-model="pickerFilter.deptName" class="filter-input" placeholder="部门" @keyup.enter="loadUsers">
        <button class="btn-search" @click="loadUsers">查询</button>
      </div>
      <el-table
        v-loading="userLoading"
        ref="userTable"
        :data="userList"
        height="360"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" :selectable="isAlreadySelected" />
        <el-table-column prop="empNo" label="员工号" width="110" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="deptName" label="部门" />
        <el-table-column prop="phone" label="手机号" width="140" />
      </el-table>
      <div slot="footer" class="dialog-footer">
        <span class="picker-count">本次新增已选：{{ pickerSelection.length }} 人</span>
        <div>
          <el-button @click="pickerVisible = false">取消</el-button>
          <el-button type="primary" :disabled="pickerSelection.length === 0" @click="confirmPick">添加所选 ({{ pickerSelection.length }})</el-button>
        </div>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { getEnabledTemplates, getTemplateDetail } from '@/api/template'
import { createTask } from '@/api/task'
import { getUserList } from '@/api/sysuser'

export default {
  name: 'DispatchModal',
  props: {
    visible: { type: Boolean, default: false }
  },
  data() {
    return {
      step: 0,
      templates: [],
      tplLoading: false,
      form: { templateId: null, taskName: '', taskDesc: '', startTime: '', endTime: '' },
      firstHandlers: [],
      submitting: false,
      pickerVisible: false,
      userLoading: false,
      userList: [],
      pickerFilter: { realName: '', deptName: '' },
      pickerSelection: []
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.handleClose() }
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.step = 0
        this.resetForm()
        this.loadTemplates()
      }
    }
  },
  methods: {
    async loadTemplates() {
      this.tplLoading = true
      try {
        const res = await getEnabledTemplates()
        this.templates = res.data || []
      } catch (e) {
        console.error(e)
      } finally {
        this.tplLoading = false
      }
    },
    pickTemplate(tpl) {
      this.form.templateId = tpl.id
    },
    async goStep2() {
      try {
        const res = await getTemplateDetail(this.form.templateId)
        const nodes = (res.data && res.data.nodes) || []
        if (nodes.length === 0) {
          this.$message.warning('该模板尚未设计流程节点，请先在设计流程中配置节点链')
          return
        }
        this.step = 1
      } catch (e) {
        console.error(e)
      }
    },
    isAlreadySelected(row) {
      return !this.firstHandlers.some(h => h.id === row.id)
    },
    async openUserPicker() {
      this.pickerVisible = true
      this.pickerSelection = []
      await this.loadUsers()
    },
    async loadUsers() {
      this.userLoading = true
      try {
        const res = await getUserList({ page: 1, limit: 200, status: 1, ...this.pickerFilter })
        this.userList = res.data.records || []
      } catch (e) {
        console.error(e)
      } finally {
        this.userLoading = false
      }
    },
    handleSelectionChange(rows) {
      this.pickerSelection = rows || []
    },
    confirmPick() {
      if (this.pickerSelection.length === 0) return
      const existing = new Set(this.firstHandlers.map(h => h.id))
      this.pickerSelection.forEach(u => {
        if (!existing.has(u.id)) this.firstHandlers.push({ ...u })
      })
      this.pickerVisible = false
      this.pickerSelection = []
    },
    removeHandler(id) {
      this.firstHandlers = this.firstHandlers.filter(h => h.id !== id)
    },
    async handleSubmit() {
      if (this.firstHandlers.length === 0) {
        this.$message.warning('请至少添加一个首节点处理人')
        return
      }
      if (new Date(this.form.startTime).getTime() > new Date(this.form.endTime).getTime()) {
        this.$message.warning('截止时间需晚于开始时间')
        return
      }
      this.submitting = true
      try {
        const payload = {
          templateId: this.form.templateId,
          taskName: this.form.taskName,
          taskDesc: this.form.taskDesc,
          startTime: this.form.startTime,
          endTime: this.form.endTime,
          firstHandlerIds: this.firstHandlers.map(h => h.id)
        }
        const res = await createTask(payload)
        const count = (res && res.data) || this.firstHandlers.length
        this.$message.success(`成功下发 ${count} 个任务`)
        this.$emit('success', count)
        this.handleClose()
      } catch (e) {
        console.error(e)
      } finally {
        this.submitting = false
      }
    },
    resetForm() {
      this.form = { templateId: null, taskName: '', taskDesc: '', startTime: '', endTime: '' }
      this.firstHandlers = []
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
$border: #e4beba;
.steps-card { background: #fff; border: 1px solid $border; border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.step-panel { background: #fff; border: 1px solid $border; border-radius: 8px; padding: 20px; }
.panel-hint { font-size: 14px; color: #606266; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.step-actions { display: flex; justify-content: space-between; margin-top: 20px; padding-top: 16px; border-top: 1px solid #f0f0f0; }

.tpl-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; min-height: 120px; }
.tpl-card { border: 2px solid #e4e7ed; border-radius: 8px; padding: 14px; cursor: pointer; transition: all 0.2s;
  &:hover { border-color: $primary; }
  &.selected { border-color: $primary; background: #FFF5F5; box-shadow: 0 0 0 1px $primary; }
}
.tpl-card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
  i { color: $primary; font-size: 18px; }
  .tpl-card-name { font-weight: 600; font-size: 14px; color: #1b1c1c; }
}
.tpl-card-meta { display: flex; gap: 6px; }
.meta-tag { padding: 1px 6px; background: #f0f3ff; color: #545f72; border-radius: 3px; font-size: 11px; }
.meta-ver { padding: 1px 6px; background: rgba(197,48,48,0.1); color: $primary; border-radius: 3px; font-size: 11px; font-weight: 600; }
.empty-state { grid-column: 1 / -1; text-align: center; padding: 40px; color: #999; }

.form-card { display: flex; flex-direction: column; gap: 16px; }
.form-row { display: flex; flex-direction: column; gap: 6px;
  &.form-row-2 { flex-direction: row; gap: 16px;
    > div { flex: 1; display: flex; flex-direction: column; gap: 6px; }
  }
}
.form-label { font-size: 13px; color: #414755; font-weight: 600; }
.req { color: $primary; }
.form-input { height: 36px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 0 10px; font-size: 13px; outline: none;
  &:focus { border-color: $primary; box-shadow: 0 0 0 1px rgba(197,48,48,0.2); }
}
.form-textarea { border: 1px solid #dcdfe6; border-radius: 4px; padding: 8px 10px; font-size: 13px; outline: none; resize: vertical;
  &:focus { border-color: $primary; box-shadow: 0 0 0 1px rgba(197,48,48,0.2); }
}

.selected-summary { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #414755; margin-bottom: 12px;
  b { color: $primary; }
  .sum-tip { color: #999; font-size: 12px; }
}
.btn-add-user { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: $primary; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { opacity: 0.9; }
}
.handler-list { display: flex; flex-direction: column; gap: 8px; }
.handler-card { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border: 1px solid $border; border-radius: 8px; background: #FFF5F5; }
.handler-info { display: flex; align-items: center; gap: 12px; }
.handler-avatar { width: 38px; height: 38px; border-radius: 50%; background: $primary; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 600; }
.handler-detail { display: flex; flex-direction: column; gap: 2px; }
.handler-name { font-size: 14px; font-weight: 700; color: #1b1c1c; }
.handler-emp { font-size: 12px; color: #757575; font-weight: 400; margin-left: 6px; font-family: monospace; }
.handler-dept { font-size: 12px; color: #757575; }
.handler-actions { display: flex; gap: 12px; align-items: center; }
.handler-idx { font-size: 12px; color: #999; }
.action-link { color: $primary; background: none; border: none; cursor: pointer; font-size: 13px;
  &:hover { text-decoration: underline; }
}
.text-error { color: #ba1a1a; }
.empty-handler { text-align: center; padding: 40px 20px; color: #bbb;
  i { font-size: 40px; display: block; margin-bottom: 8px; }
  p { margin: 4px 0; font-size: 14px; color: #999; }
  .empty-tip { font-size: 12px; color: #bbb; }
}

.btn-prev { display: flex; align-items: center; gap: 4px; padding: 8px 18px; background: #fff; border: 1px solid $border; border-radius: 6px; color: #5b403d; cursor: pointer; font-size: 13px;
  &:hover { background: #f6f3f2; }
}
.btn-next { display: flex; align-items: center; gap: 4px; padding: 8px 18px; background: $primary; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.btn-submit { display: flex; align-items: center; gap: 6px; padding: 8px 22px; background: $primary; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 700;
  &:hover { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.picker-filter { display: flex; gap: 8px; margin-bottom: 12px; }
.filter-input { height: 32px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 0 8px; font-size: 13px; outline: none; flex: 1;
  &:focus { border-color: $primary; }
}
.btn-search { padding: 0 16px; height: 32px; border: none; border-radius: 4px; background: $primary; color: #fff; cursor: pointer; font-size: 13px; }
.dialog-footer { display: flex; justify-content: space-between; align-items: center; }
.picker-count { font-size: 13px; color: $primary; font-weight: 600; }
</style>
