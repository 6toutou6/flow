<template>
  <el-dialog title="处理任务" :visible.sync="dialogVisible" width="1120px" :close-on-click-modal="false" append-to-body @close="handleClose">
    <div v-loading="loading" class="process-wrap">
      <!-- 左侧：表单与操作区 -->
      <div class="pd-left">
      <!-- 任务信息 -->
      <div class="info-section">
        <div class="info-row"><span class="il">任务名称</span><span class="iv">{{ todo && todo.taskName }}</span></div>
        <div class="info-row"><span class="il">所属模板</span><span class="iv">{{ todo && todo.templateName }}</span></div>
        <div class="info-row"><span class="il">当前节点</span><span class="iv">{{ todo && todo.nodeName }}</span></div>
        <div v-if="taskDesc" class="info-row desc-row">
          <span class="il">任务说明</span>
          <span class="iv desc-text">{{ taskDesc }}</span>
        </div>
        <!-- 任务基础信息（模板级字段，创建人下发时赋值，处理人可见） -->
        <template v-if="templateFieldRows.length > 0">
          <div class="info-tpl-title"><i class="el-icon-collection" /> 任务基础信息</div>
          <div v-for="r in templateFieldRows" :key="r.id" class="info-row desc-row">
            <span class="il">{{ r.label }}</span>
            <span class="iv desc-text">{{ r.value || '—' }}</span>
          </div>
        </template>
      </div>

      <!-- 完整流程链（含未到节点灰色骨架） -->
      <div v-if="flowChain.length > 0" class="chain-section">
        <div class="section-title">流程链 <span class="chain-hint">点击已处理节点可查看填写内容</span></div>
        <div class="chain-track">
          <div
            v-for="(item, idx) in flowChain"
            :key="idx"
            class="chain-step"
            :class="['st-' + item.status, { clickable: item.status === 'done' || item.status === 'rejected', expanded: expandedNodeId === item.nodeId }]"
            @click="toggleNodeForm(item)"
          >
            <div class="step-head">
              <span class="step-no">{{ idx + 1 }}</span>
              <span class="step-name">{{ item.nodeName }}</span>
              <span class="step-badge" :class="'badge-' + item.status">{{ statusLabel(item.status) }}</span>
              <span v-if="item.status === 'current'" class="cur-stage-tag">当前阶段</span>
            </div>
            <div class="step-meta">
              <template v-if="item.taskNode && item.taskNode.submitStatus === 1">
                <span><i class="el-icon-user" /> {{ item.taskNode.handlerName || '—' }}</span>
                <span><i class="el-icon-time" /> {{ item.taskNode.handleTime || '—' }}</span>
                <span v-if="item.taskNode.action === 1" class="meta-reject">已退回</span>
                <span v-if="item.taskNode.action === 1 && item.taskNode.rejectReason" class="meta-reason" :title="item.taskNode.rejectReason">原因：{{ item.taskNode.rejectReason }}</span>
                <span v-if="item.taskNode.action === 0 && item.taskNode.passComment" class="meta-pass" :title="item.taskNode.passComment">意见：{{ item.taskNode.passComment }}</span>
              </template>
              <template v-else-if="item.taskNode && item.taskNode.submitStatus === 0">
                <span><i class="el-icon-user" /> {{ item.pendingNames || item.taskNode.handlerName || '待处理' }}</span>
                <span v-if="item.taskNode.rejectReason" class="meta-reason"><i class="el-icon-warning-outline" /> 退回建议：{{ item.taskNode.rejectReason }}</span>
              </template>
              <template v-else>
                <span class="meta-pending">未到</span>
              </template>
            </div>
            <!-- 展开历史表单（退回节点不显示表单） -->
            <div v-if="(item.status === 'done' || item.status === 'rejected') && expandedNodeId === item.nodeId" class="step-form" @click.stop>
              <template v-if="item.status === 'rejected'">
                <div class="form-empty">该节点已退回，无需显示表单</div>
              </template>
              <template v-else>
                <div v-if="item.taskNode && item.taskNode.formDataList && item.taskNode.formDataList.length > 0">
                  <div v-for="(fd, fi) in item.taskNode.formDataList" :key="fi" class="form-row">
                    <span class="fr-label">{{ fd.fieldLabel }}</span>
                    <span class="fr-value">{{ fd.fieldValue || '—' }}</span>
                  </div>
                </div>
                <div v-else class="form-empty">该节点未填写表单数据</div>
              </template>
            </div>
          </div>
        </div>
      </div>

      <!-- 动态表单 -->
      <div v-if="currentFields.length > 0" class="form-section">
        <div class="section-title">
          <span class="cur-stage-tag">当前阶段</span>{{ todo && todo.nodeName }}
          <span v-if="isRefill" class="refill-tag"><i class="el-icon-refresh-left" /> 已回填上次数据，可修改后重新提交</span>
        </div>
        <el-form ref="processForm" :model="formData" label-width="140px" class="process-form">
          <el-form-item
            v-for="f in currentFields"
            :key="f.id"
            :label="f.fieldLabel"
            :required="f.required === 1"
          >
            <el-input v-if="f.fieldType === 'text'" v-model="formData[f.id]" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" />
            <el-input v-else-if="f.fieldType === 'textarea'" v-model="formData[f.id]" type="textarea" :rows="3" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" />
            <el-input-number v-else-if="f.fieldType === 'number'" v-model="formData[f.id]" :placeholder="f.placeholder || '请输入'" controls-position="right" style="width: 100%" />
            <el-date-picker v-else-if="f.fieldType === 'date'" v-model="formData[f.id]" type="date" placeholder="选择日期" value-format="yyyy-MM-dd" style="width: 100%" />
            <el-radio-group v-else-if="f.fieldType === 'radio'" v-model="formData[f.id]">
              <el-radio v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
            </el-radio-group>
            <el-checkbox-group v-else-if="f.fieldType === 'checkbox'" v-model="formData[f.id]">
              <el-checkbox v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-checkbox>
            </el-checkbox-group>
            <el-input v-else v-model="formData[f.id]" :placeholder="f.placeholder || (f.fieldType === 'image' ? '请输入图片名称' : '请输入文件名称')" />
            <div v-if="f.fieldTips" class="field-tip">{{ f.fieldTips }}</div>
          </el-form-item>
        </el-form>
      </div>
      <div v-else-if="!loading" class="form-section">
        <div class="section-title"><span class="cur-stage-tag">当前阶段</span>{{ todo && todo.nodeName }}</div>
        <div class="empty-form">该节点无需填写字段</div>
      </div>

      <!-- 指定下一节点处理人（含节点提示） -->
      <div v-if="!isEndNode" class="next-section">
        <div class="section-title">
          指定下一节点处理人 <span class="req" v-if="!isRejecting">*</span>
          <span v-if="todo && todo.nodeTips" class="node-tip-inline"><i class="el-icon-bell" /> {{ todo.nodeTips }}</span>
          <span class="chain-hint" v-if="isRejecting">退回时无需指定</span>
          <span class="chain-hint" v-else>多选，每人一个独立分支</span>
        </div>
        <div v-if="nextHandlers.length > 0" class="handler-list">
          <div v-for="(h, i) in nextHandlers" :key="h.id" class="handler-card">
            <div class="handler-info">
              <div class="handler-avatar">{{ h.realName ? h.realName.charAt(0) : 'U' }}</div>
              <div class="handler-detail">
                <div class="handler-name">{{ h.realName }} <span class="handler-emp">{{ h.empNo }}</span></div>
                <div class="handler-dept">{{ h.deptName || '—' }}</div>
              </div>
            </div>
            <div class="handler-actions">
              <span class="handler-idx">#{{ i + 1 }}</span>
              <button class="action-link text-error" @click="removeHandler(h.id)"><i class="el-icon-close" /> 移除</button>
            </div>
          </div>
        </div>
        <button v-if="nextHandlers.length === 0" class="btn-pick-handler" :disabled="isRejecting" @click="openUserPicker"><i class="el-icon-plus" /> 选择下一节点处理人（可多选）</button>
        <button v-else class="btn-pick-handler btn-add-more" :disabled="isRejecting" @click="openUserPicker"><i class="el-icon-plus" /> 继续添加</button>
      </div>
      <div v-else class="next-section">
        <div class="section-title">完成节点</div>
        <div class="end-tip"><i class="el-icon-success" /> 当前为结束节点，提交后任务将标记为已完成</div>
      </div>
      </div><!-- /pd-left -->

      <!-- 右侧：完整操作历史（通过/退回步骤） -->
      <div class="pd-right">
        <div class="section-title">操作历史 <span class="chain-hint">完整的通过/退回记录</span></div>
        <div v-if="allHistory.length > 0" class="history-timeline">
          <div
            v-for="(h, hi) in allHistory"
            :key="hi"
            class="tl-item"
            :class="h.action === 1 ? 'tl-reject' : 'tl-pass'"
          >
            <div class="tl-dot" />
            <div class="tl-body">
              <div class="tl-node">{{ h.nodeName }}</div>
              <div class="tl-head">
                <span class="tl-badge">{{ h.action === 1 ? '退回' : '通过' }}</span>
                <span class="tl-user"><i class="el-icon-user" /> {{ h.handlerName || '—' }}</span>
              </div>
              <div class="tl-time"><i class="el-icon-time" /> {{ h.handleTime || '—' }}</div>
              <div v-if="h.action === 0 && h.passComment" class="tl-comment">通过意见：{{ h.passComment }}</div>
              <div v-if="h.action === 1 && h.rejectReason" class="tl-reason">退回原因：{{ h.rejectReason }}</div>
            </div>
          </div>
        </div>
        <div v-else class="history-empty">暂无操作记录</div>
      </div>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取消</el-button>
      <!-- 退回按钮（非开始节点、非结束节点） -->
      <el-button v-if="!isStartNode && processedNodes.length > 0" type="warning" :loading="submitting && isRejecting" @click="onRejectClick">
        <i class="el-icon-back" /> 退回
      </el-button>
      <!-- 通过按钮 -->
      <el-button type="primary" :loading="submitting && !isRejecting" @click="onPassClick">
        <i class="el-icon-check" /> {{ isEndNode ? '提交完成' : '通过并流转' }}
      </el-button>
    </div>

    <!-- 多选选人弹窗 -->
    <UserPicker
      :visible="pickerVisible"
      title="选择下一节点处理人（可多选，每人一个独立分支）"
      :exclude-ids="nextHandlers.map(h => h.id)"
      @confirm="onPickUsers"
      @close="pickerVisible = false"
    />

    <!-- 退回目标选择弹窗 -->
    <RejectTargetModal
      :visible="rejectTargetVisible"
      :processed-nodes="processedNodes"
      @confirm="onPickRejectTarget"
      @close="rejectTargetVisible = false"
    />

    <!-- 通过/退回确认弹窗 -->
    <ConfirmActionModal
      :visible="confirmVisible"
      :action="confirmAction"
      :summary="confirmSummary"
      :loading="submitting"
      @confirm="onConfirmSubmit"
      @close="confirmVisible = false"
    />
  </el-dialog>
</template>

<script>
import UserPicker from '@/components/UserPicker'
import RejectTargetModal from './RejectTargetModal.vue'
import ConfirmActionModal from './ConfirmActionModal.vue'

export default {
  name: 'ProcessDialog',
  components: { UserPicker, RejectTargetModal, ConfirmActionModal },
  props: {
    visible: { type: Boolean, default: false },
    todo: { type: Object, default: null },
    detail: { type: Object, default: null },
    submitting: { type: Boolean, default: false }
  },
  data() {
    return {
      loading: false,
      formData: {},
      nextHandlers: [],
      expandedNodeId: null,
      isRejecting: false,
      // 子弹窗
      pickerVisible: false,
      rejectTargetVisible: false,
      confirmVisible: false,
      confirmAction: 'pass',
      confirmSummary: '',
      pendingRejectToNodeId: null,
      pendingRejectReason: null
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.handleClose() }
    },
    currentFields() {
      return this.detail && this.detail.currentNodeFields ? this.detail.currentNodeFields : []
    },
    isStartNode() {
      return this.todo && this.todo.nodeType === 1
    },
    isEndNode() {
      return this.todo && this.todo.nodeType === 3
    },
    /** 任务说明（下发时填写，处理人可见；detail.task 优先，兼容待办项带说明的情况） */
    taskDesc() {
      const d = this.detail && this.detail.task ? this.detail.task.taskDesc : null
      if (d) return d
      return (this.todo && this.todo.taskDesc) || ''
    },
    /** 是否回填了上次表单数据（退回重填场景） */
    isRefill() {
      const cf = this.detail && this.detail.currentFormData
      return cf && Array.isArray(cf) && cf.length > 0
    },
    /** 任务基础信息（模板级字段：配置 + 创建人下发的值，处理人只读可见） */
    templateFieldRows() {
      if (!this.detail) return []
      const fields = this.detail.templateFields || []
      const data = this.detail.templateData || {}
      return fields.map(f => ({
        id: f.id,
        label: f.fieldLabel,
        value: data[f.id] !== undefined && data[f.id] !== null ? String(data[f.id]) : ''
      }))
    },
    /** 当前登录用户ID（用于按处理人过滤流程链/操作历史，避免看到他人的处理记录） */
    currentUserId() {
      const ui = this.$store.getters.userInfo || {}
      return ui.id != null ? ui.id : null
    },
    /** 合并模板完整节点链 + 任务的 task_node（任务绑定，展示全部节点及提交记录，不按登录人过滤） */
    flowChain() {
      if (!this.detail) return []
      const tplNodes = this.detail.templateNodes || []
      const taskNodes = this.detail.taskNodes || []
      const myUserId = this.currentUserId
      const isPass = n => n.submitStatus === 1 && n.action !== 1
      // 按 nodeId 分组：任务的全部节点记录，处理人能看到之前的节点信息和提交记录
      const byNode = {}
      taskNodes.forEach(tn => {
        if (!byNode[tn.nodeId]) byNode[tn.nodeId] = []
        byNode[tn.nodeId].push(tn)
      })
      return tplNodes.map(tpl => {
        const nodes = byNode[tpl.id] || []
        // 退回重做判定：节点存在晚于“最近一次已处理记录”的待办 → 被退回重做，状态为处理中
        const latestHandledId = nodes.filter(n => n.submitStatus === 1).reduce((m, n) => Math.max(m, n.taskNodeId || 0), 0)
        const newerPending = nodes.some(n => n.submitStatus === 0 && (n.taskNodeId || 0) > latestHandledId)
        // 代表记录：退回重做时用待办代表；否则优先“有真实提交”的节点（有 formRecordId），
        // 排除“任一完成即可”自动完成的无表单分支；其次当前登录人的待办；其次最新
        let rep = null
        if (nodes.length > 0) {
          const submitted = nodes.filter(n => n.submitStatus === 1 && n.formRecordId != null)
          const passed = nodes.filter(isPass)
          const minePending = myUserId != null ? nodes.filter(n => n.handlerUserId === myUserId && n.submitStatus === 0) : []
          let pick
          if (newerPending) {
            pick = minePending.length > 0 ? minePending : nodes.filter(n => n.submitStatus === 0)
          } else {
            pick = submitted.length > 0 ? submitted
              : (passed.length > 0 ? passed
                : (minePending.length > 0 ? minePending : nodes))
          }
          rep = pick.reduce((a, b) => ((b.taskNodeId || 0) > (a.taskNodeId || 0) ? b : a))
        }
        let status = 'pending'
        if (rep) {
          if (newerPending) status = 'current'
          else if (rep.submitStatus === 0) status = 'current'
          else if (rep.action === 1) status = 'rejected'
          else status = 'done'
        }
        // 节点待办处理人（同一节点多处理人全部展示）
        const pendingNames = nodes
          .filter(n => n.submitStatus === 0)
          .map(n => n.handlerName)
          .filter(Boolean)
          .filter((v, i, a) => a.indexOf(v) === i)
          .join('、')
        return { nodeId: tpl.id, nodeName: tpl.nodeName, nodeType: tpl.nodeType, sortNum: tpl.sortNum, taskNode: rep, pendingNames, status }
      })
    },
    /** 可退回的目标节点：当前处理人已 done 且 sortNum < 当前节点的节点（按 nodeId 去重） */
    processedNodes() {
      if (!this.detail || !this.todo) return []
      const taskNodes = this.detail.taskNodes || []
      const myUserId = this.currentUserId
      const currentSort = this.todo.currentNodeId ? this.findSortNum(this.todo.currentNodeId) : null
      const map = {}
      taskNodes.forEach(tn => {
        if (myUserId != null && tn.handlerUserId !== myUserId) return
        if (tn.submitStatus !== 1) return // 仅已处理节点
        if (tn.nodeId === this.todo.currentNodeId) return // 排除当前节点
        if (currentSort !== null && tn.sortNum !== null && tn.sortNum >= currentSort) return // 仅前置节点
        if (!map[tn.nodeId] || tn.taskNodeId > map[tn.nodeId].taskNodeId) {
          map[tn.nodeId] = tn
        }
      })
      return Object.values(map).sort((a, b) => (a.sortNum || 0) - (b.sortNum || 0))
    },
    /** 完整操作历史：任务全部已提交的通过/退回记录（任务绑定，按时间正序），展示在弹窗右侧 */
    allHistory() {
      if (!this.detail) return []
      const tns = this.detail.taskNodes || []
      return tns
        // 仅真实提交记录（排除“任一完成即可”自动完成的无表单分支）
        .filter(tn => tn.submitStatus === 1 && tn.formRecordId != null)
        .sort((a, b) => (a.taskNodeId || 0) - (b.taskNodeId || 0))
    }
  },
  watch: {
    visible(val) {
      if (val && this.detail) {
        this.initForm()
      } else if (!val) {
        // 关闭主弹窗时，重置所有子弹窗状态（避免 append-to-body 的确认/选人弹窗残留）
        this.confirmVisible = false
        this.pickerVisible = false
        this.rejectTargetVisible = false
      }
    },
    detail(val) {
      if (val && this.visible) {
        this.initForm()
      }
    }
  },
  methods: {
    statusLabel(s) { return { done: '已通过', current: '处理中', rejected: '已退回', pending: '未到' }[s] || '未到' },
    parseEnum(str) {
      try { return JSON.parse(str) || [] } catch (e) { return [] }
    },
    findSortNum(nodeId) {
      const tplNodes = (this.detail && this.detail.templateNodes) || []
      const t = tplNodes.find(t => t.id === nodeId)
      return t ? t.sortNum : null
    },
    /** 初始化表单数据（优先回填退回时的上次数据） */
    initForm() {
      const fields = this.currentFields
      const data = {}
      // 先建空结构
      fields.forEach(f => {
        if (f.fieldType === 'checkbox') data[f.id] = []
        else data[f.id] = ''
      })
      // 回填上次数据（退回重填）
      const cf = this.detail.currentFormData
      if (Array.isArray(cf) && cf.length > 0) {
        cf.forEach(fd => {
          if (fd.fieldId == null) return
          const field = fields.find(f => f.id === fd.fieldId)
          if (!field) return
          if (field.fieldType === 'checkbox') {
            data[fd.fieldId] = fd.fieldValue ? String(fd.fieldValue).split(',') : []
          } else {
            data[fd.fieldId] = fd.fieldValue
          }
        })
      }
      this.formData = data
      this.nextHandlers = []
      this.expandedNodeId = null
      this.isRejecting = false
      this.pendingRejectToNodeId = null
      this.pendingRejectReason = null
    },
    toggleNodeForm(item) {
      if (item.status !== 'done' && item.status !== 'rejected') return
      this.expandedNodeId = this.expandedNodeId === item.nodeId ? null : item.nodeId
    },
    openUserPicker() {
      this.pickerVisible = true
    },
    onPickUsers(users) {
      const existing = new Set(this.nextHandlers.map(h => h.id))
      users.forEach(u => {
        if (!existing.has(u.id)) this.nextHandlers.push({ ...u })
      })
      this.pickerVisible = false
    },
    removeHandler(id) {
      this.nextHandlers = this.nextHandlers.filter(h => h.id !== id)
    },
    /** 表单必填校验（通过和退回共用） */
    validateForm() {
      for (const f of this.currentFields) {
        if (f.required === 1) {
          const val = this.formData[f.id]
          const empty = (f.fieldType === 'checkbox') ? (!val || val.length === 0) : (val === null || val === undefined || val === '')
          if (empty) {
            this.$message.warning(`字段「${f.fieldLabel}」为必填项`)
            return false
          }
        }
      }
      return true
    },
    /** 点击通过：校验后弹确认 */
    onPassClick() {
      if (!this.isEndNode && this.nextHandlers.length === 0) {
        this.$message.warning('请指定下一节点处理人')
        return
      }
      if (!this.validateForm()) return
      this.isRejecting = false
      this.confirmAction = 'pass'
      const names = this.nextHandlers.map(h => h.realName).join('、')
      this.confirmSummary = this.isEndNode ? '提交后任务将标记为已完成' : `将流转给 ${this.nextHandlers.length} 人：${names}`
      this.confirmVisible = true
    },
    /** 点击退回：选目标节点+原因，再弹确认（退回不校验表单，可不填） */
    onRejectClick() {
      if (this.processedNodes.length === 0) {
        this.$message.warning('没有可退回的节点')
        return
      }
      this.rejectTargetVisible = true
    },
    onPickRejectTarget(payload) {
      const { nodeId, reason } = payload || {}
      this.rejectTargetVisible = false
      this.pendingRejectToNodeId = nodeId
      this.pendingRejectReason = reason
      const target = this.processedNodes.find(n => n.nodeId === nodeId)
      const targetName = target ? target.nodeName : '目标节点'
      this.isRejecting = true
      this.confirmAction = 'reject'
      this.confirmSummary = `将退回到节点「${targetName}」\n退回原因：${reason}\n表单将回填上次数据可修改重交`
      this.confirmVisible = true
    },
    /** 确认提交：组装 payload 并 emit（接收 ConfirmActionModal 的 passComment） */
    onConfirmSubmit(modalPayload) {
      const isReject = this.confirmAction === 'reject'
      const formDataList = this.currentFields.map(f => {
        let val = this.formData[f.id]
        if (f.fieldType === 'checkbox' && Array.isArray(val)) val = val.join(',')
        return {
          fieldId: f.id,
          fieldKey: f.fieldKey,
          fieldValue: val == null ? '' : String(val)
        }
      })
      const payload = {
        taskId: this.todo.taskId,
        taskNodeId: this.todo.taskNodeId,
        formData: formDataList,
        action: isReject ? 'reject' : 'pass'
      }
      if (isReject) {
        payload.rejectToNodeId = this.pendingRejectToNodeId
        payload.rejectReason = this.pendingRejectReason
      } else {
        payload.passComment = (modalPayload && modalPayload.passComment) || ''
        if (!this.isEndNode) {
          payload.nextHandlerIds = this.nextHandlers.map(h => h.id)
        }
      }
      this.$emit('submit', payload)
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
.process-wrap { display: flex; gap: 20px; max-height: 65vh; }
.pd-left { flex: 1.5; min-width: 0; overflow-y: auto; padding-right: 8px; }
.pd-right { flex: 1; min-width: 0; overflow-y: auto; border-left: 1px solid #f0f0f0; padding-left: 16px; }
.info-section { padding-bottom: 16px; margin-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.info-row { display: flex; padding: 6px 0; font-size: 14px; }
.il { width: 90px; color: #757575; flex-shrink: 0; }
.iv { color: #1b1c1c; flex: 1; }
.desc-row { align-items: flex-start;
  .desc-text { line-height: 1.6; white-space: pre-wrap; word-break: break-all; }
}
.info-tpl-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: $primary; margin: 12px 0 4px; padding-top: 10px; border-top: 1px dashed #e4beba; }
.section-title { font-size: 15px; font-weight: 700; color: $primary; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.chain-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 0; }
.req { color: $primary; }
.node-tip-inline { font-size: 13px; color: #fff; background: $primary; padding: 2px 10px; border-radius: 4px; font-weight: 500;
  i { margin-right: 3px; }
}
.refill-tag { font-size: 12px; color: #b7791f; background: rgba(183,121,31,0.1); padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.cur-stage-tag { font-size: 11px; color: #fff; background: $primary; padding: 2px 8px; border-radius: 4px; font-weight: 700; letter-spacing: .5px; }

// 流程链
.chain-section { padding-bottom: 16px; margin-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.chain-track { display: flex; flex-direction: column; gap: 10px; }
.chain-step { border: 1px solid #ebeef5; border-radius: 8px; padding: 12px 14px; background: #fff; transition: all .2s;
  &.st-done { border-color: rgba(38,109,0,0.3); background: rgba(38,109,0,0.03); }
  &.st-current { border-color: $primary; background: #FFF5F5; box-shadow: 0 0 0 2px rgba(197,48,48,0.1); }
  &.st-rejected { border-color: rgba(183,121,31,0.4); background: rgba(183,121,31,0.05); }
  &.st-pending { opacity: 0.55; background: #f7f7f7; }
  &.clickable { cursor: pointer;
    &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
  }
  &.expanded { box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
}
.step-head { display: flex; align-items: center; gap: 10px; }
.step-no { width: 22px; height: 22px; border-radius: 50%; background: #e4beba; color: #5b403d; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.st-current .step-no { background: $primary; color: #fff; }
.st-done .step-no { background: #266d00; color: #fff; }
.st-rejected .step-no { background: #b7791f; color: #fff; }
.step-name { font-size: 14px; font-weight: 600; color: #1b1c1c; flex: 1; }
.step-badge { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; }
.badge-done { background: rgba(38,109,0,0.12); color: #266d00; }
.badge-current { background: rgba(197,48,48,0.12); color: $primary; }
.badge-rejected { background: rgba(183,121,31,0.15); color: #b7791f; }
.badge-pending { background: #e8e8e8; color: #999; }
.step-meta { display: flex; gap: 16px; margin-top: 6px; padding-left: 32px; font-size: 12px; color: #757575;
  i { margin-right: 3px; }
  .meta-pending { color: #bbb; font-style: italic; }
  .meta-reject { color: #b7791f; font-weight: 600; }
  .meta-reason { color: #b7791f; max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .meta-pass { color: #266d00; max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
.step-form { margin-top: 10px; padding: 10px 12px; background: #fff; border-radius: 6px; border: 1px dashed #e4beba; }
.form-row { display: flex; padding: 5px 0; font-size: 13px; border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.fr-label { width: 130px; color: #757575; flex-shrink: 0; }
.fr-value { color: #1b1c1c; flex: 1; word-break: break-all; }
.form-empty { font-size: 12px; color: #bbb; text-align: center; padding: 8px; }

// 表单
.form-section { padding-bottom: 16px; margin-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.process-form { max-width: 620px; }

// 右侧操作历史时间线
.history-timeline { position: relative; padding-left: 18px;
  &::before { content: ''; position: absolute; left: 5px; top: 4px; bottom: 4px; width: 2px; background: #e4beba; }
}
.tl-item { position: relative; padding-bottom: 18px;
  &:last-child { padding-bottom: 0; }
}
.tl-dot { position: absolute; left: -18px; top: 4px; width: 12px; height: 12px; border-radius: 50%; border: 2px solid #fff; box-shadow: 0 0 0 1px rgba(0,0,0,0.1); }
.tl-pass .tl-dot { background: #266d00; }
.tl-reject .tl-dot { background: #b7791f; }
.tl-body { padding: 8px 10px; border-radius: 6px; background: #fafafa; font-size: 12px; }
.tl-node { font-size: 13px; font-weight: 700; color: #1b1c1c; margin-bottom: 4px; }
.tl-head { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.tl-badge { padding: 1px 7px; border-radius: 3px; font-weight: 700; font-size: 11px; color: #fff; flex-shrink: 0; }
.tl-pass .tl-badge { background: #266d00; }
.tl-reject .tl-badge { background: #b7791f; }
.tl-user { color: #414755; i { margin-right: 2px; } }
.tl-time { color: #999; i { margin-right: 2px; } }
.tl-comment { margin-top: 5px; color: #266d00; line-height: 1.5; word-break: break-all; }
.tl-reason { margin-top: 5px; color: #b7791f; line-height: 1.5; word-break: break-all; }
.history-empty { font-size: 13px; color: #bbb; text-align: center; padding: 32px 0; }
.field-tip { font-size: 12px; color: #999; margin-top: 4px; }
.empty-form { font-size: 13px; color: #999; padding: 16px 0; }

// 下一处理人
.next-section { padding-bottom: 8px; }
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
.btn-pick-handler { display: flex; align-items: center; gap: 4px; padding: 12px 20px; background: #fff; border: 1px dashed $primary; border-radius: 8px; cursor: pointer; color: $primary; font-size: 14px; font-weight: 600;
  &:hover { background: #FFF5F5; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.btn-add-more { display: inline-flex; padding: 6px 14px; font-size: 13px; margin-top: 8px; }
.end-tip { display: flex; align-items: center; gap: 8px; padding: 14px 16px; background: rgba(38,109,0,0.08); border-radius: 8px; color: #266d00; font-size: 14px;
  i { font-size: 18px; }
}
.dialog-footer { text-align: right; }
</style>
