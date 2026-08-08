<template>
  <div class="attach-field" :class="{ 'is-readonly': readonly }">
    <!-- 上传操作区（编辑模式） -->
    <div v-if="!readonly" class="af-upload">
      <el-button class="af-pick-btn" size="small" :disabled="uploading" @click="pickFiles"><i class="el-icon-paperclip" /> 选择{{ fieldType === 'image' ? '图片' : '文件' }}</el-button>
      <el-button class="af-up-btn" type="primary" size="small" :loading="uploading" :disabled="pendingFiles.length === 0" @click="doUpload">
        <i class="el-icon-upload2" /> 上传{{ pendingFiles.length > 0 ? `（${pendingFiles.length} 个）` : '' }}
      </el-button>
      <span class="af-upload-hint">可多选，选中后点「上传」一次性提交</span>
      <input ref="fileInput" type="file" :accept="acceptAttr" multiple style="display: none" @change="onPickFiles">
    </div>

    <!-- 待上传文件（选中立即展示，可单独移除） -->
    <div v-if="!readonly && pendingFiles.length > 0" class="af-pending">
      <div class="af-pending-title"><i class="el-icon-folder-add" /> 待上传文件（{{ pendingFiles.length }}）</div>
      <div class="af-list">
        <div v-for="(f, i) in pendingFiles" :key="'p' + i" class="af-row af-row-pending">
          <div class="af-left">
            <img v-if="isImageName(f.name)" class="af-thumb" :src="thumbUrl(f)" :alt="f.name">
            <i v-else class="af-icon el-icon-document" />
            <div class="af-info">
              <div class="af-name" :title="f.name">{{ f.name }}</div>
              <div class="af-meta">{{ formatSize(f.size) }}</div>
            </div>
          </div>
          <button type="button" class="af-btn af-del" :disabled="uploading" @click="removePending(i)"><i class="el-icon-close" /> 移除</button>
        </div>
      </div>
    </div>

    <!-- 已上传文件列表 -->
    <div v-if="fileList.length > 0">
      <div v-if="!readonly && pendingFiles.length > 0" class="af-divider"><i class="el-icon-folder-opened" /> 已上传</div>
      <div class="af-list">
        <div v-for="(f, i) in fileList" :key="f.attachId || i" class="af-row">
          <div class="af-left">
            <i class="af-icon" :class="isImage(f) ? 'el-icon-picture' : 'el-icon-document'" />
            <div class="af-info">
              <div class="af-name" :title="f.fileName">{{ f.fileName }}</div>
              <div class="af-meta">
                <span v-if="f.createTime"><i class="el-icon-time" /> {{ f.createTime }}</span>
                <span v-if="f.creator"><i class="el-icon-user" /> {{ f.creator }}</span>
              </div>
            </div>
          </div>
          <div class="af-actions">
            <el-tooltip content="预览" placement="top">
              <button type="button" class="af-icon-btn" :disabled="actingId === f.attachId" @click="onPreview(f)"><i class="el-icon-view" /></button>
            </el-tooltip>
            <el-tooltip content="下载" placement="top">
              <button type="button" class="af-icon-btn" :disabled="actingId === f.attachId" @click="onDownload(f)"><i class="el-icon-download" /></button>
            </el-tooltip>
            <el-tooltip v-if="!readonly" content="删除" placement="top">
              <button type="button" class="af-icon-btn af-del" :disabled="actingId === f.attachId" @click="onDelete(f)"><i class="el-icon-delete" /></button>
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="!readonly && pendingFiles.length === 0" class="af-empty">尚未上传{{ fieldType === 'image' ? '图片' : '文件' }}</div>

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
import { uploadAttaches, getAttachList, previewAttach, downloadAttach, deleteAttach } from '@/api/attach'

const IMG_RE = /\.(png|jpe?g|gif|webp|bmp|svg|ico)$/i

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
      /** 已选中待上传的原生 File 列表（独立管理，不依赖 el-upload 内部状态） */
      pendingFiles: [],
      uploading: false,
      actingId: null,
      previewVisible: false,
      previewFile: null
    }
  },
  computed: {
    acceptAttr() {
      return this.fieldType === 'image' ? 'image/*' : ''
    },
    /** 当前登录用户姓名（无鉴权环境下前端自行带上传人） */
    currentCreator() {
      const g = this.$store && this.$store.getters
      return (g && (g.realName || g.username)) || ''
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
  async mounted() {
    // 非只读且字段值初始为空时，按业务id回显已上传文件（处理人上传后未提交刷新，文件已与该节点字段绑定）
    if (this.readonly || !this.bizId || String(this.value || '').trim()) return
    try {
      const res = await getAttachList(this.bizId)
      const list = (res && res.data) || []
      if (list && list.length > 0) {
        this.fileList = list
        this.emitValue()
      }
    } catch (e) { /* 静默：拉取失败不影响使用 */ }
  },
  beforeDestroy() {
    this.releaseThumbs()
  },
  methods: {
    isImage(f) {
      return this.fieldType === 'image' || IMG_RE.test(f.fileName || '')
    },
    isImageName(name) {
      return this.fieldType === 'image' || IMG_RE.test(name || '')
    },
    /** 文件大小格式化 */
    formatSize(size) {
      if (size == null || isNaN(size)) return ''
      if (size < 1024) return size + ' B'
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
      if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(1) + ' MB'
      return (size / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
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
    /** 触发原生文件选择 */
    pickFiles() {
      this.$refs.fileInput && this.$refs.fileInput.click()
    },
    /** 选择文件：追加到待上传列表（按 名称+大小+修改时间 去重），并重置 input 以支持重复选择同一文件 */
    onPickFiles(e) {
      const files = Array.from(e.target.files || [])
      files.forEach(nf => {
        const dup = this.pendingFiles.some(p => p.name === nf.name && p.size === nf.size && p.lastModified === nf.lastModified)
        if (!dup) this.pendingFiles.push(nf)
      })
      e.target.value = ''
    },
    /** 从待上传列表中移除（未上传，不调接口；同时释放本地缩略图） */
    removePending(i) {
      const f = this.pendingFiles[i]
      if (f && f._url) { try { URL.revokeObjectURL(f._url) } catch (e) { /* ignore */ } }
      this.pendingFiles.splice(i, 1)
    },
    /** 本地图片缩略图（待上传预览，objectURL 按文件缓存） */
    thumbUrl(f) {
      if (f && !f._url) {
        try { f._url = URL.createObjectURL(f) } catch (e) { f._url = '' }
      }
      return (f && f._url) || ''
    },
    /** 释放所有本地缩略图 URL */
    releaseThumbs() {
      this.pendingFiles.forEach(f => {
        if (f && f._url) { try { URL.revokeObjectURL(f._url) } catch (e) { /* ignore */ } f._url = null }
      })
    },
    /** 批量上传：把待上传文件一次传给后端 MultipartFile[] 入库 */
    async doUpload() {
      const raw = this.pendingFiles.filter(f => f && f.size > -1)
      if (raw.length === 0) {
        this.$message.warning('请先选择文件')
        return
      }
      this.uploading = true
      try {
        const res = await uploadAttaches(raw, this.bizId, this.currentCreator)
        const list = res.data || []
        list.forEach(a => this.fileList.push(a))
        this.emitValue()
        this.releaseThumbs()
        this.pendingFiles = []
        this.$message.success(`成功上传 ${list.length} 个文件`)
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
        await downloadAttach(f.attachId)
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
      }).then(async() => {
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
.attach-field { display: flex; flex-direction: column; gap: 10px; }
.af-upload { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 2px; }
.af-upload-hint { font-size: 12px; color: #999; margin-left: 2px; }
.af-pick-btn.el-button--default { color: $primary; border-color: $primary; background: #fff;
  &:hover, &:focus { color: #fff; background: $primary; border-color: $primary; }
}
.af-up-btn { background: $primary; border-color: $primary;
  &:hover, &:focus { background: darken($primary, 6%) !important; border-color: darken($primary, 6%) !important; }
}
.af-thumb { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; border: 1px solid #f0e0c0; background: #fafafa; flex-shrink: 0; }
// 只读展示：更轻量的卡片
.attach-field.is-readonly .af-row { background: #FAFAFB; border-color: #e8eaef;
  &:hover { border-color: $primary; background: #FDF9F9; box-shadow: none; }
}
.attach-field.is-readonly .af-icon { color: #9aa0ac; }
.attach-field.is-readonly .af-meta { color: #b0b4bd; }

// 待上传区（虚线框黄底，与已上传区分）
.af-pending { border: 1px dashed #e8c98f; background: #FFFAF0; border-radius: 8px; padding: 10px 12px; }
.af-pending-title { font-size: 12px; font-weight: 700; color: #b7791f; margin-bottom: 8px; display: flex; align-items: center; gap: 5px;
  i { font-size: 14px; }
}
.af-divider { font-size: 12px; font-weight: 700; color: #757575; margin-bottom: 8px; display: flex; align-items: center; gap: 5px;
  i { font-size: 14px; color: $primary; }
}

.af-list { display: flex; flex-direction: column; gap: 8px; max-height: 260px; overflow-y: auto; padding-right: 2px; }
.af-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; background: #FDF9F9; border: 1px solid $border; border-radius: 8px; padding: 8px 12px; transition: all .2s;
  &:hover { border-color: $primary; background: #FFF5F5; box-shadow: 0 2px 6px rgba(197,48,48,0.08); }
}
.af-row-pending { background: #fff; border-color: #ecd9ae;
  &:hover { border-color: #d9a95a; background: #FFF7E8; box-shadow: 0 2px 6px rgba(183,121,31,0.08); }
  .af-icon { color: #d9a95a; }
}
.af-left { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.af-icon { font-size: 20px; color: $primary; flex-shrink: 0; }
.af-info { min-width: 0; flex: 1; line-height: 1.6; }
.af-name { font-size: 13px; color: #414755; font-weight: 600; min-width: 0; line-height: 1.6; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.af-meta { display: flex; align-items: center; gap: 8px; font-size: 11px; color: #999; line-height: 1.6; margin-top: 2px;
  span { display: inline-flex; align-items: center; gap: 3px; }
  i { font-size: 12px; color: #c9a3a0; }
}
.af-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.af-btn { display: inline-flex; align-items: center; gap: 3px; padding: 4px 9px; border: none; background: transparent; color: $primary; font-size: 12px; cursor: pointer; border-radius: 5px;
  &:hover { background: rgba(197,48,48,0.08); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.af-icon-btn { width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; border: none; background: transparent; color: $primary; font-size: 15px; cursor: pointer; border-radius: 6px; transition: all .2s;
  &:hover { background: rgba(197,48,48,0.08); transform: translateY(-1px); }
  &:disabled { opacity: 0.45; cursor: not-allowed; }
}
.af-del { color: #ba1a1a;
  &:hover { background: rgba(186,26,26,0.08); }
}
.af-empty { font-size: 12px; color: #bbb; padding: 3px 0; margin-top: -26px; }
.af-preview { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 10px 0;
  > i { font-size: 56px; color: $primary; }
}
.af-preview-name { font-size: 15px; font-weight: 700; color: #1b1c1c; word-break: break-all; text-align: center; }
.af-preview-meta { width: 100%; display: flex; flex-direction: column; gap: 6px; background: #FFF5F5; border: 1px dashed $border; border-radius: 8px; padding: 12px 14px; font-size: 13px; color: #5b403d;
  div { word-break: break-all; }
}
</style>
