<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="460px"
    :close-on-click-modal="false"
    append-to-body
    @close="handleClose"
  >
    <div class="confirm-body">
      <div class="confirm-icon" :class="action === 'reject' ? 'icon-reject' : 'icon-pass'">
        <i :class="action === 'reject' ? 'el-icon-back' : 'el-icon-check'" />
      </div>
      <div class="confirm-text">
        <p class="confirm-title">{{ title }}</p>
        <p v-if="summary" class="confirm-summary">{{ summary }}</p>
        <p class="confirm-tip">{{ action === 'reject' ? '退回后目标节点表单将回填上次数据，可修改后重新提交' : '提交后将流转至下一节点，请确认信息无误' }}</p>
      </div>
    </div>
    <!-- 通过意见（仅通过时显示，非必填） -->
    <div v-if="action === 'pass'" class="comment-area">
      <label class="comment-label">通过意见 <span class="comment-optional">（选填）</span></label>
      <el-input
        v-model="passComment"
        type="textarea"
        :rows="3"
        placeholder="可填写审批意见或备注，选填"
        maxlength="500"
        show-word-limit
      />
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button :type="action === 'reject' ? 'warning' : 'primary'" :loading="loading" @click="handleConfirm">
        确认{{ action === 'reject' ? '退回' : '通过' }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ConfirmActionModal',
  props: {
    visible: { type: Boolean, default: false },
    action: { type: String, default: 'pass' }, // 'pass' | 'reject'
    summary: { type: String, default: '' },
    loading: { type: Boolean, default: false }
  },
  data() {
    return {
      passComment: ''
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.$emit('close') }
    },
    title() {
      return this.action === 'reject' ? '确认退回' : '确认通过'
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.passComment = ''
      }
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    },
    handleConfirm() {
      this.$emit('confirm', { passComment: this.passComment || '' })
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
.confirm-body { display: flex; gap: 16px; align-items: flex-start; padding: 8px 4px; }
.confirm-icon { width: 44px; height: 44px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; flex-shrink: 0;
  &.icon-pass { background: $primary; }
  &.icon-reject { background: #b7791f; }
}
.confirm-text { flex: 1; }
.confirm-title { font-size: 16px; font-weight: 700; color: #1b1c1c; margin: 0 0 6px; }
.confirm-summary { font-size: 14px; color: #414755; margin: 0 0 6px; line-height: 1.5; white-space: pre-line; }
.confirm-tip { font-size: 12px; color: #999; margin: 0; line-height: 1.5; }
.comment-area { margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0; }
.comment-label { display: block; font-size: 13px; font-weight: 600; color: #414755; margin-bottom: 8px; }
.comment-optional { font-size: 12px; color: #999; font-weight: 400; }
.dialog-footer { text-align: right; }
</style>
