<template>
  <div class="attach-field" :class="{ 'is-readonly': readonly }">
    <!-- 上传操作区（编辑模式）：选择后立即上传（el-upload auto-upload） -->
    <div v-if="!readonly" class="af-upload">
      <el-upload
        ref="up"
        class="af-upload-wrap"
        :accept="acceptAttr"
        action="#"
        multiple
        :auto-upload="true"
        :show-file-list="false"
        :http-request="httpUpload"
        :before-upload="beforeUpload"
        :disabled="uploading"
        style="display: inline-block"
      >
        <el-button class="af-pick-btn" size="small" :loading="uploading" :disabled="uploading">
          <i class="el-icon-upload2" /> {{ uploading ? '上传中...' : '选择' }}{{ fieldType === 'image' ? '图片' : '文件' }}
        </el-button>
      </el-upload>
      <span class="af-upload-hint">可多选，选择后立即上传</span>
    </div>

    <!-- 已上传文件列表 -->
    <div v-if="fileList.length > 0" class="af-list">
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
import { uploadAttaches, getAttachList, previewAttach, downloadAttach, deleteAttach } from '@/service/base/AttachService'

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
      /** 是否正在上传（选择后立即上传，上传期间禁用再次选择） */
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
  methods: {
    isImage(f) {
      return this.fieldType === 'image' || IMG_RE.test(f.fileName || '')
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
    /** el-upload 选择前校验（单文件最大 50MB） */
    beforeUpload(file) {
      const MAX = 50 * 1024 * 1024
      if (file && file.size > MAX) {
        this.$message.error(`文件「${file.name}」超过 50MB 限制`)
        return false
      }
      return true
    },
    /** el-upload 自定义上传：选择后立即上传单文件，成功即入已上传列表 */
    async httpUpload(option) {
      const file = option.file
      this.uploading = true
      try {
        const res = await uploadAttaches([file], this.bizId, this.currentCreator)
        const list = res.data || []
        if (list.length > 0) {
          this.fileList.push(list[0])
          this.emitValue()
          option.onSuccess(list[0])
        } else {
          option.onError(new Error('上传响应为空'))
        }
      } catch (e) {
        console.error(e)
        this.$message.error(`「${file.name}」上传失败：${(e && e.message) || '请重试'}`)
        option.onError(e)
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
        confirmButtonClass: 'el-button--primary'
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
$primary: var(--color-primary);
$border: #CBD5E1;
.attach-field { display: flex; flex-direction: column; gap: 10px; }
.af-upload { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 2px; }
.af-upload-hint { font-size: 12px; color: #999; margin-left: 2px; }
.af-pick-btn.el-button--default { color: $primary; border-color: $primary; background: #fff;
  &:hover, &:focus { color: #fff; background: $primary; border-color: $primary; }
}
// 只读展示：更轻量的卡片
.attach-field.is-readonly .af-row { background: #FAFAFB; border-color: #e8eaef;
  &:hover { border-color: $primary; background: var(--color-primary-surface); box-shadow: none; }
}
.attach-field.is-readonly .af-icon { color: #9aa0ac; }
.attach-field.is-readonly .af-meta { color: #b0b4bd; }

.af-list { display: flex; flex-direction: column; gap: 8px; max-height: 260px; overflow-y: auto; padding-right: 2px; }
.af-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; background: var(--color-primary-surface); border: 1px solid $border; border-radius: 3px; padding: 8px 12px; transition: all .2s;
  &:hover { border-color: $primary; background: var(--color-primary-light); box-shadow: 0 2px 6px rgba(var(--color-primary-rgb),0.08); }
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
.af-icon-btn { width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; border: none; background: transparent; color: $primary; font-size: 15px; cursor: pointer; border-radius: 2px; transition: all .2s;
  &:hover { background: rgba(var(--color-primary-rgb),0.08); transform: translateY(-1px); }
  &:disabled { opacity: 0.45; cursor: not-allowed; }
}
.af-del { color: #DC2626;
  &:hover { background: rgba(220,38,38,0.06); }
}
.af-preview { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 10px 0;
  > i { font-size: 56px; color: $primary; }
}
.af-preview-name { font-size: 15px; font-weight: 700; color: #1b1c1c; word-break: break-all; text-align: center; }
.af-preview-meta { width: 100%; display: flex; flex-direction: column; gap: 6px; background: var(--color-primary-light); border: 1px dashed $border; border-radius: 3px; padding: 12px 14px; font-size: 13px; color: var(--color-primary);
  div { word-break: break-all; }
}
</style>
