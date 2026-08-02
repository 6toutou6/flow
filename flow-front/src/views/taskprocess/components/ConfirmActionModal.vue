<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="480px"
    :close-on-click-modal="false"
    append-to-body
    @close="handleClose"
  >
    <!-- 顶部横幅 -->
    <div class="modal-banner" :class="action === 'reject' ? 'banner-reject' : 'banner-pass'">
      <div class="banner-icon"><i :class="action === 'reject' ? 'el-icon-warning-outline' : 'el-icon-circle-check'" /></div>
      <div class="banner-text">
        <div class="banner-title">{{ title }}</div>
        <div class="banner-sub">{{ action === 'reject' ? '将流程退回到指定节点，需确认以下信息' : '提交后流转至下一节点，请确认以下信息' }}</div>
      </div>
    </div>

    <!-- 信息卡片（按行展示，退回重点高亮） -->
    <div class="confirm-card" :class="action === 'reject' ? 'card-reject' : 'card-pass'">
      <div v-for="(line, li) in summaryLines" :key="li" class="card-item">
        <span class="card-dot" />
        <span class="card-text">{{ line }}</span>
      </div>
      <div v-if="summaryLines.length === 0" class="card-item">
        <span class="card-dot" />
        <span class="card-text">无额外信息</span>
      </div>
    </div>

    <!-- 提示 -->
    <div class="confirm-tip">
      <i class="el-icon-info" />
      <span>{{ action === 'reject' ? '退回后目标节点表单将回填上次数据，可修改后重新提交' : '请确认信息无误，提交后不可撤销' }}</span>
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
    },
    /** 按行拆分 summary，逐行展示 */
    summaryLines() {
      return (this.summary || '').split('\n').filter(l => l && l.trim())
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

.modal-banner { display: flex; gap: 14px; align-items: center; padding: 16px; border-radius: 10px; margin-bottom: 16px;
  &.banner-reject { background: rgba(183,121,31,0.1); border: 1px solid rgba(183,121,31,0.3); }
  &.banner-pass { background: rgba(197,48,48,0.07); border: 1px solid rgba(197,48,48,0.22); }
}
.banner-icon { width: 44px; height: 44px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; flex-shrink: 0;
  .banner-reject & { background: #b7791f; }
  .banner-pass & { background: $primary; }
}
.banner-text { flex: 1; }
.banner-title { font-size: 17px; font-weight: 700; color: #1b1c1c; }
.banner-sub { font-size: 12px; color: #757575; margin-top: 3px; }

.confirm-card { border-radius: 10px; padding: 12px 16px; margin-bottom: 12px;
  &.card-reject { background: #FFF8EF; border: 1px dashed #e0b070; }
  &.card-pass { background: #FFF5F5; border: 1px dashed #e4beba; }
}
.card-item { display: flex; gap: 9px; align-items: flex-start; padding: 5px 0; line-height: 1.6;
  & + .card-item { border-top: 1px dashed rgba(0,0,0,0.06); }
}
.card-dot { width: 6px; height: 6px; border-radius: 50%; margin-top: 9px; flex-shrink: 0;
  .card-reject & { background: #b7791f; }
  .card-pass & { background: $primary; }
}
.card-text { font-size: 14px; color: #414755; word-break: break-all; }

.confirm-tip { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #999; padding: 0 4px;
  i { color: #b7791f; }
}

.comment-area { margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0; }
.comment-label { display: block; font-size: 13px; font-weight: 600; color: #414755; margin-bottom: 8px; }
.comment-optional { font-size: 12px; color: #999; font-weight: 400; }
.dialog-footer { text-align: right; }
</style>
