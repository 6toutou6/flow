<template>
  <div class="attach-field">
    <!-- 上传区（编辑模式） -->
    <div v-if="!readonly" class="af-upload">
      <el-upload
        :show-file-list="false"
        :http-request="doUpload"
        multiple
        :disabled="uploading"
      >
        <el-button type="primary" size="small" :loading="uploading" icon="el-icon-upload2">
          {{ fieldType === 'image' ? '上传图片' : '上传文件' }}
        </el-button>
      </el-upload>
      <span class="af-upload-hint">支持多文件上传，记录文件名等信息</span>
    </div>

    <!-- 文件列表 -->
    <div v-if="fileList.length > 0" class="af-list">
      <div v-for="(f, i) in fileList" :key="f.attachId || i" class="af-row">
        <div class="af-left">
          <i class="af-icon" :class="isImage(f) ? 'el-icon-picture' : 'el-icon-document'" />
          <div class="af-info">
            <div class="af-name" :title="f.fileName">{{ f.fileName }}</div>
            <div class="af-meta">
              <template v-if="f.createTime">{{ f.createTime }}</template>
              <template v-if="f.creator"> · {{ f.creator }}</template>
            </div>
          </div>
        </div>
        <div class="af-actions">
          <button class="af-btn" :disabled="actingId === f.attachId" @click="onPreview(f)"><i class="el-icon-view" /> 预览</button>
          <button class="af-btn" :disabled="actingId === f.attachId" @click="onDownload(f)"><i class="el-icon-download" /> 下载</button>
          <button class="af-btn af-del" :disabled="actingId === f.attachId" @click="onDelete(f)"><i class="el-icon-delete" /> 删除</button>
        </div>
      </div>
    </div>
    <div v-else-if="!readonly" class="af-empty">尚未上传{{ fieldType === 'image' ? '图片' : '文件' }}</div>
    <div v-else class="af-empty">—</div>

    <!-- 预览弹窗 -->
    <el-dialog :visible.sync="previewVisible" title="附件预览" width="480px" append-to-body>
      <div v-if="previewFile" class="af-preview">
        <i class="el-icon-picture-outline" />
        <div class="af-preview-name">{{ previewFile.fileName }}</div>
        <div class="af-preview-meta">
          <div>附件ID：{{ previewFile.attachId }}</div>
          <div>ECS地址：{{ previewFile.ecsUrl || '—' }}</div>
          <div v-if="previewFile.creator">创建人：{{ previewFile.creator }}</div>
          <div v-if="previewFile.createTime">创建时间：{{ previewFile.createTime }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { uploadAttach, previewAttach, downloadAttach, deleteAttach } from '@/api/attach'

export default {
  name: 'AttachField',
  props: {
    /** 字段值：JSON 字符串 [{"attachId":"","fileName":"","ecsUrl":"","creator":"","createTime":""}] */
    value: { type: String, default: '' },
    /** 字段类型：file / image */
    fieldType: { type: String, default: 'file' },
    /** 业务id（上传时写入 attach.biz_id，如任务id） */
    bizId: { type: [String, Number], default: null },
    /** 只读展示模式（历史表单/流程详情） */
    readonly: { type: Boolean, default: false }
  },
  data() {
    return {
      fileList: [],
      uploading: false,
      actingId: null,
      previewVisible: false,
      previewFile: null
    }
  },
  watch: {
    value: {
      immediate: true,
      handler() {
        this.fileList = this.parseValue(this.value)
      }
    }
  },
  methods: {
    isImage(f) {
      const n = (f.fileName || '').toLowerCase()
      return this.fieldType === 'image' || /\.(png|jpe?g|gif|webp|bmp|svg|ico)$/.test(n)
    },
    /** 解析字段值：JSON 数组优先；兼容旧版纯文本文件名 */
    parseValue(v) {
      if (!v) return []
      const s = String(v).trim()
      if (!s) return []
      if (s.startsWith('[')) {
        try {
          const arr = JSON.parse(s)
          return Array.isArray(arr) ? arr.filter(x => x && x.fileName) : []
        } catch (e) {
          return [{ fileName: s }]
        }
      }
      return [{ fileName: s }]
    },
    emitValue() {
      const list = this.fileList.map(f => ({
        attachId: f.attachId || '',
        fileName: f.fileName || '',
        ecsUrl: f.ecsUrl || '',
        creator: f.creator || '',
        createTime: f.createTime || ''
      }))
      this.$emit('input', list.length ? JSON.stringify(list) : '')
    },
    async doUpload({ file }) {
      this.uploading = true
      try {
        const res = await uploadAttach(file, this.bizId)
        const a = res.data || {}
        this.fileList.push(a)
        this.emitValue()
        this.$message.success(`「${a.fileName || file.name}」上传成功`)
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '上传失败')
      } finally {
        this.uploading = false
      }
    },
    async onPreview(f) {
      if (!f.attachId) return
      this.actingId = f.attachId
      try {
        const res = await previewAttach(f.attachId)
        this.previewFile = res.data || f
        this.previewVisible = true
      } catch (e) {
        this.$message.error((e && e.message) || '预览失败')
      } finally {
        this.actingId = null
      }
    },
    async onDownload(f) {
      if (!f.attachId) return
      this.actingId = f.attachId
      try {
        const res = await downloadAttach(f.attachId)
        this.$message.success(`已发起下载：${f.fileName}`)
      } catch (e) {
        this.$message.error((e && e.message) || '下载失败')
      } finally {
        this.actingId = null
      }
    },
    onDelete(f) {
      if (!f.attachId) return
      this.$confirm(`确定删除附件「${f.fileName}」吗？`, '删除附件确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }).then(async () => {
        this.actingId = f.attachId
        try {
          await deleteAttach(f.attachId)
          this.$message.success('删除成功')
          this.fileList = this.fileList.filter(x => x.attachId !== f.attachId)
          this.emitValue()
        } catch (e) {
          this.$message.error((e && e.message) || '删除失败')
        } finally {
          this.actingId = null
        }
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
$border: #e4beba;
.attach-field { display: flex; flex-direction: column; gap: 8px; }
.af-upload { display: flex; align-items: center; gap: 10px; }
.af-upload-hint { font-size: 12px; color: #999; }
.af-list { display: flex; flex-direction: column; gap: 6px; max-height: 220px; overflow-y: auto; }
.af-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; background: #FDF9F9; border: 1px solid $border; border-radius: 6px; padding: 6px 10px; transition: all .2s;
  &:hover { border-color: $primary; background: #FFF5F5; }
}
.af-left { display: flex; align-items: center; gap: 8px; min-width: 0; flex: 1; }
.af-icon { font-size: 18px; color: $primary; flex-shrink: 0; }
.af-info { min-width: 0; }
.af-name { font-size: 13px; color: #414755; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 240px; }
.af-meta { font-size: 11px; color: #999; margin-top: 1px; }
.af-actions { display: flex; align-items: center; gap: 2px; flex-shrink: 0; }
.af-btn { display: inline-flex; align-items: center; gap: 3px; padding: 3px 8px; border: none; background: transparent; color: $primary; font-size: 12px; cursor: pointer; border-radius: 4px;
  &:hover { background: rgba(197,48,48,0.08); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.af-del { color: #ba1a1a;
  &:hover { background: rgba(186,26,26,0.08); }
}
.af-empty { font-size: 12px; color: #bbb; padding: 2px 0; }
.af-preview { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 10px 0;
  > i { font-size: 56px; color: $primary; }
}
.af-preview-name { font-size: 15px; font-weight: 700; color: #1b1c1c; word-break: break-all; text-align: center; }
.af-preview-meta { width: 100%; display: flex; flex-direction: column; gap: 6px; background: #FFF5F5; border: 1px dashed $border; border-radius: 8px; padding: 12px 14px; font-size: 13px; color: #5b403d;
  div { word-break: break-all; }
}
</style>
