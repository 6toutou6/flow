<template>
  <el-dialog title="保存流程" :visible.sync="dialogVisible" width="840px" :close-on-click-modal="false" append-to-body>
    <div class="sf-wrap">
      <div class="sf-tip">保存为新版本时，当前配置将归档为历史版本。</div>
      <!-- 模板已被任务/期次使用时的提示：明确告知修改不影响已下发期次（期次持有独立快照） -->
      <div v-if="hasUsage" :key="usageFlashKey" class="sf-usage flash">
        <i class="el-icon-warning-outline" />
        <div class="sf-usage-body">
          <p class="sf-usage-title">该模板已被 <b>{{ usage.taskCount }}</b> 个任务、<b>{{ usage.dispatchCount }}</b> 个期次使用</p>
          <p v-if="briefTaskNames" class="sf-usage-names"><span class="sf-usage-tag">使用任务</span>{{ briefTaskNames }}</p>
          <p v-if="briefDispatchNames" class="sf-usage-names"><span class="sf-usage-tag">使用期次</span>{{ briefDispatchNames }}</p>
          <p class="sf-usage-desc">修改仅对新下发的期次生效，已下发期次沿用保存前的节点配置，<b>不会被同步修改</b>。</p>
        </div>
      </div>
      <div class="sf-group">
        <label class="sf-label">保存方式 <span class="req">*</span></label>
        <div class="save-mode-row">
          <div class="mode-card" :class="{ active: saveMode === 'current' }" @click="saveMode = 'current'">
            <i class="el-icon-refresh" />
            <div class="mode-body">
              <div class="mode-title">保存到当前版本</div>
              <div class="mode-desc">覆盖当前配置，版本号不变（v{{ templateVersion }}）</div>
            </div>
          </div>
          <div class="mode-card" :class="{ active: saveMode === 'new' }" @click="saveMode = 'new'">
            <i class="el-icon-plus" />
            <div class="mode-body">
              <div class="mode-title">保存为新版本</div>
              <div class="mode-desc">版本号 +1（v{{ templateVersion + 1 }}），旧配置归档保留</div>
            </div>
          </div>
        </div>
      </div>
      <div class="sf-group">
        <label class="sf-label">改动说明</label>
        <el-input v-model="versionDesc" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="本次改动了什么？如：新增需求设计节点字段、调整流程节点顺序（选填，建议填写便于追溯）" />
      </div>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">确认保存</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'SaveFlowModal',
  props: {
    visible: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
    templateVersion: { type: Number, default: 1 },
    /** 模板被使用情况 { taskCount, dispatchCount }，用于保存前提示用户 */
    usage: { type: Object, default: null }
  },
  data() {
    return {
      saveMode: 'current',
      versionDesc: '',
      /** 弹窗每次打开自增，用于强制重放提示条闪烁动画 */
      usageFlashKey: 0
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.handleClose() }
    },
    hasUsage() {
      const u = this.usage
      return !!u && (u.taskCount > 0 || u.dispatchCount > 0)
    },
    briefTaskNames() {
      return this.briefNames(this.usage && this.usage.taskNames)
    },
    briefDispatchNames() {
      return this.briefNames(this.usage && this.usage.dispatchNames)
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.saveMode = 'current'
        this.versionDesc = ''
        // 强制重建提示条，重新播放闪烁动画
        this.usageFlashKey++
      }
    }
  },
  methods: {
    /** 名称列表 → 展示串：最多显示 3 个，超出显示「等 N 个」 */
    briefNames(list) {
      if (!list || !list.length) return ''
      const head = list.slice(0, 3).join('、')
      return list.length > 3 ? `${head} 等 ${list.length} 个` : head
    },
    handleConfirm() {
      this.$emit('confirm', { saveMode: this.saveMode, versionDesc: this.versionDesc })
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
.sf-wrap { padding: 4px 0; }
.sf-tip { font-size: 12px; color: #999; background: #f7f8fa; border-radius: 2px; padding: 10px 12px; margin-bottom: 16px; line-height: 1.6; }
.sf-usage { display: flex; align-items: flex-start; gap: 10px; background: #F0F4FF; border: 1px solid #BFDBFE; border-radius: 2px; padding: 12px 14px; margin-bottom: 16px;
  i { color: #D97706; font-size: 16px; line-height: 22px; flex-shrink: 0; }
}
.sf-usage-body { flex: 1; min-width: 0; }
.sf-usage-title { font-size: 14px; font-weight: 700; color: var(--color-primary-hover); line-height: 1.5;
  b { font-size: 16px; color: var(--color-primary-hover); }
}
.sf-usage-names { font-size: 13px; color: var(--color-primary); margin-top: 6px; line-height: 1.6; }
.sf-usage-tag { display: inline-block; background: #DBEAFE; color: var(--color-primary-hover); font-size: 11px; font-weight: 600; border-radius: 3px; padding: 1px 6px; margin-right: 6px; vertical-align: 1px; }
.sf-usage-desc { font-size: 12px; color: #64748B; margin-top: 6px; line-height: 1.6;
  b { color: var(--color-primary); }
}
/* 弹窗出现时提示条闪烁两次，引起注意 */
@keyframes sfFlash {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.2; }
}
.flash { animation: sfFlash 0.45s ease 2; }
.sf-group { margin-bottom: 16px; }
.sf-label { display: block; font-size: 13px; font-weight: 600; color: #414755; margin-bottom: 8px; }
.req { color: var(--color-primary); }
.save-mode-row { display: flex; gap: 12px; }
.mode-card { flex: 1; display: flex; align-items: flex-start; gap: 10px; border: 1px solid #e4e7ed; border-radius: 3px; padding: 14px; cursor: pointer; transition: all 0.2s;
  i { font-size: 20px; color: #909399; margin-top: 2px; }
  &.active { border-color: var(--color-primary); background: var(--color-primary-light); box-shadow: 0 0 0 1px var(--color-primary);
    i { color: var(--color-primary); }
  }
}
.mode-title { font-size: 14px; font-weight: 700; color: #1b1c1c; }
.mode-desc { font-size: 12px; color: #999; margin-top: 4px; line-height: 1.5; }
.dialog-footer { text-align: right; }
</style>
