<template>
  <el-dialog title="保存流程" :visible.sync="dialogVisible" width="520px" :close-on-click-modal="false" append-to-body>
    <div class="sf-wrap">
      <div class="sf-tip">保存流程前请确认节点链与字段配置无误。保存为新版本时，当前配置将归档为历史版本，已下发任务仍沿用锁定版本。</div>
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
    templateVersion: { type: Number, default: 1 }
  },
  data() {
    return {
      saveMode: 'current',
      versionDesc: ''
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
        this.saveMode = 'current'
        this.versionDesc = ''
      }
    }
  },
  methods: {
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
.sf-tip { font-size: 12px; color: #999; background: #f7f8fa; border-radius: 6px; padding: 10px 12px; margin-bottom: 16px; line-height: 1.6; }
.sf-group { margin-bottom: 16px; }
.sf-label { display: block; font-size: 13px; font-weight: 600; color: #414755; margin-bottom: 8px; }
.req { color: #C53030; }
.save-mode-row { display: flex; gap: 12px; }
.mode-card { flex: 1; display: flex; align-items: flex-start; gap: 10px; border: 1px solid #e4e7ed; border-radius: 8px; padding: 14px; cursor: pointer; transition: all 0.2s;
  i { font-size: 20px; color: #909399; margin-top: 2px; }
  &.active { border-color: #C53030; background: #FFF5F5; box-shadow: 0 0 0 1px #C53030;
    i { color: #C53030; }
  }
}
.mode-title { font-size: 14px; font-weight: 700; color: #1b1c1c; }
.mode-desc { font-size: 12px; color: #999; margin-top: 4px; line-height: 1.5; }
.dialog-footer { text-align: right; }
</style>
