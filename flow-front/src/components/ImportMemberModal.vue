<template>
  <el-dialog
    title="批量导入人员"
    :visible.sync="dialogVisible"
    width="580px"
    append-to-body
    :close-on-click-modal="false"
    @close="reset"
  >
    <!-- 第 1 步：下载模板 -->
    <div class="im-step">
      <span class="im-no">1</span>
      <div class="im-main">
        <div class="im-text">下载导入模板，按格式填写人员信息</div>
        <div class="im-hint">
          模板三列：<b>用户号</b>（必填，必须是系统内真实账号）、<b>用户姓名</b>（需与用户号一致，留空则不校验）、<b>任务名</b>（可留空，留空时自动生成「下发给{姓名}的任务」）
        </div>
        <button class="im-download" @click="downloadTemplate"><i class="el-icon-download" /> 下载导入模板</button>
      </div>
    </div>

    <!-- 第 2 步：选择文件 -->
    <div class="im-step">
      <span class="im-no">2</span>
      <div class="im-main">
        <div class="im-text">选择填好的 Excel 文件</div>
        <div class="im-file-row">
          <el-upload
            class="im-upload"
            accept=".xlsx,.xls"
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="onFileChange"
          >
            <button class="im-choose"><i class="el-icon-folder-opened" /> 选择文件</button>
          </el-upload>
          <span v-if="file" class="im-filename" :title="file.name">{{ file.name }}</span>
          <span v-else class="im-nofile">未选择文件</span>
        </div>
        <div class="im-hint">支持 .xlsx / .xls；导入的人员会追加到现有配置里，已在列表中的人自动跳过</div>
      </div>
    </div>

    <!-- 校验失败：一次性列出全部问题行 -->
    <div v-if="errorText" class="im-error">
      <div class="im-error-head"><i class="el-icon-warning-outline" /> 校验未通过，本次未导入任何人员</div>
      <div class="im-error-list">{{ errorText }}</div>
    </div>

    <template #footer>
      <div class="im-footer">
        <button class="btn-cancel" @click="dialogVisible = false">取消</button>
        <button class="btn-ok" :disabled="!file || importing" @click="onSubmit">
          <i v-if="importing" class="el-icon-loading" /> {{ importing ? '校验中…' : '开始导入' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { importMembers, MEMBER_IMPORT_TEMPLATE_URL } from '@/service/sys/FlowDispatchService'

/** 批量导入人员：下载模板 → 上传 Excel → 后端按用户号校验（整批拒绝式，有错则列出问题行） */
export default {
  name: 'ImportMemberModal',
  props: {
    visible: { type: Boolean, default: false }
  },
  data() {
    return {
      file: null,
      importing: false,
      errorText: ''
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.$emit('close') }
    }
  },
  watch: {
    visible(val) {
      if (val) this.reset()
    }
  },
  methods: {
    reset() {
      this.file = null
      this.importing = false
      this.errorText = ''
    },
    downloadTemplate() {
      window.open(MEMBER_IMPORT_TEMPLATE_URL)
    },
    onFileChange(file) {
      this.file = file && file.raw ? file.raw : null
      this.errorText = ''
    },
    async onSubmit() {
      if (!this.file) return
      this.importing = true
      this.errorText = ''
      try {
        const res = await importMembers(this.file)
        if (!res || res.code !== 200) {
          // 校验未通过：后端把全部问题行放在 message 里，一次性展示
          this.errorText = (res && res.message) || '校验未通过'
          return
        }
        const list = res.data || []
        if (list.length === 0) {
          this.errorText = '文件里没有可导入的人员'
          return
        }
        this.$emit('success', list)
        this.$message.success(`校验通过，共读取到 ${list.length} 位人员`)
        this.dialogVisible = false
      } catch (e) {
        this.$notifyError(e, '导入失败')
      } finally {
        this.importing = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.im-step { display: flex; gap: 10px; padding: 12px 0; border-bottom: 1px dashed #E2E8F0; }
.im-no { width: 20px; height: 20px; border-radius: 50%; background: $primary; color: #fff; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 1px; }
.im-main { flex: 1; min-width: 0; }
.im-text { font-size: 13px; color: #414755; font-weight: 600; margin-bottom: 6px; }
.im-hint { font-size: 12px; color: #94A3B8; line-height: 1.6; margin-top: 6px;
  b { color: #64748B; }
}
.im-download { display: inline-flex; align-items: center; gap: 4px; padding: 6px 14px; background: #fff; border: 1px solid $border; border-radius: 3px; color: $primary; font-size: 12px; font-weight: 600; cursor: pointer;
  &:hover { background: var(--color-primary-light); border-color: $primary; }
}
.im-file-row { display: flex; align-items: center; gap: 10px; }
.im-upload { display: inline-block; }
.im-choose { display: inline-flex; align-items: center; gap: 4px; padding: 6px 14px; background: $primary; border: 1px solid $primary; border-radius: 3px; color: #fff; font-size: 12px; font-weight: 600; cursor: pointer;
  &:hover { opacity: 0.9; }
}
.im-filename { font-size: 12px; color: #414755; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.im-nofile { font-size: 12px; color: #bbb; }
.im-error { margin-top: 12px; background: #FEF2F2; border: 1px solid #FECACA; border-radius: 3px; padding: 10px 12px; }
.im-error-head { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #DC2626; margin-bottom: 6px; }
.im-error-list { font-size: 12px; color: #B91C1C; line-height: 1.8; white-space: pre-line; max-height: 180px; overflow-y: auto; }
.im-footer { display: flex; justify-content: flex-end; gap: 10px; }
.btn-cancel { padding: 8px 18px; background: #fff; border: 1px solid $border; border-radius: 3px; color: #64748B; font-size: 13px; cursor: pointer;
  &:hover { color: $primary; border-color: $primary; }
}
.btn-ok { display: inline-flex; align-items: center; gap: 4px; padding: 8px 18px; background: $primary; border: 1px solid $primary; border-radius: 3px; color: #fff; font-size: 13px; font-weight: 600; cursor: pointer;
  &:hover { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
</style>
