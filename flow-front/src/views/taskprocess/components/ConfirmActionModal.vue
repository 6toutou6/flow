<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="560px"
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

    <!-- 下一节点处理人（通过且非结束节点时，在确认弹窗内选择） -->
    <div v-if="action === 'pass' && !endNode" class="handler-area">
      <label class="comment-label">下一节点处理人 <span class="comment-optional">（必选，可多选，每人一个独立分支）</span></label>
      <!-- 创建人配置的「下一步处理人提示」 -->
      <div v-if="nextHandlerTip" class="handler-tip">
        <i class="el-icon-info" />
        <span>{{ nextHandlerTip }}</span>
      </div>
      <div v-if="nextHandlers.length > 0" class="handler-list">
        <div v-for="(h, i) in nextHandlers" :key="h.id" class="handler-chip">
          <span class="hc-avatar">{{ h.realName ? h.realName.charAt(0) : 'U' }}</span>
          <span class="hc-name">{{ h.realName }} <span class="hc-emp">{{ h.empNo }}</span></span>
          <i class="el-icon-close hc-remove" @click="removeHandler(h.id)" />
        </div>
      </div>
      <button class="btn-pick" @click="pickerVisible = true">
        <i class="el-icon-plus" /> {{ nextHandlers.length ? '继续添加' : '选择处理人' }}
      </button>
      <UserPicker
        :visible="pickerVisible"
        title="选择下一节点处理人（可多选，每人一个独立分支）"
        :exclude-ids="nextHandlers.map(h => h.id)"
        @confirm="onPickUsers"
        @close="pickerVisible = false"
      />
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
import UserPicker from '@/components/UserPicker'

export default {
  name: 'ConfirmActionModal',
  components: { UserPicker },
  props: {
    visible: { type: Boolean, default: false },
    action: { type: String, default: 'pass' }, // 'pass' | 'reject'
    summary: { type: String, default: '' },
    loading: { type: Boolean, default: false },
    /** 当前节点是否为结束节点（通过时无需选择下一处理人） */
    endNode: { type: Boolean, default: false },
    /** 创建人配置的「下一步处理人提示」，在选择下一处理人时展示 */
    nextHandlerTip: { type: String, default: '' }
  },
  data() {
    return {
      passComment: '',
      /** 下一节点处理人（通过且非结束节点时在弹窗内选择，每人一个独立分支） */
      nextHandlers: [],
      pickerVisible: false
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
        this.nextHandlers = []
        this.pickerVisible = false
      }
    }
  },
  methods: {
    onPickUsers(users) {
      const ids = this.nextHandlers.map(h => h.id)
      ;(users || []).forEach(u => {
        if (!ids.includes(u.id)) {
          this.nextHandlers.push({ id: u.id, realName: u.realName, empNo: u.empNo })
          ids.push(u.id)
        }
      })
      this.pickerVisible = false
    },
    removeHandler(id) {
      this.nextHandlers = this.nextHandlers.filter(h => h.id !== id)
    },
    handleClose() {
      this.$emit('close')
    },
    handleConfirm() {
      // 通过且非结束节点：下一节点处理人为必选
      if (this.action === 'pass' && !this.endNode && this.nextHandlers.length === 0) {
        this.$message.warning('请选择下一节点处理人')
        return
      }
      this.$emit('confirm', {
        passComment: this.passComment || '',
        nextHandlerIds: this.action === 'pass' ? this.nextHandlers.map(h => h.id) : []
      })
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);

.modal-banner { display: flex; gap: 14px; align-items: center; padding: 16px; border-radius: 3px; margin-bottom: 16px;
  &.banner-reject { background: rgba(180, 83, 9,0.1); border: 1px solid rgba(180, 83, 9,0.3); }
  &.banner-pass { background: rgba(var(--color-primary-rgb),0.07); border: 1px solid rgba(var(--color-primary-rgb),0.22); }
}
.banner-icon { width: 44px; height: 44px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; flex-shrink: 0;
  .banner-reject & { background: #B45309; }
  .banner-pass & { background: $primary; }
}
.banner-text { flex: 1; }
.banner-title { font-size: 17px; font-weight: 700; color: #1b1c1c; }
.banner-sub { font-size: 12px; color: #757575; margin-top: 3px; }

.confirm-card { border-radius: 3px; padding: 12px 16px; margin-bottom: 12px;
  &.card-reject { background: #F0F4FF; border: 1px dashed rgba(180, 83, 9,0.45); }
  &.card-pass { background: var(--color-primary-light); border: 1px dashed rgba(var(--color-primary-rgb),0.4); }
}
.card-item { display: flex; gap: 9px; align-items: flex-start; padding: 5px 0; line-height: 1.6;
  & + .card-item { border-top: 1px dashed rgba(0,0,0,0.06); }
}
.card-dot { width: 6px; height: 6px; border-radius: 50%; margin-top: 9px; flex-shrink: 0;
  .card-reject & { background: #B45309; }
  .card-pass & { background: $primary; }
}
.card-text { font-size: 14px; color: #414755; word-break: break-all; }

.confirm-tip { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #999; padding: 0 4px;
  i { color: #B45309; }
}

.comment-area { margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0; }
.comment-label { display: block; font-size: 13px; font-weight: 600; color: #414755; margin-bottom: 8px; }
.comment-optional { font-size: 12px; color: #999; font-weight: 400; }

// 下一节点处理人选择区（通过且非结束节点）
.handler-area { margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0; }
.handler-tip { display: flex; align-items: flex-start; gap: 6px; background: var(--color-primary-light); border: 1px dashed rgba(var(--color-primary-rgb),0.4); border-radius: 2px; padding: 8px 10px; font-size: 12px; color: var(--color-primary-hover); line-height: 1.6; margin-bottom: 10px;
  i { color: $primary; margin-top: 2px; flex-shrink: 0; }
}
.handler-list { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; }
.handler-chip { display: inline-flex; align-items: center; gap: 6px; padding: 3px 8px 3px 4px; background: #f7f7f9; border: 1px solid #e8e8e8; border-radius: 4px; font-size: 12px; }
.hc-avatar { width: 22px; height: 22px; border-radius: 50%; background: $primary; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.hc-name { color: #414755; font-weight: 600; }
.hc-emp { color: #999; font-weight: 400; margin-left: 2px; }
.hc-remove { cursor: pointer; color: #bbb; font-size: 13px;
  &:hover { color: $primary; }
}
.btn-pick { width: 100%; height: 34px; border: 1px dashed rgba(var(--color-primary-rgb),0.4); border-radius: 2px; background: var(--color-primary-light); color: $primary; cursor: pointer; font-size: 13px; display: flex; align-items: center; justify-content: center; gap: 4px;
  &:hover { border-color: $primary; background: rgba(var(--color-primary-rgb),0.08); }
}
.dialog-footer { text-align: right; }
</style>
