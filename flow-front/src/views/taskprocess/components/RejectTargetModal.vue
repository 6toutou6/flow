<template>
  <el-dialog title="选择退回目标节点" :visible.sync="dialogVisible" width="560px" :close-on-click-modal="false" append-to-body @close="handleClose">
    <div class="reject-hint">请选择要退回到的节点（仅可选择已到达过的前置节点）：</div>
    <div class="target-list">
      <div
        v-for="n in processedNodes"
        :key="n.nodeId"
        class="target-item"
        :class="{ active: selectedNodeId === n.nodeId }"
        @click="selectedNodeId = n.nodeId"
      >
        <div class="target-radio">
          <i :class="selectedNodeId === n.nodeId ? 'el-icon-circle-check' : 'el-icon-circle-check-empty'" />
        </div>
        <div class="target-info">
          <div class="target-name">
            <span class="node-type-badge" :class="badgeClass(n.nodeType)">{{ nodeTypeText(n.nodeType) }}</span>
            {{ n.nodeName }}
          </div>
          <div class="target-meta">
            <span class="target-handler">
              <i class="el-icon-user" />
              <span class="actual-handler">{{ n.handlerName || '—' }}</span>
              <template v-if="n.handlerCount > 1">、{{ n.allHandlerNames.filter(h => h !== n.handlerName).join('、') }}</template>
            </span>
            <span><i class="el-icon-time" /> {{ n.handleTime || '—' }}</span>
          </div>
        </div>
      </div>
      <div v-if="processedNodes.length === 0" class="empty-state">
        <i class="el-icon-warning-outline" />
        <p>没有可退回的节点</p>
      </div>
    </div>
    <!-- 退回原因（必填） -->
    <div class="reason-section">
      <label class="reason-label"><span class="req">*</span> 退回原因</label>
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="请填写退回原因，将记录并展示给相关人员"
      />
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="warning" :disabled="!selectedNodeId" @click="handleConfirm">确认退回</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'RejectTargetModal',
  props: {
    visible: { type: Boolean, default: false },
    /** 已处理过的节点列表（不含当前节点），每项含 { nodeId, nodeName, nodeType, handlerName, handlerUserId, handleTime, sortNum, allHandlerNames, handlerCount } */
    processedNodes: { type: Array, default: () => [] }
  },
  data() {
    return {
      selectedNodeId: null,
      rejectReason: ''
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.$emit('close') }
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.selectedNodeId = null
        this.rejectReason = ''
      }
    }
  },
  methods: {
    nodeTypeText(t) { return { 1: '开始', 2: '中间', 3: '结束' }[t] || '中间' },
    badgeClass(t) { return { 1: 'badge-start', 3: 'badge-end' }[t] || 'badge-mid' },
    handleConfirm() {
      if (!this.selectedNodeId) {
        this.$message.warning('请选择退回目标节点')
        return
      }
      if (!this.rejectReason || !this.rejectReason.trim()) {
        this.$message.warning('请填写退回原因')
        return
      }
      this.$emit('confirm', { nodeId: this.selectedNodeId, reason: this.rejectReason.trim() })
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.reject-hint { font-size: 13px; color: #757575; margin-bottom: 12px; }
.target-list { display: flex; flex-direction: column; gap: 8px; max-height: 360px; overflow-y: auto; }
.target-item { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid $border; border-radius: 3px; cursor: pointer; transition: all .2s;
  &:hover { border-color: $primary; background: var(--color-primary-light); }
  &.active { border-color: $primary; background: var(--color-primary-light); box-shadow: 0 0 0 1px $primary; }
}
.target-radio { font-size: 22px; color: $primary; flex-shrink: 0; }
.target-info { flex: 1; }
.target-name { font-size: 14px; font-weight: 600; color: #1b1c1c; display: flex; align-items: center; gap: 6px; }
.target-meta { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 4px; font-size: 12px; color: #757575; align-items: center;
  i { margin-right: 3px; }
}
.target-handler { display: inline-flex; align-items: center; gap: 3px; }
// 实际处理人（绿色高亮，与 FlowChain 一致）
.actual-handler { display: inline-block; padding: 1px 8px; border-radius: 4px; font-weight: 600; color: #fff; background: #15803D; font-size: 12px; line-height: 1.7; }
.node-type-badge { padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 600; }
.badge-start { background: rgba(21, 128, 61,0.1); color: #15803D; }
.badge-mid { background: #f0f3ff; color: #545f72; }
.badge-end { background: rgba(var(--color-primary-rgb),0.1); color: $primary; }
.empty-state { text-align: center; padding: 40px 20px; color: #bbb;
  i { font-size: 36px; display: block; margin-bottom: 8px; }
  p { font-size: 13px; margin: 0; }
}
.dialog-footer { text-align: right; }
.reason-section { margin-top: 16px; }
.reason-label { display: block; font-size: 13px; font-weight: 600; color: #414755; margin-bottom: 8px; }
.req { color: $primary; margin-right: 2px; }
</style>
