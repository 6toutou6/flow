<template>
  <el-dialog title="处理任务" :visible.sync="dialogVisible" width="1300px" :close-on-click-modal="false" append-to-body @close="handleClose">
    <div v-loading="loading" class="process-wrap">
      <!-- 左侧：表单与操作区 -->
      <div class="pd-left">
      <!-- 任务信息 -->
      <div class="info-section">
        <div class="section-title"><i class="el-icon-document" /> 任务信息</div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">任务名称</span>
            <span class="info-value">{{ todo && todo.taskName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">所属模板</span>
            <span class="info-value">{{ todo && todo.templateName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">当前节点</span>
            <span class="info-value">{{ todo && todo.nodeName }}</span>
          </div>
          <div v-if="taskDesc" class="info-item info-item-full">
            <span class="info-label">任务说明</span>
            <span class="info-value">{{ taskDesc }}</span>
          </div>
        </div>
        <!-- 任务基础信息（模板级字段，创建人下发时赋值，处理人可见） -->
        <template v-if="templateFieldRows.length > 0">
          <div class="tpl-header"><i class="el-icon-collection" /> 任务基础信息</div>
          <div class="tpl-grid">
            <div v-for="r in templateFieldRows" :key="r.id" class="tpl-item">
              <span class="tpl-label">
                {{ r.label }}
                <el-tooltip v-if="r.roleTip" :content="r.roleTip" placement="top">
                  <span class="role-hint-icon" :class="r.role === 2 ? 'role-handler' : 'role-creator'">
                    <i :class="r.role === 2 ? 'el-icon-user' : 'el-icon-s-custom'" />
                  </span>
                </el-tooltip>
              </span>
              <span class="tpl-value">{{ r.value || '—' }}</span>
            </div>
          </div>
        </template>
      </div>

      <!-- 完整流程链（横向展示：已通过绿 / 处理中红 / 退回黄 / 未到灰，点击已处理节点展开详情） -->
      <div v-if="flowChain.length > 0" class="chain-section">
        <div class="section-title">流程链 <span class="chain-hint">横向展示全流程，点击已处理/已退回节点可查看填写内容</span></div>
        <div class="chain-track-h">
          <template v-for="(item, idx) in flowChain">
            <div
              :key="idx"
              class="chain-chip"
              :class="['chip-' + item.status, { clickable: item.status === 'done' || item.status === 'rejected', expanded: expandedNodeId === item.nodeId }]"
              :title="`${idx + 1}. ${item.nodeName}（${statusLabel(item.status)}）`"
              @click="toggleNodeForm(item)"
            >
              <span class="cc-no">{{ idx + 1 }}</span>
              <span class="cc-name">{{ item.nodeName }}</span>
              <span v-if="item.status === 'current'" class="cc-now">当前</span>
            </div>
            <span v-if="idx < flowChain.length - 1" :key="'arr-' + idx" class="chain-arrow"><i class="el-icon-right" /></span>
          </template>
        </div>
        <!-- 点击节点展开的详情 -->
        <div v-if="expandedItem" class="step-detail" @click.stop>
          <div class="sd-head">
            <span class="sd-no">{{ expandedIdx + 1 }}</span>
            <span class="sd-name">{{ expandedItem.nodeName }}</span>
            <span class="step-badge" :class="'badge-' + expandedItem.status">{{ statusLabel(expandedItem.status) }}</span>
            <template v-if="expandedItem.taskNode && expandedItem.taskNode.submitStatus === 1">
              <span class="sd-meta"><i class="el-icon-user" /> {{ expandedItem.taskNode.handlerName || '—' }}</span>
              <span class="sd-meta"><i class="el-icon-time" /> {{ expandedItem.taskNode.handleTime || '—' }}</span>
              <span v-if="expandedItem.taskNode.action === 1 && expandedItem.taskNode.rejectReason" class="meta-reason" :title="expandedItem.taskNode.rejectReason">原因：{{ expandedItem.taskNode.rejectReason }}</span>
              <span v-if="expandedItem.taskNode.action === 0 && expandedItem.taskNode.passComment" class="meta-pass" :title="expandedItem.taskNode.passComment">意见：{{ expandedItem.taskNode.passComment }}</span>
            </template>
            <template v-else-if="expandedItem.taskNode && expandedItem.taskNode.submitStatus === 0">
              <span class="sd-meta"><i class="el-icon-user" /> {{ expandedItem.pendingNames || expandedItem.taskNode.handlerName || '待处理' }}</span>
              <span v-if="expandedItem.taskNode.rejectReason" class="meta-reason"><i class="el-icon-warning-outline" /> 退回建议：{{ expandedItem.taskNode.rejectReason }}</span>
            </template>
          </div>
          <template v-if="expandedItem.status === 'rejected'">
            <div class="form-empty">该节点已退回，无需显示表单</div>
          </template>
          <template v-else>
            <div v-if="expandedItem.taskNode && expandedItem.taskNode.formDataList && expandedItem.taskNode.formDataList.length > 0">
              <div class="sd-sub-title">表单数据（最近一次提交）</div>
              <div v-for="(fd, fi) in expandedItem.taskNode.formDataList" :key="fi" class="form-row">
                <span class="fr-label">{{ fd.fieldLabel }}</span>
                <span class="fr-value">{{ fd.fieldValue || '—' }}</span>
              </div>
            </div>
            <div v-else class="form-empty">该节点未填写表单数据</div>
          </template>
          <!-- 该节点处理人填写的任务基础字段（fieldRole=2） -->
          <div v-if="expandedItem.taskNode && expandedItem.taskNode.baseDataList && expandedItem.taskNode.baseDataList.length > 0" class="bd-block">
            <div class="bd-title">
              <i class="el-icon-collection" /> 任务基础信息
              <el-tooltip :content="`在「${expandedItem.nodeName}」节点由处理人填写`" placement="top">
                <span class="role-hint-icon role-handler"><i class="el-icon-user" /></span>
              </el-tooltip>
            </div>
            <div v-for="(bd, bi) in expandedItem.taskNode.baseDataList" :key="bi" class="form-row">
              <span class="fr-label">{{ bd.fieldLabel }}</span>
              <span class="fr-value">{{ bd.fieldValue || '—' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 处理人填写的任务基础信息（fieldRole=2，不依附节点，随本节点提交；置于流程字段前填写） -->
      <div v-if="!isReadonly && handlerBaseFields.length > 0" class="form-section bd-form-section">
        <div class="section-title">
          <i class="el-icon-collection" /> 任务基础信息
          <el-tooltip :content="`在「${todo && todo.nodeName}」节点由处理人填写，仅此处填写，随提交保存`" placement="top">
            <span class="role-hint-icon role-handler"><i class="el-icon-user" /></span>
          </el-tooltip>
          <span class="chain-hint">绑定当前节点「{{ todo && todo.nodeName }}」，仅此处填写，随提交保存</span>
        </div>
        <el-form ref="baseForm" :model="handlerBaseForm" label-position="top" class="process-form">
          <el-form-item
            v-for="f in handlerBaseFields"
            :key="f.id"
            :label="f.fieldLabel"
            :required="f.required === 1"
          >
            <el-input v-if="f.fieldType === 'text'" v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" />
            <el-input v-else-if="f.fieldType === 'textarea'" v-model="handlerBaseForm[f.id]" type="textarea" :rows="3" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" />
            <el-input-number v-else-if="f.fieldType === 'number'" v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || '请输入'" controls-position="right" style="width: 100%" />
            <el-date-picker v-else-if="f.fieldType === 'date'" v-model="handlerBaseForm[f.id]" type="date" placeholder="选择日期" value-format="yyyy-MM-dd" style="width: 100%" />
            <el-radio-group v-else-if="f.fieldType === 'radio'" v-model="handlerBaseForm[f.id]">
              <el-radio v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
            </el-radio-group>
            <el-checkbox-group v-else-if="f.fieldType === 'checkbox'" v-model="handlerBaseForm[f.id]">
              <el-checkbox v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-checkbox>
            </el-checkbox-group>
            <el-input v-else v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || (f.fieldType === 'image' ? '请输入图片名称' : '请输入文件名称')" />
            <div v-if="f.fieldTips" class="field-tip">{{ f.fieldTips }}</div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 动态表单（流程节点字段） -->
      <div v-if="!isReadonly && currentFields.length > 0" class="form-section">
        <div class="section-title">
          <span class="cur-stage-tag">当前阶段</span>{{ todo && todo.nodeName }}
          <span v-if="isRefill" class="refill-tag"><i class="el-icon-refresh-left" /> 已回填上次数据，可修改后重新提交</span>
        </div>
        <el-form ref="processForm" :model="formData" label-position="top" class="process-form">
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

      <div v-else-if="!isReadonly && handlerBaseFields.length === 0 && !loading" class="form-section">
        <div class="section-title"><span class="cur-stage-tag">当前阶段</span>{{ todo && todo.nodeName }}</div>
        <div class="empty-form">该节点无需填写字段</div>
      </div>

      <!-- 指定下一节点处理人（含节点提示） -->
      <div v-if="!isReadonly && !isEndNode" class="next-section">
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
      <!-- 只读模式：已处理完成，仅查看 -->
      <span v-if="isReadonly" class="readonly-tip"><i class="el-icon-finished" /> 您已处理完成该任务，可查看上方流程链与操作记录</span>
      <template v-else>
        <!-- 退回按钮（非开始节点、非结束节点） -->
        <el-button v-if="!isStartNode && processedNodes.length > 0" type="warning" :loading="submitting && isRejecting" @click="onRejectClick">
          <i class="el-icon-back" /> 退回
        </el-button>
        <!-- 通过按钮 -->
        <el-button type="primary" :loading="submitting && !isRejecting" @click="onPassClick">
          <i class="el-icon-check" /> {{ isEndNode ? '提交完成' : '通过并流转' }}
        </el-button>
      </template>
      <el-button @click="handleClose">{{ isReadonly ? '关闭' : '取消' }}</el-button>
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
      /** 处理人填写的任务基础字段值（fieldRole=2） */
      handlerBaseForm: {},
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
    /** 处理人填写的任务基础字段配置（fieldRole=2，不依附节点） */
    handlerBaseFields() {
      return (this.detail && this.detail.handlerBaseFields) || []
    },
    isStartNode() {
      return this.todo && this.todo.nodeType === 1
    },
    isEndNode() {
      return this.todo && this.todo.nodeType === 3
    },
    /** 只读模式：该用户已处理完成（todoStatus=1），仅查看详情 */
    isReadonly() {
      return this.todo && this.todo.todoStatus === 1
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
    /** 任务基础信息（模板级字段：创建人下发的值 + 处理人在各节点填写的汇总值，处理人只读可见） */
    templateFieldRows() {
      if (!this.detail) return []
      const fields = this.detail.templateFields || []
      const data = this.detail.templateData || {}
      const hbData = this.detail.handlerBaseData || {}
      const tplNodes = this.detail.templateNodes || []
      return fields.map(f => {
        // 处理人填写字段（fieldRole=2）值来自各节点提交汇总；创建人填写字段来自下发值
        const map = f.fieldRole === 2 ? hbData : data
        const node = f.fieldRole === 2 ? tplNodes.find(n => n.id === f.bindNodeId) : null
        return {
          id: f.id,
          label: f.fieldLabel,
          role: f.fieldRole === 2 ? 2 : 1,
          roleTip: f.fieldRole === 2
            ? (node ? `在「${node.nodeName}」节点由处理人填写` : '由处理人填写')
            : '创建人填写',
          value: map[f.id] !== undefined && map[f.id] !== null ? String(map[f.id]) : ''
        }
      })
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
    /** 当前展开的流程链节点详情（点击已处理/已退回节点展开） */
    expandedItem() {
      if (this.expandedNodeId == null) return null
      return this.flowChain.find(item => item.nodeId === this.expandedNodeId) || null
    },
    /** 展开节点在流程链中的序号（从1开始） */
    expandedIdx() {
      if (!this.expandedItem) return 0
      return this.flowChain.indexOf(this.expandedItem)
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
      // 处理人填写的任务基础字段：初始化 + 回填本人最近一次提交（退回重做）
      const base = {}
      this.handlerBaseFields.forEach(f => {
        if (f.fieldType === 'checkbox') base[f.id] = []
        else base[f.id] = ''
      })
      const cb = this.detail.currentBaseData || {}
      Object.keys(cb).forEach(fid => {
        const field = this.handlerBaseFields.find(f => f.id === Number(fid) || f.id === fid)
        if (!field) return
        if (field.fieldType === 'checkbox') {
          base[field.id] = cb[fid] ? String(cb[fid]).split(',') : []
        } else {
          base[field.id] = cb[fid]
        }
      })
      this.handlerBaseForm = base
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
    /** 处理人填写任务基础字段必填校验（退回不校验） */
    validateBaseForm() {
      for (const f of this.handlerBaseFields) {
        if (f.required === 1) {
          const val = this.handlerBaseForm[f.id]
          const empty = (f.fieldType === 'checkbox') ? (!val || val.length === 0) : (val === null || val === undefined || val === '')
          if (empty) {
            this.$message.warning(`任务基础字段「${f.fieldLabel}」为必填项`)
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
      if (!this.validateBaseForm()) return
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
      // 处理人填写的任务基础字段（fieldRole=2）
      const baseData = {}
      this.handlerBaseFields.forEach(f => {
        let val = this.handlerBaseForm[f.id]
        if (val === undefined || val === null || val === '') return
        if (f.fieldType === 'checkbox' && Array.isArray(val)) val = val.join(',')
        baseData[f.id] = String(val)
      })
      const payload = {
        taskId: this.todo.taskId,
        taskNodeId: this.todo.taskNodeId,
        formData: formDataList,
        baseData,
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
.process-wrap { display: flex; gap: 20px; max-height: 68vh; }
.pd-left { flex: 1.5; min-width: 0; overflow-y: auto; padding-right: 6px; display: flex; flex-direction: column; gap: 14px; }
.pd-right { flex: 1; min-width: 0; overflow-y: auto; background: #fff; border: 1px solid $border; border-radius: 10px; padding: 20px; }
.info-section, .chain-section, .form-section, .next-section { background: #fff; border: 1px solid $border; border-radius: 10px; padding: 16px 18px; }
.info-section { padding-bottom: 16px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 32px; }
.info-item { display: flex; flex-direction: column; gap: 6px; min-width: 0;
  &.info-item-full { grid-column: 1 / -1; }
}
.info-label { font-size: 12px; color: #999; }
.info-value { font-size: 14px; color: #1b1c1c; font-weight: 500; word-break: break-all; line-height: 1.5; }
.tpl-header { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: $primary; margin: 18px 0 10px; padding-top: 14px; border-top: 1px dashed $border; }
.tpl-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 32px; }
.tpl-item { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.tpl-label { font-size: 12px; color: #999; display: inline-flex; align-items: center; gap: 4px; }
.tpl-value { font-size: 14px; color: #5b403d; font-weight: 500; word-break: break-all; line-height: 1.5; }
.section-title { font-size: 15px; font-weight: 700; color: $primary; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.chain-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 0; }
.req { color: $primary; }
.node-tip-inline { font-size: 13px; color: #fff; background: $primary; padding: 2px 10px; border-radius: 4px; font-weight: 500;
  i { margin-right: 3px; }
}
.refill-tag { font-size: 12px; color: #b7791f; background: rgba(183,121,31,0.1); padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.cur-stage-tag { font-size: 11px; color: #fff; background: $primary; padding: 2px 8px; border-radius: 4px; font-weight: 700; letter-spacing: .5px; }

// 流程链（横向展示）
.chain-track-h { display: flex; align-items: center; gap: 6px; overflow-x: auto; white-space: nowrap; padding: 4px 0 6px;
  &::-webkit-scrollbar { height: 4px; }
  &::-webkit-scrollbar-thumb { background: #ddd; border-radius: 2px; }
}
.chain-chip { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 6px; font-size: 13px; font-weight: 600; flex-shrink: 0; line-height: 1.5; cursor: default; border: 1px solid transparent;
  &.chip-done { background: #266d00; color: #fff; }
  &.chip-current { background: rgba(197,48,48,0.14); color: $primary; border-color: rgba(197,48,48,0.4); }
  &.chip-rejected { background: rgba(183,121,31,0.16); color: #b7791f; border-color: rgba(183,121,31,0.45); }
  &.chip-pending { background: #f0f0f0; color: #aaa; }
  &.clickable { cursor: pointer;
    &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.12); }
  }
  &.expanded { box-shadow: 0 0 0 2px rgba(197,48,48,0.25); }
}
.cc-no { width: 20px; height: 20px; border-radius: 50%; background: rgba(0,0,0,0.12); display: inline-flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.chip-current .cc-no { background: rgba(197,48,48,0.2); }
.cc-now { font-size: 11px; background: $primary; color: #fff; padding: 0 6px; border-radius: 3px; line-height: 1.6; }
.chain-arrow { color: #ccc; font-size: 14px; flex-shrink: 0; }
.step-badge { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; }
.badge-done { background: rgba(38,109,0,0.12); color: #266d00; }
.badge-current { background: rgba(197,48,48,0.12); color: $primary; }
.badge-rejected { background: rgba(183,121,31,0.15); color: #b7791f; }
.badge-pending { background: #e8e8e8; color: #999; }
.meta-reason { color: #b7791f; max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta-pass { color: #266d00; max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
// 展开的节点详情
.step-detail { margin-top: 12px; padding: 14px 16px; background: #fff; border: 1px dashed $border; border-radius: 8px;
  .sd-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }
  .sd-no { width: 22px; height: 22px; border-radius: 50%; background: #e4beba; color: #5b403d; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
  .sd-name { font-size: 14px; font-weight: 700; color: #1b1c1c; }
  .sd-meta { font-size: 12px; color: #757575; display: inline-flex; align-items: center; gap: 3px; i { margin-right: 2px; } }
  .sd-sub-title { font-size: 12px; font-weight: 700; color: #414755; margin: 6px 0 4px; }
}
.bd-block { margin-top: 10px; padding-top: 8px; border-top: 1px dashed #e4beba; }
.bd-title { font-size: 13px; font-weight: 700; color: #b7791f; margin-bottom: 2px; display: flex; align-items: center; gap: 4px; }
.role-hint-icon { width: 18px; height: 18px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; cursor: help; font-size: 12px;
  &.role-creator { background: rgba(38,109,0,0.1); color: #266d00; }
  &.role-handler { background: rgba(183,121,31,0.12); color: #b7791f; }
}
.bd-form-section { background: #FFFBF5; }
.form-row { display: flex; padding: 5px 0; font-size: 13px; border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.fr-label { width: 130px; color: #757575; flex-shrink: 0; }
.fr-value { color: #1b1c1c; flex: 1; word-break: break-all; }
.form-empty { font-size: 12px; color: #bbb; text-align: center; padding: 8px; }

// 表单（el-form-item 为子组件内部元素，需 ::v-deep 深度选择器才能命中）
.process-form { max-width: 640px;
  ::v-deep .el-form-item { margin-bottom: 16px; }
  ::v-deep .el-form-item__label { line-height: 1.2; padding-bottom: 8px; margin-bottom: 0; font-weight: 600; color: #414755; }
  ::v-deep .el-input, ::v-deep .el-textarea, ::v-deep .el-select, ::v-deep .el-date-editor { width: 100%; }
  ::v-deep .el-input-number { width: 100%; }
}

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
.readonly-tip { display: inline-flex; align-items: center; gap: 5px; margin-right: 12px; font-size: 13px; color: #266d00; background: rgba(38,109,0,0.08); padding: 6px 12px; border-radius: 6px; }
</style>
