<template>
  <el-dialog title="版本记录" :visible.sync="dialogVisible" width="680px" :close-on-click-modal="false" append-to-body>
    <div v-loading="loading" class="ver-wrap">
      <div v-if="!loading && versions.length === 0" class="ver-empty">
        <i class="el-icon-document" />
        <p>暂无历史版本</p>
        <p class="ver-empty-tip">在设计器中「保存为新版本」后，当前配置会在此归档</p>
      </div>
      <div v-else class="ver-list">
        <!-- 当前版本（最新） -->
        <div v-if="currentVersion" class="ver-card current">
          <div class="ver-head">
            <span class="ver-tag">当前版本</span>
            <span class="ver-no">v{{ currentVersion.version }}</span>
            <span class="ver-time"><i class="el-icon-time" /> {{ currentVersion.updateTime || currentVersion.createTime || '—' }}</span>
          </div>
          <div v-if="currentVersion.versionDesc" class="ver-desc">{{ currentVersion.versionDesc }}</div>
          <div v-else class="ver-desc muted">暂无改动说明</div>
        </div>
        <!-- 历史版本 -->
        <div v-for="v in versions" :key="v.id" class="ver-card">
          <div class="ver-head">
            <span class="ver-tag">历史版本</span>
            <span class="ver-no">v{{ v.version }}</span>
            <span class="ver-time"><i class="el-icon-time" /> {{ v.createTime || '—' }}</span>
            <span v-if="v.modifierName" class="ver-user"><i class="el-icon-user" /> {{ v.modifierName }}</span>
          </div>
          <div v-if="v.versionDesc" class="ver-desc">{{ v.versionDesc }}</div>
          <div v-else class="ver-desc muted">暂无改动说明</div>
        </div>
      </div>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getTemplateVersions } from '@/api/template'

export default {
  name: 'VersionListModal',
  props: {
    visible: { type: Boolean, default: false },
    templateId: { type: Number, default: null },
    /** 当前版本信息（模板行，含 version/versionDesc/updateTime） */
    currentVersion: { type: Object, default: null }
  },
  data() {
    return {
      loading: false,
      versions: []
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
      if (val) this.fetchVersions()
    }
  },
  methods: {
    async fetchVersions() {
      if (!this.templateId) return
      this.loading = true
      try {
        const res = await getTemplateVersions(this.templateId)
        this.versions = res.data || []
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
.ver-wrap { min-height: 120px; }
.ver-empty { text-align: center; padding: 40px 0; color: #bbb;
  i { font-size: 40px; display: block; margin-bottom: 8px; }
  p { margin: 4px 0; font-size: 14px; color: #999; }
  .ver-empty-tip { font-size: 12px; color: #bbb; }
}
.ver-list { display: flex; flex-direction: column; gap: 12px; }
.ver-card { border: 1px solid #e4e7ed; border-radius: 3px; padding: 14px 16px;
  &.current { border-color: rgba(var(--color-primary-rgb),0.5); background: var(--color-primary-light); }
}
.ver-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 6px; }
.ver-tag { font-size: 11px; font-weight: 700; color: #fff; background: #909399; padding: 2px 8px; border-radius: 3px;
  .current & { background: var(--color-primary); }
}
.ver-no { font-size: 15px; font-weight: 700; color: #1b1c1c; }
.ver-time { font-size: 12px; color: #999; display: inline-flex; align-items: center; gap: 3px; }
.ver-user { font-size: 12px; color: #606266; display: inline-flex; align-items: center; gap: 3px; }
.ver-desc { font-size: 13px; color: #414755; line-height: 1.6; background: #f7f8fa; border-radius: 4px; padding: 8px 10px; white-space: pre-wrap;
  &.muted { color: #bbb; }
}
.dialog-footer { text-align: right; }
</style>
