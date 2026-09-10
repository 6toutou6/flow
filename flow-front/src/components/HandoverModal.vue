<template>
  <el-dialog
    title="交接任务"
    :visible.sync="dialogVisible"
    width="560px"
    append-to-body
    :close-on-click-modal="false"
    @close="reset"
  >
    <!-- 无可交接内容：入口常显，点开后如实提示 -->
    <div v-if="emptyScope" class="ho-tip ho-tip-empty">
      <i class="el-icon-info" />
      您在该任务下<b>暂无可交接的内容</b>——既没有归属您的期次，也没有您参与过的节点记录。
      <div class="ho-tip-sub">无需发起交接。若您确实有待办或归属期次，请刷新后重试。</div>
    </div>

    <template v-else>
      <div class="ho-tip">
        <i class="el-icon-warning-outline" />
        将把该任务下<b>您名下的 {{ scopeText }}</b>交接给接手人，提交后需<b>创建人同部门的部门管理员</b>审批通过方可生效；是否同步任务配置名单由审批人确认。
        <div class="ho-tip-sub">归属我的期次会整期交出（含该期下我办过的节点）；我在其他期次下参与过的节点（含已提交的历史节点）仅换处理人，不影响该期次归属——您在这条任务里的经办痕迹将转给接手人查看。</div>
      </div>
      <div class="ho-row">
        <span class="ho-label">交接任务</span>
        <div class="ho-static">{{ dispatchName || '—' }}</div>
      </div>
      <div class="ho-row">
        <span class="ho-label"><span class="req">*</span> 接手人</span>
        <div class="ho-picker">
          <span v-if="recipient" class="ho-chosen">{{ recipient.userName }}<em>{{ recipient.yyytId }}</em></span>
          <span v-else class="ho-placeholder">请选择接手人</span>
          <button class="ho-pick-btn" @click="pickerVisible = true">
            <i class="el-icon-user" /> {{ recipient ? '重选' : '选择' }}
          </button>
        </div>
      </div>
      <div class="ho-row ho-row-top">
        <span class="ho-label">交接说明</span>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="3"
          maxlength="120"
          show-word-limit
          placeholder="选填：说明交接原因（如：岗位调整）"
        />
      </div>
    </template>

    <div class="ho-footer">
      <template v-if="emptyScope">
        <el-button @click="dialogVisible = false">知道了</el-button>
      </template>
      <template v-else>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!recipient" @click="handleSubmit">
          <i class="el-icon-s-promotion" /> 提交交接申请
        </el-button>
      </template>
    </div>

    <!-- 选人弹窗（交接为单人，取首位） -->
    <UserPicker
      :visible="pickerVisible"
      title="选择接手人"
      :exclude-ids="selfId ? [selfId] : []"
      @confirm="confirmPick"
      @close="pickerVisible = false"
    />
  </el-dialog>
</template>

<script>
import { applyHandover } from '@/service/sys/TaskHandoverService'
import { getUserInfo } from '@/utils/auth'
import UserPicker from '@/components/UserPicker'

export default {
  name: 'HandoverModal',
  components: { UserPicker },
  props: {
    visible: { type: Boolean, default: false },
    /** 任务配置ID（flow_dispatch.id） */
    dispatchId: { type: String, default: '' },
    /** 任务配置名 */
    dispatchName: { type: String, default: '' },
    /** 交接期次数（仅展示用） */
    periodCount: { type: Number, default: 0 },
    /** 我在该任务下的节点席位数（含已提交历史节点，仅展示用） */
    nodeCount: { type: Number, default: 0 }
  },
  data() {
    return {
      pickerVisible: false,
      recipient: null,
      remark: '',
      saving: false
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.$emit('close') }
    },
    /** 当前登录用户号（选人时排除自己） */
    selfId() {
      return getUserInfo().yyytId || ''
    },
    /** 交接范围文案：期次 / 节点席位按实际存在项组合展示 */
    scopeText() {
      const parts = []
      if (this.periodCount > 0) parts.push(this.periodCount + ' 个期次')
      if (this.nodeCount > 0) parts.push(this.nodeCount + ' 条节点记录')
      return parts.length ? parts.join(' 与 ') : '交接范围'
    },
    /** 无可交接内容：既无归属我的期次，也无我在该任务下的任何节点席位 */
    emptyScope() {
      return (this.periodCount || 0) === 0 && (this.nodeCount || 0) === 0
    }
  },
  watch: {
    visible(val) {
      if (val) this.reset()
    }
  },
  methods: {
    confirmPick(rows) {
      if (!rows || rows.length === 0) return
      if (rows.length > 1) this.$message.warning('交接为单人操作，已取首位「' + rows[0].userName + '」')
      this.recipient = { yyytId: rows[0].yyytId, userName: rows[0].userName }
      this.pickerVisible = false
    },
    reset() {
      this.recipient = null
      this.remark = ''
    },
    async handleSubmit() {
      if (!this.recipient) {
        this.$message.warning('请选择接手人')
        return
      }
      this.saving = true
      try {
        const res = await applyHandover({
          dispatchId: this.dispatchId,
          toUserId: this.recipient.yyytId,
          remark: (this.remark && this.remark.trim()) || null
        })
        this.$message.success((res && res.message) || '交接申请已提交，等待审批')
        this.reset()
        this.$emit('success', (res && res.data) || {})
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '提交失败')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);

.ho-tip { font-size: 12px; color: #8a93a5; background: #FFF7ED; border: 1px dashed #F0B775; border-radius: 3px; padding: 8px 10px; margin-bottom: 14px; line-height: 1.6;
  i { color: #D97706; }
  b { color: #414755; }
}
.ho-tip-sub { margin-top: 4px; color: #a3aab8; }
.ho-tip-empty { background: #F4F6F9; border-color: #D8DEE9;
  i { color: #909399; }
  b { color: #606266; }
}
.ho-row { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.ho-row-top { align-items: flex-start; }
.ho-label { width: 100px; flex-shrink: 0; text-align: right; font-size: 13px; font-weight: 600; color: #414755; }
.req { color: $primary; }
.ho-static { flex: 1; font-size: 13px; color: #1b1c1c; }
.ho-picker { flex: 1; display: flex; align-items: center; justify-content: space-between; gap: 8px; border: 1px solid #CBD5E1; border-radius: 2px; padding: 0 8px; height: 36px; background: #fff; }
.ho-chosen { font-size: 13px; color: #1b1c1c;
  em { font-style: normal; color: #909399; font-size: 12px; margin-left: 6px; }
}
.ho-placeholder { font-size: 13px; color: #c0c4cc; }
.ho-pick-btn { border: none; background: var(--color-primary-light); color: $primary; font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 2px; cursor: pointer;
  &:hover { background: rgba(var(--color-primary-rgb), 0.18); }
}
.ho-footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
</style>
